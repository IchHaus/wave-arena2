package com.wavearena.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.wavearena.WaveArena;
import com.wavearena.arena.ArenaBuilder;
import com.wavearena.item.ModItems;
import com.wavearena.shop.ShopMenu;

/**
 * Das Herz des Modus: Ablauf der Partie (Pause, Welle, Spielende), Spieler, Belohnungen, Tod und Händler.
 * Es gibt immer höchstens eine Partie pro Server, sie läuft in der Oberwelt rund um den Arena-Mittelpunkt.
 */
public final class GameManager {
	public static final String TAG_ENEMY = "wa_enemy";
	public static final String TAG_SHOP = "wa_shopkeeper";

	public enum Phase {
		IDLE, INTERMISSION, WAVE, ENDED
	}

	private static final int START_MONEY = 30;
	private static final int FIRST_PAUSE_TICKS = 30 * 20;
	private static final int PAUSE_TICKS = 25 * 20;
	private static final int BOSS_PAUSE_TICKS = 40 * 20;
	private static final int ENDED_TICKS = 8 * 20;
	private static final double DEATH_PENALTY = 0.30;
	private static final int COMBO_WINDOW_TICKS = 70;

	private record Delayed(long at, Runnable action) {
	}

	private static final Map<UUID, PlayerData> PLAYERS = new LinkedHashMap<>();
	private static final Set<UUID> LOBBY = new LinkedHashSet<>();
	private static final Map<UUID, EnemyData> ENEMIES = new HashMap<>();
	private static final List<Delayed> DELAYED = new ArrayList<>();
	private static final Deque<EnemyType> QUEUE = new ArrayDeque<>();
	private static final ServerBossEvent BAR = new ServerBossEvent(Component.literal("Wave Arena"),
			BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);

	private static MinecraftServer server;
	private static Phase phase = Phase.IDLE;
	private static BlockPos center;

	private static int wave;
	private static WaveComposer.Plan plan;
	private static BossType pendingBoss;
	private static int waveTotal = 1;
	private static int aliveCount;
	private static long waveStart;
	private static long timer;
	private static long timerMax = 1;
	private static int totalKills;
	private static boolean bossKilledThisWave;
	private static boolean cheated;
	private static boolean scoreSubmitted;
	private static boolean previousGriefing = true;

	private GameManager() {
	}

	// ------------------------------------------------------------------ Einstieg

	public static void init() {
		Persist.load();
		center = Persist.center();

		ServerTickEvents.END_SERVER_TICK.register(GameManager::tick);
		// Statische Felder überleben in einem Einzelspieler-Client den Wechsel zwischen Welten.
		ServerLifecycleEvents.SERVER_STARTING.register(srv -> reset());
		ServerLifecycleEvents.SERVER_STOPPING.register(srv -> {
			if (phase != Phase.IDLE) {
				finish(srv, "Der Server wird beendet.", true);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, srv) -> onDisconnect(srv, handler.player));

		ServerLivingEntityEvents.ALLOW_DAMAGE.register(GameManager::allowDamage);
		ServerLivingEntityEvents.ALLOW_DEATH.register(GameManager::allowDeath);
		ServerLivingEntityEvents.AFTER_DEATH.register(GameManager::afterDeath);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(GameManager::afterDamage);
		ServerEntityEvents.ENTITY_LOAD.register(GameManager::onEntityLoad);

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			if (!entity.getTags().contains(TAG_SHOP) || !(player instanceof ServerPlayer sp)) {
				return InteractionResult.PASS;
			}
			String error = openShop(sp);
			if (error != null) {
				sp.sendSystemMessage(Text.chat(error, ChatFormatting.YELLOW));
			}
			return InteractionResult.SUCCESS;
		});
	}

	private static void reset() {
		PLAYERS.clear();
		LOBBY.clear();
		ENEMIES.clear();
		DELAYED.clear();
		QUEUE.clear();
		pendingBoss = null;
		plan = null;
		phase = Phase.IDLE;
		wave = 0;
		BAR.removeAllPlayers();
	}

	// ------------------------------------------------------------------ Zugriff für Befehle und andere Klassen

	public static Phase phase() {
		return phase;
	}

	public static BlockPos center() {
		return center;
	}

	public static PlayerData data(ServerPlayer player) {
		return PLAYERS.get(player.getUUID());
	}

	/** Spieler, die gerade mitkämpfen können (nicht ausgeschieden). */
	public static List<ServerPlayer> alivePlayers() {
		List<ServerPlayer> list = new ArrayList<>();
		if (server == null) {
			return list;
		}
		for (PlayerData data : PLAYERS.values()) {
			ServerPlayer player = server.getPlayerList().getPlayer(data.id);
			if (player != null && data.alive && player.isAlive()) {
				list.add(player);
			}
		}
		return list;
	}

	private static List<ServerPlayer> participants() {
		List<ServerPlayer> list = new ArrayList<>();
		if (server == null) {
			return list;
		}
		for (PlayerData data : PLAYERS.values()) {
			ServerPlayer player = server.getPlayerList().getPlayer(data.id);
			if (player != null) {
				list.add(player);
			}
		}
		return list;
	}

	private static ServerPlayer playerOf(PlayerData data) {
		return server == null ? null : server.getPlayerList().getPlayer(data.id);
	}

	/** Vom ArenaBuilder aufgerufen, wenn der Bau fertig ist. */
	public static void onArenaBuilt(ServerLevel level, BlockPos newCenter) {
		server = level.getServer();
		if (center != null && !center.equals(newCenter)) {
			removeShopkeeper(level);
		}
		center = newCenter.immutable();
		Persist.saveCenter(center);
		ensureShopkeeper(level);
	}

	/** Setzt den Mittelpunkt für eine bereits gebaute Arena. */
	public static void setCenter(ServerLevel level, BlockPos newCenter) {
		server = level.getServer();
		if (center != null && !center.equals(newCenter)) {
			removeShopkeeper(level);
		}
		center = newCenter.immutable();
		Persist.saveCenter(center);
		ensureShopkeeper(level);
	}

	public static void later(long atTick, Runnable action) {
		DELAYED.add(new Delayed(atTick, action));
	}

	public static void register(Spawner.Spawned spawned) {
		ENEMIES.put(spawned.mob().getUUID(), spawned.data());
		aliveCount++;
	}

	public static int minionCount() {
		int count = 0;
		for (EnemyData data : ENEMIES.values()) {
			if (data.minion) {
				count++;
			}
		}
		return count;
	}

	// ------------------------------------------------------------------ Befehle: Partie steuern

	/** Gibt eine Fehlermeldung zurück oder null bei Erfolg. */
	public static String start(MinecraftServer srv, ServerPlayer starter, int firstWave) {
		server = srv;
		ServerLevel level = srv.overworld();
		if (phase != Phase.IDLE) {
			return "Es läuft schon eine Partie. In der Pause kannst du mit /wave join einsteigen.";
		}
		if (ArenaBuilder.isBuilding()) {
			return "Die Arena wird noch gebaut. Gleich geht es los!";
		}
		if (center == null) {
			return "Es gibt noch keine Arena. Stell dich auf freies, flaches Gelände und tippe /wave build.";
		}
		if (starter.level() != level) {
			return "Der Wellenmodus läuft nur in der Oberwelt.";
		}
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return "Auf Friedlich gibt es keine Monster. Stelle die Schwierigkeit mit /difficulty easy (oder höher) ein.";
		}
		if (level.getBlockState(center).isAir()) {
			return "An der Arena-Position ist nichts gebaut. Tippe /wave build, um sie neu zu bauen.";
		}

		previousGriefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
		level.getGameRules().set(GameRules.MOB_GRIEFING, false, srv);

		clearEnemies(level);
		ENEMIES.clear();
		QUEUE.clear();
		DELAYED.clear();
		pendingBoss = null;
		plan = null;
		Cooldowns.clearAll();

		cheated = firstWave > 1;
		scoreSubmitted = false;
		totalKills = 0;
		wave = firstWave - 1;
		int money = START_MONEY + (firstWave - 1) * 80;

		List<ServerPlayer> joining = new ArrayList<>();
		joining.add(starter);
		for (UUID id : LOBBY) {
			ServerPlayer other = srv.getPlayerList().getPlayer(id);
			if (other != null && other != starter && other.level() == level) {
				joining.add(other);
			}
		}
		LOBBY.clear();
		for (ServerPlayer player : joining) {
			enter(level, player, money);
		}

		phase = Phase.INTERMISSION;
		timer = FIRST_PAUSE_TICKS;
		timerMax = FIRST_PAUSE_TICKS;
		bossKilledThisWave = false;
		ensureShopkeeper(level);

		for (ServerPlayer player : joining) {
			Fx.title(player, Text.bold("WAVE ARENA", ChatFormatting.DARK_RED),
					Text.line("Kaufe Ausrüstung beim Händler, gleich kommt Welle " + (wave + 1), ChatFormatting.GRAY), 10, 70, 20);
			Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDER_DRAGON_GROWL, 0.5F, 1.2F);
		}
		broadcast("Die Partie beginnt! Du hast " + money + " Münzen. Sprich mit dem Händler in der Mitte (Rechtsklick oder /wave shop).",
				ChatFormatting.GOLD);
		broadcast("Mit /wave ready überspringst du die Wartezeit, sobald alle bereit sind.", ChatFormatting.GRAY);
		if (cheated) {
			broadcast("Du hast bei Welle " + firstWave + " gestartet. Dieser Lauf kommt nicht in die Bestenliste.", ChatFormatting.YELLOW);
		}
		WaveArena.LOGGER.info("Partie gestartet mit {} Spieler(n), erste Welle {}.", joining.size(), firstWave);
		return null;
	}

	public static String join(MinecraftServer srv, ServerPlayer player) {
		server = srv;
		ServerLevel level = srv.overworld();
		if (PLAYERS.containsKey(player.getUUID())) {
			return "Du spielst schon mit.";
		}
		if (player.level() != level) {
			return "Der Wellenmodus läuft nur in der Oberwelt.";
		}
		if (phase == Phase.IDLE) {
			LOBBY.add(player.getUUID());
			player.sendSystemMessage(Text.chat("Du bist vorgemerkt. Sobald jemand /wave start tippt, bist du dabei.", ChatFormatting.GREEN));
			return null;
		}
		if (phase != Phase.INTERMISSION) {
			return "Mitten in der Welle geht das nicht. Versuche es in der nächsten Pause.";
		}
		enter(level, player, START_MONEY + 40 * wave);
		player.sendSystemMessage(Text.chat("Willkommen im Kampf! Du bekommst Startgeld passend zur Welle.", ChatFormatting.GREEN));
		broadcast(player.getName().getString() + " ist beigetreten.", ChatFormatting.AQUA);
		return null;
	}

	public static String leave(MinecraftServer srv, ServerPlayer player) {
		server = srv;
		if (LOBBY.remove(player.getUUID())) {
			player.sendSystemMessage(Text.chat("Du bist nicht mehr vorgemerkt.", ChatFormatting.GRAY));
			return null;
		}
		PlayerData data = PLAYERS.remove(player.getUUID());
		if (data == null) {
			return "Du spielst gerade nicht mit.";
		}
		restore(srv.overworld(), player, data);
		player.sendSystemMessage(Text.chat("Du hast die Partie verlassen. Dein Inventar ist wieder da.", ChatFormatting.GRAY));
		afterPlayerLeft(srv, player.getName().getString() + " hat die Partie verlassen.");
		return null;
	}

	public static String stop(MinecraftServer srv) {
		server = srv;
		if (phase == Phase.IDLE) {
			return "Es läuft keine Partie.";
		}
		finish(srv, "Die Partie wurde beendet.", false);
		return null;
	}

	public static String ready(ServerPlayer player) {
		PlayerData data = data(player);
		if (data == null) {
			return "Du spielst gerade nicht mit.";
		}
		if (phase != Phase.INTERMISSION) {
			return "Bereit melden kannst du dich nur in der Pause.";
		}
		data.ready = true;
		int ready = 0;
		int total = 0;
		for (PlayerData other : PLAYERS.values()) {
			if (other.alive) {
				total++;
				if (other.ready) {
					ready++;
				}
			}
		}
		broadcast(player.getName().getString() + " ist bereit (" + ready + "/" + total + ").", ChatFormatting.GREEN);
		return null;
	}

	/** Für Moderatoren: überspringt die Pause oder beendet die laufende Welle sofort. */
	public static String skip(MinecraftServer srv) {
		server = srv;
		ServerLevel level = srv.overworld();
		if (phase == Phase.INTERMISSION) {
			timer = Math.min(timer, 20);
			return null;
		}
		if (phase == Phase.WAVE) {
			QUEUE.clear();
			pendingBoss = null;
			clearEnemies(level);
			ENEMIES.clear();
			return null;
		}
		return "Gerade gibt es nichts zu überspringen.";
	}

	public static String openShop(ServerPlayer player) {
		PlayerData data = data(player);
		if (data == null) {
			return "Du spielst gerade nicht mit. Starte mit /wave start oder tritt mit /wave join bei.";
		}
		if (phase != Phase.INTERMISSION) {
			return "Der Händler ist nur in den Pausen da.";
		}
		SimpleContainer container = new SimpleContainer(54);
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new ShopMenu(id, inventory, container, (ServerPlayer) p),
				Text.bold("Händler", ChatFormatting.GOLD)));
		return null;
	}

	public static void status(ServerPlayer player) {
		player.sendSystemMessage(Text.chat("Bestleistung: " + Persist.bestWave() + " Wellen (" + Persist.bestKills() + " Kills)", ChatFormatting.GOLD));
		if (phase == Phase.IDLE) {
			player.sendSystemMessage(Text.chat("Keine Partie aktiv. Mit /wave start geht es los.", ChatFormatting.GRAY));
			return;
		}
		String where = switch (phase) {
			case INTERMISSION -> "Pause vor Welle " + (wave + 1);
			case WAVE -> "Welle " + wave + " läuft";
			default -> "Spiel vorbei";
		};
		player.sendSystemMessage(Text.chat(where + " | Spieler: " + PLAYERS.size() + " | Kills gesamt: " + totalKills, ChatFormatting.YELLOW));
		PlayerData data = data(player);
		if (data != null) {
			player.sendSystemMessage(Text.chat("Du: " + data.money + " Münzen, " + data.kills + " Kills, insgesamt " + data.earned + " verdient.", ChatFormatting.GREEN));
		}
	}

	// ------------------------------------------------------------------ Spieler aufnehmen und zurückgeben

	private static void enter(ServerLevel level, ServerPlayer player, int money) {
		PlayerData data = new PlayerData(player.getUUID(), player.getName().getString());
		data.money = money;

		Inventory inventory = player.getInventory();
		data.stash = new ItemStack[inventory.getContainerSize()];
		for (int i = 0; i < data.stash.length; i++) {
			data.stash[i] = inventory.getItem(i).copy();
		}
		data.returnX = player.getX();
		data.returnY = player.getY();
		data.returnZ = player.getZ();
		data.returnYaw = player.getYRot();
		data.returnPitch = player.getXRot();
		data.returnMode = player.gameMode.getGameModeForPlayer();

		player.closeContainer();
		inventory.clearContent();
		inventory.setItem(0, unbreakable(new ItemStack(Items.WOODEN_SWORD)));
		player.setItemSlot(EquipmentSlot.HEAD, unbreakable(new ItemStack(Items.LEATHER_HELMET)));
		player.setItemSlot(EquipmentSlot.FEET, unbreakable(new ItemStack(Items.LEATHER_BOOTS)));
		player.removeAllEffects();
		player.clearFire();

		data.applyAttributes(player);
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(17);
		player.setGameMode(GameType.ADVENTURE);

		PLAYERS.put(player.getUUID(), data);
		BAR.addPlayer(player);
		teleportIntoArena(level, player, 4.0);
	}

	private static ItemStack unbreakable(ItemStack stack) {
		stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		return stack;
	}

	private static void restore(ServerLevel level, ServerPlayer player, PlayerData data) {
		player.closeContainer();
		BAR.removePlayer(player);
		data.clearAttributes(player);
		player.removeAllEffects();
		player.clearFire();

		Inventory inventory = player.getInventory();
		inventory.clearContent();
		if (data.stash != null) {
			for (int i = 0; i < data.stash.length && i < inventory.getContainerSize(); i++) {
				inventory.setItem(i, data.stash[i]);
			}
		}
		player.setGameMode(data.returnMode);
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.teleportTo(level, data.returnX, data.returnY, data.returnZ, Set.of(), data.returnYaw, data.returnPitch, false);
	}

	private static void revive(ServerLevel level, ServerPlayer player, PlayerData data) {
		data.alive = true;
		player.setGameMode(GameType.ADVENTURE);
		player.removeAllEffects();
		player.clearFire();
		player.setHealth(player.getMaxHealth());
		teleportIntoArena(level, player, 4.0);
	}

	private static void teleportIntoArena(ServerLevel level, ServerPlayer player, double radius) {
		double angle = level.getRandom().nextDouble() * Math.PI * 2;
		double x = center.getX() + 0.5 + Math.cos(angle) * radius;
		double z = center.getZ() + 0.5 + Math.sin(angle) * radius;
		// Blick zur Mitte (Minecraft: Yaw 0 = Süden, 90 = Westen).
		float yaw = (float) Math.toDegrees(Math.atan2(-(center.getX() + 0.5 - x), center.getZ() + 0.5 - z));
		player.teleportTo(level, x, center.getY() + 1.0, z, Set.of(), yaw, 0.0F, false);
	}

	private static void onDisconnect(MinecraftServer srv, ServerPlayer player) {
		LOBBY.remove(player.getUUID());
		PlayerData data = PLAYERS.remove(player.getUUID());
		if (data == null) {
			return;
		}
		server = srv;
		restore(srv.overworld(), player, data);
		afterPlayerLeft(srv, data.name + " hat die Verbindung verloren.");
	}

	private static void afterPlayerLeft(MinecraftServer srv, String message) {
		if (PLAYERS.isEmpty()) {
			finish(srv, null, false);
			return;
		}
		broadcast(message, ChatFormatting.GRAY);
		if (phase == Phase.WAVE && allDown()) {
			gameOver(srv.overworld());
		}
	}

	// ------------------------------------------------------------------ Tick

	private static void tick(MinecraftServer srv) {
		server = srv;
		ArenaBuilder.tick(srv);
		if (phase == Phase.IDLE) {
			return;
		}
		ServerLevel level = srv.overworld();
		long now = level.getGameTime();
		runDelayed(now);

		switch (phase) {
			case INTERMISSION -> tickIntermission(level, now);
			case WAVE -> tickWave(level, now);
			case ENDED -> {
				if (--timer <= 0) {
					finish(srv, null, false);
				}
			}
			default -> {
			}
		}
		if (phase == Phase.IDLE) {
			return;
		}
		if (now % 10 == 0) {
			updateHud(now);
		}
		if (now % 20 == 0) {
			upkeep(level);
		}
	}

	private static void runDelayed(long now) {
		if (DELAYED.isEmpty()) {
			return;
		}
		List<Delayed> due = new ArrayList<>();
		for (Iterator<Delayed> it = DELAYED.iterator(); it.hasNext();) {
			Delayed delayed = it.next();
			if (delayed.at() <= now) {
				due.add(delayed);
				it.remove();
			}
		}
		for (Delayed delayed : due) {
			try {
				delayed.action().run();
			} catch (RuntimeException e) {
				WaveArena.LOGGER.warn("Fehler in einer verzögerten Aktion: {}", e.toString());
			}
		}
	}

	private static void tickIntermission(ServerLevel level, long now) {
		timer--;
		if (timer <= 0) {
			beginWave(level, now);
			return;
		}
		if (timer <= 60 && timer % 20 == 0) {
			for (ServerPlayer player : participants()) {
				Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.6F);
			}
		}
		if (timer > 60 && allReady()) {
			timer = 60;
		}
	}

	private static boolean allReady() {
		boolean any = false;
		for (PlayerData data : PLAYERS.values()) {
			if (!data.alive) {
				continue;
			}
			any = true;
			if (!data.ready) {
				return false;
			}
		}
		return any;
	}

	private static boolean allDown() {
		for (PlayerData data : PLAYERS.values()) {
			if (data.alive) {
				return false;
			}
		}
		return true;
	}

	// ------------------------------------------------------------------ Wellen

	private static void beginWave(ServerLevel level, long now) {
		wave++;
		plan = WaveComposer.compose(wave, Math.max(1, PLAYERS.size()), level.getRandom());

		List<EnemyType> list = new ArrayList<>(plan.enemies());
		Collections.shuffle(list);
		QUEUE.clear();
		QUEUE.addAll(list);
		pendingBoss = plan.boss();
		waveTotal = Math.max(1, plan.total());
		aliveCount = 0;
		waveStart = now;
		bossKilledThisWave = false;
		phase = Phase.WAVE;

		removeShopkeeper(level);
		boolean bossWave = plan.boss() != null;
		WaveModifier modifier = plan.modifier();

		for (PlayerData data : PLAYERS.values()) {
			data.ready = false;
			data.combo = 0;
			ServerPlayer player = playerOf(data);
			if (player == null) {
				continue;
			}
			player.closeContainer();
			Component title = bossWave
					? Text.bold("BOSS-WELLE " + wave, ChatFormatting.DARK_RED)
					: Text.bold("Welle " + wave, ChatFormatting.RED);
			Component subtitle;
			if (bossWave) {
				subtitle = Text.line(plan.boss().label + " erscheint!", ChatFormatting.GOLD);
			} else if (modifier != WaveModifier.NONE) {
				subtitle = Text.bold(modifier.label, modifier.color);
			} else {
				subtitle = Text.line(plan.total() + " Gegner", ChatFormatting.GRAY);
			}
			Fx.title(player, title, subtitle, 5, 40, 15);
			if (bossWave) {
				Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, 0.8F, 0.9F);
			} else {
				Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.ANVIL_USE, 0.7F, 0.6F);
			}
		}
		if (modifier != WaveModifier.NONE) {
			broadcast(modifier.label + ": " + modifier.description, modifier.color);
		}
		if (bossWave) {
			broadcast("Achtung, Boss: " + plan.boss().label + "! Halte dich bereit.", ChatFormatting.DARK_RED);
		}
	}

	private static void tickWave(ServerLevel level, long now) {
		long age = now - waveStart;
		if (age >= 40 && age % 10 == 5) {
			spawnStep(level, now);
		}
		if (age % 10 != 0) {
			return;
		}

		List<Mob> alive = scan(level);
		aliveCount = alive.size();
		cleanupDrops(level);
		if (age % 20 == 0) {
			manage(level, alive, now);
		}
		bossStep(level, alive, now);
		updateBar(alive);

		if (age > 40 && QUEUE.isEmpty() && pendingBoss == null && alive.isEmpty()) {
			endWave(level);
		}
	}

	private static void spawnStep(ServerLevel level, long now) {
		if (QUEUE.isEmpty() && pendingBoss == null) {
			return;
		}
		List<ServerPlayer> targets = alivePlayers();
		if (targets.isEmpty()) {
			return;
		}

		if (pendingBoss != null) {
			Vec3 pos = Spawner.gatePoint(center, targets, 11.0, level.getRandom());
			Spawner.Spawned boss = Spawner.boss(level, pos, pendingBoss, wave, Math.max(1, PLAYERS.size()), now);
			if (boss != null) {
				register(boss);
				Fx.sound(level, pos.x, pos.y, pos.z, SoundEvents.ENDER_DRAGON_GROWL, 1.5F, 0.7F);
			}
			pendingBoss = null;
			return;
		}

		int max = WaveComposer.maxAlive(wave, Math.max(1, PLAYERS.size()));
		int batch = 1 + wave / 6;
		for (int i = 0; i < batch && !QUEUE.isEmpty() && aliveCount < max; i++) {
			EnemyType type = QUEUE.poll();
			Vec3 pos = Spawner.gatePoint(center, targets, Spawner.GATE_RADIUS, level.getRandom());
			Spawner.Spawned spawned = Spawner.normal(level, pos, type, wave, plan.modifier(), false, now);
			if (spawned != null) {
				register(spawned);
			}
		}
	}

	/** Zielwahl, Rettung festhängender Gegner und Leuchten der letzten Gegner. */
	private static void manage(ServerLevel level, List<Mob> alive, long now) {
		List<ServerPlayer> targets = alivePlayers();
		boolean lastOnes = QUEUE.isEmpty() && pendingBoss == null && alive.size() <= 4;

		for (Mob mob : alive) {
			EnemyData data = ENEMIES.get(mob.getUUID());
			if (data == null) {
				// Überbleibsel einer früheren Partie.
				mob.discard();
				continue;
			}

			if (!targets.isEmpty()) {
				boolean valid = mob.getTarget() instanceof ServerPlayer current && isFighting(current);
				if (!valid || (now / 20) % 4 == 0) {
					ServerPlayer nearest = nearest(mob.position(), targets);
					if (nearest != null) {
						mob.setTarget(nearest);
					}
				}
			}

			boolean outside = flatDistance(mob.position(), center) > ArenaBuilder.R + 1.5
					|| mob.getY() < center.getY() - 2 || mob.getY() > center.getY() + 14;
			if (outside) {
				rescue(level, mob);
				data.nextRescue = now + 1200;
			} else if (now >= data.nextRescue) {
				ServerPlayer nearest = nearest(mob.position(), targets);
				if (nearest != null && nearest.distanceTo(mob) > 14.0F) {
					rescue(level, mob);
				}
				data.nextRescue = now + 600;
			}

			if (lastOnes && !mob.hasEffect(MobEffects.GLOWING)) {
				mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
			}
		}
	}

	private static void rescue(ServerLevel level, Mob mob) {
		double angle = level.getRandom().nextDouble() * Math.PI * 2;
		double radius = 5.0 + level.getRandom().nextDouble() * 4.0;
		mob.setPos(center.getX() + 0.5 + Math.cos(angle) * radius, center.getY() + 1.0, center.getZ() + 0.5 + Math.sin(angle) * radius);
		mob.getNavigation().stop();
		Fx.particles(level, ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + 0.5, mob.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
	}

	private static boolean isFighting(ServerPlayer player) {
		PlayerData data = PLAYERS.get(player.getUUID());
		return data != null && data.alive && player.isAlive();
	}

	private static ServerPlayer nearest(Vec3 pos, List<ServerPlayer> players) {
		ServerPlayer best = null;
		double bestDistance = Double.MAX_VALUE;
		for (ServerPlayer player : players) {
			double distance = player.position().distanceToSqr(pos);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = player;
			}
		}
		return best;
	}

	private static void bossStep(ServerLevel level, List<Mob> alive, long now) {
		List<ServerPlayer> targets = alivePlayers();
		if (targets.isEmpty()) {
			return;
		}
		for (Mob mob : alive) {
			EnemyData data = ENEMIES.get(mob.getUUID());
			if (data == null || data.boss == null) {
				continue;
			}
			double enrage = mob.getHealth() < mob.getMaxHealth() * 0.5F ? 0.7 : 1.0;
			double cycleFactor = Math.max(0.6, 1.0 - 0.1 * (BossType.cycle(wave) - 1));
			for (int i = 0; i < data.boss.abilities.length; i++) {
				if (now < data.nextAbility[i]) {
					continue;
				}
				BossType.Ability ability = data.boss.abilities[i];
				data.nextAbility[i] = now + (long) (ability.intervalTicks * enrage * cycleFactor);
				BossAbilities.cast(level, mob, ability, wave, now, targets);
			}
		}
	}

	private static void updateBar(List<Mob> alive) {
		Mob boss = null;
		for (Mob mob : alive) {
			EnemyData data = ENEMIES.get(mob.getUUID());
			if (data != null && data.boss != null) {
				boss = mob;
				break;
			}
		}
		if (boss != null) {
			EnemyData data = ENEMIES.get(boss.getUUID());
			BAR.setName(Text.bold(data.label, ChatFormatting.DARK_RED));
			BAR.setColor(BossEvent.BossBarColor.PURPLE);
			BAR.setProgress(clamp(boss.getHealth() / boss.getMaxHealth()));
			return;
		}
		int remaining = QUEUE.size() + alive.size() + (pendingBoss != null ? 1 : 0);
		String modifier = plan != null && plan.modifier() != WaveModifier.NONE ? " | " + plan.modifier().label : "";
		BAR.setName(Text.bold("Welle " + wave + " | " + remaining + " Gegner übrig" + modifier, ChatFormatting.RED));
		BAR.setColor(barColor(plan == null ? WaveModifier.NONE : plan.modifier()));
		BAR.setProgress(clamp(remaining / (float) waveTotal));
	}

	private static BossEvent.BossBarColor barColor(WaveModifier modifier) {
		return switch (modifier) {
			case BLOOD_MOON -> BossEvent.BossBarColor.RED;
			case SWARM, GOLD_RUSH -> BossEvent.BossBarColor.YELLOW;
			case ARMORED -> BossEvent.BossBarColor.WHITE;
			case RUSH -> BossEvent.BossBarColor.BLUE;
			default -> BossEvent.BossBarColor.GREEN;
		};
	}

	private static float clamp(float value) {
		if (Float.isNaN(value)) {
			return 0.0F;
		}
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static void endWave(ServerLevel level) {
		phase = Phase.INTERMISSION;
		boolean bossWave = WaveComposer.isBossWave(wave);
		timer = bossWave ? BOSS_PAUSE_TICKS : PAUSE_TICKS;
		timerMax = timer;
		DELAYED.clear();
		cleanupDrops(level);

		int bonus = WaveComposer.clearBonus(wave);
		for (PlayerData data : PLAYERS.values()) {
			ServerPlayer player = playerOf(data);
			if (player == null) {
				continue;
			}
			int gain = data.alive ? bonus : bonus / 2;
			data.money += gain;
			data.earned += gain;
			data.ready = false;

			if (!data.alive) {
				revive(level, player, data);
			} else {
				player.removeAllEffects();
				player.clearFire();
				player.heal((float) (player.getMaxHealth() * 0.6));
			}

			Fx.title(player, Text.bold("Welle " + wave + " geschafft!", ChatFormatting.GREEN),
					Text.line("+" + gain + " Münzen", ChatFormatting.GOLD), 5, 50, 15);
			Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, 0.8F, 0.9F);

			if (bossKilledThisWave) {
				player.getInventory().add(new ItemStack(ModItems.HEAL_CRYSTAL));
				player.sendSystemMessage(Text.chat("Boss-Belohnung: ein Heilkristall!", ChatFormatting.LIGHT_PURPLE));
			}
		}
		ensureShopkeeper(level);
		broadcast("Pause! Der Händler ist da (Rechtsklick oder /wave shop). Mit /wave ready geht es schneller weiter.", ChatFormatting.AQUA);
	}

	// ------------------------------------------------------------------ Spielende

	private static void gameOver(ServerLevel level) {
		phase = Phase.ENDED;
		timer = ENDED_TICKS;
		timerMax = ENDED_TICKS;
		QUEUE.clear();
		pendingBoss = null;
		DELAYED.clear();
		clearEnemies(level);
		ENEMIES.clear();

		int cleared = Math.max(0, wave - 1);
		boolean record = submitScore(cleared);

		for (PlayerData data : PLAYERS.values()) {
			ServerPlayer player = playerOf(data);
			if (player == null) {
				continue;
			}
			Fx.title(player, Text.bold("GAME OVER", ChatFormatting.DARK_RED),
					Text.line("Welle " + wave + " erreicht | " + totalKills + " Kills", ChatFormatting.GRAY), 10, 90, 30);
			Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, 0.6F, 0.5F);
			player.sendSystemMessage(Text.chat("Deine Bilanz: " + data.kills + " Kills, " + data.earned + " Münzen verdient.", ChatFormatting.YELLOW));
		}
		broadcast("Alle sind gefallen. Geschafft: " + cleared + " Wellen.", ChatFormatting.RED);
		if (record) {
			broadcast("Neuer Rekord! " + cleared + " Wellen und " + totalKills + " Kills.", ChatFormatting.GOLD);
		} else if (!cheated) {
			broadcast("Bestleistung: " + Persist.bestWave() + " Wellen.", ChatFormatting.GRAY);
		}
		BAR.setName(Text.bold("GAME OVER", ChatFormatting.DARK_RED));
		BAR.setProgress(0.0F);
	}

	private static boolean submitScore(int clearedWaves) {
		if (cheated || scoreSubmitted || clearedWaves < 1) {
			return false;
		}
		scoreSubmitted = true;
		return Persist.submit(clearedWaves, totalKills);
	}

	/** Beendet die Partie und gibt allen ihre Sachen zurück. */
	private static void finish(MinecraftServer srv, String reason, boolean shutdown) {
		ServerLevel level = srv.overworld();
		if (phase == Phase.WAVE) {
			submitScore(wave - 1);
		} else if (phase == Phase.INTERMISSION) {
			submitScore(wave);
		}

		for (PlayerData data : new ArrayList<>(PLAYERS.values())) {
			ServerPlayer player = srv.getPlayerList().getPlayer(data.id);
			if (player == null) {
				continue;
			}
			if (reason != null) {
				player.sendSystemMessage(Text.chat(reason, ChatFormatting.GRAY));
			}
			restore(level, player, data);
		}
		PLAYERS.clear();
		LOBBY.clear();
		ENEMIES.clear();
		QUEUE.clear();
		DELAYED.clear();
		pendingBoss = null;
		plan = null;
		clearEnemies(level);
		BAR.removeAllPlayers();
		Cooldowns.clearAll();
		level.getGameRules().set(GameRules.MOB_GRIEFING, previousGriefing, srv);
		phase = Phase.IDLE;
		if (!shutdown) {
			ensureShopkeeper(level);
		}
	}

	// ------------------------------------------------------------------ Anzeige

	private static void updateHud(long now) {
		for (PlayerData data : PLAYERS.values()) {
			ServerPlayer player = playerOf(data);
			if (player == null) {
				continue;
			}
			MutableComponent hud;
			if (!data.alive && phase == Phase.WAVE) {
				hud = Text.line("Ausgeschieden. Nach der Welle bist du wieder dabei.", ChatFormatting.RED);
			} else if (phase == Phase.INTERMISSION) {
				hud = Text.bold("Pause", ChatFormatting.AQUA)
						.append(Text.line("  Welle " + (wave + 1) + " in " + (timer / 20 + 1) + " s", ChatFormatting.GRAY))
						.append(Text.line("  |  ", ChatFormatting.DARK_GRAY))
						.append(Text.bold(data.money + " Münzen", ChatFormatting.GOLD));
			} else if (phase == Phase.WAVE) {
				hud = Text.bold("Welle " + wave, ChatFormatting.RED)
						.append(Text.line("  |  ", ChatFormatting.DARK_GRAY))
						.append(Text.bold(data.money + " Münzen", ChatFormatting.GOLD))
						.append(Text.line("  |  ", ChatFormatting.DARK_GRAY))
						.append(Text.line(data.kills + " Kills", ChatFormatting.GRAY));
				if (data.combo >= 2 && now - data.lastKillTick <= COMBO_WINDOW_TICKS) {
					hud.append(Text.line("  |  ", ChatFormatting.DARK_GRAY))
							.append(Text.bold("Serie x" + data.combo, ChatFormatting.YELLOW));
				}
			} else {
				continue;
			}
			Fx.actionBar(player, hud);
		}

		if (phase == Phase.INTERMISSION) {
			BAR.setName(Text.bold("Welle " + (wave + 1) + " beginnt in " + (timer / 20 + 1) + " s", ChatFormatting.GREEN));
			BAR.setColor(BossEvent.BossBarColor.GREEN);
			BAR.setProgress(clamp(timer / (float) timerMax));
		}
	}

	/** Einmal pro Sekunde: Hunger, Attribute, Arena-Grenzen. */
	private static void upkeep(ServerLevel level) {
		for (PlayerData data : PLAYERS.values()) {
			ServerPlayer player = playerOf(data);
			if (player == null) {
				continue;
			}
			// Hunger bei 17: Sprinten geht, aber es gibt keine natürliche Regeneration.
			if (player.getFoodData().getFoodLevel() != 17) {
				player.getFoodData().setFoodLevel(17);
			}
			if (data.attributesDirty) {
				data.applyAttributes(player);
			}

			double distance = flatDistance(player.position(), center);
			if (data.alive) {
				boolean out = distance > ArenaBuilder.R + 1.5
						|| player.getY() < center.getY() - 3 || player.getY() > center.getY() + 8;
				if (out) {
					teleportIntoArena(level, player, 4.0);
					player.sendSystemMessage(Text.chat("Bleib in der Kampfarena!", ChatFormatting.YELLOW));
				}
			} else if (distance > ArenaBuilder.OUTER) {
				player.teleportTo(level, center.getX() + 0.5, center.getY() + 8.0, center.getZ() + 0.5, Set.of(), 0.0F, 30.0F, false);
			}
		}
	}

	private static void broadcast(String text, ChatFormatting color) {
		Component message = Text.chat(text, color);
		for (ServerPlayer player : participants()) {
			player.sendSystemMessage(message);
		}
	}

	// ------------------------------------------------------------------ Ereignisse

	private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
		if (phase == Phase.IDLE) {
			return true;
		}
		if (entity instanceof ServerPlayer victim && PLAYERS.containsKey(victim.getUUID())) {
			if (phase == Phase.ENDED) {
				return false;
			}
			// Kein Spieler-gegen-Spieler-Schaden.
			return !(source.getEntity() instanceof ServerPlayer attacker && attacker != victim);
		}
		Entity attacker = source.getEntity();
		if (attacker != null && attacker != entity && entity.getTags().contains(TAG_ENEMY) && attacker.getTags().contains(TAG_ENEMY)) {
			// Gegner verletzen sich nicht gegenseitig (Pfeile, Creeper-Explosionen).
			return false;
		}
		return true;
	}

	private static boolean allowDeath(LivingEntity entity, DamageSource source, float amount) {
		if (!(entity instanceof ServerPlayer player)) {
			return true;
		}
		PlayerData data = PLAYERS.get(player.getUUID());
		if (data == null) {
			return true;
		}
		if (phase != Phase.WAVE || !data.alive) {
			// In der Pause oder nach dem Ende stirbt niemand.
			player.setHealth(player.getMaxHealth());
			return false;
		}
		knockOut(player, data, source);
		return false;
	}

	private static void knockOut(ServerPlayer player, PlayerData data, DamageSource source) {
		ServerLevel level = (ServerLevel) player.level();
		int lost = (int) Math.round(data.money * DEATH_PENALTY);
		data.money -= lost;
		data.alive = false;
		data.combo = 0;

		player.setHealth(player.getMaxHealth());
		player.clearFire();
		player.removeAllEffects();
		player.closeContainer();
		player.setGameMode(GameType.SPECTATOR);

		Fx.particles(level, ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
		Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.WITHER_SPAWN, 0.4F, 1.8F);
		Fx.title(player, Text.bold("BESIEGT", ChatFormatting.DARK_RED),
				Text.line("-" + lost + " Münzen | nach der Welle bist du zurück", ChatFormatting.GRAY), 5, 60, 20);

		Component death = source.getLocalizedDeathMessage(player);
		Component message = Text.chat("", ChatFormatting.RED).append(death);
		for (ServerPlayer other : participants()) {
			other.sendSystemMessage(message);
		}
		if (allDown()) {
			gameOver(level);
		} else {
			broadcast("Haltet durch! Wer fällt, kommt nach der Welle zurück.", ChatFormatting.GRAY);
		}
	}

	private static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (phase != Phase.WAVE || blocked || damageTaken <= 0.0F) {
			return;
		}

		// Wer hat den Gegner zuletzt getroffen? Wichtig für das Kopfgeld bei Feuer oder Fernkampf.
		if (source.getEntity() instanceof ServerPlayer hitter && victim.getTags().contains(TAG_ENEMY)) {
			EnemyData data = ENEMIES.get(victim.getUUID());
			if (data != null) {
				data.lastPlayer = hitter.getUUID();
			}
		}

		// Eigenschaften von Elite-Gegnern.
		if (victim instanceof ServerPlayer player && PLAYERS.containsKey(player.getUUID())
				&& source.getEntity() instanceof Mob attacker) {
			EnemyData data = ENEMIES.get(attacker.getUUID());
			if (data == null || data.affix == null) {
				return;
			}
			switch (data.affix) {
				case FIERY -> player.igniteForSeconds(4.0F);
				case VAMPIRIC -> {
					attacker.heal(damageTaken * 0.6F);
					Fx.particles((ServerLevel) attacker.level(), ParticleTypes.HEART, attacker.getX(), attacker.getY() + attacker.getBbHeight(),
							attacker.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
				}
				default -> {
				}
			}
		}
	}

	private static void afterDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof Mob mob)) {
			return;
		}
		EnemyData data = ENEMIES.remove(mob.getUUID());
		if (data == null) {
			return;
		}
		ServerLevel level = (ServerLevel) mob.level();

		PlayerData killer = null;
		if (source.getEntity() instanceof ServerPlayer player) {
			killer = PLAYERS.get(player.getUUID());
		}
		if (killer == null && data.lastPlayer != null) {
			killer = PLAYERS.get(data.lastPlayer);
		}
		if (killer != null) {
			reward(level, killer, data, mob);
		}

		if (data.affix == Affix.EXPLOSIVE) {
			Fx.particles(level, ParticleTypes.EXPLOSION, mob.getX(), mob.getY() + 0.5, mob.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
			level.explode(null, null, null, mob.position(), 1.6F, false, Level.ExplosionInteraction.NONE);
		}
		if (data.boss != null) {
			bossKilledThisWave = true;
			broadcast(data.label + " wurde besiegt!", ChatFormatting.GOLD);
			Fx.sound(level, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.TOTEM_USE, 1.0F, 1.0F);
		}
	}

	private static void reward(ServerLevel level, PlayerData killer, EnemyData data, Mob mob) {
		long now = level.getGameTime();
		killer.combo = now - killer.lastKillTick <= COMBO_WINDOW_TICKS ? killer.combo + 1 : 1;
		killer.lastKillTick = now;
		killer.kills++;
		totalKills++;

		double factor = WaveComposer.bountyScale(wave);
		if (plan != null) {
			factor *= plan.modifier().bountyFactor;
		}
		factor *= 1.0 + 0.10 * killer.level(Upgrade.GREED);
		factor *= 1.0 + 0.03 * Math.min(killer.combo - 1, 10);
		if (data.isElite()) {
			factor *= 3.0;
		}
		int coins = Math.max(1, (int) Math.round(data.bounty * factor));
		killer.money += coins;
		killer.earned += coins;

		ServerPlayer player = playerOf(killer);
		if (player != null) {
			float pitch = 0.8F + Math.min(killer.combo, 12) * 0.05F;
			Fx.sound(level, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, 0.3F, pitch);
			if (data.isElite() || data.boss != null) {
				player.sendSystemMessage(Text.chat("+" + coins + " Münzen für " + data.label, ChatFormatting.GOLD));
			}
		}
	}

	private static void onEntityLoad(Entity entity, ServerLevel world) {
		if (phase != Phase.WAVE || !(entity instanceof AbstractArrow arrow)) {
			return;
		}
		// Pfeile von Gegnern werden mit der Welle stärker.
		if (arrow.getOwner() instanceof Mob owner && ENEMIES.containsKey(owner.getUUID())) {
			double factor = WaveComposer.damageScale(wave);
			if (plan != null) {
				factor *= plan.modifier().damageFactor;
			}
			arrow.setBaseDamage(arrow.getBaseDamage() * factor);
		}
	}

	// ------------------------------------------------------------------ Arena-Hilfen

	private static AABB arenaBox() {
		double r = ArenaBuilder.OUTER + 1.0;
		return new AABB(center.getX() + 0.5 - r, center.getY() - 3, center.getZ() + 0.5 - r,
				center.getX() + 0.5 + r, center.getY() + ArenaBuilder.TOP + 3, center.getZ() + 0.5 + r);
	}

	private static List<Mob> scan(ServerLevel level) {
		return level.getEntitiesOfClass(Mob.class, arenaBox(), mob -> mob.isAlive() && mob.getTags().contains(TAG_ENEMY));
	}

	private static void clearEnemies(ServerLevel level) {
		if (center == null) {
			return;
		}
		for (Mob mob : level.getEntitiesOfClass(Mob.class, arenaBox(), m -> m.getTags().contains(TAG_ENEMY))) {
			mob.discard();
		}
	}

	/** Verhindert, dass Beute und Erfahrung die Wirtschaft aushebeln. */
	private static void cleanupDrops(ServerLevel level) {
		AABB box = arenaBox();
		for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
			item.discard();
		}
		for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
			orb.discard();
		}
	}

	private static List<Mob> shopkeepers(ServerLevel level) {
		return level.getEntitiesOfClass(Mob.class, arenaBox(), mob -> mob.getTags().contains(TAG_SHOP));
	}

	private static void ensureShopkeeper(ServerLevel level) {
		if (center == null) {
			return;
		}
		double x = center.getX() + 0.5;
		double y = center.getY() + 1.0;
		double z = center.getZ() + 0.5;

		List<Mob> found = shopkeepers(level);
		for (int i = 1; i < found.size(); i++) {
			found.get(i).discard();
		}
		if (!found.isEmpty()) {
			// Spieler können den Händler anschubsen, deshalb wird er zurückgestellt.
			found.get(0).setPos(x, y, z);
			return;
		}

		Entity entity = EntityType.WANDERING_TRADER.create(level, EntitySpawnReason.EVENT);
		if (!(entity instanceof Mob trader)) {
			if (entity != null) {
				entity.discard();
			}
			return;
		}
		trader.setPos(x, y, z);
		trader.setNoAi(true);
		trader.setInvulnerable(true);
		trader.setPersistenceRequired();
		trader.setSilent(true);
		trader.setCustomName(Text.bold("Händler", ChatFormatting.GOLD));
		trader.setCustomNameVisible(true);
		trader.addTag(TAG_SHOP);
		level.addFreshEntity(trader);
	}

	private static void removeShopkeeper(ServerLevel level) {
		if (center == null) {
			return;
		}
		for (Mob trader : shopkeepers(level)) {
			Fx.particles(level, ParticleTypes.CLOUD, trader.getX(), trader.getY() + 1.0, trader.getZ(), 15, 0.3, 0.6, 0.3, 0.02);
			trader.discard();
		}
	}

	private static double flatDistance(Vec3 a, BlockPos b) {
		double dx = a.x - (b.getX() + 0.5);
		double dz = a.z - (b.getZ() + 0.5);
		return Math.sqrt(dx * dx + dz * dz);
	}
}

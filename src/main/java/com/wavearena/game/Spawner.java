package com.wavearena.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.ChatFormatting;

import com.wavearena.arena.ArenaBuilder;

/** Erzeugt Gegner: Spawn-Orte an den Toren, Werte nach Welle, Elite-Eigenschaften und Bosse. */
public final class Spawner {
	/** Abstand der Tore vom Mittelpunkt (auf der Kampffläche vor der Mauer). */
	public static final double GATE_RADIUS = ArenaBuilder.R - 2.5;

	public record Spawned(Mob mob, EnemyData data) {
	}

	private Spawner() {
	}

	// ------------------------------------------------------------------ Orte

	/**
	 * Wählt eines der acht Tore, bevorzugt eines, das weit von den Spielern entfernt ist.
	 * Die Tore liegen in der Mitte der Sektoren (22,5° + k * 45°).
	 */
	public static Vec3 gatePoint(BlockPos center, List<ServerPlayer> players, double radius, RandomSource random) {
		List<Vec3> gates = new ArrayList<>(8);
		for (int k = 0; k < 8; k++) {
			double angle = Math.PI / 8 + k * (Math.PI / 4);
			gates.add(new Vec3(center.getX() + 0.5 + Math.cos(angle) * radius, center.getY() + 1.0,
					center.getZ() + 0.5 + Math.sin(angle) * radius));
		}
		if (!players.isEmpty()) {
			gates.sort(Comparator.comparingDouble((Vec3 gate) -> -nearest(gate, players)));
		}
		int pool = Math.min(3, gates.size());
		Vec3 chosen = gates.get(random.nextInt(pool));
		// Etwas Streuung, damit nicht alle auf einem Block stehen.
		return chosen.add((random.nextDouble() - 0.5) * 2.4, 0.0, (random.nextDouble() - 0.5) * 2.4);
	}

	private static double nearest(Vec3 point, List<ServerPlayer> players) {
		double best = Double.MAX_VALUE;
		for (ServerPlayer player : players) {
			best = Math.min(best, player.position().distanceTo(point));
		}
		return best;
	}

	// ------------------------------------------------------------------ Gegner

	/** Normaler Gegner, ggf. als Elite. Gibt null zurück, wenn die Erzeugung fehlschlug. */
	public static Spawned normal(ServerLevel level, Vec3 pos, EnemyType type, int wave, WaveModifier modifier, boolean minion, long now) {
		RandomSource random = level.getRandom();
		Mob mob = create(level, type.entity, pos);
		if (mob == null) {
			return null;
		}
		type.equip.accept(mob);

		Affix affix = null;
		if (!minion && type != EnemyType.CAVE_SPIDER && wave >= 4) {
			double chance = Math.min(0.03 + 0.012 * wave, 0.22);
			if (random.nextDouble() < chance) {
				Affix[] all = Affix.values();
				affix = all[random.nextInt(all.length)];
			}
		}

		double hp = baseOf(mob, Attributes.MAX_HEALTH, 20.0) * type.hpFactor
				* WaveComposer.healthScale(wave) * modifier.hpFactor;
		double speed = baseOf(mob, Attributes.MOVEMENT_SPEED, 0.23) * type.speedFactor * modifier.speedFactor
				* (1.0 + Math.min(wave, 25) * 0.004);
		double damage = baseOf(mob, Attributes.ATTACK_DAMAGE, 2.0) * type.damageFactor
				* WaveComposer.damageScale(wave) * modifier.damageFactor;

		if (affix != null) {
			hp *= 1.8;
			if (affix == Affix.SWIFT) {
				speed *= 1.45;
			}
			if (affix == Affix.ARMORED) {
				setBase(mob, Attributes.ARMOR, baseOf(mob, Attributes.ARMOR, 0.0) + 12.0);
			}
		}

		setBase(mob, Attributes.MAX_HEALTH, hp);
		mob.setHealth((float) hp);
		setBase(mob, Attributes.MOVEMENT_SPEED, speed);
		if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
			setBase(mob, Attributes.ATTACK_DAMAGE, damage);
		}
		setBase(mob, Attributes.FOLLOW_RANGE, 64.0);

		int bounty = minion ? Math.max(1, type.bounty / 4) : type.bounty;
		String label = type.label;
		if (minion) {
			label = "Diener";
		}

		if (affix != null) {
			mob.setCustomName(Text.bold("★ " + type.label + " (" + affix.label + ")", ChatFormatting.GOLD));
			mob.setCustomNameVisible(true);
			mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, MobEffectInstance.INFINITE_DURATION, 0, false, false));
			label = type.label + " (" + affix.label + ")";
		} else {
			mob.setCustomName(Text.line(label, ChatFormatting.WHITE));
		}

		return finish(level, mob, new EnemyData(label, bounty, null, affix, minion, now));
	}

	/** Eine Art Diener ohne eigenen EnemyType, z. B. Rachegeister der Hexenkönigin. */
	public static Spawned custom(ServerLevel level, Vec3 pos, EntityType<?> entity, String label, int wave, int bounty, long now) {
		Mob mob = create(level, entity, pos);
		if (mob == null) {
			return null;
		}
		double hp = baseOf(mob, Attributes.MAX_HEALTH, 14.0) * WaveComposer.healthScale(wave);
		setBase(mob, Attributes.MAX_HEALTH, hp);
		mob.setHealth((float) hp);
		if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
			setBase(mob, Attributes.ATTACK_DAMAGE, baseOf(mob, Attributes.ATTACK_DAMAGE, 3.0) * WaveComposer.damageScale(wave));
		}
		setBase(mob, Attributes.FOLLOW_RANGE, 64.0);
		mob.setCustomName(Text.line(label, ChatFormatting.WHITE));
		return finish(level, mob, new EnemyData(label, bounty, null, null, true, now));
	}

	/** Boss mit eigener Größe, Leben und Fähigkeiten. */
	public static Spawned boss(ServerLevel level, Vec3 pos, BossType type, int wave, int players, long now) {
		Mob mob = create(level, type.entity, pos);
		if (mob == null) {
			return null;
		}
		type.equip.accept(mob);

		int cycle = BossType.cycle(wave);
		double hp = type.baseHealth * WaveComposer.playerFactor(players) * (1.0 + 0.5 * (cycle - 1));
		setBase(mob, Attributes.MAX_HEALTH, hp);
		mob.setHealth((float) hp);

		if (type.baseDamage > 0.0 && mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
			setBase(mob, Attributes.ATTACK_DAMAGE, type.baseDamage * (1.0 + 0.03 * wave));
		}
		setBase(mob, Attributes.MOVEMENT_SPEED, baseOf(mob, Attributes.MOVEMENT_SPEED, 0.23) * type.speedFactor);
		setBase(mob, Attributes.SCALE, type.scale);
		setBase(mob, Attributes.FOLLOW_RANGE, 64.0);
		setBase(mob, Attributes.KNOCKBACK_RESISTANCE, Math.max(0.7, baseOf(mob, Attributes.KNOCKBACK_RESISTANCE, 0.0)));

		Component name = Text.bold(type.label, ChatFormatting.DARK_RED);
		mob.setCustomName(name);
		mob.setCustomNameVisible(true);

		int bounty = 60 + 14 * wave;
		return finish(level, mob, new EnemyData(type.label, bounty, type, null, false, now));
	}

	// ------------------------------------------------------------------ intern

	private static Mob create(ServerLevel level, EntityType<?> type, Vec3 pos) {
		Entity entity = type.create(level, EntitySpawnReason.EVENT);
		if (!(entity instanceof Mob mob)) {
			if (entity != null) {
				entity.discard();
			}
			return null;
		}
		mob.setPos(pos.x, pos.y, pos.z);
		mob.setYRot(level.getRandom().nextFloat() * 360.0F);
		return mob;
	}

	private static Spawned finish(ServerLevel level, Mob mob, EnemyData data) {
		mob.addTag(GameManager.TAG_ENEMY);
		mob.setPersistenceRequired();
		mob.setCanPickUpLoot(false);
		level.addFreshEntity(mob);
		Fx.particles(level, ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + 0.5, mob.getZ(), 12, 0.4, 0.6, 0.4, 0.03);
		return new Spawned(mob, data);
	}

	private static double baseOf(Mob mob, Holder<Attribute> attribute, double fallback) {
		AttributeInstance instance = mob.getAttribute(attribute);
		return instance == null ? fallback : instance.getBaseValue();
	}

	private static void setBase(Mob mob, Holder<Attribute> attribute, double value) {
		AttributeInstance instance = mob.getAttribute(attribute);
		if (instance != null) {
			instance.setBaseValue(value);
		}
	}
}

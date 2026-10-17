package com.wavearena.shop;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.wavearena.game.PlayerData;
import com.wavearena.game.Upgrade;
import com.wavearena.item.ModItems;

/**
 * Alles, was der Händler verkauft. Die Preise sind so gewählt, dass man in den ersten Wellen
 * nur wenig bekommt und sich Netherit und die besten Upgrades erst nach vielen Wellen leisten kann.
 */
public final class ShopCatalog {
	public enum Category {
		WEAPONS("Nahkampf", Items.IRON_SWORD),
		RANGED("Fernkampf", Items.BOW),
		ARMOR("Rüstung", Items.IRON_CHESTPLATE),
		SUPPLIES("Vorräte", Items.GOLDEN_APPLE),
		UPGRADES("Upgrades", Items.EXPERIENCE_BOTTLE);

		public final String label;
		public final Item icon;

		Category(String label, Item icon) {
			this.label = label;
			this.icon = icon;
		}
	}

	private static final Map<Category, List<ShopEntry>> ENTRIES = new EnumMap<>(Category.class);

	static {
		// Nahkampf
		weapon("Steinschwert", Items.STONE_SWORD, 35, "Solide Waffe für den Anfang.");
		weapon("Kupferschwert", Items.COPPER_SWORD, 60, "Etwas besser als Stein.");
		weapon("Eisenschwert", Items.IRON_SWORD, 110, "Zuverlässiger Allrounder.");
		weapon("Eisenaxt", Items.IRON_AXE, 150, "Hoher Schaden, aber langsamer Schlag.");
		weapon("Diamantschwert", Items.DIAMOND_SWORD, 360, "Schnell und stark.");
		weapon("Netherit-Schwert", Items.NETHERITE_SWORD, 1100, "Die beste normale Klinge.");
		weapon("Flammenklinge", ModItems.FLAME_BLADE, 260, "Setzt Gegner in Brand.", "Treffer zünden 5 Sekunden lang.");
		weapon("Frostbrecher", ModItems.FROST_BREAKER, 340, "Verlangsamt und schwächt Gegner.");
		weapon("Seelenernter", ModItems.SOUL_REAPER, 460, "Heilt dich bei jedem Treffer.");
		weapon("Donnerhammer", ModItems.THUNDER_HAMMER, 640, "Rechtsklick löst ein Erdbeben aus.", "Sehr hoher Schaden, langsamer Schlag.");

		// Fernkampf
		add(Category.RANGED, new ItemEntry("Bogen", Items.BOW, 1, 70, true, true, false, "Fernkampf mit Pfeilen."));
		add(Category.RANGED, new ItemEntry("Armbrust", Items.CROSSBOW, 1, 160, true, true, false, "Mehr Schaden pro Schuss."));
		add(Category.RANGED, new ItemEntry("Pfeile x16", Items.ARROW, 16, 20, false, false, false));
		add(Category.RANGED, new ItemEntry("Pfeile x64", Items.ARROW, 64, 65, false, false, false, "Etwas günstiger im Großpack."));
		add(Category.RANGED, new ItemEntry("Sturmstab", ModItems.STORM_STAFF, 1, 520, true, false, false,
				"Rechtsklick: Kettenblitz auf bis zu 4 Gegner.", "Braucht keine Munition."));

		// Rüstung
		armor("Kupferhelm", Items.COPPER_HELMET, EquipmentSlot.HEAD, 30);
		armor("Kupferbrustpanzer", Items.COPPER_CHESTPLATE, EquipmentSlot.CHEST, 55);
		armor("Kupferhose", Items.COPPER_LEGGINGS, EquipmentSlot.LEGS, 45);
		armor("Kupferstiefel", Items.COPPER_BOOTS, EquipmentSlot.FEET, 25);
		armor("Eisenhelm", Items.IRON_HELMET, EquipmentSlot.HEAD, 55);
		armor("Eisenbrustpanzer", Items.IRON_CHESTPLATE, EquipmentSlot.CHEST, 100);
		armor("Eisenhose", Items.IRON_LEGGINGS, EquipmentSlot.LEGS, 80);
		armor("Eisenstiefel", Items.IRON_BOOTS, EquipmentSlot.FEET, 45);
		armor("Diamanthelm", Items.DIAMOND_HELMET, EquipmentSlot.HEAD, 190);
		armor("Diamantbrustpanzer", Items.DIAMOND_CHESTPLATE, EquipmentSlot.CHEST, 330);
		armor("Diamanthose", Items.DIAMOND_LEGGINGS, EquipmentSlot.LEGS, 280);
		armor("Diamantstiefel", Items.DIAMOND_BOOTS, EquipmentSlot.FEET, 160);
		armor("Netherit-Helm", Items.NETHERITE_HELMET, EquipmentSlot.HEAD, 420);
		armor("Netherit-Brustpanzer", Items.NETHERITE_CHESTPLATE, EquipmentSlot.CHEST, 700);
		armor("Netherit-Hose", Items.NETHERITE_LEGGINGS, EquipmentSlot.LEGS, 600);
		armor("Netherit-Stiefel", Items.NETHERITE_BOOTS, EquipmentSlot.FEET, 360);
		add(Category.ARMOR, new ItemEntry("Schild", Items.SHIELD, 1, 60, true, true, true, "Blockt Nahkampf und Pfeile."));
		add(Category.ARMOR, new ItemEntry("Totem der Unsterblichkeit", Items.TOTEM_OF_UNDYING, 1, 700, true, false, false,
				"Rettet dich einmal vor dem Tod.", "Muss in einer Hand oder im Inventar liegen."));

		// Vorräte
		add(Category.SUPPLIES, new ItemEntry("Heilkristall", ModItems.HEAL_CRYSTAL, 1, 40, false, false, false,
				"Rechtsklick: heilt 4 Herzen."));
		add(Category.SUPPLIES, new ItemEntry("Heilkristall x3", ModItems.HEAL_CRYSTAL, 3, 110, false, false, false,
				"Etwas günstiger im Dreierpack."));
		add(Category.SUPPLIES, new ItemEntry("Heilkristall x8", ModItems.HEAL_CRYSTAL, 8, 280, false, false, false,
				"Für lange Kämpfe."));
		add(Category.SUPPLIES, new ItemEntry("Adrenalin-Spritze", ModItems.ADRENALINE, 1, 70, false, false, false,
				"20 s lang schneller und stärker."));
		add(Category.SUPPLIES, new ItemEntry("Adrenalin x3", ModItems.ADRENALINE, 3, 195, false, false, false));
		add(Category.SUPPLIES, new ItemEntry("Goldener Apfel", Items.GOLDEN_APPLE, 1, 110, false, false, false,
				"Regeneration und goldene Herzen."));
		add(Category.SUPPLIES, new ItemEntry("Verzauberter Goldapfel", Items.ENCHANTED_GOLDEN_APPLE, 1, 700, false, false, false,
				"Starke Regeneration, Resistenz und viele goldene Herzen."));

		// Upgrades
		add(Category.UPGRADES, new UpgradeEntry(Upgrade.LIFE, Items.GOLDEN_CARROT));
		add(Category.UPGRADES, new UpgradeEntry(Upgrade.POWER, Items.BLAZE_POWDER));
		add(Category.UPGRADES, new UpgradeEntry(Upgrade.SPEED, Items.SUGAR));
		add(Category.UPGRADES, new UpgradeEntry(Upgrade.GREED, Items.GOLD_NUGGET));
		add(Category.UPGRADES, new UpgradeEntry(Upgrade.SHARPNESS, Items.FLINT));
		add(Category.UPGRADES, new UpgradeEntry(Upgrade.PROTECTION, Items.IRON_INGOT));
		add(Category.UPGRADES, new UpgradeEntry(Upgrade.DRAW, Items.ARROW));
	}

	private ShopCatalog() {
	}

	public static List<ShopEntry> entries(Category category) {
		return ENTRIES.getOrDefault(category, List.of());
	}

	private static void add(Category category, ShopEntry entry) {
		ENTRIES.computeIfAbsent(category, key -> new ArrayList<>()).add(entry);
	}

	private static void weapon(String name, Item item, int price, String... description) {
		add(Category.WEAPONS, new ItemEntry(name, item, 1, price, true, true, false, description));
	}

	private static void armor(String name, Item item, EquipmentSlot slot, int price) {
		add(Category.ARMOR, new ArmorEntry(name, item, slot, price));
	}

	private static boolean owns(ServerPlayer player, Item item) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(item)) {
				return true;
			}
		}
		return false;
	}

	/** Gegenstand, der ins Inventar gelegt wird. */
	private static final class ItemEntry extends ShopEntry {
		private final Item item;
		private final int amount;
		private final int price;
		private final boolean unique;
		private final boolean unbreakable;
		private final boolean offhand;

		ItemEntry(String name, Item item, int amount, int price, boolean unique, boolean unbreakable, boolean offhand,
				String... description) {
			super(name, item, amount, description);
			this.item = item;
			this.amount = amount;
			this.price = price;
			this.unique = unique;
			this.unbreakable = unbreakable;
			this.offhand = offhand;
		}

		@Override
		public int price(PlayerData data) {
			return price;
		}

		@Override
		public String blockReason(ServerPlayer player, PlayerData data) {
			if (unique && owns(player, item)) {
				return "Das besitzt du bereits.";
			}
			boolean offhandFree = offhand && player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty();
			if (!offhandFree && player.getInventory().getFreeSlot() < 0) {
				return "Dein Inventar ist voll.";
			}
			return null;
		}

		@Override
		public void apply(ServerPlayer player, PlayerData data) {
			ItemStack stack = new ItemStack(item, amount);
			if (unbreakable) {
				stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
			}
			if (offhand && player.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) {
				player.setItemSlot(EquipmentSlot.OFFHAND, stack);
			} else {
				player.getInventory().add(stack);
			}
			Enchants.sync(player, data);
		}

		@Override
		protected String status(ServerPlayer player, PlayerData data) {
			return unique && owns(player, item) ? "Im Besitz" : null;
		}
	}

	/** Rüstungsteil, das direkt angelegt wird und das aktuelle ersetzt. */
	private static final class ArmorEntry extends ShopEntry {
		private final Item item;
		private final EquipmentSlot slot;
		private final int price;

		ArmorEntry(String name, Item item, EquipmentSlot slot, int price) {
			super(name, item, 1, "Wird sofort angelegt und ersetzt", "dein aktuelles Teil an dieser Stelle.");
			this.item = item;
			this.slot = slot;
			this.price = price;
		}

		@Override
		public int price(PlayerData data) {
			return price;
		}

		@Override
		public String blockReason(ServerPlayer player, PlayerData data) {
			if (player.getItemBySlot(slot).is(item)) {
				return "Das trägst du schon.";
			}
			return null;
		}

		@Override
		public void apply(ServerPlayer player, PlayerData data) {
			ItemStack stack = new ItemStack(item);
			stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
			player.setItemSlot(slot, stack);
			Enchants.sync(player, data);
		}

		@Override
		protected String status(ServerPlayer player, PlayerData data) {
			return player.getItemBySlot(slot).is(item) ? "Angelegt" : null;
		}
	}

	/** Dauerhafte Verbesserung mit mehreren Stufen. */
	private static final class UpgradeEntry extends ShopEntry {
		private final Upgrade upgrade;

		UpgradeEntry(Upgrade upgrade, Item icon) {
			super(upgrade.label, icon, 1, upgrade.description);
			this.upgrade = upgrade;
		}

		@Override
		public int price(PlayerData data) {
			return upgrade.priceFor(data.level(upgrade));
		}

		@Override
		public String blockReason(ServerPlayer player, PlayerData data) {
			return price(data) < 0 ? "Bereits maximal ausgebaut." : null;
		}

		@Override
		public void apply(ServerPlayer player, PlayerData data) {
			data.levels[upgrade.ordinal()]++;
			data.attributesDirty = true;
			data.applyAttributes(player);
			if (upgrade == Upgrade.LIFE) {
				player.heal(2.0F);
			}
			Enchants.sync(player, data);
		}

		@Override
		protected String status(ServerPlayer player, PlayerData data) {
			return "Stufe " + data.level(upgrade) + " / " + upgrade.maxLevel();
		}
	}
}

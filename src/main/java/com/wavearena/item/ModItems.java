package com.wavearena.item;

import java.util.function.Function;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;

import com.wavearena.WaveArena;

/** Eigene Waffen und Verbrauchsgegenstände. Die Fähigkeiten stecken in den Item-Klassen und in {@link WeaponEvents}. */
public final class ModItems {
	// Leerer Tag, nur damit das Material eine Reparatur-Zutat angeben kann. Die Waffen sind ohnehin unzerbrechlich.
	private static final TagKey<Item> REPAIRS = TagKey.create(Registries.ITEM, WaveArena.id("repairs_arena_weapons"));

	public static final Item FLAME_BLADE = register("flame_blade",
			p -> new TooltipItem(p,
					"Treffer setzen Gegner 5 Sekunden lang in Brand."),
			new Item.Properties()
					.sword(material(1500), 6.0F, -2.4F)
					.rarity(Rarity.RARE)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));

	public static final Item FROST_BREAKER = register("frost_breaker",
			p -> new TooltipItem(p,
					"Treffer verlangsamen Gegner 3 Sekunden lang",
					"und schwächen ihren Schaden."),
			new Item.Properties()
					.sword(material(1500), 6.5F, -2.4F)
					.rarity(Rarity.RARE)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));

	public static final Item SOUL_REAPER = register("soul_reaper",
			p -> new TooltipItem(p,
					"Heilt dich bei jedem Treffer um 20 % des Schadens.",
					"Perfekt für lange Kämpfe."),
			new Item.Properties()
					.sword(material(1800), 7.0F, -2.6F)
					.rarity(Rarity.EPIC)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));

	public static final Item THUNDER_HAMMER = register("thunder_hammer",
			ThunderHammerItem::new,
			new Item.Properties()
					.sword(material(2000), 9.0F, -3.1F)
					.rarity(Rarity.EPIC)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));

	public static final Item STORM_STAFF = register("storm_staff",
			StormStaffItem::new,
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
					.component(DataComponents.UNBREAKABLE, Unit.INSTANCE));

	public static final Item HEAL_CRYSTAL = register("heal_crystal",
			HealCrystalItem::new,
			new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

	public static final Item ADRENALINE = register("adrenaline",
			AdrenalineItem::new,
			new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON));

	private static final ResourceKey<CreativeModeTab> TAB_KEY =
			ResourceKey.create(BuiltInRegistries.CREATIVE_MODE_TAB.key(), WaveArena.id("main"));

	private static final CreativeModeTab TAB = FabricItemGroup.builder()
			.icon(() -> new ItemStack(FLAME_BLADE))
			.title(Component.translatable("itemGroup.wavearena"))
			.displayItems((params, output) -> {
				output.accept(FLAME_BLADE);
				output.accept(FROST_BREAKER);
				output.accept(SOUL_REAPER);
				output.accept(THUNDER_HAMMER);
				output.accept(STORM_STAFF);
				output.accept(HEAL_CRYSTAL);
				output.accept(ADRENALINE);
			})
			.build();

	private ModItems() {
	}

	private static ToolMaterial material(int durability) {
		return new ToolMaterial(BlockTags.INCORRECT_FOR_WOODEN_TOOL, durability, 5.0F, 0.0F, 15, REPAIRS);
	}

	private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(WaveArena.MOD_ID, name));
		T item = factory.apply(properties.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	/** Lädt die Klasse, damit alle Items beim Mod-Start registriert werden. */
	public static void init() {
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY, TAB);
		WeaponEvents.init();
	}

	/** Eigene Nahkampfwaffen, die Schärfe-Upgrades bekommen sollen. */
	public static boolean isCustomMelee(ItemStack stack) {
		return stack.is(FLAME_BLADE) || stack.is(FROST_BREAKER) || stack.is(SOUL_REAPER) || stack.is(THUNDER_HAMMER);
	}
}

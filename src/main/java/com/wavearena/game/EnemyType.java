package com.wavearena.game;

import java.util.function.Consumer;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;

/**
 * Normale Gegner. Es sind Vanilla-Mobs mit eigenem Namen, eigener Ausrüstung und eigenen Werten,
 * deshalb braucht niemand Texturen oder Client-Code dafür.
 *
 * <p>Spalten: Anzeigename, Entity, Kosten im Wellen-Budget, ab welcher Welle verfügbar,
 * Kopfgeld, Auswahl-Gewicht, Leben-, Schaden- und Tempo-Faktor, Ausrüstung.
 */
public enum EnemyType {
	ZOMBIE("Zombie", EntityType.ZOMBIE, 1, 1, 4, 10, 1.0, 1.0, 1.0, m -> { }),
	SKELETON("Skelett-Schütze", EntityType.SKELETON, 2, 4, 7, 5, 0.8, 1.0, 1.0, m -> Gear.hand(m, Items.BOW)),
	SPIDER("Riesenspinne", EntityType.SPIDER, 2, 3, 6, 6, 1.1, 1.0, 1.15, m -> { }),
	HUSK("Wüstenkrieger", EntityType.HUSK, 2, 3, 7, 5, 1.3, 1.1, 1.0, m -> Gear.armor(m, Items.IRON_HELMET, null, null, null)),
	CREEPER("Sprengmeister", EntityType.CREEPER, 3, 4, 9, 5, 1.0, 1.0, 1.05, m -> { }),
	IRON_ZOMBIE("Eisensöldner", EntityType.ZOMBIE, 3, 5, 11, 6, 1.5, 1.2, 1.0, m -> {
		Gear.armor(m, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
		Gear.hand(m, Items.IRON_SWORD);
	}),
	VINDICATOR("Axtkämpfer", EntityType.VINDICATOR, 4, 6, 14, 5, 1.0, 1.1, 1.05, m -> Gear.hand(m, Items.IRON_AXE)),
	PILLAGER("Armbrustjäger", EntityType.PILLAGER, 3, 7, 10, 5, 1.0, 1.0, 1.0, m -> Gear.hand(m, Items.CROSSBOW)),
	WITCH("Hexe", EntityType.WITCH, 4, 8, 14, 3, 1.0, 1.0, 1.0, m -> { }),
	CAVE_SPIDER("Giftspinne", EntityType.CAVE_SPIDER, 1, 8, 3, 6, 1.0, 1.0, 1.2, m -> { }),
	WITHER_SKELETON("Todesritter", EntityType.WITHER_SKELETON, 5, 10, 20, 4, 1.1, 1.1, 1.05, m -> Gear.hand(m, Items.IRON_SWORD)),
	BLAZE("Flammengeist", EntityType.BLAZE, 4, 12, 16, 3, 1.0, 1.0, 1.0, m -> { }),
	RAVAGER("Zerstörer", EntityType.RAVAGER, 10, 13, 40, 2, 1.0, 1.0, 1.0, m -> { }),
	EVOKER("Beschwörer", EntityType.EVOKER, 7, 14, 28, 2, 1.0, 1.0, 1.0, m -> { }),
	DIAMOND_GUARD("Diamantgardist", EntityType.ZOMBIE, 6, 16, 26, 4, 1.8, 1.3, 1.0, m -> {
		Gear.armor(m, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
		Gear.hand(m, Items.DIAMOND_SWORD);
	});

	public final String label;
	public final EntityType<?> entity;
	public final int cost;
	public final int unlockWave;
	public final int bounty;
	public final int weight;
	public final double hpFactor;
	public final double damageFactor;
	public final double speedFactor;
	public final Consumer<Mob> equip;

	EnemyType(String label, EntityType<?> entity, int cost, int unlockWave, int bounty, int weight,
			double hpFactor, double damageFactor, double speedFactor, Consumer<Mob> equip) {
		this.label = label;
		this.entity = entity;
		this.cost = cost;
		this.unlockWave = unlockWave;
		this.bounty = bounty;
		this.weight = weight;
		this.hpFactor = hpFactor;
		this.damageFactor = damageFactor;
		this.speedFactor = speedFactor;
		this.equip = equip;
	}
}

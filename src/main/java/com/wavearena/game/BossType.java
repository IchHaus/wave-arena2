package com.wavearena.game;

import java.util.function.Consumer;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Items;

/** Boss-Gegner. Alle fünf Wellen kommt der nächste in der Reihe, danach wieder von vorn, nur stärker. */
public enum BossType {
	BONE_LORD("Der Knochenfürst", EntityType.ZOMBIE, 2.3, 170, 9, 1.0, m -> {
		// Nur Helm und Brustpanzer: volle Eisenrüstung hätte das Leben fast verdoppelt.
		Gear.armor(m, Items.IRON_HELMET, Items.IRON_CHESTPLATE, null, null);
		Gear.hand(m, Items.IRON_AXE);
	}, Ability.SLAM, Ability.SUMMON_ZOMBIES),

	WITCH_QUEEN("Die Hexenkönigin", EntityType.WITCH, 1.9, 200, 0, 1.0, m -> { },
			Ability.CURSE, Ability.SUMMON_VEXES),

	TITAN("Der Titan", EntityType.RAVAGER, 1.5, 370, 18, 1.0, m -> { },
			Ability.SLAM, Ability.SUMMON_ZOMBIES),

	DEATH_LORD("Der Todeslord", EntityType.WITHER_SKELETON, 2.5, 400, 14, 1.0, m -> {
		Gear.armor(m, Items.DIAMOND_HELMET, null, null, null);
		Gear.hand(m, Items.DIAMOND_SWORD);
	}, Ability.SOUL_BLAST, Ability.SUMMON_SKELETONS),

	COLOSSUS("Der Koloss", EntityType.IRON_GOLEM, 2.0, 640, 26, 1.0, m -> { },
			Ability.SLAM, Ability.SOUL_BLAST);

	/** Fähigkeiten, die Bosse regelmäßig einsetzen. */
	public enum Ability {
		SLAM(140),
		SOUL_BLAST(170),
		CURSE(120),
		SUMMON_ZOMBIES(330),
		SUMMON_SKELETONS(360),
		SUMMON_VEXES(300);

		public final int intervalTicks;

		Ability(int intervalTicks) {
			this.intervalTicks = intervalTicks;
		}
	}

	public final String label;
	public final EntityType<?> entity;
	public final double scale;
	public final double baseHealth;
	/** 0 = Wert des Mobs unverändert lassen (z. B. Hexe hat keinen Nahkampfschaden). */
	public final double baseDamage;
	public final double speedFactor;
	public final Consumer<Mob> equip;
	public final Ability[] abilities;

	BossType(String label, EntityType<?> entity, double scale, double baseHealth, double baseDamage,
			double speedFactor, Consumer<Mob> equip, Ability... abilities) {
		this.label = label;
		this.entity = entity;
		this.scale = scale;
		this.baseHealth = baseHealth;
		this.baseDamage = baseDamage;
		this.speedFactor = speedFactor;
		this.equip = equip;
		this.abilities = abilities;
	}

	/** Boss für Welle 5, 10, 15, ... */
	public static BossType forWave(int wave) {
		int index = Math.max(0, wave / 5 - 1);
		return values()[index % values().length];
	}

	/** 1 beim ersten Auftritt, 2 beim zweiten Durchlauf usw. */
	public static int cycle(int wave) {
		int index = Math.max(0, wave / 5 - 1);
		return index / values().length + 1;
	}
}

package com.wavearena.game;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Rüstet Gegner aus. Null-Einträge werden übersprungen. */
public final class Gear {
	private Gear() {
	}

	public static void hand(Mob mob, Item item) {
		mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(item));
	}

	public static void armor(Mob mob, Item head, Item chest, Item legs, Item feet) {
		if (head != null) {
			mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(head));
		}
		if (chest != null) {
			mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
		}
		if (legs != null) {
			mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
		}
		if (feet != null) {
			mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(feet));
		}
	}
}

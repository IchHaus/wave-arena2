package com.wavearena.shop;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import com.wavearena.game.PlayerData;
import com.wavearena.game.Upgrade;
import com.wavearena.item.ModItems;

/** Sorgt dafür, dass die gekauften Verzauberungs-Upgrades auf allen passenden Gegenständen liegen. */
public final class Enchants {
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private Enchants() {
	}

	public static boolean isMelee(ItemStack stack) {
		return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || ModItems.isCustomMelee(stack);
	}

	public static void sync(ServerPlayer player, PlayerData data) {
		ServerLevel level = (ServerLevel) player.level();
		int sharpness = data.level(Upgrade.SHARPNESS);
		int protection = data.level(Upgrade.PROTECTION);
		int draw = data.level(Upgrade.DRAW);

		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.isEmpty()) {
				continue;
			}
			if (sharpness > 0 && isMelee(stack)) {
				ensure(level, stack, Enchantments.SHARPNESS, sharpness);
			} else if (draw > 0 && stack.is(Items.BOW)) {
				ensure(level, stack, Enchantments.POWER, draw);
			}
		}
		if (protection > 0) {
			for (EquipmentSlot slot : ARMOR_SLOTS) {
				ItemStack stack = player.getItemBySlot(slot);
				if (!stack.isEmpty()) {
					ensure(level, stack, Enchantments.PROTECTION, protection);
				}
			}
		}
	}

	private static void ensure(ServerLevel level, ItemStack stack, ResourceKey<Enchantment> key, int wanted) {
		Holder<Enchantment> holder = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
		if (EnchantmentHelper.getItemEnchantmentLevel(holder, stack) < wanted) {
			stack.enchant(holder, wanted);
		}
	}
}

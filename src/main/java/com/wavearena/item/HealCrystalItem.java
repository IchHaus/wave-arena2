package com.wavearena.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.wavearena.game.Cooldowns;
import com.wavearena.game.Fx;
import com.wavearena.game.Text;

/** Rechtsklick: heilt sofort 4 Herzen. Wird verbraucht. */
public class HealCrystalItem extends TooltipItem {
	public static final float HEAL_AMOUNT = 8.0F;

	public HealCrystalItem(Item.Properties properties) {
		super(properties, "Rechtsklick: heilt sofort 4 Herzen.", "Wird dabei verbraucht.");
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.PASS;
		}
		ServerLevel serverLevel = (ServerLevel) level;
		if (sp.getHealth() >= sp.getMaxHealth()) {
			Fx.actionBar(sp, Text.line("Du bist bereits bei voller Gesundheit", ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		if (!Cooldowns.tryUse(sp.getUUID(), "heal_crystal", serverLevel.getGameTime(), 30)) {
			return InteractionResult.FAIL;
		}

		sp.heal(HEAL_AMOUNT);
		ItemStack stack = sp.getItemInHand(hand);
		stack.shrink(1);

		Fx.particles(serverLevel, ParticleTypes.HEART, sp.getX(), sp.getY() + 1.2, sp.getZ(), 8, 0.4, 0.5, 0.4, 0.02);
		Fx.sound(serverLevel, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_LEVELUP, 0.5F, 1.8F);
		return InteractionResult.SUCCESS;
	}
}

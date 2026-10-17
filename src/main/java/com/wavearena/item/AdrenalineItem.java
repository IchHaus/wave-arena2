package com.wavearena.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.wavearena.WaveArena;
import com.wavearena.game.Cooldowns;
import com.wavearena.game.Fx;
import com.wavearena.game.Text;
import com.wavearena.game.TimedModifiers;

/** Rechtsklick: 20 Sekunden schneller und stärker, dazu zwei goldene Herzen. */
public class AdrenalineItem extends TooltipItem {
	private static final Identifier SPEED_ID = WaveArena.id("adrenaline_speed");
	private static final Identifier DAMAGE_ID = WaveArena.id("adrenaline_damage");
	private static final int DURATION_TICKS = 400;
	private static final int COOLDOWN_TICKS = 500;

	public AdrenalineItem(Item.Properties properties) {
		super(properties,
				"Rechtsklick: 20 s lang +40 % Tempo und +30 % Schaden.",
				"Dazu 2 goldene Herzen. Abklingzeit " + COOLDOWN_TICKS / 20 + " s.");
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.PASS;
		}
		ServerLevel serverLevel = (ServerLevel) level;
		long now = serverLevel.getGameTime();

		long left = Cooldowns.remaining(sp.getUUID(), "adrenaline", now);
		if (left > 0) {
			Fx.actionBar(sp, Text.line("Adrenalin wirkt erst in " + (left / 20 + 1) + " s wieder", ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		Cooldowns.tryUse(sp.getUUID(), "adrenaline", now, COOLDOWN_TICKS);

		long until = now + DURATION_TICKS;
		TimedModifiers.apply(sp, Attributes.MOVEMENT_SPEED, SPEED_ID, 0.4, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, until);
		TimedModifiers.apply(sp, Attributes.ATTACK_DAMAGE, DAMAGE_ID, 0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, until);
		sp.setAbsorptionAmount(Math.max(sp.getAbsorptionAmount(), 4.0F));

		ItemStack stack = sp.getItemInHand(hand);
		stack.shrink(1);

		Fx.particles(serverLevel, ParticleTypes.CRIT, sp.getX(), sp.getY() + 1.0, sp.getZ(), 20, 0.4, 0.6, 0.4, 0.2);
		Fx.sound(serverLevel, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PLAYER_LEVELUP, 0.6F, 0.7F);
		return InteractionResult.SUCCESS;
	}
}

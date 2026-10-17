package com.wavearena.item;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import com.wavearena.WaveArena;
import com.wavearena.game.Fx;
import com.wavearena.game.TimedModifiers;

/** Passive Effekte der eigenen Nahkampfwaffen: Feuer, Frost und Lebensraub. */
public final class WeaponEvents {
	private static final Identifier FROST_SLOW = WaveArena.id("frost_slow");

	private WeaponEvents() {
	}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register(WeaponEvents::afterDamage);
		ServerTickEvents.END_SERVER_TICK.register(TimedModifiers::tick);
	}

	private static void afterDamage(LivingEntity victim, DamageSource source, float baseDamage, float damageTaken, boolean blocked) {
		if (blocked || damageTaken <= 0.0F) {
			return;
		}
		// Nur direkte Nahkampftreffer, keine Pfeile oder Fähigkeiten.
		if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player) {
			return;
		}
		ServerLevel level = (ServerLevel) victim.level();
		ItemStack held = player.getMainHandItem();

		if (held.is(ModItems.FLAME_BLADE)) {
			victim.igniteForSeconds(5.0F);
			Fx.particles(level, ParticleTypes.FLAME, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
					victim.getZ(), 10, 0.3, 0.4, 0.3, 0.03);
		} else if (held.is(ModItems.FROST_BREAKER)) {
			long until = level.getGameTime() + 60;
			TimedModifiers.apply(victim, Attributes.MOVEMENT_SPEED, FROST_SLOW, -0.45,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, until);
			victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
			Fx.particles(level, ParticleTypes.SNOWFLAKE, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
					victim.getZ(), 14, 0.35, 0.45, 0.35, 0.02);
		} else if (held.is(ModItems.SOUL_REAPER)) {
			player.heal(damageTaken * 0.2F);
			Fx.particles(level, ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5,
					victim.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
		}
	}
}

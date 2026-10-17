package com.wavearena.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Fähigkeiten der Bosse. Die gefährlichen Angriffe werden vorher mit Partikeln angekündigt und
 * treffen erst eine Sekunde später, damit man ausweichen kann.
 */
public final class BossAbilities {
	private static final double SLAM_RADIUS = 6.5;
	private static final double BLAST_RADIUS = 2.5;

	private BossAbilities() {
	}

	public static void cast(ServerLevel level, Mob boss, BossType.Ability ability, int wave, long now,
			List<ServerPlayer> targets) {
		switch (ability) {
			case SLAM -> slam(level, boss, wave, now);
			case SOUL_BLAST -> soulBlast(level, boss, wave, now, targets);
			case CURSE -> curse(level, boss, targets);
			case SUMMON_ZOMBIES -> summon(level, boss, wave, now, 0);
			case SUMMON_SKELETONS -> summon(level, boss, wave, now, 1);
			case SUMMON_VEXES -> summon(level, boss, wave, now, 2);
		}
	}

	// ------------------------------------------------------------------ Schlag

	private static void slam(ServerLevel level, Mob boss, int wave, long now) {
		Vec3 at = boss.position();
		float damage = (float) (5.0 + 0.45 * wave);

		Fx.sound(level, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_GROWL, 1.4F, 0.8F);
		ring(level, ParticleTypes.FLAME, at, SLAM_RADIUS, 40);
		GameManager.later(now + 8, () -> ring(level, ParticleTypes.FLAME, at, SLAM_RADIUS * 0.7, 28));
		GameManager.later(now + 16, () -> ring(level, ParticleTypes.CRIT, at, SLAM_RADIUS, 40));

		GameManager.later(now + 24, () -> {
			if (!boss.isAlive()) {
				return;
			}
			ring(level, ParticleTypes.EXPLOSION, at, SLAM_RADIUS * 0.5, 6);
			ring(level, ParticleTypes.LARGE_SMOKE, at, SLAM_RADIUS, 36);
			Fx.sound(level, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.6F);
			for (ServerPlayer player : GameManager.alivePlayers()) {
				if (flatDistance(player.position(), at) <= SLAM_RADIUS && Math.abs(player.getY() - at.y) < 3.5) {
					player.hurtServer(level, level.damageSources().mobAttack(boss), damage);
					player.knockback(1.2, at.x - player.getX(), at.z - player.getZ());
				}
			}
		});
	}

	// ------------------------------------------------------------------ Seelenschlag

	private static void soulBlast(ServerLevel level, Mob boss, int wave, long now, List<ServerPlayer> targets) {
		List<ServerPlayer> sorted = new ArrayList<>(targets);
		sorted.sort(Comparator.comparingDouble(player -> player.distanceToSqr(boss)));
		float damage = (float) (4.0 + 0.3 * wave);

		Fx.sound(level, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.WITHER_SPAWN, 0.6F, 1.6F);
		int shots = 0;
		for (ServerPlayer target : sorted) {
			if (shots >= 3 || target.distanceTo(boss) > 36.0F) {
				continue;
			}
			shots++;
			// Gezielt wird auf die Position von jetzt: wer sich bewegt, weicht aus.
			Vec3 spot = target.position();
			ring(level, ParticleTypes.SOUL_FIRE_FLAME, spot, BLAST_RADIUS, 24);
			GameManager.later(now + 10, () -> ring(level, ParticleTypes.SOUL_FIRE_FLAME, spot, BLAST_RADIUS * 0.7, 18));
			GameManager.later(now + 20, () -> ring(level, ParticleTypes.SOUL_FIRE_FLAME, spot, BLAST_RADIUS * 0.4, 12));
			GameManager.later(now + 28, () -> {
				if (!boss.isAlive()) {
					return;
				}
				LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
				if (bolt != null) {
					bolt.setVisualOnly(true);
					bolt.setPos(spot.x, spot.y, spot.z);
					level.addFreshEntity(bolt);
				}
				Fx.particles(level, ParticleTypes.EXPLOSION, spot.x, spot.y + 0.5, spot.z, 1, 0.0, 0.0, 0.0, 0.0);
				for (ServerPlayer player : GameManager.alivePlayers()) {
					if (flatDistance(player.position(), spot) <= BLAST_RADIUS && Math.abs(player.getY() - spot.y) < 3.5) {
						player.hurtServer(level, level.damageSources().magic(), damage);
					}
				}
			});
		}
	}

	// ------------------------------------------------------------------ Fluch

	private static void curse(ServerLevel level, Mob boss, List<ServerPlayer> targets) {
		Fx.sound(level, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 0.6F);
		for (ServerPlayer player : targets) {
			if (player.distanceTo(boss) > 28.0F) {
				continue;
			}
			player.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 0));
			player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 0));
			Fx.particles(level, ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 24, 0.4, 0.7, 0.4, 0.05);
			Fx.sound(level, player.getX(), player.getY(), player.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, 0.5F, 1.2F);
		}
	}

	// ------------------------------------------------------------------ Diener

	private static void summon(ServerLevel level, Mob boss, int wave, long now, int kind) {
		int cap = 8 + wave / 4;
		int existing = GameManager.minionCount();
		if (existing >= cap) {
			return;
		}
		int count = Math.min(Math.min(2 + wave / 8 + BossType.cycle(wave), 6), cap - existing);

		Fx.sound(level, boss.getX(), boss.getY(), boss.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.2F, 0.9F);
		for (int i = 0; i < count; i++) {
			double angle = level.getRandom().nextDouble() * Math.PI * 2;
			double radius = 2.5 + boss.getBbWidth();
			Vec3 pos = new Vec3(boss.getX() + Math.cos(angle) * radius, boss.getY(), boss.getZ() + Math.sin(angle) * radius);

			Spawner.Spawned spawned = switch (kind) {
				case 0 -> Spawner.normal(level, pos, EnemyType.ZOMBIE, wave, WaveModifier.NONE, true, now);
				case 1 -> Spawner.normal(level, pos, EnemyType.SKELETON, wave, WaveModifier.NONE, true, now);
				default -> Spawner.custom(level, pos.add(0.0, 1.5, 0.0), EntityType.VEX, "Rachegeist", wave, 4, now);
			};
			if (spawned != null) {
				GameManager.register(spawned);
			}
		}
	}

	// ------------------------------------------------------------------ Helfer

	private static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int points) {
		for (int i = 0; i < points; i++) {
			double angle = i * (Math.PI * 2 / points);
			Fx.particles(level, particle, center.x + Math.cos(angle) * radius, center.y + 0.15,
					center.z + Math.sin(angle) * radius, 1, 0.05, 0.02, 0.05, 0.0);
		}
	}

	private static double flatDistance(Vec3 a, Vec3 b) {
		double dx = a.x - b.x;
		double dz = a.z - b.z;
		return Math.sqrt(dx * dx + dz * dz);
	}
}

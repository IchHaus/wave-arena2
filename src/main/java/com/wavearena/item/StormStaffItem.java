package com.wavearena.item;

import java.util.Comparator;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.wavearena.game.Cooldowns;
import com.wavearena.game.Fx;
import com.wavearena.game.Targets;
import com.wavearena.game.Text;

/** Rechtsklick: Kettenblitz auf die nächsten Gegner. Benötigt keine Munition. */
public class StormStaffItem extends TooltipItem {
	private static final int COOLDOWN_TICKS = 100;
	private static final double RANGE = 16.0;
	private static final int MAX_TARGETS = 4;
	private static final float DAMAGE = 9.0F;

	public StormStaffItem(Item.Properties properties) {
		super(properties,
				"Rechtsklick: Kettenblitz (" + COOLDOWN_TICKS / 20 + " s Abklingzeit)",
				"Trifft bis zu " + MAX_TARGETS + " Gegner in " + (int) RANGE + " Blöcken Umkreis.",
				"Braucht keine Munition. Der Blitz verletzt dich nicht.");
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.PASS;
		}
		ServerLevel serverLevel = (ServerLevel) level;
		long now = serverLevel.getGameTime();

		long left = Cooldowns.remaining(sp.getUUID(), "staff", now);
		if (left > 0) {
			Fx.actionBar(sp, Text.line("Sturmstab lädt noch " + (left / 20 + 1) + " s", ChatFormatting.RED));
			return InteractionResult.FAIL;
		}

		Vec3 origin = sp.position();
		List<Mob> targets = Targets.enemiesAround(serverLevel, origin, RANGE);
		if (targets.isEmpty()) {
			Fx.actionBar(sp, Text.line("Keine Gegner in Reichweite", ChatFormatting.GRAY));
			return InteractionResult.FAIL;
		}
		targets.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(origin)));
		Cooldowns.tryUse(sp.getUUID(), "staff", now, COOLDOWN_TICKS);

		Vec3 from = sp.getEyePosition();
		int hits = Math.min(MAX_TARGETS, targets.size());
		for (int i = 0; i < hits; i++) {
			Mob mob = targets.get(i);
			Vec3 to = mob.position().add(0.0, mob.getBbHeight() * 0.5, 0.0);

			// Sichtbarer Funkenstrahl zwischen den Zielen.
			int steps = Math.max(2, (int) from.distanceTo(to) * 2);
			for (int s = 0; s <= steps; s++) {
				double t = s / (double) steps;
				Fx.particles(serverLevel, ParticleTypes.ELECTRIC_SPARK,
						from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t,
						1, 0.05, 0.05, 0.05, 0.0);
			}

			mob.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(sp), DAMAGE);

			LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(serverLevel, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.setVisualOnly(true);
				bolt.setPos(mob.getX(), mob.getY(), mob.getZ());
				serverLevel.addFreshEntity(bolt);
			}
			from = to;
		}
		Fx.sound(serverLevel, origin.x, origin.y, origin.z, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.5F, 1.6F);
		return InteractionResult.SUCCESS;
	}
}

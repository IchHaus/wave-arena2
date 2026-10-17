package com.wavearena.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.wavearena.game.Cooldowns;
import com.wavearena.game.Fx;
import com.wavearena.game.Targets;
import com.wavearena.game.Text;

/** Rechtsklick: Erdbeben. Schadet allen Gegnern im Umkreis und schleudert sie weg. */
public class ThunderHammerItem extends TooltipItem {
	private static final int COOLDOWN_TICKS = 200;
	private static final double RADIUS = 6.5;
	private static final float DAMAGE = 11.0F;

	public ThunderHammerItem(Item.Properties properties) {
		super(properties,
				"Sehr schwerer Hammer, langsamer Schlag.",
				"Rechtsklick: Erdbeben (" + COOLDOWN_TICKS / 20 + " s Abklingzeit)",
				"Schadet allen Gegnern im Umkreis und schleudert sie weg.");
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer sp)) {
			return InteractionResult.PASS;
		}
		ServerLevel serverLevel = (ServerLevel) level;
		long now = serverLevel.getGameTime();

		long left = Cooldowns.remaining(sp.getUUID(), "hammer", now);
		if (left > 0) {
			Fx.actionBar(sp, Text.line("Donnerhammer lädt noch " + (left / 20 + 1) + " s", ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		Cooldowns.tryUse(sp.getUUID(), "hammer", now, COOLDOWN_TICKS);

		Vec3 pos = sp.position();
		List<Mob> targets = Targets.enemiesAround(serverLevel, pos, RADIUS);
		for (Mob mob : targets) {
			mob.hurtServer(serverLevel, serverLevel.damageSources().playerAttack(sp), DAMAGE);
			mob.knockback(1.4, sp.getX() - mob.getX(), sp.getZ() - mob.getZ());
		}

		for (int ring = 1; ring <= 3; ring++) {
			double radius = ring * 2.1;
			for (int i = 0; i < 20; i++) {
				double angle = i * (Math.PI * 2 / 20);
				Fx.particles(serverLevel, ParticleTypes.CRIT, pos.x + Math.cos(angle) * radius, pos.y + 0.2,
						pos.z + Math.sin(angle) * radius, 2, 0.1, 0.1, 0.1, 0.08);
			}
		}
		Fx.particles(serverLevel, ParticleTypes.EXPLOSION, pos.x, pos.y + 0.3, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
		Fx.sound(serverLevel, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, 0.9F, 0.7F);
		Fx.sound(serverLevel, pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, 0.8F, 0.6F);
		return InteractionResult.SUCCESS;
	}
}

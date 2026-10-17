package com.wavearena.game;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Findet Gegner für Waffen-Fähigkeiten und den Wellen-Manager. */
public final class Targets {
	private Targets() {
	}

	/** Ein Gegner ist alles, was der Wellenmodus gespawnt hat, sowie jedes andere Monster. */
	public static boolean isEnemy(Entity entity) {
		if (!(entity instanceof Mob mob) || !mob.isAlive()) {
			return false;
		}
		return mob.getTags().contains(GameManager.TAG_ENEMY) || mob.getType().getCategory() == MobCategory.MONSTER;
	}

	public static List<Mob> enemiesAround(ServerLevel level, Vec3 center, double radius) {
		AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius,
				center.x + radius, center.y + radius, center.z + radius);
		List<Mob> found = new ArrayList<>(level.getEntitiesOfClass(Mob.class, box, Targets::isEnemy));
		found.removeIf(mob -> mob.position().distanceTo(center) > radius);
		return found;
	}
}

package com.wavearena.game;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Zeitlich begrenzte Attribut-Änderungen (Verlangsamung durch Frost, Adrenalin usw.).
 * Die Änderungen sind "transient", werden also nicht gespeichert und verschwinden spätestens beim Neustart.
 */
public final class TimedModifiers {
	private record Entry(UUID entity, Holder<Attribute> attribute, Identifier id, long untilTick) {
	}

	private static final Map<String, Entry> ACTIVE = new HashMap<>();

	private TimedModifiers() {
	}

	public static void apply(LivingEntity target, Holder<Attribute> attribute, Identifier id, double amount,
			AttributeModifier.Operation operation, long untilTick) {
		AttributeInstance instance = target.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		instance.removeModifier(id);
		instance.addTransientModifier(new AttributeModifier(id, amount, operation));
		ACTIVE.put(target.getUUID() + "|" + id, new Entry(target.getUUID(), attribute, id, untilTick));
	}

	/** Pro Server-Tick aufrufen. Prüft nur alle 5 Ticks. */
	public static void tick(MinecraftServer server) {
		if (ACTIVE.isEmpty()) {
			return;
		}
		long now = server.overworld().getGameTime();
		if (now % 5 != 0) {
			return;
		}
		Iterator<Map.Entry<String, Entry>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Entry entry = it.next().getValue();
			if (entry.untilTick() > now) {
				continue;
			}
			it.remove();
			for (ServerLevel level : server.getAllLevels()) {
				Entity found = level.getEntity(entry.entity());
				if (found instanceof LivingEntity living) {
					AttributeInstance instance = living.getAttribute(entry.attribute());
					if (instance != null) {
						instance.removeModifier(entry.id());
					}
					break;
				}
			}
		}
	}

	/** Entfernt eine Änderung sofort (z. B. beim Spielende). */
	public static void clear(LivingEntity target, Identifier id) {
		AttributeInstance any = null;
		Entry entry = ACTIVE.remove(target.getUUID() + "|" + id);
		if (entry != null) {
			any = target.getAttribute(entry.attribute());
		}
		if (any != null) {
			any.removeModifier(id);
		}
	}
}

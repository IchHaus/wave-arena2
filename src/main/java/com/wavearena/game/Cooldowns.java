package com.wavearena.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Einfache Spieler-Cooldowns für Fähigkeiten (in Server-Ticks). */
public final class Cooldowns {
	private static final Map<String, Long> READY_AT = new HashMap<>();

	private Cooldowns() {
	}

	private static String key(UUID id, String ability) {
		return id + ":" + ability;
	}

	/** Verbleibende Ticks, 0 wenn bereit. */
	public static long remaining(UUID player, String ability, long now) {
		Long readyAt = READY_AT.get(key(player, ability));
		if (readyAt == null || readyAt <= now) {
			return 0L;
		}
		return readyAt - now;
	}

	/** Startet den Cooldown, falls die Fähigkeit bereit ist. Gibt false zurück, wenn sie noch lädt. */
	public static boolean tryUse(UUID player, String ability, long now, int cooldownTicks) {
		if (remaining(player, ability, now) > 0) {
			return false;
		}
		READY_AT.put(key(player, ability), now + cooldownTicks);
		return true;
	}

	public static void clearAll() {
		READY_AT.clear();
	}
}

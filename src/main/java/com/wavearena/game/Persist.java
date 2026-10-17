package com.wavearena.game;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.BlockPos;

import com.wavearena.WaveArena;

/** Speichert Arena-Mittelpunkt und Highscore in einer kleinen Datei im config-Ordner. */
public final class Persist {
	private static final Properties PROPS = new Properties();

	private Persist() {
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("wavearena.properties");
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try (Reader reader = Files.newBufferedReader(path)) {
			PROPS.load(reader);
		} catch (IOException e) {
			WaveArena.LOGGER.warn("Konnte {} nicht lesen: {}", path, e.getMessage());
		}
	}

	private static void save() {
		Path path = file();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				PROPS.store(writer, "Wave Arena");
			}
		} catch (IOException e) {
			WaveArena.LOGGER.warn("Konnte {} nicht schreiben: {}", path, e.getMessage());
		}
	}

	public static BlockPos center() {
		if (!PROPS.containsKey("center.x")) {
			return null;
		}
		try {
			return new BlockPos(
					Integer.parseInt(PROPS.getProperty("center.x")),
					Integer.parseInt(PROPS.getProperty("center.y")),
					Integer.parseInt(PROPS.getProperty("center.z")));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static void saveCenter(BlockPos pos) {
		PROPS.setProperty("center.x", Integer.toString(pos.getX()));
		PROPS.setProperty("center.y", Integer.toString(pos.getY()));
		PROPS.setProperty("center.z", Integer.toString(pos.getZ()));
		save();
	}

	public static int bestWave() {
		return intValue("best.wave");
	}

	public static int bestKills() {
		return intValue("best.kills");
	}

	/** Gibt true zurück, wenn ein neuer Rekord gesetzt wurde. */
	public static boolean submit(int wave, int kills) {
		boolean record = wave > bestWave() || (wave == bestWave() && kills > bestKills());
		if (record) {
			PROPS.setProperty("best.wave", Integer.toString(wave));
			PROPS.setProperty("best.kills", Integer.toString(kills));
			save();
		}
		return record;
	}

	private static int intValue(String key) {
		try {
			return Integer.parseInt(PROPS.getProperty(key, "0"));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}

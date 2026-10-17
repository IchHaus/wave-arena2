package com.wavearena;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wavearena.command.ArenaCommands;
import com.wavearena.game.GameManager;
import com.wavearena.item.ModItems;

/**
 * Wave Arena: Wellen-Überlebensmodus für Minecraft 1.21.11 (Fabric).
 */
public class WaveArena implements ModInitializer {
	public static final String MOD_ID = "wavearena";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.init();
		GameManager.init();
		ArenaCommands.init();
		LOGGER.info("Wave Arena geladen. Mit /wave help geht es los.");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

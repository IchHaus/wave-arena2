package com.wavearena.game;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/** Sounds, Partikel und Titel an einem Ort. */
public final class Fx {
	private Fx() {
	}

	public static void sound(ServerLevel level, double x, double y, double z, SoundEvent sound, float volume, float pitch) {
		level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	public static void sound(ServerLevel level, double x, double y, double z, Holder<SoundEvent> sound, float volume, float pitch) {
		sound(level, x, y, z, sound.value(), volume, pitch);
	}

	public static void particles(ServerLevel level, ParticleOptions type, double x, double y, double z, int count, double spreadX, double spreadY, double spreadZ, double speed) {
		level.sendParticles(type, x, y, z, count, spreadX, spreadY, spreadZ, speed);
	}

	public static void title(ServerPlayer player, Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	public static void actionBar(ServerPlayer player, Component text) {
		player.displayClientMessage(text, true);
	}
}

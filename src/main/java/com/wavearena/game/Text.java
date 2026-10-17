package com.wavearena.game;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Kleine Helfer für farbige Texte ohne die Standard-Kursivschrift von Item-Namen und Lore. */
public final class Text {
	private Text() {
	}

	public static MutableComponent line(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(style -> style.withColor(color).withItalic(false));
	}

	public static MutableComponent bold(String text, ChatFormatting color) {
		return Component.literal(text).withStyle(style -> style.withColor(color).withBold(true).withItalic(false));
	}

	/** Chat-Nachricht mit Präfix. */
	public static MutableComponent chat(String text, ChatFormatting color) {
		MutableComponent prefix = Component.literal("[Arena] ").withStyle(style -> style.withColor(ChatFormatting.DARK_RED).withBold(true));
		return prefix.append(Component.literal(text).withStyle(style -> style.withColor(color)));
	}
}

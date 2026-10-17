package com.wavearena.game;

import net.minecraft.ChatFormatting;

/** Zufällige Sonderregeln für einzelne Wellen. */
public enum WaveModifier {
	NONE("", "", ChatFormatting.WHITE, 1.0, 1.0, 1.0, 1.0, 1.0),
	BLOOD_MOON("Blutmond", "Gegner schlagen 40 % härter zu, Kopfgelder x1,5", ChatFormatting.DARK_RED, 1.0, 1.0, 1.4, 1.0, 1.5),
	SWARM("Schwarm", "Viel mehr Gegner, aber mit weniger Leben", ChatFormatting.YELLOW, 1.7, 0.6, 1.0, 1.0, 0.8),
	ARMORED("Panzerwelle", "Gegner haben mehr Leben und Kopfgelder x1,4", ChatFormatting.GRAY, 1.0, 1.5, 1.0, 1.0, 1.4),
	RUSH("Eilmarsch", "Gegner sind 25 % schneller, Kopfgelder x1,25", ChatFormatting.AQUA, 1.0, 1.0, 1.0, 1.25, 1.25),
	GOLD_RUSH("Goldrausch", "Doppelte Kopfgelder, aber zähere Gegner", ChatFormatting.GOLD, 1.0, 1.25, 1.0, 1.0, 2.0);

	public final String label;
	public final String description;
	public final ChatFormatting color;
	public final double budgetFactor;
	public final double hpFactor;
	public final double damageFactor;
	public final double speedFactor;
	public final double bountyFactor;

	WaveModifier(String label, String description, ChatFormatting color, double budgetFactor, double hpFactor,
			double damageFactor, double speedFactor, double bountyFactor) {
		this.label = label;
		this.description = description;
		this.color = color;
		this.budgetFactor = budgetFactor;
		this.hpFactor = hpFactor;
		this.damageFactor = damageFactor;
		this.speedFactor = speedFactor;
		this.bountyFactor = bountyFactor;
	}
}

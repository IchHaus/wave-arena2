package com.wavearena.game;

import java.util.UUID;

/** Laufzeit-Daten eines Gegners, den der Wellenmodus gespawnt hat. */
public final class EnemyData {
	public final String label;
	public final int bounty;
	public final BossType boss;
	public final Affix affix;
	public final boolean minion;
	public final long spawnTick;
	/** Zeitpunkt (Tick) der nächsten Boss-Fähigkeit, je Fähigkeit. */
	public final long[] nextAbility;
	/** Letzter Spieler, der diesen Gegner getroffen hat (für das Kopfgeld bei Feuer- oder Fernkampf-Kills). */
	public UUID lastPlayer;
	/** Wann der Gegner das nächste Mal zurück in die Arena geholt wird, falls er festhängt. */
	public long nextRescue;

	public EnemyData(String label, int bounty, BossType boss, Affix affix, boolean minion, long spawnTick) {
		this.label = label;
		this.bounty = bounty;
		this.boss = boss;
		this.affix = affix;
		this.minion = minion;
		this.spawnTick = spawnTick;
		this.nextRescue = spawnTick + 2400L;
		this.nextAbility = new long[boss == null ? 0 : boss.abilities.length];
		for (int i = 0; i < nextAbility.length; i++) {
			// Erste Anwendung gestaffelt, damit nicht alles gleichzeitig kommt.
			nextAbility[i] = spawnTick + 80L + i * 70L;
		}
	}

	public boolean isElite() {
		return affix != null;
	}
}

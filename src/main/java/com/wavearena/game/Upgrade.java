package com.wavearena.game;

/** Dauerhafte Verbesserungen für die laufende Partie. Jede Stufe wird teurer. */
public enum Upgrade {
	LIFE("Lebenskraft", "+1 Herz pro Stufe", 120, 180, 260, 380, 540, 760),
	POWER("Kampfkraft", "+7 % Nahkampfschaden pro Stufe", 150, 240, 380, 580, 880),
	SPEED("Wendigkeit", "+4 % Lauftempo pro Stufe", 100, 200, 360),
	GREED("Beutegier", "+10 % Münzen pro Stufe", 200, 380, 650, 1000),
	SHARPNESS("Schärfe", "Schärfe auf all deinen Nahkampfwaffen", 90, 170, 280, 430, 640),
	PROTECTION("Schutz", "Schutz auf deiner ganzen Rüstung", 110, 200, 330, 520),
	DRAW("Kraftschuss", "Stärke auf all deinen Bögen", 80, 150, 250, 390, 600);

	public final String label;
	public final String description;
	private final int[] prices;

	Upgrade(String label, String description, int... prices) {
		this.label = label;
		this.description = description;
		this.prices = prices;
	}

	public int maxLevel() {
		return prices.length;
	}

	/** Preis für die nächste Stufe, wenn man aktuell auf {@code currentLevel} ist. -1 wenn schon maximal. */
	public int priceFor(int currentLevel) {
		if (currentLevel < 0 || currentLevel >= prices.length) {
			return -1;
		}
		return prices[currentLevel];
	}
}

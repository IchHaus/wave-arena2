package com.wavearena.game;

/** Besondere Eigenschaften von Elite-Gegnern. Elites leuchten und geben dreifaches Kopfgeld. */
public enum Affix {
	FIERY("Feurig", "setzt Ziele in Brand"),
	VAMPIRIC("Vampirisch", "heilt sich beim Treffen"),
	EXPLOSIVE("Explosiv", "explodiert beim Tod"),
	SWIFT("Flink", "ist deutlich schneller"),
	ARMORED("Gepanzert", "hat viel mehr Rüstung");

	public final String label;
	public final String description;

	Affix(String label, String description) {
		this.label = label;
		this.description = description;
	}
}

package com.wavearena.game;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.RandomSource;

/** Stellt die Gegner einer Welle aus einem Budget zusammen, das mit jeder Welle wächst. */
public final class WaveComposer {
	private WaveComposer() {
	}

	public record Plan(List<EnemyType> enemies, BossType boss, WaveModifier modifier) {
		public int total() {
			return enemies.size() + (boss != null ? 1 : 0);
		}
	}

	public static boolean isBossWave(int wave) {
		return wave > 0 && wave % 5 == 0;
	}

	public static double playerFactor(int players) {
		return 1.0 + 0.65 * Math.max(0, players - 1);
	}

	public static Plan compose(int wave, int players, RandomSource random) {
		boolean bossWave = isBossWave(wave);

		WaveModifier modifier = WaveModifier.NONE;
		if (!bossWave && wave >= 3 && random.nextFloat() < 0.38f) {
			WaveModifier[] all = WaveModifier.values();
			modifier = all[1 + random.nextInt(all.length - 1)];
		}

		double budget = (4.0 + 3.4 * wave + 0.22 * wave * wave) * playerFactor(players) * modifier.budgetFactor;
		if (bossWave) {
			budget *= 0.6;
		}

		List<EnemyType> available = new ArrayList<>();
		for (EnemyType type : EnemyType.values()) {
			if (type.unlockWave <= wave) {
				available.add(type);
			}
		}

		List<EnemyType> picked = new ArrayList<>();
		double remaining = budget;
		// Fernkämpfer sind früh sehr gefährlich, deshalb gibt es pro Welle nur wenige.
		int rangedCap = 1 + wave / 3;
		int rangedCount = 0;
		int guard = 0;
		while (remaining >= 1.0 && guard++ < 500) {
			double totalWeight = 0;
			for (EnemyType type : available) {
				if (type.cost <= remaining && !capped(type, rangedCount, rangedCap)) {
					totalWeight += weight(type, wave);
				}
			}
			if (totalWeight <= 0) {
				break;
			}
			double roll = random.nextDouble() * totalWeight;
			EnemyType chosen = null;
			for (EnemyType type : available) {
				if (type.cost > remaining || capped(type, rangedCount, rangedCap)) {
					continue;
				}
				roll -= weight(type, wave);
				if (roll <= 0) {
					chosen = type;
					break;
				}
			}
			if (chosen == null) {
				chosen = EnemyType.ZOMBIE;
			}
			picked.add(chosen);
			if (isRanged(chosen)) {
				rangedCount++;
			}
			remaining -= chosen.cost;
		}

		return new Plan(picked, bossWave ? BossType.forWave(wave) : null, modifier);
	}

	private static boolean isRanged(EnemyType type) {
		return type == EnemyType.SKELETON || type == EnemyType.PILLAGER;
	}

	private static boolean capped(EnemyType type, int rangedCount, int rangedCap) {
		return isRanged(type) && rangedCount >= rangedCap;
	}

	/** Neu freigeschaltete Gegner tauchen etwas häufiger auf, damit man sie bald kennenlernt. */
	private static double weight(EnemyType type, int wave) {
		double w = type.weight;
		if (wave - type.unlockWave <= 2) {
			w *= 1.5;
		}
		return w;
	}

	/** Maximale Anzahl gleichzeitig lebender Gegner. */
	public static int maxAlive(int wave, int players) {
		int base = Math.min(14 + wave * 2, 46);
		return base + 6 * Math.max(0, players - 1);
	}

	public static double healthScale(int wave) {
		return Math.min(1.0 + 0.06 * wave, 6.0);
	}

	public static double damageScale(int wave) {
		return Math.min(1.0 + 0.045 * wave, 4.0);
	}

	public static double bountyScale(int wave) {
		return 1.0 + 0.04 * wave;
	}

	public static int clearBonus(int wave) {
		int bonus = 15 + 6 * wave;
		return isBossWave(wave) ? bonus * 2 : bonus;
	}
}

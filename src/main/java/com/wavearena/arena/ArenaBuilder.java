package com.wavearena.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;

import com.wavearena.WaveArena;
import com.wavearena.game.GameManager;
import com.wavearena.game.Text;

/**
 * Baut ein Kolosseum rund um einen Mittelpunkt:
 * gemusterter Kampfboden, Mauer mit acht Spawn-Toren, Tribünen, Wandteppiche und eine Glaskuppel.
 *
 * <p>Die Blöcke werden zuerst geplant und dann über mehrere Server-Ticks gesetzt, damit das Spiel nicht hängt.
 * Der Mittelpunkt ist der Block, auf dem man im Zentrum steht; die Spieler laufen bei y + 1.
 */
public final class ArenaBuilder {
	/** Radius der Kampffläche in Blöcken. */
	public static final int R = 20;
	public static final int TIERS = 7;
	public static final int WALL_H = 12;
	public static final int DOME_B = 18;
	/** Höhe über dem Boden, bis zu der die Arena reicht (Kuppelspitze). */
	public static final int TOP = WALL_H + DOME_B + 1;
	/** Äußerster Radius des Bauwerks. */
	public static final double OUTER = R + 2.5 + TIERS + 2.0;

	private static final double FLOOR_EDGE = R + 0.5;
	private static final double INNER_WALL_OUT = R + 2.5;
	private static final double STANDS_OUT = R + 2.5 + TIERS;
	private static final double DOME_A = OUTER + 0.5;
	private static final int HALF = (int) Math.ceil(DOME_A) + 1;
	private static final int BLOCKS_PER_TICK = 2500;

	private static final BlockState AIR = Blocks.AIR.defaultBlockState();

	public record Placement(BlockPos pos, BlockState state) {
	}

	private static Job current;

	private ArenaBuilder() {
	}

	// ------------------------------------------------------------------ Job-Verwaltung

	public static boolean isBuilding() {
		return current != null;
	}

	/** Startet den Bau. Gibt false zurück, wenn schon einer läuft. */
	public static boolean start(ServerLevel level, BlockPos center, boolean clear, ServerPlayer starter) {
		if (current != null) {
			return false;
		}
		List<Placement> plan = plan(level, center, clear);
		current = new Job(level, center, plan, starter == null ? null : starter.getUUID());
		return true;
	}

	/** Pro Server-Tick aufrufen. */
	public static void tick(MinecraftServer server) {
		if (current == null) {
			return;
		}
		if (current.step(server)) {
			current = null;
		}
	}

	private static final class Job {
		private final ServerLevel level;
		private final BlockPos center;
		private final List<Placement> plan;
		private final UUID starter;
		private int index;
		private int lastPercent = -1;

		Job(ServerLevel level, BlockPos center, List<Placement> plan, UUID starter) {
			this.level = level;
			this.center = center;
			this.plan = plan;
			this.starter = starter;
		}

		/** Gibt true zurück, wenn der Bau fertig ist. */
		boolean step(MinecraftServer server) {
			int end = Math.min(plan.size(), index + BLOCKS_PER_TICK);
			for (; index < end; index++) {
				Placement placement = plan.get(index);
				level.setBlock(placement.pos(), placement.state(), Block.UPDATE_CLIENTS);
			}

			ServerPlayer player = starter == null ? null : server.getPlayerList().getPlayer(starter);
			int percent = plan.isEmpty() ? 100 : (int) (index * 100L / plan.size());
			if (player != null && percent / 20 != lastPercent / 20 && percent < 100) {
				player.sendSystemMessage(Text.chat("Arena wird gebaut: " + percent + " %", ChatFormatting.GRAY));
			}
			lastPercent = percent;

			if (index < plan.size()) {
				return false;
			}

			GameManager.onArenaBuilt(level, center);
			if (player != null) {
				player.sendSystemMessage(Text.chat("Die Arena steht! Mit /wave start geht es los.", ChatFormatting.GREEN));
				player.teleportTo(level, center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 3.5, Set.of(), 0.0F, 0.0F, false);
			}
			WaveArena.LOGGER.info("Arena bei {} fertig gebaut ({} Blöcke).", center, plan.size());
			return true;
		}
	}

	// ------------------------------------------------------------------ Planung

	/** Gibt die Zahl der Blöcke zurück, die ein Neubau ungefähr braucht (für Rückmeldungen). */
	public static int estimateBlocks() {
		return 32000;
	}

	private static List<Placement> plan(ServerLevel level, BlockPos center, boolean clear) {
		List<Placement> out = new ArrayList<>(40000);
		int cx = center.getX();
		int cz = center.getZ();
		int y0 = center.getY();

		// 1) Platz freiräumen
		if (clear) {
			for (int dx = -HALF; dx <= HALF; dx++) {
				for (int dz = -HALF; dz <= HALF; dz++) {
					double d = Math.sqrt(dx * dx + dz * dz);
					if (d > DOME_A + 0.5) {
						continue;
					}
					for (int y = y0 + 1; y <= y0 + TOP; y++) {
						BlockPos pos = new BlockPos(cx + dx, y, cz + dz);
						if (!level.getBlockState(pos).isAir()) {
							out.add(new Placement(pos, AIR));
						}
					}
				}
			}
		}

		// 2) Boden, Mauern, Tribünen, Außenwand
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dz = -HALF; dz <= HALF; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > OUTER) {
					continue;
				}
				double ang = Math.atan2(dz, dx);
				int x = cx + dx;
				int z = cz + dz;

				// Fundament verhindert Löcher im Gelände.
				put(out, x, y0 - 1, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());

				if (d <= FLOOR_EDGE) {
					put(out, x, y0, z, floorBlock(dx, dz, d, ang));
				} else if (d <= INNER_WALL_OUT) {
					innerWall(out, x, y0, z, d, ang);
				} else if (d <= STANDS_OUT) {
					stands(out, x, y0, z, d, ang, dx, dz);
				} else {
					outerWall(out, x, y0, z, d, ang);
				}

				// Dach über der Kampffläche und der Mauer: überhängender Sims gegen kletternde Spinnen.
				if (d > R - 0.5 && d <= INNER_WALL_OUT) {
					put(out, x, y0 + 6, z, Blocks.BLACKSTONE.defaultBlockState());
				}
			}
		}

		// 3) Podest-Laternen um den Händler
		int[][] posts = {{3, 0}, {-3, 0}, {0, 3}, {0, -3}};
		for (int[] post : posts) {
			put(out, cx + post[0], y0 + 1, cz + post[1], Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
			put(out, cx + post[0], y0 + 2, cz + post[1], Blocks.SOUL_LANTERN.defaultBlockState());
		}

		// 4) Kuppel
		dome(out, cx, y0, cz);
		return out;
	}

	private static void put(List<Placement> out, int x, int y, int z, BlockState state) {
		out.add(new Placement(new BlockPos(x, y, z), state));
	}

	/** Seitlicher Abstand (in Blöcken) zur nächsten Linie eines Strahlenmusters. */
	private static double lateral(double d, double ang, double step, double offset) {
		double k = Math.rint((ang - offset) / step);
		double target = offset + k * step;
		return d * Math.abs(ang - target);
	}

	/** Acht Sektoren, an den Grenzen liegen die Pfeiler, in der Mitte die Tore. */
	private static int sector(double ang) {
		return (int) Math.floor((ang + Math.PI) / (Math.PI / 4)) % 8;
	}

	private static BlockState floorBlock(int dx, int dz, double d, double ang) {
		boolean checker = ((dx + dz) & 1) == 0;
		if (d <= 2.6) {
			return Blocks.GOLD_BLOCK.defaultBlockState();
		}
		if (d <= 3.6) {
			return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		}
		if (d <= 9.5) {
			return (checker ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES).defaultBlockState();
		}
		if (d <= 10.5) {
			return (checker ? Blocks.GILDED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE).defaultBlockState();
		}
		if (d <= 15.5) {
			boolean spoke = lateral(d, ang, Math.PI / 6, 0) <= 0.55;
			if (spoke) {
				return (checker ? Blocks.CRYING_OBSIDIAN : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
			}
			return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		}
		if (d <= 16.5) {
			boolean lamp = lateral(d, ang, Math.PI / 8, 0) <= 0.6;
			return (lamp ? Blocks.SEA_LANTERN : Blocks.BLACKSTONE).defaultBlockState();
		}
		if (d <= 19.5) {
			boolean spoke = lateral(d, ang, Math.PI / 4, Math.PI / 8) <= 0.6;
			if (spoke) {
				return Blocks.RED_NETHER_BRICKS.defaultBlockState();
			}
			return (checker ? Blocks.DEEPSLATE_TILES : Blocks.POLISHED_DEEPSLATE).defaultBlockState();
		}
		// Randring in abwechselnden Farben
		return (sector(ang) % 2 == 0 ? Blocks.GILDED_BLACKSTONE : Blocks.RED_NETHER_BRICKS).defaultBlockState();
	}

	private static void innerWall(List<Placement> out, int x, int y0, int z, double d, double ang) {
		boolean gateZone = lateral(d, ang, Math.PI / 4, Math.PI / 8) <= 1.6;
		boolean gateFrame = !gateZone && lateral(d, ang, Math.PI / 4, Math.PI / 8) <= 2.6;
		boolean frontLayer = d <= R + 1.5;
		boolean pilaster = lateral(d, ang, Math.PI / 4, 0) <= 0.9;

		put(out, x, y0, z, Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		for (int y = y0 + 1; y <= y0 + 4; y++) {
			BlockState state;
			if (gateZone && frontLayer) {
				state = AIR; // Nische, hier erscheinen die Gegner
			} else if (gateZone) {
				state = Blocks.RED_NETHER_BRICKS.defaultBlockState();
			} else if (gateFrame && frontLayer) {
				state = Blocks.CRYING_OBSIDIAN.defaultBlockState();
			} else if (pilaster) {
				state = (y % 2 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE).defaultBlockState();
			} else {
				state = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
			}
			put(out, x, y, z, state);
		}
		put(out, x, y0 + 5, z, Blocks.GILDED_BLACKSTONE.defaultBlockState());
		if (gateZone && frontLayer && lateral(d, ang, Math.PI / 4, Math.PI / 8) <= 0.6) {
			put(out, x, y0 + 4, z, Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
		}
	}

	private static void stands(List<Placement> out, int x, int y0, int z, double d, double ang, int dx, int dz) {
		int tier = Math.min(TIERS - 1, (int) Math.floor(d - INNER_WALL_OUT));
		int topY = y0 + 1 + tier;
		for (int y = y0; y < topY; y++) {
			put(out, x, y, z, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
		}

		double aisle = lateral(d, ang, Math.PI / 4, Math.PI / 8);
		if (aisle <= 1.0) {
			boolean lit = aisle <= 0.5 && (tier == 2 || tier == 5);
			put(out, x, topY, z, (lit ? Blocks.SEA_LANTERN : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState());
		} else {
			Block seat = sector(ang) % 2 == 0 ? Blocks.RED_NETHER_BRICK_STAIRS : Blocks.DEEPSLATE_TILE_STAIRS;
			put(out, x, topY, z, seat.defaultBlockState()
					.setValue(BlockStateProperties.HORIZONTAL_FACING, outward(dx, dz))
					.setValue(StairBlock.HALF, Half.BOTTOM));
		}

		// Laternenpfosten an den Sektorgrenzen
		boolean postRow = tier == 0 || tier == 3 || tier == 6;
		if (postRow && lateral(d, ang, Math.PI / 4, 0) <= 0.5) {
			put(out, x, topY + 1, z, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
			put(out, x, topY + 2, z, Blocks.POLISHED_BLACKSTONE_WALL.defaultBlockState());
			put(out, x, topY + 3, z, Blocks.SOUL_LANTERN.defaultBlockState());
		}
	}

	private static Direction outward(int dx, int dz) {
		if (Math.abs(dx) >= Math.abs(dz)) {
			return dx >= 0 ? Direction.EAST : Direction.WEST;
		}
		return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
	}

	private static void outerWall(List<Placement> out, int x, int y0, int z, double d, double ang) {
		boolean inner = d <= STANDS_OUT + 1.0;
		boolean pilaster = lateral(d, ang, Math.PI / 4, 0) <= 1.2;
		boolean tapestry = inner && lateral(d, ang, Math.PI / 4, Math.PI / 8) <= 1.6;
		boolean lamp = inner && lateral(d, ang, Math.PI / 8, 0) <= 0.6;

		for (int y = y0; y < y0 + WALL_H; y++) {
			int rel = y - y0;
			BlockState state = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
			if (pilaster) {
				state = ((rel == 4 || rel == 8) ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE_BRICKS).defaultBlockState();
			}
			if (tapestry && rel >= 3 && rel <= 10) {
				state = Blocks.RED_CONCRETE.defaultBlockState();
			}
			if (tapestry && (rel == 2 || rel == 11)) {
				state = Blocks.GOLD_BLOCK.defaultBlockState();
			}
			if (lamp && !tapestry && rel == 9) {
				state = Blocks.SEA_LANTERN.defaultBlockState();
			}
			put(out, x, y, z, state);
		}
		put(out, x, y0 + WALL_H, z, Blocks.GILDED_BLACKSTONE.defaultBlockState());
	}

	private static void dome(List<Placement> out, int cx, int y0, int cz) {
		double a2 = DOME_A * DOME_A;
		double b2 = (double) DOME_B * DOME_B;
		for (int dx = -HALF; dx <= HALF; dx++) {
			for (int dz = -HALF; dz <= HALF; dz++) {
				double d2 = dx * dx + dz * dz;
				double d = Math.sqrt(d2);
				if (d > DOME_A) {
					continue;
				}
				double ang = Math.atan2(dz, dx);
				boolean rib = lateral(d, ang, Math.PI / 8, 0) <= 0.9;
				for (int ry = 1; ry <= DOME_B; ry++) {
					double e = d2 / a2 + (double) (ry * ry) / b2;
					if (e > 1.0 || e <= 0.87) {
						continue;
					}
					BlockState state;
					if (d <= 2.5 && ry >= DOME_B - 2) {
						state = Blocks.SEA_LANTERN.defaultBlockState();
					} else if (ry % 6 == 0) {
						state = Blocks.GILDED_BLACKSTONE.defaultBlockState();
					} else if (rib) {
						state = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
					} else if (ry <= 5) {
						state = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
					} else if (ry <= 11) {
						state = Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState();
					} else {
						state = Blocks.GLASS.defaultBlockState();
					}
					put(out, cx + dx, y0 + WALL_H + ry, cz + dz, state);
				}
			}
		}
	}
}

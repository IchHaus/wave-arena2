package com.wavearena.command;

import java.util.function.Predicate;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import com.wavearena.arena.ArenaBuilder;
import com.wavearena.game.GameManager;
import com.wavearena.game.Text;

/** Der Befehl /wave und seine Unterbefehle. */
public final class ArenaCommands {
	private static final Predicate<CommandSourceStack> MODERATOR =
			source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR);

	private ArenaCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(Commands.literal("wave")
						.executes(ArenaCommands::help)
						.then(Commands.literal("help").executes(ArenaCommands::help))
						.then(Commands.literal("build").requires(MODERATOR)
								.executes(context -> build(context, true))
								.then(Commands.literal("keep").executes(context -> build(context, false))))
						.then(Commands.literal("setcenter").requires(MODERATOR).executes(ArenaCommands::setCenter))
						.then(Commands.literal("start")
								.executes(context -> start(context, 1))
								.then(Commands.argument("wave", IntegerArgumentType.integer(1, 200)).requires(MODERATOR)
										.executes(context -> start(context, IntegerArgumentType.getInteger(context, "wave")))))
						.then(Commands.literal("stop").executes(ArenaCommands::stop))
						.then(Commands.literal("join").executes(context -> player(context,
								(server, player) -> GameManager.join(server, player))))
						.then(Commands.literal("leave").executes(context -> player(context,
								(server, player) -> GameManager.leave(server, player))))
						.then(Commands.literal("ready").executes(context -> player(context,
								(server, player) -> GameManager.ready(player))))
						.then(Commands.literal("shop").executes(context -> player(context,
								(server, player) -> GameManager.openShop(player))))
						.then(Commands.literal("status").executes(ArenaCommands::status))
						.then(Commands.literal("skip").requires(MODERATOR).executes(context -> player(context,
								(server, player) -> GameManager.skip(server))))));
	}

	/** Führt eine Aktion für den ausführenden Spieler aus. Rückgabe der Aktion: Fehlertext oder null. */
	private static int player(CommandContext<CommandSourceStack> context, PlayerAction action) {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			source.sendFailure(Component.literal("Dieser Befehl geht nur für Spieler."));
			return 0;
		}
		String error = action.run(source.getServer(), player);
		if (error != null) {
			source.sendFailure(Component.literal(error));
			return 0;
		}
		return 1;
	}

	private interface PlayerAction {
		String run(MinecraftServer server, ServerPlayer player);
	}

	private static int help(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String[] lines = {
				"=== Wave Arena ===",
				"/wave build - baut die Arena an deiner Position (nur mit Cheats / OP)",
				"/wave build keep - baut sie, ohne das Gelände darin wegzuräumen",
				"/wave start - startet eine Partie (alle, die vorher /wave join getippt haben, sind dabei)",
				"/wave join - in der Pause in eine laufende Partie einsteigen",
				"/wave leave - Partie verlassen und dein Inventar zurückbekommen",
				"/wave shop - öffnet den Shop (nur in den Pausen)",
				"/wave ready - Pause überspringen, sobald alle bereit sind",
				"/wave status - zeigt Stand und Bestleistung",
				"/wave stop - beendet die Partie",
				"/wave skip - überspringt Pause oder Welle (nur Cheats / OP)",
				"/wave setcenter - merkt sich deine Position als Mitte einer schon gebauten Arena (nur Cheats / OP)",
		};
		for (String line : lines) {
			source.sendSuccess(() -> Text.line(line, ChatFormatting.GOLD), false);
		}
		return 1;
	}

	private static int build(CommandContext<CommandSourceStack> context, boolean clear) {
		return player(context, (server, player) -> {
			ServerLevel level = server.overworld();
			if (player.level() != level) {
				return "Die Arena kann nur in der Oberwelt gebaut werden.";
			}
			if (GameManager.phase() != GameManager.Phase.IDLE) {
				return "Beende zuerst die laufende Partie mit /wave stop.";
			}
			if (ArenaBuilder.isBuilding()) {
				return "Es wird schon gebaut.";
			}
			// Der Mittelpunkt ist der Boden unter den Füßen.
			BlockPos center = player.blockPosition().below();
			if (!ArenaBuilder.start(level, center, clear, player)) {
				return "Es wird schon gebaut.";
			}
			player.sendSystemMessage(Text.chat("Die Arena entsteht um dich herum (Durchmesser etwa 65 Blöcke). Bleib in der Nähe, das dauert nur ein paar Sekunden.",
					ChatFormatting.AQUA));
			return null;
		});
	}

	private static int setCenter(CommandContext<CommandSourceStack> context) {
		return player(context, (server, player) -> {
			if (GameManager.phase() != GameManager.Phase.IDLE) {
				return "Beende zuerst die laufende Partie mit /wave stop.";
			}
			if (player.level() != server.overworld()) {
				return "Der Mittelpunkt muss in der Oberwelt liegen.";
			}
			BlockPos center = player.blockPosition().below();
			GameManager.setCenter(server.overworld(), center);
			player.sendSystemMessage(Text.chat("Arena-Mitte gespeichert: " + center.getX() + " " + center.getY() + " " + center.getZ(),
					ChatFormatting.GREEN));
			return null;
		});
	}

	private static int start(CommandContext<CommandSourceStack> context, int firstWave) {
		return player(context, (server, player) -> GameManager.start(server, player, firstWave));
	}

	private static int stop(CommandContext<CommandSourceStack> context) {
		return player(context, (server, player) -> {
			boolean allowed = GameManager.data(player) != null || MODERATOR.test(context.getSource());
			if (!allowed) {
				return "Nur Mitspieler oder Moderatoren können die Partie beenden.";
			}
			return GameManager.stop(server);
		});
	}

	private static int status(CommandContext<CommandSourceStack> context) {
		return player(context, (server, player) -> {
			GameManager.status(player);
			return null;
		});
	}
}

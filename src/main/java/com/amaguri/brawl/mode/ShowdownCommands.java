package com.amaguri.brawl.mode;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import com.amaguri.brawl.arena.ArenaMap;
import com.amaguri.brawl.bot.BrawlBot;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.lobby.Lobby;

/** /brawl [pick <id> | start [ボット数] | stop | setlobby | lobby | bots <easy|normal|hard> | map] */
final class ShowdownCommands {
	private ShowdownCommands() {}

	static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> register(dispatcher));
	}

	private static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("brawl")
				.executes(ShowdownCommands::list)
				.then(Commands.literal("pick")
						.then(Commands.argument("id", StringArgumentType.word())
								.suggests((ctx, b) -> {
									for (BrawlerDef def : Brawlers.all()) b.suggest(def.id());
									return b.buildFuture();
								})
								.executes(ctx -> pick(ctx, StringArgumentType.getString(ctx, "id")))))
				.then(Commands.literal("start")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ctx -> start(ctx, null))
						.then(Commands.argument("bots", IntegerArgumentType.integer(0, Showdown.TOTAL - 1))
								.executes(ctx -> start(ctx, IntegerArgumentType.getInteger(ctx, "bots")))))
				.then(Commands.literal("stop")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ShowdownCommands::stop))
				.then(Commands.literal("setlobby")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ShowdownCommands::setLobby))
				.then(Commands.literal("lobby")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ShowdownCommands::buildLobby))
				.then(Commands.literal("bots")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.argument("level", StringArgumentType.word())
								.suggests((ctx, b) -> {
									b.suggest("easy");
									b.suggest("normal");
									b.suggest("hard");
									return b.buildFuture();
								})
								.executes(ctx -> bots(ctx, StringArgumentType.getString(ctx, "level")))))
				.then(Commands.literal("map")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.executes(ShowdownCommands::map)));
	}

	private static int list(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		ServerPlayer p = src.getPlayer();
		String cur = p == null ? null : Lobby.selected(p);
		MutableComponent msg = Msg.t("brawl.mode.list_head", "キャラを選んでください（クリックで選択）").withStyle(ChatFormatting.YELLOW);
		for (BrawlerDef def : Brawlers.all()) {
			MutableComponent btn = Component.literal("[" + def.nameJa() + "]")
					.withStyle(s -> s.withColor(def.id().equals(cur) ? ChatFormatting.GREEN : ChatFormatting.AQUA)
							.withClickEvent(new ClickEvent.RunCommand("/brawl pick " + def.id()))
							.withHoverEvent(new HoverEvent.ShowText(Component.literal(def.roleJa()))));
			msg.append("\n ").append(btn).append(Component.literal(" " + def.roleJa()).withStyle(ChatFormatting.GRAY));
		}
		msg.append("\n").append(Msg.t("brawl.mode.list_foot", "ロビーの像を右クリックしても選べます").withStyle(ChatFormatting.DARK_GRAY));
		src.sendSystemMessage(msg);
		return 1;
	}

	private static int pick(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
		CommandSourceStack src = ctx.getSource();
		ServerPlayer p = src.getPlayerOrException();
		BrawlerDef def = Brawlers.get(id);
		if (def == null) {
			src.sendFailure(Msg.t("brawl.mode.pick_unknown", "そのキャラはいません: %s", id));
			return 0;
		}
		if (!Lobby.pick(p, def.id())) {          // 保存・練習のキャラ切り替え・「〇〇を選んだ！」は Lobby が行う
			src.sendFailure(Msg.t("brawl.mode.pick_busy", "試合中はキャラを変えられません"));
			return 0;
		}
		return 1;
	}

	private static int start(CommandContext<CommandSourceStack> ctx, Integer bots) {
		CommandSourceStack src = ctx.getSource();
		String err = Showdown.start(src.getServer(), bots);
		if (err != null) {
			src.sendFailure(Component.literal(err));
			return 0;
		}
		return 1;
	}

	private static int stop(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		if (!Showdown.running()) {
			src.sendFailure(Msg.t("brawl.mode.not_running", "試合はしていません"));
			return 0;
		}
		Showdown.stop(src.getServer());
		src.sendSuccess(() -> Msg.t("brawl.mode.stopped", "試合を終了しました"), true);
		return 1;
	}

	private static int setLobby(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack src = ctx.getSource();
		ServerPlayer p = src.getPlayerOrException();
		Lobby.setSpawn(p);
		src.sendSuccess(() -> Msg.t("brawl.mode.lobby_set", "ロビーのスポーンをここに設定しました"), true);
		return 1;
	}

	private static int buildLobby(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		if (Showdown.running()) {
			src.sendFailure(Msg.t("brawl.mode.busy", "試合中はマップを建てられません"));
			return 0;
		}
		if (!Lobby.build(src.getServer())) {
			src.sendFailure(Msg.t("brawl.lobby.build_failed", "ロビーを建てられませんでした（ログを確認してください）"));
			return 0;
		}
		src.sendSuccess(() -> Msg.t("brawl.lobby.built", "ロビーを建てて、像とカカシを置き直しました"), true);
		return 1;
	}

	private static int bots(CommandContext<CommandSourceStack> ctx, String level) {
		CommandSourceStack src = ctx.getSource();
		if (!level.equals("easy") && !level.equals("normal") && !level.equals("hard")) {
			src.sendFailure(Msg.t("brawl.mode.bots_bad", "easy / normal / hard から選んでください"));
			return 0;
		}
		BrawlBot.difficulty = level;
		src.sendSuccess(() -> Msg.t("brawl.mode.bots_ok", "ボットの強さを %s にしました", level), true);
		return 1;
	}

	private static int map(CommandContext<CommandSourceStack> ctx) {
		CommandSourceStack src = ctx.getSource();
		if (Showdown.running()) {
			src.sendFailure(Msg.t("brawl.mode.busy", "試合中はマップを建てられません"));
			return 0;
		}
		ArenaMap.build(src.getServer().overworld());
		src.sendSuccess(() -> Msg.t("brawl.mode.map_built", "マップを建てました"), true);
		return 1;
	}
}

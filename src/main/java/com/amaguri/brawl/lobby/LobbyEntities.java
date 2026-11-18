package com.amaguri.brawl.lobby;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import com.amaguri.brawl.bot.BrawlBot;
import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;
import com.amaguri.brawl.core.BrawlState;
import com.amaguri.brawl.core.Combat;

/**
 * ロビーの実体（像5体・カカシ3体・文字）。どれも保存しない。
 * 二重に湧かないよう、置き直すとき（{@link #rebuild}）は「タグ brawl_lobby が付いた実体」を先に全部消す。
 */
final class LobbyEntities {
	private LobbyEntities() {}

	private static final Logger LOG = LoggerFactory.getLogger("brawl-lobby");

	static final String TAG = "brawl_lobby";
	static final String TAG_STATUE = "brawl_lobby_statue";
	static final String TAG_DUMMY = "brawl_lobby_dummy";
	/** カカシのキャラ（北→南）。 */
	static final String[] DUMMY_BRAWLERS = {"gouen", "kaden", "takomaru"};

	private static BrawlBot[] statues = new BrawlBot[0];
	private static BrawlBot[] dummies = new BrawlBot[0];
	private static LobbyText[] statueNames = new LobbyText[0];
	private static LobbyText padText, boardText, practiceText;

	static boolean isDummy(Entity e) {
		return e != null && e.entityTags().contains(TAG_DUMMY);
	}

	static boolean isStatue(Entity e) {
		return e != null && e.entityTags().contains(TAG_STATUE);
	}

	/** 像・カカシ・文字を全部消して置き直す。 */
	static void rebuild(ServerLevel lvl, LobbyLayout L) {
		purge(lvl, L);
		LobbyEntities.statues = new BrawlBot[L.statues().size()];
		LobbyEntities.statueNames = new LobbyText[L.statues().size()];
		LobbyEntities.dummies = new BrawlBot[L.dummies().size()];
		padText = null;
		boardText = null;
		practiceText = null;
		maintain(lvl, L);
	}

	/** タグ付きの実体を広場の範囲から全部消す（カカシは Combat からも外す）。 */
	static void purge(ServerLevel lvl, LobbyLayout L) {
		for (Entity e : lvl.getEntitiesOfClass(Entity.class, L.bounds(), x -> x.entityTags().contains(TAG))) {
			if (e instanceof LivingEntity le) {
				try {
					Combat.leave(le);
					Combat.clearDisplay(le);
				} catch (RuntimeException ex) {
					LOG.warn("実体の後始末に失敗", ex);
				}
			}
			e.discard();
		}
	}

	/** 試合が始まる前にカカシを消す（Combat.clearAll で状態が消えるので、残すと BrawlBot が自分で discard する）。 */
	static void removeDummies() {
		for (int k = 0; k < dummies.length; k++) {
			BrawlBot d = dummies[k];
			if (d != null && !d.isRemoved()) {
				try {
					Combat.leave(d);
				} catch (RuntimeException ex) {
					LOG.warn("カカシの leave に失敗", ex);
				}
				d.discard();
			}
			dummies[k] = null;
		}
	}

	/** 足りない実体を作り、ずれた実体を元の場所に戻す。試合中は呼ばない。 */
	static void maintain(ServerLevel lvl, LobbyLayout L) {
		List<BrawlerDef> defs = Brawlers.all();
		// 像
		for (int k = 0; k < statues.length && k < defs.size(); k++) {
			Vec3 p = L.statues().get(k);
			BrawlerDef def = defs.get(k);
			if (statues[k] == null || statues[k].isRemoved()) statues[k] = spawnStatue(lvl, p, def);
			else hold(statues[k], p, 0f);
			if (statueNames[k] == null || statueNames[k].isRemoved()) {
				MutableComponent name = Component.literal(def.nameJa()).withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
				name.append(Component.literal("\n" + def.roleJa()).withStyle(ChatFormatting.WHITE));
				statueNames[k] = LobbyText.spawn(lvl, p.add(0, 2.35, 0), name, 1.4f, 160, TAG);
			}
		}
		// カカシ
		for (int k = 0; k < dummies.length; k++) {
			Vec3 p = L.dummies().get(k);
			String id = DUMMY_BRAWLERS[k % DUMMY_BRAWLERS.length];
			BrawlBot d = dummies[k];
			if (d == null || d.isRemoved() || Combat.state(d) == null) {
				if (d != null && !d.isRemoved()) d.discard();
				dummies[k] = spawnDummy(lvl, p, id);
			} else {
				hold(d, p, -90f);
			}
		}
		// 文字
		if (padText == null || padText.isRemoved()) {
			MutableComponent t = Component.translatableWithFallback("brawl.lobby.pad", "プレイ").withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW);
			t.append(Component.translatableWithFallback("brawl.lobby.pad_sub", "\n全員が乗ると開始").withStyle(ChatFormatting.WHITE));
			padText = LobbyText.spawn(lvl, L.padText(), t, 2.4f, 200, TAG);
		}
		if (practiceText == null || practiceText.isRemoved()) {
			MutableComponent t = Component.translatableWithFallback("brawl.lobby.practice", "練習場").withStyle(ChatFormatting.BOLD, ChatFormatting.GREEN);
			t.append(Component.translatableWithFallback("brawl.lobby.practice_sub", "\nカカシを撃とう").withStyle(ChatFormatting.WHITE));
			practiceText = LobbyText.spawn(lvl, L.practiceText(), t, 1.6f, 200, TAG);
		}
		if (boardText == null || boardText.isRemoved()) {
			boardText = LobbyText.spawn(lvl, L.boardText(), boardComponent(), 1.3f, 200, TAG);
		}
	}

	/** トロフィー掲示板の文字を最新にする。 */
	static void refreshBoard() {
		if (boardText != null && !boardText.isRemoved()) boardText.setLabel(boardComponent());
	}

	private static Component boardComponent() {
		MutableComponent t = Component.translatableWithFallback("brawl.lobby.board_title", "トロフィー ランキング").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
		List<Stats.Entry> top = Stats.top(5);
		ChatFormatting[] cols = {ChatFormatting.YELLOW, ChatFormatting.WHITE, ChatFormatting.GOLD, ChatFormatting.AQUA, ChatFormatting.AQUA};
		for (int i = 0; i < 5; i++) {
			if (i < top.size()) {
				Stats.Entry e = top.get(i);
				t.append(Component.literal("\n" + (i + 1) + "位  " + e.name + "  ★" + e.trophies).withStyle(cols[i]));
			} else {
				t.append(Component.literal("\n" + (i + 1) + "位  ---").withStyle(ChatFormatting.GRAY));
			}
		}
		return t;
	}

	private static BrawlBot spawnStatue(ServerLevel lvl, Vec3 p, BrawlerDef def) {
		BrawlBot b = BrawlBot.spawn(lvl, p, def.id(), def.nameJa());
		if (b == null) return null;
		try {
			Combat.leave(b);                         // 参加者にしない
			Combat.setDisplay(b, def.id());          // 見た目だけ
			b.setNoAi(true);
			b.setInvulnerable(true);
			b.setNoGravity(true);
			b.setSilent(true);
			b.addTag(TAG);
			b.addTag(TAG_STATUE);
			hold(b, p, 0f);                          // 南（広場の入口側）を向く
		} catch (RuntimeException ex) {
			LOG.warn("像の設定に失敗", ex);
		}
		return b;
	}

	private static BrawlBot spawnDummy(ServerLevel lvl, Vec3 p, String brawlerId) {
		BrawlBot b = BrawlBot.spawn(lvl, p, brawlerId, "カカシ");   // spawn の中で Combat.join 済み（HPは自然回復で戻る）
		if (b == null) return null;
		try {
			b.setNoAi(true);
			b.setInvulnerable(true);                 // バニラのダメージは無効（HPは Combat 側）
			b.setNoGravity(true);
			b.setSilent(true);
			b.addTag(TAG);
			b.addTag(TAG_DUMMY);
			hold(b, p, -90f);                        // 東（撃つ側）を向く
		} catch (RuntimeException ex) {
			LOG.warn("カカシの設定に失敗", ex);
		}
		return b;
	}

	/** 位置と向きを固定する（押されたり、ノックバックでずれたりしたら戻す）。 */
	private static void hold(BrawlBot b, Vec3 p, float yaw) {
		if (b == null || b.isRemoved()) return;
		double dx = b.getX() - p.x, dy = b.getY() - p.y, dz = b.getZ() - p.z;
		if (dx * dx + dy * dy + dz * dz > 0.01 || Math.abs(b.getYRot() - yaw) > 1f) {
			b.snapTo(p.x, p.y, p.z, yaw, 0f);
			b.setYHeadRot(yaw);
			b.setYBodyRot(yaw);
		}
		b.setDeltaMovement(Vec3.ZERO);
	}

	/** 練習場のカカシが倒れたら、すぐ全回復して立て直す。 */
	static void reviveDummy(LivingEntity victim) {
		BrawlState st = Combat.state(victim);
		if (st == null) return;
		st.hp = st.maxHp();
		st.alive = true;
		if (victim.level() instanceof ServerLevel sl) {
			sl.sendParticles(ParticleTypes.POOF, victim.getX(), victim.getY() + 1.0, victim.getZ(), 10, 0.3, 0.5, 0.3, 0.03);
		}
	}
}

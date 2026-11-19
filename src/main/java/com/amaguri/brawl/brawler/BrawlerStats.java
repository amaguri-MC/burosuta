package com.amaguri.brawl.brawler;

import java.util.List;

/**
 * 11キャラ＋相棒の数値の唯一の置き場所（ブロスタ準拠）。距離はタイル（使う側で Tiles.b() によりブロックへ）、
 * 弾速はブロック/tick、角度は度、時間は tick（20tick=1秒）。
 */
public final class BrawlerStats {
	private BrawlerStats() {}

	/** パワーレベル。パワー1から1上がるごとに基礎値の +10%（パワー11 = 2倍）。HP・ダメージ・回復量に効く。 */
	public static final int POWER = 11;
	/** 1 + 0.1 * (POWER - 1) */
	public static final double POWER_MUL = 1.0 + 0.1 * (POWER - 1);

	/** パワー1の基礎値 → 現在のパワーの値。数値表（stats.md）の値をそのまま渡す。 */
	public static int p(int base) {
		return (int) Math.round(base * POWER_MUL);
	}

	static final int MAX_AMMO = 3;
	static final int ATTACK_COOLDOWN_TICKS = 8;

	// ---- ラスティ ----
	static final int RUSTY_HP = p(3900);
	static final double RUSTY_SPEED = 2.57; // wiki: 770（2023/8/15）
	static final double RUSTY_RELOAD = 1.5;
	static final float RUSTY_SUPER_PER_HIT = 0.067f;
	static final int RUSTY_PELLETS = 5, RUSTY_DMG = p(300);
	static final double RUSTY_RANGE = 7.67, RUSTY_FAN = 30, RUSTY_SPEED_BPT = 1.0, RUSTY_PELLET_RADIUS = 0.35;
	static final int RUSTY_SUPER_PELLETS = 9, RUSTY_SUPER_DMG = p(320);
	static final double RUSTY_SUPER_RANGE = 7.67, RUSTY_SUPER_FAN = 40, RUSTY_SUPER_KNOCK_TILES = 3;

	// ---- ゴウエン ----
	static final int GOUEN_HP = p(5000);
	static final double GOUEN_SPEED = 2.57;
	static final double GOUEN_RELOAD = 1.6;
	static final float GOUEN_SUPER_PER_HIT = 0.1f;
	static final int GOUEN_PELLETS = 5, GOUEN_DMG = p(400);
	static final double GOUEN_RANGE = 5.33, GOUEN_FAN = 25, GOUEN_SPEED_BPT = 1.0, GOUEN_PELLET_RADIUS = 0.4;
	static final double GOUEN_SUPER_DASH_TILES = 10;
	static final int GOUEN_SUPER_DASH_TICKS = 12;           // 約0.6秒
	static final int GOUEN_SUPER_DMG = p(800);
	static final double GOUEN_SUPER_KNOCK_TILES = 1.5;      // 押し出し（数値指定なしのため仮）
	static final double GOUEN_SUPER_HIT_RADIUS_BLOCKS = 1.4;

	// ---- ミモザ ----
	static final int MIMOSA_HP = p(2400);
	static final double MIMOSA_SPEED = 2.4;
	static final double MIMOSA_RELOAD = 2.1;
	static final float MIMOSA_SUPER_PER_HIT = 0.25f;
	static final int MIMOSA_DMG = p(1180);
	static final double MIMOSA_RANGE = 9.67, MIMOSA_SPEED_BPT = 0.8, MIMOSA_ROCKET_RADIUS = 0.4, MIMOSA_BLAST_TILES = 1;
	static final int MIMOSA_SUPER_ROCKETS = 9, MIMOSA_SUPER_DMG = p(1180), MIMOSA_SUPER_INTERVAL_TICKS = 3, MIMOSA_SUPER_FALL_TICKS = 8;
	static final double MIMOSA_SUPER_CENTER_TILES = 7.67, MIMOSA_SUPER_AREA_TILES = 3;

	// ---- フランク（アイアンゴーレム）。HP7000・1680・射程6・溜め1.3秒・スタン2秒・射程7 は stats.md。速さ・ダメ・リロード・ゲージは推定 ----
	static final int FRANK_HP = p(7000);
	static final double FRANK_SPEED = 2.4;   // 推定
	static final double FRANK_RELOAD = 1.6;  // 推定
	static final float FRANK_SUPER_PER_HIT = 0.34f; // 推定
	static final int FRANK_DMG = p(1680);       // 推定
	static final double FRANK_RANGE = 6, FRANK_FAN = 50, FRANK_BULLET_SPEED_BPT = 1.4, FRANK_BULLET_RADIUS = 0.6;
	static final int FRANK_CHARGE_TICKS = 26; // 1.3秒（動けない）
	static final double FRANK_SUPER_RANGE = 7;
	static final int FRANK_SUPER_DMG = p(1680), FRANK_SUPER_STUN_TICKS = 40; // 推定（ダメージ）

	// ---- ダリル（カッパーゴーレム）。HP5000・速さ・240×5を2回(0.25秒)・リロード1.8・樽7タイル400・自動30秒 は stats.md。射程6は推定 ----
	static final int DARRYL_HP = p(5000);
	static final double DARRYL_SPEED = 2.57;
	static final double DARRYL_RELOAD = 1.8;
	static final float DARRYL_SUPER_PER_HIT = 0.02f;
	static final float DARRYL_AUTO_SUPER_PER_TICK = 1f / (30 * 20);
	static final int DARRYL_PELLETS = 5, DARRYL_DMG = p(240), DARRYL_VOLLEYS = 2, DARRYL_VOLLEY_INTERVAL_TICKS = 5;
	static final double DARRYL_RANGE = 6, DARRYL_FAN = 25, DARRYL_SPEED_BPT = 1.0, DARRYL_PELLET_RADIUS = 0.4; // 射程は推定
	static final double DARRYL_SUPER_DASH_TILES = 7;
	static final int DARRYL_SUPER_DASH_TICKS = 12, DARRYL_SUPER_DMG = p(400);
	static final double DARRYL_SUPER_KNOCK_TILES = 1.5, DARRYL_SUPER_HIT_RADIUS_BLOCKS = 1.4; // 押し出しの距離は推定

	// ---- ポコ（アレイ）。HP4000・速さ・700・射程7・扇45°・リロード1.6・回復2100 は stats.md。ゲージ・超音波の扇は推定 ----
	static final int POCO_HP = p(4000);
	static final double POCO_SPEED = 2.4;
	static final double POCO_RELOAD = 1.6;
	static final float POCO_SUPER_PER_HIT = 0.2f; // 推定
	static final int POCO_DMG = p(700);
	static final double POCO_RANGE = 7, POCO_FAN = 45, POCO_WAVE_SPEED_BPT = 1.1, POCO_WAVE_RADIUS = 0.6;
	static final int POCO_SUPER_HEAL = p(2100), POCO_SUPER_DMG = p(700); // 超音波のダメージは推定（通常と同じ）
	static final double POCO_SUPER_RANGE = 7.67, POCO_SUPER_FAN = 70; // 推定

	// ---- モーティス（行商人）。HP3800・速さ・突進2.67タイル900・リロード2.4・コウモリ射程10・900 は stats.md。回復900・ゲージは推定 ----
	static final int MORTIS_HP = p(3800);
	static final double MORTIS_SPEED = 2.73;
	static final double MORTIS_RELOAD = 2.4;
	static final float MORTIS_SUPER_PER_HIT = 0.25f; // 推定
	static final double MORTIS_DASH_TILES = 2.67, MORTIS_DASH_HIT_RADIUS_BLOCKS = 1.3;
	static final int MORTIS_DASH_TICKS = 4, MORTIS_DMG = p(900);
	static final double MORTIS_SUPER_RANGE = 10, MORTIS_SUPER_FAN = 50, MORTIS_BAT_SPEED_BPT = 1.1, MORTIS_BAT_RADIUS = 0.7;
	static final int MORTIS_SUPER_DMG = p(900), MORTIS_SUPER_HEAL_PER_HIT = p(900); // 回復は推定

	// ---- エドガー（ウィザースケルトン）。HP2800・2連パンチ・跳躍7タイル・2.5秒・毎秒3.333%・速さ以外の多くは推定 ----
	static final int EDGAR_HP = p(2800);
	static final double EDGAR_SPEED = 2.73;   // 推定
	static final double EDGAR_RELOAD = 0.5;   // 推定
	static final float EDGAR_SUPER_PER_HIT = 0.05f; // 推定
	static final float EDGAR_AUTO_SUPER_PER_TICK = 1f / (30 * 20);
	static final int EDGAR_DMG = p(360), EDGAR_PUNCHES = 2, EDGAR_PUNCH_INTERVAL_TICKS = 4; // 360は推定
	static final double EDGAR_RANGE = 1.67, EDGAR_PUNCH_SPEED_BPT = 1.0, EDGAR_PUNCH_RADIUS = 0.9; // 射程は推定
	static final float EDGAR_LIFESTEAL = 0.25f;
	static final double EDGAR_SUPER_LEAP_TILES = 7, EDGAR_SUPER_HEIGHT_BLOCKS = 3.5;
	static final int EDGAR_SUPER_FLIGHT_TICKS = 14, EDGAR_HASTE_TICKS = 50;
	static final double EDGAR_HASTE_TILES_PER_SEC = 0.67;

	// ---- ルー（スノーゴーレム）。HP3100・雪玉3連400・凍結14.3%・氷の床 半径3射程8 は stats.md。速さ・射程9・リロード・5秒・ゲージは推定 ----
	static final int LOU_HP = p(3100);
	static final double LOU_SPEED = 2.4;   // 推定
	static final double LOU_RELOAD = 1.7;  // 推定
	static final float LOU_SUPER_PER_HIT = 0.1f; // 推定
	static final int LOU_DMG = p(400), LOU_BALLS = 3, LOU_BALL_INTERVAL_TICKS = 3;
	static final double LOU_RANGE = 9, LOU_BALL_SPEED_BPT = 1.1, LOU_BALL_RADIUS = 0.4; // 射程は推定
	static final float LOU_FROST_PER_HIT = 14.3f;
	static final double LOU_SUPER_RANGE = 8, LOU_FLOOR_RADIUS_TILES = 3;
	static final int LOU_FLOOR_TICKS = 100; // 5秒は推定
	static final int LOU_SLOW_TICKS = 30, LOU_STOP_TICKS = 6; // 0.3秒止まる
	static final double LOU_SLOW_MUL = 0.5; // 推定

	// ---- ニタ（シロクマ）。HP・速さ・ダメージ・リロード・ゲージは wiki の更新履歴の値。射程・幅は UNVERIFIED（stats.md 参照）----
	static final int NITA_HP = p(4000);
	static final double NITA_SPEED = 2.4;
	static final double NITA_RELOAD = 1.1;
	static final float NITA_SUPER_PER_HIT = 1f / 6f;
	static final int NITA_DMG = p(880);
	static final double NITA_RANGE = 5.5, NITA_WAVE_SPEED_BPT = 0.9, NITA_WAVE_RADIUS = 0.9; // UNVERIFIED
	static final double NITA_SUPER_RANGE = 6.0; // UNVERIFIED: 相棒を呼ぶ最大距離（タイル）

	// ---- 相棒のクマ（nitabear）。HP=ニタと同じ4000、速さ610/300、攻撃は UNVERIFIED ----
	public static final int BEAR_HP = p(4000);
	public static final double BEAR_SPEED = 610.0 / 300.0;
	public static final int BEAR_DMG = p(600), BEAR_BITE_INTERVAL_TICKS = 15; // UNVERIFIED
	public static final double BEAR_REACH_BLOCKS = 0.9, BEAR_SIGHT_BLOCKS = 40.0;

	// ---- バーリー（ウィッチ）。速さ・水たまり半径2タイル・時間2秒/4秒・680/秒・5本は wiki。HP・射程・リロード・ゲージは UNVERIFIED ----
	static final int BARLEY_HP = p(2400);
	static final double BARLEY_SPEED = 2.4;
	static final double BARLEY_RELOAD = 2.1;
	static final float BARLEY_SUPER_PER_HIT = 0.1f;
	static final double BARLEY_RANGE = 7.67;
	static final int BARLEY_LOB_TICKS = 14;
	static final double BARLEY_PUDDLE_RADIUS_TILES = 2;
	static final int BARLEY_PUDDLE_TICKS = 30, BARLEY_SUPER_PUDDLE_TICKS = 80;
	static final int BARLEY_DPS = p(680), BARLEY_TICK_INTERVAL = 10; // 10tickごとに DPS/2
	static final int BARLEY_SUPER_BOTTLES = 5, BARLEY_SUPER_INTERVAL_TICKS = 5;
	static final double BARLEY_SUPER_CENTER_TILES = 7.0, BARLEY_SUPER_AREA_TILES = 3.0; // UNVERIFIED

	/** 自動の必殺チャージ（1tickあたり）。無いキャラは 0。 */
	static float autoSuperPerTick(String id) {
		return switch (id) {
			case "darryl" -> DARRYL_AUTO_SUPER_PER_TICK;
			case "edgar" -> EDGAR_AUTO_SUPER_PER_TICK;
			default -> 0f;
		};
	}

	/** 与えたダメージのうち回復する割合。 */
	static float lifesteal(String id) {
		return "edgar".equals(id) ? EDGAR_LIFESTEAL : 0f;
	}

	static BrawlerDef bearDef() {
		return new BrawlerDef("nitabear", "シロクマの相棒", BEAR_HP, BEAR_SPEED, MAX_AMMO, 1.0, ATTACK_COOLDOWN_TICKS,
				BEAR_REACH_BLOCKS / 2.0, 0, 0f, "相棒");
	}

	static List<BrawlerDef> defs() {
		return List.of(
				new BrawlerDef("rusty", "襲撃者（アタッカー）", RUSTY_HP, RUSTY_SPEED, MAX_AMMO, RUSTY_RELOAD, ATTACK_COOLDOWN_TICKS,
						RUSTY_RANGE, RUSTY_SUPER_RANGE, RUSTY_SUPER_PER_HIT, "アタッカー"),
				new BrawlerDef("gouen", "ウシ（タンク）", GOUEN_HP, GOUEN_SPEED, MAX_AMMO, GOUEN_RELOAD, ATTACK_COOLDOWN_TICKS,
						GOUEN_RANGE, GOUEN_SUPER_DASH_TILES, GOUEN_SUPER_PER_HIT, "タンク"),
				new BrawlerDef("mimosa", "ブレイズ（スナイパー）", MIMOSA_HP, MIMOSA_SPEED, MAX_AMMO, MIMOSA_RELOAD, ATTACK_COOLDOWN_TICKS,
						MIMOSA_RANGE, MIMOSA_SUPER_CENTER_TILES, MIMOSA_SUPER_PER_HIT, "スナイパー"),
				new BrawlerDef("frank", "アイアンゴーレム（タンク）", FRANK_HP, FRANK_SPEED, MAX_AMMO, FRANK_RELOAD, ATTACK_COOLDOWN_TICKS,
						FRANK_RANGE, FRANK_SUPER_RANGE, FRANK_SUPER_PER_HIT, "タンク"),
				new BrawlerDef("darryl", "カッパーゴーレム（タンク）", DARRYL_HP, DARRYL_SPEED, MAX_AMMO, DARRYL_RELOAD, ATTACK_COOLDOWN_TICKS,
						DARRYL_RANGE, DARRYL_SUPER_DASH_TILES, DARRYL_SUPER_PER_HIT, "タンク"),
				new BrawlerDef("poco", "アレイ（サポート）", POCO_HP, POCO_SPEED, MAX_AMMO, POCO_RELOAD, ATTACK_COOLDOWN_TICKS,
						POCO_RANGE, POCO_SUPER_RANGE, POCO_SUPER_PER_HIT, "サポート"),
				new BrawlerDef("mortis", "行商人（アサシン）", MORTIS_HP, MORTIS_SPEED, MAX_AMMO, MORTIS_RELOAD, ATTACK_COOLDOWN_TICKS,
						MORTIS_DASH_TILES, MORTIS_SUPER_RANGE, MORTIS_SUPER_PER_HIT, "アサシン"),
				new BrawlerDef("edgar", "ウィザースケルトン（アサシン）", EDGAR_HP, EDGAR_SPEED, MAX_AMMO, EDGAR_RELOAD, ATTACK_COOLDOWN_TICKS,
						EDGAR_RANGE, EDGAR_SUPER_LEAP_TILES, EDGAR_SUPER_PER_HIT, "アサシン"),
				new BrawlerDef("lou", "スノーゴーレム（コントローラー）", LOU_HP, LOU_SPEED, MAX_AMMO, LOU_RELOAD, ATTACK_COOLDOWN_TICKS,
						LOU_RANGE, LOU_SUPER_RANGE, LOU_SUPER_PER_HIT, "コントローラー"),
				new BrawlerDef("nita", "シロクマ（アタッカー）", NITA_HP, NITA_SPEED, MAX_AMMO, NITA_RELOAD, ATTACK_COOLDOWN_TICKS,
						NITA_RANGE, NITA_SUPER_RANGE, NITA_SUPER_PER_HIT, "アタッカー"),
				new BrawlerDef("barley", "ウィッチ（グレネーディア）", BARLEY_HP, BARLEY_SPEED, MAX_AMMO, BARLEY_RELOAD, ATTACK_COOLDOWN_TICKS,
						BARLEY_RANGE, BARLEY_SUPER_CENTER_TILES, BARLEY_SUPER_PER_HIT, "グレネーディア"));
	}
}

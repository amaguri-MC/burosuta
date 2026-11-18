package com.amaguri.brawl.brawler;

import java.util.List;

/**
 * 7キャラ＋相棒の数値の唯一の置き場所（ブロスタ準拠）。距離はタイル（使う側で Tiles.b() によりブロックへ）、
 * 弾速はブロック/tick、角度は度、時間は tick（20tick=1秒）。
 */
public final class BrawlerStats {
	private BrawlerStats() {}

	static final int MAX_AMMO = 3;
	static final int ATTACK_COOLDOWN_TICKS = 8;

	// ---- ラスティ ----
	static final int RUSTY_HP = 3900;
	static final double RUSTY_SPEED = 2.57;
	static final double RUSTY_RELOAD = 1.5;
	static final float RUSTY_SUPER_PER_HIT = 0.067f;
	static final int RUSTY_PELLETS = 5, RUSTY_DMG = 300;
	static final double RUSTY_RANGE = 7.67, RUSTY_FAN = 30, RUSTY_SPEED_BPT = 1.0, RUSTY_PELLET_RADIUS = 0.35;
	static final int RUSTY_SUPER_PELLETS = 9, RUSTY_SUPER_DMG = 320;
	static final double RUSTY_SUPER_RANGE = 7.67, RUSTY_SUPER_FAN = 40, RUSTY_SUPER_KNOCK_TILES = 3;

	// ---- ケイデン ----
	static final int KADEN_HP = 2800;
	static final double KADEN_SPEED = 2.4;
	static final double KADEN_RELOAD = 1.6;
	static final float KADEN_SUPER_PER_HIT = 0.042f;
	static final int KADEN_BULLETS = 6, KADEN_DMG = 360, KADEN_BULLET_INTERVAL_TICKS = 2;
	static final double KADEN_RANGE = 9.33, KADEN_SPEED_BPT = 1.2, KADEN_SCATTER_DEG = 2, KADEN_BULLET_RADIUS = 0.3;
	static final int KADEN_SUPER_BULLETS = 12, KADEN_SUPER_DMG = 360;
	static final double KADEN_SUPER_RANGE = 12.67;

	// ---- ゴウエン ----
	static final int GOUEN_HP = 5000;
	static final double GOUEN_SPEED = 2.57;
	static final double GOUEN_RELOAD = 1.6;
	static final float GOUEN_SUPER_PER_HIT = 0.0625f;
	static final int GOUEN_PELLETS = 5, GOUEN_DMG = 400;
	static final double GOUEN_RANGE = 5.33, GOUEN_FAN = 25, GOUEN_SPEED_BPT = 1.0, GOUEN_PELLET_RADIUS = 0.4;
	static final double GOUEN_SUPER_DASH_TILES = 10;
	static final int GOUEN_SUPER_DASH_TICKS = 12;           // 約0.6秒
	static final int GOUEN_SUPER_DMG = 800;
	static final double GOUEN_SUPER_KNOCK_TILES = 1.5;      // 押し出し（数値指定なしのため仮）
	static final double GOUEN_SUPER_HIT_RADIUS_BLOCKS = 1.4;

	// ---- ミモザ ----
	static final int MIMOSA_HP = 2400;
	static final double MIMOSA_SPEED = 2.4;
	static final double MIMOSA_RELOAD = 2.1;
	static final float MIMOSA_SUPER_PER_HIT = 0.25f;
	static final int MIMOSA_DMG = 1040;
	static final double MIMOSA_RANGE = 9.67, MIMOSA_SPEED_BPT = 0.8, MIMOSA_ROCKET_RADIUS = 0.4, MIMOSA_BLAST_TILES = 1;
	static final int MIMOSA_SUPER_ROCKETS = 9, MIMOSA_SUPER_DMG = 1040, MIMOSA_SUPER_INTERVAL_TICKS = 3, MIMOSA_SUPER_FALL_TICKS = 8;
	static final double MIMOSA_SUPER_CENTER_TILES = 7.67, MIMOSA_SUPER_AREA_TILES = 3;

	// ---- タコまる ----
	static final int TAKOMARU_HP = 6000;
	static final double TAKOMARU_SPEED = 2.57;
	static final double TAKOMARU_RELOAD = 1.1;
	static final float TAKOMARU_SUPER_PER_HIT = 0.0625f;
	static final int TAKOMARU_PUNCHES = 4, TAKOMARU_DMG = 380;
	static final double TAKOMARU_RANGE = 3, TAKOMARU_SPEED_BPT = 0.9, TAKOMARU_FAN = 40, TAKOMARU_PUNCH_RADIUS = 0.6;
	static final double TAKOMARU_SUPER_JUMP_TILES = 6, TAKOMARU_SUPER_RADIUS_TILES = 1.5, TAKOMARU_SUPER_KNOCK_TILES = 1.5;
	static final int TAKOMARU_SUPER_DMG = 800, TAKOMARU_SUPER_FLIGHT_TICKS = 16;
	static final double TAKOMARU_SUPER_JUMP_HEIGHT_BLOCKS = 3.5;

	// ---- ニタ（シロクマ）。HP・速さ・ダメージ・リロード・ゲージは wiki の更新履歴の値。射程・幅は UNVERIFIED（stats.md 参照）----
	static final int NITA_HP = 4000;
	static final double NITA_SPEED = 2.4;
	static final double NITA_RELOAD = 1.1;
	static final float NITA_SUPER_PER_HIT = 1f / 6f;
	static final int NITA_DMG = 880;
	static final double NITA_RANGE = 5.5, NITA_WAVE_SPEED_BPT = 0.9, NITA_WAVE_RADIUS = 0.9; // UNVERIFIED
	static final double NITA_SUPER_RANGE = 6.0; // UNVERIFIED: 相棒を呼ぶ最大距離（タイル）

	// ---- 相棒のクマ（nitabear）。HP=ニタと同じ4000、速さ610/300、攻撃は UNVERIFIED ----
	public static final int BEAR_HP = 4000;
	public static final double BEAR_SPEED = 610.0 / 300.0;
	public static final int BEAR_DMG = 600, BEAR_BITE_INTERVAL_TICKS = 15; // UNVERIFIED
	public static final double BEAR_REACH_BLOCKS = 0.9, BEAR_SIGHT_BLOCKS = 40.0;

	// ---- バーリー（ウィッチ）。速さ・水たまり半径2タイル・時間2秒/4秒・680/秒・5本は wiki。HP・射程・リロード・ゲージは UNVERIFIED ----
	static final int BARLEY_HP = 2400;
	static final double BARLEY_SPEED = 2.4;
	static final double BARLEY_RELOAD = 2.0;
	static final float BARLEY_SUPER_PER_HIT = 0.1f;
	static final double BARLEY_RANGE = 8.67;
	static final int BARLEY_LOB_TICKS = 14;
	static final double BARLEY_PUDDLE_RADIUS_TILES = 2;
	static final int BARLEY_PUDDLE_TICKS = 40, BARLEY_SUPER_PUDDLE_TICKS = 80;
	static final int BARLEY_DPS = 680, BARLEY_TICK_INTERVAL = 10; // 10tickごとに DPS/2
	static final int BARLEY_SUPER_BOTTLES = 5, BARLEY_SUPER_INTERVAL_TICKS = 4;
	static final double BARLEY_SUPER_CENTER_TILES = 7.0, BARLEY_SUPER_AREA_TILES = 3.0; // UNVERIFIED

	static BrawlerDef bearDef() {
		return new BrawlerDef("nitabear", "シロクマの相棒", BEAR_HP, BEAR_SPEED, MAX_AMMO, 1.0, ATTACK_COOLDOWN_TICKS,
				BEAR_REACH_BLOCKS / 2.0, 0, 0f, "相棒");
	}

	static List<BrawlerDef> defs() {
		return List.of(
				new BrawlerDef("rusty", "襲撃者（アタッカー）", RUSTY_HP, RUSTY_SPEED, MAX_AMMO, RUSTY_RELOAD, ATTACK_COOLDOWN_TICKS,
						RUSTY_RANGE, RUSTY_SUPER_RANGE, RUSTY_SUPER_PER_HIT, "アタッカー"),
				new BrawlerDef("kaden", "スケルトン（アタッカー）", KADEN_HP, KADEN_SPEED, MAX_AMMO, KADEN_RELOAD, ATTACK_COOLDOWN_TICKS,
						KADEN_RANGE, KADEN_SUPER_RANGE, KADEN_SUPER_PER_HIT, "アタッカー"),
				new BrawlerDef("gouen", "ホグリン（タンク）", GOUEN_HP, GOUEN_SPEED, MAX_AMMO, GOUEN_RELOAD, ATTACK_COOLDOWN_TICKS,
						GOUEN_RANGE, GOUEN_SUPER_DASH_TILES, GOUEN_SUPER_PER_HIT, "タンク"),
				new BrawlerDef("mimosa", "ブレイズ（スナイパー）", MIMOSA_HP, MIMOSA_SPEED, MAX_AMMO, MIMOSA_RELOAD, ATTACK_COOLDOWN_TICKS,
						MIMOSA_RANGE, MIMOSA_SUPER_CENTER_TILES, MIMOSA_SUPER_PER_HIT, "スナイパー"),
				new BrawlerDef("takomaru", "アイアンゴーレム（タンク）", TAKOMARU_HP, TAKOMARU_SPEED, MAX_AMMO, TAKOMARU_RELOAD, ATTACK_COOLDOWN_TICKS,
						TAKOMARU_RANGE, TAKOMARU_SUPER_JUMP_TILES, TAKOMARU_SUPER_PER_HIT, "タンク"),
				new BrawlerDef("nita", "シロクマ（アタッカー）", NITA_HP, NITA_SPEED, MAX_AMMO, NITA_RELOAD, ATTACK_COOLDOWN_TICKS,
						NITA_RANGE, NITA_SUPER_RANGE, NITA_SUPER_PER_HIT, "アタッカー"),
				new BrawlerDef("barley", "ウィッチ（グレネーディア）", BARLEY_HP, BARLEY_SPEED, MAX_AMMO, BARLEY_RELOAD, ATTACK_COOLDOWN_TICKS,
						BARLEY_RANGE, BARLEY_SUPER_CENTER_TILES, BARLEY_SUPER_PER_HIT, "グレネーディア"));
	}
}

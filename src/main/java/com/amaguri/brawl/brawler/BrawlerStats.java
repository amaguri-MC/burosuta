package com.amaguri.brawl.brawler;

import java.util.List;

/**
 * 5キャラの数値の唯一の置き場所（ブロスタ準拠）。距離はタイル（使う側で Tiles.b() によりブロックへ）、
 * 弾速はブロック/tick、角度は度、時間は tick（20tick=1秒）。
 */
final class BrawlerStats {
	private BrawlerStats() {}

	static final int MAX_AMMO = 3;
	static final int ATTACK_COOLDOWN_TICKS = 8;

	// ---- ラスティ ----
	static final int RUSTY_HP = 3600;
	static final double RUSTY_SPEED = 2.4;
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

	static List<BrawlerDef> defs() {
		return List.of(
				new BrawlerDef("rusty", "ラスティ", RUSTY_HP, RUSTY_SPEED, MAX_AMMO, RUSTY_RELOAD, ATTACK_COOLDOWN_TICKS,
						RUSTY_RANGE, RUSTY_SUPER_RANGE, RUSTY_SUPER_PER_HIT, "ショットガン使い"),
				new BrawlerDef("kaden", "ケイデン", KADEN_HP, KADEN_SPEED, MAX_AMMO, KADEN_RELOAD, ATTACK_COOLDOWN_TICKS,
						KADEN_RANGE, KADEN_SUPER_RANGE, KADEN_SUPER_PER_HIT, "ガンマン"),
				new BrawlerDef("gouen", "ゴウエン", GOUEN_HP, GOUEN_SPEED, MAX_AMMO, GOUEN_RELOAD, ATTACK_COOLDOWN_TICKS,
						GOUEN_RANGE, GOUEN_SUPER_DASH_TILES, GOUEN_SUPER_PER_HIT, "タンク"),
				new BrawlerDef("mimosa", "ミモザ", MIMOSA_HP, MIMOSA_SPEED, MAX_AMMO, MIMOSA_RELOAD, ATTACK_COOLDOWN_TICKS,
						MIMOSA_RANGE, MIMOSA_SUPER_CENTER_TILES, MIMOSA_SUPER_PER_HIT, "花火砲手"),
				new BrawlerDef("takomaru", "タコまる", TAKOMARU_HP, TAKOMARU_SPEED, MAX_AMMO, TAKOMARU_RELOAD, ATTACK_COOLDOWN_TICKS,
						TAKOMARU_RANGE, TAKOMARU_SUPER_JUMP_TILES, TAKOMARU_SUPER_PER_HIT, "格闘家"));
	}
}

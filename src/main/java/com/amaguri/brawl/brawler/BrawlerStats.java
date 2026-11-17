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

	// ---- シェリー ----
	static final int SHELLY_HP = 3600;
	static final double SHELLY_SPEED = 2.4;
	static final double SHELLY_RELOAD = 1.5;
	static final float SHELLY_SUPER_PER_HIT = 0.067f;
	static final int SHELLY_PELLETS = 5, SHELLY_DMG = 300;
	static final double SHELLY_RANGE = 7.67, SHELLY_FAN = 30, SHELLY_SPEED_BPT = 1.0, SHELLY_PELLET_RADIUS = 0.35;
	static final int SHELLY_SUPER_PELLETS = 9, SHELLY_SUPER_DMG = 320;
	static final double SHELLY_SUPER_RANGE = 7.67, SHELLY_SUPER_FAN = 40, SHELLY_SUPER_KNOCK_TILES = 3;

	// ---- コルト ----
	static final int COLT_HP = 2800;
	static final double COLT_SPEED = 2.4;
	static final double COLT_RELOAD = 1.6;
	static final float COLT_SUPER_PER_HIT = 0.042f;
	static final int COLT_BULLETS = 6, COLT_DMG = 360, COLT_BULLET_INTERVAL_TICKS = 2;
	static final double COLT_RANGE = 9.33, COLT_SPEED_BPT = 1.2, COLT_SCATTER_DEG = 2, COLT_BULLET_RADIUS = 0.3;
	static final int COLT_SUPER_BULLETS = 12, COLT_SUPER_DMG = 360;
	static final double COLT_SUPER_RANGE = 12.67;

	// ---- ブル ----
	static final int BULL_HP = 5000;
	static final double BULL_SPEED = 2.57;
	static final double BULL_RELOAD = 1.6;
	static final float BULL_SUPER_PER_HIT = 0.0625f;
	static final int BULL_PELLETS = 5, BULL_DMG = 400;
	static final double BULL_RANGE = 5.33, BULL_FAN = 25, BULL_SPEED_BPT = 1.0, BULL_PELLET_RADIUS = 0.4;
	static final double BULL_SUPER_DASH_TILES = 10;
	static final int BULL_SUPER_DASH_TICKS = 12;           // 約0.6秒
	static final int BULL_SUPER_DMG = 800;
	static final double BULL_SUPER_KNOCK_TILES = 1.5;      // 押し出し（数値指定なしのため仮）
	static final double BULL_SUPER_HIT_RADIUS_BLOCKS = 1.4;

	// ---- ブロック ----
	static final int BROCK_HP = 2400;
	static final double BROCK_SPEED = 2.4;
	static final double BROCK_RELOAD = 2.1;
	static final float BROCK_SUPER_PER_HIT = 0.25f;
	static final int BROCK_DMG = 1040;
	static final double BROCK_RANGE = 9.67, BROCK_SPEED_BPT = 0.8, BROCK_ROCKET_RADIUS = 0.4, BROCK_BLAST_TILES = 1;
	static final int BROCK_SUPER_ROCKETS = 9, BROCK_SUPER_DMG = 1040, BROCK_SUPER_INTERVAL_TICKS = 3, BROCK_SUPER_FALL_TICKS = 8;
	static final double BROCK_SUPER_CENTER_TILES = 7.67, BROCK_SUPER_AREA_TILES = 3;

	// ---- エルプリモ ----
	static final int ELPRIMO_HP = 6000;
	static final double ELPRIMO_SPEED = 2.57;
	static final double ELPRIMO_RELOAD = 1.1;
	static final float ELPRIMO_SUPER_PER_HIT = 0.0625f;
	static final int ELPRIMO_PUNCHES = 4, ELPRIMO_DMG = 380;
	static final double ELPRIMO_RANGE = 3, ELPRIMO_SPEED_BPT = 0.9, ELPRIMO_FAN = 40, ELPRIMO_PUNCH_RADIUS = 0.6;
	static final double ELPRIMO_SUPER_JUMP_TILES = 6, ELPRIMO_SUPER_RADIUS_TILES = 1.5, ELPRIMO_SUPER_KNOCK_TILES = 1.5;
	static final int ELPRIMO_SUPER_DMG = 800, ELPRIMO_SUPER_FLIGHT_TICKS = 16;
	static final double ELPRIMO_SUPER_JUMP_HEIGHT_BLOCKS = 3.5;

	static List<BrawlerDef> defs() {
		return List.of(
				new BrawlerDef("shelly", "シェリー", SHELLY_HP, SHELLY_SPEED, MAX_AMMO, SHELLY_RELOAD, ATTACK_COOLDOWN_TICKS,
						SHELLY_RANGE, SHELLY_SUPER_RANGE, SHELLY_SUPER_PER_HIT, "ショットガン使い"),
				new BrawlerDef("colt", "コルト", COLT_HP, COLT_SPEED, MAX_AMMO, COLT_RELOAD, ATTACK_COOLDOWN_TICKS,
						COLT_RANGE, COLT_SUPER_RANGE, COLT_SUPER_PER_HIT, "ガンマン"),
				new BrawlerDef("bull", "ブル", BULL_HP, BULL_SPEED, MAX_AMMO, BULL_RELOAD, ATTACK_COOLDOWN_TICKS,
						BULL_RANGE, BULL_SUPER_DASH_TILES, BULL_SUPER_PER_HIT, "タンク"),
				new BrawlerDef("brock", "ブロック", BROCK_HP, BROCK_SPEED, MAX_AMMO, BROCK_RELOAD, ATTACK_COOLDOWN_TICKS,
						BROCK_RANGE, BROCK_SUPER_CENTER_TILES, BROCK_SUPER_PER_HIT, "ロケット砲手"),
				new BrawlerDef("elprimo", "エルプリモ", ELPRIMO_HP, ELPRIMO_SPEED, MAX_AMMO, ELPRIMO_RELOAD, ATTACK_COOLDOWN_TICKS,
						ELPRIMO_RANGE, ELPRIMO_SUPER_JUMP_TILES, ELPRIMO_SUPER_PER_HIT, "格闘家"));
	}
}

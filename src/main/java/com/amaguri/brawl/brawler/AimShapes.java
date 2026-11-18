package com.amaguri.brawl.brawler;

import com.amaguri.brawl.api.Tiles;

import static com.amaguri.brawl.brawler.BrawlerStats.*;

/**
 * 照準ガイドの形（common 側。クライアントの WorldOverlay からも読む）。数値はすべて BrawlerStats / Attacks の実際の処理から取る。
 * 距離・幅・半径はすべてブロック。角度は度。
 */
public final class AimShapes {
	private AimShapes() {}

	public enum Kind {
		/** 扇: angleDeg, range。 */
		FAN,
		/** 直線: width（全幅）, range。 */
		LINE,
		/** 投てき: range=最大射程, radius=着弾の円の半径。照準点に円、放物線の点線。 */
		LOB,
		/** 突進・転がり: width（全幅）, range=距離。 */
		DASH,
		/** 跳躍: range=最大距離, radius=着地の円の半径。 */
		JUMP,
		/** 自分中心の円: radius。 */
		CIRCLE_SELF,
		/** 呼び出し・範囲: range=最大射程, radius=照準点の円の半径。 */
		SUMMON
	}

	public record Shape(Kind kind, double angleDeg, double width, double range, double radius) {
		/** 照準点を持つ形（照準点を最大射程で切って円を描く）。 */
		public boolean aimed() { return kind == Kind.LOB || kind == Kind.JUMP || kind == Kind.SUMMON; }
	}

	private static Shape fan(double deg, double rangeTiles) { return new Shape(Kind.FAN, deg, 0, Tiles.b(rangeTiles), 0); }
	private static Shape line(double widthBlocks, double rangeTiles) { return new Shape(Kind.LINE, 0, widthBlocks, Tiles.b(rangeTiles), 0); }
	private static Shape dash(double widthBlocks, double tiles) { return new Shape(Kind.DASH, 0, widthBlocks, Tiles.b(tiles), 0); }
	private static Shape lob(double rangeTiles, double radiusBlocks) { return new Shape(Kind.LOB, 0, 0, Tiles.b(rangeTiles), radiusBlocks); }
	private static Shape jump(double rangeTiles, double radiusBlocks) { return new Shape(Kind.JUMP, 0, 0, Tiles.b(rangeTiles), radiusBlocks); }
	private static Shape summon(double rangeTiles, double radiusBlocks) { return new Shape(Kind.SUMMON, 0, 0, Tiles.b(rangeTiles), radiusBlocks); }

	/** 通常攻撃の形。不明なキャラは null。 */
	public static Shape normal(String id) {
		if (id == null) return null;
		return switch (id) {
			case "rusty" -> fan(RUSTY_FAN, RUSTY_RANGE);
			case "gouen" -> fan(GOUEN_FAN, GOUEN_RANGE);
			case "mimosa" -> line(MIMOSA_ROCKET_RADIUS * 2, MIMOSA_RANGE);
			case "frank" -> fan(FRANK_FAN, FRANK_RANGE);
			case "darryl" -> fan(DARRYL_FAN, DARRYL_RANGE);
			case "poco" -> fan(POCO_FAN, POCO_RANGE);
			case "mortis" -> dash(MORTIS_DASH_HIT_RADIUS_BLOCKS * 2, MORTIS_DASH_TILES);
			case "edgar" -> line(EDGAR_PUNCH_RADIUS * 2, EDGAR_RANGE);
			case "lou" -> line(LOU_BALL_RADIUS * 2, LOU_RANGE);
			case "nita" -> line(NITA_WAVE_RADIUS * 2, NITA_RANGE);
			case "barley" -> lob(BARLEY_RANGE, Tiles.b(BARLEY_PUDDLE_RADIUS_TILES));
			default -> null;
		};
	}

	/** 必殺の形。不明なキャラは null。 */
	public static Shape superShape(String id) {
		if (id == null) return null;
		return switch (id) {
			case "rusty" -> fan(RUSTY_SUPER_FAN, RUSTY_SUPER_RANGE);
			case "gouen" -> dash(GOUEN_SUPER_HIT_RADIUS_BLOCKS * 2, GOUEN_SUPER_DASH_TILES);
			// 照準点の周り MIMOSA_SUPER_AREA_TILES に落ち、各着弾の爆発が MIMOSA_BLAST_TILES
			case "mimosa" -> summon(MIMOSA_SUPER_CENTER_TILES, Tiles.b(MIMOSA_SUPER_AREA_TILES + MIMOSA_BLAST_TILES));
			case "frank" -> fan(FRANK_FAN + 10, FRANK_SUPER_RANGE);
			case "darryl" -> dash(DARRYL_SUPER_HIT_RADIUS_BLOCKS * 2, DARRYL_SUPER_DASH_TILES);
			case "poco" -> fan(POCO_SUPER_FAN, POCO_SUPER_RANGE);
			case "mortis" -> fan(MORTIS_SUPER_FAN, MORTIS_SUPER_RANGE);
			case "edgar" -> jump(EDGAR_SUPER_LEAP_TILES, 1.0);
			case "lou" -> summon(LOU_SUPER_RANGE, Tiles.b(LOU_FLOOR_RADIUS_TILES));
			case "nita" -> summon(NITA_SUPER_RANGE, 1.0);
			// 照準点の周り BARLEY_SUPER_AREA_TILES に投げ、各水たまりが BARLEY_PUDDLE_RADIUS_TILES
			case "barley" -> lob(BARLEY_SUPER_CENTER_TILES, Tiles.b(BARLEY_SUPER_AREA_TILES + BARLEY_PUDDLE_RADIUS_TILES));
			default -> null;
		};
	}
}

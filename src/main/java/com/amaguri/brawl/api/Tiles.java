package com.amaguri.brawl.api;

/** 単位の契約（司令塔所有）。1タイル = 2ブロック。 */
public final class Tiles {
	private Tiles() {}

	public static final double BLOCKS_PER_TILE = 2.0;

	/** タイル → ブロック。 */
	public static double b(double tiles) {
		return tiles * BLOCKS_PER_TILE;
	}
}

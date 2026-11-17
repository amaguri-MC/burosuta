package com.amaguri.brawl.brawler;

/** キャラ定義（CONTRACT W2 の署名どおり）。数値は BrawlerStats に集約。 */
public record BrawlerDef(String id, String nameJa, int maxHp, double speedTiles, int maxAmmo, double reloadSec,
		int attackCooldownTicks, double attackRangeTiles, double superRangeTiles, float superPerHit, String roleJa) {}

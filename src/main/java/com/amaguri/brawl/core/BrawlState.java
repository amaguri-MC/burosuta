package com.amaguri.brawl.core;

import java.util.UUID;

import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.Brawlers;

/** 試合参加者1人ぶんの状態（プレイヤーもボットも同じ）。サーバースレッド専用。 */
public final class BrawlState {
	public final UUID id;
	public final String brawlerId;
	public int hp;
	public float ammo;
	public float superCharge;
	public int cubes;
	public boolean alive = true;
	public long lastActTick;
	public long lastHurtTick;
	public long revealUntilTick;
	/** 召喚物（ニタの相棒など）なら召喚主の UUID。普通の参加者は null。 */
	public UUID ownerId;

	BrawlState(UUID id, String brawlerId) {
		this.id = id;
		this.brawlerId = brawlerId;
	}

	/** def.maxHp + 400*cubes */
	public int maxHp() {
		BrawlerDef d = Brawlers.get(brawlerId);
		int base = d != null ? d.maxHp() : 3000;
		return base + 400 * cubes;
	}

	/** 1 + 0.1*cubes */
	public float damageMul() {
		return 1.0f + 0.1f * cubes;
	}
}

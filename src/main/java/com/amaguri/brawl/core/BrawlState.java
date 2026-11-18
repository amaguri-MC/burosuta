package com.amaguri.brawl.core;

import java.util.UUID;

import com.amaguri.brawl.brawler.BrawlerDef;
import com.amaguri.brawl.brawler.BrawlerStats;
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

	// ---- 状態異常（tick は Combat.now() 基準。サーバースレッド専用）----
	public long stunUntil;
	public long freezeUntil;
	public long slowUntil;
	public double slowMul = 1.0;
	/** 溜め攻撃中（動けない）。攻撃・必殺も不可。 */
	public long lockUntil;
	/** 加速（エドガーの着地後）。+hasteBlocksPerSec ブロック/秒。 */
	public long hasteUntil;
	public double hasteBlocksPerSec;
	/** 凍結ゲージ 0〜100 と、最後に加わった tick。 */
	public float frost;
	public long frostAddedTick;
	/** 氷の床の上にいる間（この tick まで）。 */
	public long onIceUntil;
	/** この tick までは押し出し（ノックバック・突進）中なので、動けない状態でも速度を止めない。 */
	public long pushUntil;
	/** 溜め攻撃の世代。スタンなどで中断されたら増える。 */
	public int chargeToken;
	/** join 時に設定した移動速度属性値（状態異常で変える前の値）。 */
	double baseSpeed = -1;
	double appliedSpeed = -1;

	BrawlState(UUID id, String brawlerId) {
		this.id = id;
		this.brawlerId = brawlerId;
	}

	/** キューブ1個ぶんの最大HP（そのキャラのパワー11の最大HPの10%）。 */
	public int cubeHp() {
		BrawlerDef d = Brawlers.get(brawlerId);
		int base = d != null ? d.maxHp() : BrawlerStats.p(3000);
		return Math.round(base * 0.1f);
	}

	/** def.maxHp（パワー11）+ 10% * cubes */
	public int maxHp() {
		BrawlerDef d = Brawlers.get(brawlerId);
		int base = d != null ? d.maxHp() : BrawlerStats.p(3000);
		return base + cubeHp() * cubes;
	}

	/** 1 + 0.1*cubes */
	public float damageMul() {
		return 1.0f + 0.1f * cubes;
	}
}

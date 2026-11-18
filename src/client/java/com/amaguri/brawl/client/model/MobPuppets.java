package com.amaguri.brawl.client.model;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.client.renderer.entity.state.IronGolemRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 「人形」方式: キャラ id に対応するバニラのモブ実体を、クライアント専用（level に追加しない）で実体ごとに持ち、
 * 本体の描画状態（位置・向き・歩き・攻撃の振り・被弾）を写してバニラの描画器で描く。
 * 高さは約1.9ブロックに揃える（寸法から倍率を出す）。
 */
public final class MobPuppets {
	private MobPuppets() {
	}

	/** 見た目の高さ（ブロック）。 */
	public static final float TARGET_HEIGHT = 1.9f;

	private enum Kind { PILLAGER, WITHER_SKELETON, COW, BLAZE, GOLEM, SNOW_GOLEM, BEAR, WITCH, ALLAY, TRADER }

	/**
	 * face = 頭の正面の UV（テクセル）と、そのテクスチャ全体の大きさ。
	 * UNVERIFIED: 各モデルクラスの createBodyLayer の texOffs と箱の寸法（バニラの知識）から求めた値。
	 */
	public record Face(int u, int v, int w, int h, int tw, int th) {}

	private record Spec(EntityType<? extends LivingEntity> type, Kind kind, float hover, float widthCap, Face face) {
		/** 倍率: 高さを1.9に。幅が widthCap を超えるなら幅で抑える。 */
		float factor() {
			float h = Math.max(0.1f, type.getHeight()), w = Math.max(0.1f, type.getWidth());
			return Math.min(TARGET_HEIGHT / h, widthCap / w);
		}
	}

	private static final Map<String, Spec> SPECS = new HashMap<>();

	static {
		// 幅の上限: 二足は1.2。四足（牛・シロクマ）は体が横に長く、1.2だと高さが揃わないので1.9まで許す。
		// 顔 UNVERIFIED（上のとおり）: 頭の箱の texOffs=(u0,v0), 寸法 w×h×d のとき 正面=(u0+d, v0+d, w, h)
		SPECS.put("rusty", new Spec(EntityTypes.PILLAGER, Kind.PILLAGER, 0f, 1.2f, new Face(8, 8, 8, 10, 64, 64)));
		SPECS.put("gouen", new Spec(EntityTypes.COW, Kind.COW, 0f, 1.9f, new Face(6, 6, 8, 8, 64, 32)));
		SPECS.put("frank", new Spec(EntityTypes.IRON_GOLEM, Kind.GOLEM, 0f, 1.2f, new Face(8, 8, 8, 10, 128, 128)));
		// 銅のゴーレム: 頭 8x5x10 (texOffs 0,0) → 正面 (10,10,8,5)。テクスチャ 64x64
		SPECS.put("darryl", new Spec(EntityTypes.COPPER_GOLEM, Kind.GOLEM, 0f, 1.2f, new Face(10, 10, 8, 5, 64, 64)));
		SPECS.put("mimosa", new Spec(EntityTypes.BLAZE, Kind.BLAZE, 0.15f, 1.2f, new Face(8, 8, 8, 8, 64, 32)));
		SPECS.put("barley", new Spec(EntityTypes.WITCH, Kind.WITCH, 0f, 1.2f, new Face(10, 10, 10, 10, 64, 128)));
		SPECS.put("nita", new Spec(EntityTypes.POLAR_BEAR, Kind.BEAR, 0f, 1.9f, new Face(7, 7, 7, 7, 128, 64)));
		// アレイは高さ0.6の小さな妖精で、拡大すると地面にめり込んで見えるので少し浮かせる。頭 5x5x5 (texOffs 0,0)。テクスチャ 32x32
		SPECS.put("poco", new Spec(EntityTypes.ALLAY, Kind.ALLAY, 0.3f, 1.2f, new Face(5, 5, 5, 5, 32, 32)));
		// 行商人: 村人と同じ 64x64。頭 8x10x8 (texOffs 0,0)
		SPECS.put("mortis", new Spec(EntityTypes.WANDERING_TRADER, Kind.TRADER, 0f, 1.2f, new Face(8, 8, 8, 10, 64, 64)));
		SPECS.put("edgar", new Spec(EntityTypes.WITHER_SKELETON, Kind.WITHER_SKELETON, 0f, 1.2f, new Face(8, 8, 8, 8, 64, 32)));
		// スノーゴーレム: 頭 8x8x8 (texOffs 0,0)。カボチャ頭は人形の実体で hasPumpkin=true にして描く（顔アイコンは頭の素の面）
		SPECS.put("lou", new Spec(EntityTypes.SNOW_GOLEM, Kind.SNOW_GOLEM, 0f, 1.2f, new Face(8, 8, 8, 8, 64, 64)));
	}

	public static boolean has(String brawlerId) {
		return brawlerId != null && SPECS.containsKey(brawlerId) && !failed;
	}

	/** 顔の UV。無ければ null。 */
	public static Face faceOf(String brawlerId) {
		Spec s = SPECS.get(brawlerId);
		return s == null ? null : s.face;
	}

	/** 倍率をかけたあとの見た目の高さ（ブロック）。 */
	public static float visualHeight(String brawlerId) {
		Spec s = SPECS.get(brawlerId);
		return s == null ? TARGET_HEIGHT : s.type.getHeight() * s.factor();
	}

	/** 描画で止まったら以降は使わず、今の自作モデル/通常の見た目に戻す。 */
	private static boolean failed = false;

	// ---- 実体の管理 ----

	private static final class Puppet {
		final String brawler;
		final ClientLevel level;
		final LivingEntity entity;
		float potionUntil = -1f;
		float lastAttack = 0f;

		Puppet(String brawler, ClientLevel level, LivingEntity entity) {
			this.brawler = brawler;
			this.level = level;
			this.entity = entity;
		}
	}

	private static final Map<Integer, Puppet> WORLD = new HashMap<>();
	private static final Map<String, Puppet> GUI = new HashMap<>();
	private static int nextId = -2_000_000;

	private static Puppet make(String bid, Spec sp, ClientLevel lvl) {
		LivingEntity e = sp.type.create(lvl, EntitySpawnReason.LOAD);
		if (e == null) {
			return null;
		}
		e.setId(nextId--); // level に載せない仮の実体は負のID
		if (e instanceof Mob m) {
			if (sp.kind == Kind.PILLAGER || sp.kind == Kind.WITHER_SKELETON) {
				m.setAggressive(true); // 構えたままにする
			}
			m.setNoAi(true);
		}
		switch (sp.kind) {
			case PILLAGER -> e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
			case WITHER_SKELETON -> e.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
			case SNOW_GOLEM -> {
				if (e instanceof SnowGolem sg) {
					sg.setPumpkin(true); // カボチャ頭あり（既定でも true）
				}
			}
			default -> {
			}
		}
		return new Puppet(bid, lvl, e);
	}

	private static Puppet worldPuppet(int id, String bid, Spec sp, ClientLevel lvl) {
		Puppet p = WORLD.get(id);
		if (p == null || p.level != lvl || !p.brawler.equals(bid)) {
			if (WORLD.size() > 256) {
				WORLD.clear();
			}
			p = make(bid, sp, lvl);
			if (p == null) {
				return null;
			}
			WORLD.put(id, p);
		}
		return p;
	}

	private static Puppet guiPuppet(String bid, Spec sp, ClientLevel lvl) {
		Puppet p = GUI.get(bid);
		if (p == null || p.level != lvl) {
			p = make(bid, sp, lvl);
			if (p == null) {
				return null;
			}
			GUI.put(bid, p);
		}
		return p;
	}

	public static void clear() {
		WORLD.clear();
		GUI.clear();
	}

	// ---- 世界での描画 ----

	/** 本体の描画状態 src から、人形の描画状態を作る。作れなければ null（= 今までの描画に任せる）。 */
	public static LivingEntityRenderState build(EntityRenderDispatcher d, AvatarRenderState src, String bid) {
		Spec sp = SPECS.get(bid);
		ClientLevel lvl = Minecraft.getInstance().level;
		if (sp == null || lvl == null || failed) {
			return null;
		}
		try {
			Puppet p = worldPuppet(src.id, bid, sp, lvl);
			if (p == null) {
				return null;
			}
			LivingEntity e = p.entity;
			e.snapTo(src.x, src.y, src.z, src.bodyRot, 0f);
			e.tickCount = (int) src.ageInTicks;

			// ウィッチは攻撃した直後だけポーションを持つ
			float atk = src.attackTime;
			if (sp.kind == Kind.WITCH) {
				if (atk > 0f && p.lastAttack <= 0f) {
					p.potionUntil = src.ageInTicks + 14f;
				}
				boolean hold = src.ageInTicks < p.potionUntil;
				ItemStack cur = e.getItemBySlot(EquipmentSlot.MAINHAND);
				if (hold == cur.isEmpty()) {
					e.setItemSlot(EquipmentSlot.MAINHAND, hold ? new ItemStack(Items.POTION) : ItemStack.EMPTY);
				}
			}
			p.lastAttack = atk;

			EntityRenderState raw = d.extractEntity(e, 1.0f);
			if (!(raw instanceof LivingEntityRenderState ps)) {
				return null;
			}
			ps.x = src.x;
			ps.y = src.y;
			ps.z = src.z;
			ps.bodyRot = src.bodyRot;
			ps.yRot = src.yRot;
			ps.xRot = src.xRot;
			ps.walkAnimationPos = src.walkAnimationPos;
			ps.walkAnimationSpeed = src.walkAnimationSpeed;
			ps.ageInTicks = src.ageInTicks;
			ps.hasRedOverlay = src.hasRedOverlay;
			ps.deathTime = src.deathTime;
			ps.lightCoords = src.lightCoords;
			ps.outlineColor = src.outlineColor;
			ps.distanceToCameraSq = src.distanceToCameraSq;
			ps.displayFireAnimation = src.displayFireAnimation;
			ps.isInvisible = src.isInvisible;
			ps.isInWater = false;
			ps.isFullyFrozen = false;
			ps.isBaby = false;
			ps.pose = Pose.STANDING;
			ps.nameTag = null; // 名前は自前の頭上表示
			ps.scoreText = null;
			ps.scale *= sp.factor();
			ps.shadowRadius = src.shadowRadius;
			ps.shadowPieces.clear();
			ps.shadowPieces.addAll(src.shadowPieces);
			applyAttack(sp.kind, ps, atk);
			return ps;
		} catch (Throwable t) {
			fail(t);
			return null;
		}
	}

	/**
	 * レンダラの submit の中から人形を描く（BotRenderer 用の保険。ディスパッチャの mixin が先に人形に差し替えていれば、ここへは来ない）。
	 * 描いた（または透明・脱落で描かないと決めた）なら true。人形にしないなら false（呼び出し側が元の見た目を描く）。
	 * poseStack はディスパッチャが実体の位置まで移動済みのものをそのまま渡す。
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static boolean submitInRenderer(AvatarRenderState a, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		String bid = BrawlerLooks.brawlerOf(a.id);
		if (bid == null || !has(bid)) {
			return false;
		}
		if (a.isSpectator || a.isInvisible) {
			return true;
		}
		EntityRenderDispatcher d = Minecraft.getInstance().getEntityRenderDispatcher();
		LivingEntityRenderState ps = build(d, a, bid);
		if (ps == null) {
			return false;
		}
		try {
			EntityRenderer r = d.getRenderer((EntityRenderState) ps);
			pose.pushPose();
			pose.translate(0.0, hover(bid), 0.0);
			r.submit(ps, pose, collector, camera);
			pose.popPose();
			return true;
		} catch (Throwable t) {
			fail(t);
			return false;
		}
	}

	/** 攻撃の振り（0=待機〜1）を、そのモブの自然なアニメに変える。 */
	private static void applyAttack(Kind kind, LivingEntityRenderState ps, float atk) {
		if (ps instanceof ArmedEntityRenderState a) {
			a.attackTime = atk; // 襲撃者・スケルトン・ウィッチ: 腕の振り
		}
		if (ps instanceof IllagerRenderState il) {
			il.attackAnim = atk;
		}
		if (ps instanceof IronGolemRenderState g) {
			g.attackTicksRemaining = atk > 0f ? 10f * (1f - atk) : 0f; // 腕振り
		}
		if (ps instanceof PolarBearRenderState b) {
			b.standScale = atk > 0f ? (float) Math.sin(atk * Math.PI) : 0f; // 立ち上がって叩く
		}
	}

	/** 世界での描画のとき、地面からの浮かせ量（ブロック）。 */
	public static float hover(String bid) {
		Spec s = SPECS.get(bid);
		return s == null ? 0f : s.hover;
	}

	// ---- GUI（選択画面・顔のテクスチャ取得） ----

	/**
	 * GUI 用の描画状態（待機・全身明るく）。rotY=体の向き（180で正面）、age=経過ティック（ブレイズの棒などが動く）。
	 * 作れなければ null。
	 */
	public static LivingEntityRenderState guiState(EntityRenderDispatcher d, String bid, float rotY, float age) {
		Spec sp = SPECS.get(bid);
		ClientLevel lvl = Minecraft.getInstance().level;
		if (sp == null || lvl == null || failed) {
			return null;
		}
		try {
			Puppet p = guiPuppet(bid, sp, lvl);
			if (p == null) {
				return null;
			}
			EntityRenderState raw = d.extractEntity(p.entity, 1.0f);
			if (!(raw instanceof LivingEntityRenderState ps)) {
				return null;
			}
			ps.bodyRot = rotY;
			ps.yRot = 0f;
			ps.xRot = 0f;
			ps.walkAnimationPos = 0f;
			ps.walkAnimationSpeed = 0f;
			ps.ageInTicks = age;
			ps.lightCoords = 0xF000F0;
			ps.nameTag = null;
			ps.scoreText = null;
			ps.displayFireAnimation = false;
			ps.scale *= sp.factor();
			return ps;
		} catch (Throwable t) {
			fail(t);
			return null;
		}
	}

	/** 人形のバニラのテクスチャ（顔の切り出し用）。level が無いなど取れないときは null。 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Identifier textureOf(String bid) {
		Minecraft mc = Minecraft.getInstance();
		LivingEntityRenderState ps = guiState(mc.getEntityRenderDispatcher(), bid, 180f, 0f);
		if (ps == null) {
			return null;
		}
		try {
			EntityRenderer r = mc.getEntityRenderDispatcher().getRenderer((EntityRenderState) ps);
			if (r instanceof LivingEntityRenderer lr) {
				return lr.getTextureLocation(ps);
			}
		} catch (Throwable t) {
			fail(t);
		}
		return null;
	}

	private static void fail(Throwable t) {
		if (!failed) {
			failed = true;
			LogUtils.getLogger().error("[brawl] モブの人形の描画に失敗したので、自作モデルに戻します", t);
		}
	}
}

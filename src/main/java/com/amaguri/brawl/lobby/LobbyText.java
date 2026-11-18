package com.amaguri.brawl.lobby;

import java.lang.reflect.Method;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.math.Transformation;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * ロビーの文字（TextDisplay）。保存しない（shouldBeSaved=false）。常にこちらを向く。
 * TextDisplay の setText などは private なので、リフレクションで呼ぶ（名前は 26.2 の common.txt で確認済み。MC 26.x は難読化なし）。
 */
public final class LobbyText extends Display.TextDisplay {
	private static final Logger LOG = LoggerFactory.getLogger("brawl-lobby");

	private LobbyText(Level level) {
		super(EntityType.TEXT_DISPLAY, level);
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	/** 作ってワールドに出す。失敗したら null。tags は付けるタグ。 */
	static LobbyText spawn(ServerLevel lvl, Vec3 pos, Component text, float scale, int lineWidth, String... tags) {
		LobbyText t = new LobbyText(lvl);
		t.snapTo(pos.x, pos.y, pos.z, 0f, 0f);
		try {
			call(t, "setBillboardConstraints", Display.BillboardConstraints.class, Display.BillboardConstraints.CENTER);
			call(t, "setViewRange", float.class, 2.0f);                // 既定の2倍まで離れても見える
			call(t, "setLineWidth", int.class, lineWidth);
			call(t, "setText", Component.class, text);
			if (scale != 1.0f) {
				call(t, "setTransformation", Transformation.class,
						new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(scale, scale, scale), new Quaternionf()));
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOG.error("TextDisplay の設定に失敗（UNVERIFIED のリフレクション）", e);
		}
		for (String tag : tags) t.addTag(tag);
		if (!lvl.addFreshEntity(t)) return null;
		return t;
	}

	void setLabel(Component text) {
		try {
			call(this, "setText", Component.class, text);
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOG.error("TextDisplay の文字を変えられません", e);
		}
	}

	private static void call(Object target, String name, Class<?> argType, Object arg) throws ReflectiveOperationException {
		Class<?> c = target.getClass();
		while (c != null) {
			try {
				Method m = c.getDeclaredMethod(name, argType);
				m.setAccessible(true);
				m.invoke(target, arg);
				return;
			} catch (NoSuchMethodException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchMethodException(name);
	}
}

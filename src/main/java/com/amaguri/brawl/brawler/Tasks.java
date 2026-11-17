package com.amaguri.brawl.brawler;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

/** 数tick後・毎tickに動く処理（連射・突進・ジャンプ・ロケット雨）の小さなスケジューラ。 */
final class Tasks {
	private Tasks() {}

	/** true を返すと終了。 */
	interface Task { boolean run(); }

	private static final List<Task> ACTIVE = new ArrayList<>();
	private static final List<Task> INCOMING = new ArrayList<>();

	static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ACTIVE.addAll(INCOMING);
			INCOMING.clear();
			ACTIVE.removeIf(t -> {
				try {
					return t.run();
				} catch (RuntimeException e) {
					return true;
				}
			});
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			ACTIVE.clear();
			INCOMING.clear();
		});
	}

	static void add(Task t) { INCOMING.add(t); }

	/** delay tick 後に1回だけ実行。0 なら今すぐ。 */
	static void after(int delay, Runnable r) {
		if (delay <= 0) {
			r.run();
			return;
		}
		int[] left = {delay};
		add(() -> {
			if (--left[0] > 0) return false;
			r.run();
			return true;
		});
	}
}

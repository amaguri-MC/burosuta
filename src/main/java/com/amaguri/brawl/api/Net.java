package com.amaguri.brawl.api;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * 通信の契約（司令塔所有。変更は司令塔に相談）。
 * 送る側・受ける側の処理は各担当が登録する（ServerPlayNetworking / ClientPlayNetworking）。
 */
public final class Net {
	private Net() {}

	private static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> typeOf(String path) {
		return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("brawl", path));
	}

	/** S2C 自分のステータス（core が 2tick ごとに試合中の各プレイヤーへ送る）。brawler="" は試合外。 */
	public record HudPayload(String brawler, int hp, int maxHp, float ammo, int maxAmmo, float superCharge,
			int cubes, boolean alive) implements CustomPacketPayload {
		public static final Type<HudPayload> TYPE = typeOf("hud");
		public static final StreamCodec<RegistryFriendlyByteBuf, HudPayload> CODEC = StreamCodec.of(
				(b, p) -> {
					b.writeUtf(p.brawler); b.writeVarInt(p.hp); b.writeVarInt(p.maxHp); b.writeFloat(p.ammo);
					b.writeVarInt(p.maxAmmo); b.writeFloat(p.superCharge); b.writeVarInt(p.cubes); b.writeBoolean(p.alive);
				},
				b -> new HudPayload(b.readUtf(), b.readVarInt(), b.readVarInt(), b.readFloat(), b.readVarInt(),
						b.readFloat(), b.readVarInt(), b.readBoolean()));
		@Override public Type<HudPayload> type() { return TYPE; }
	}

	/**
	 * S2C 試合の状況（mode が 10tick ごとに試合参加者全員へ送る）。
	 * phase: 0=試合なし 1=カウントダウン 2=試合中 3=終了表示。countdown は秒。
	 * gas* は安全地帯の四角（ワールド座標、ブロック）。毒ガスがまだ無いときは安全地帯=アリーナ全体。
	 * gasSecondsToNext: 次に縮み始めるまでの秒（縮み中は0）。myRank: 脱落/勝利で確定した順位（未確定は0）。
	 */
	public record MatchPayload(int phase, int countdown, int alive, int total, double gasMinX, double gasMinZ,
			double gasMaxX, double gasMaxZ, int gasSecondsToNext, int myRank) implements CustomPacketPayload {
		public static final Type<MatchPayload> TYPE = typeOf("match");
		public static final StreamCodec<RegistryFriendlyByteBuf, MatchPayload> CODEC = StreamCodec.of(
				(b, p) -> {
					b.writeVarInt(p.phase); b.writeVarInt(p.countdown); b.writeVarInt(p.alive); b.writeVarInt(p.total);
					b.writeDouble(p.gasMinX); b.writeDouble(p.gasMinZ); b.writeDouble(p.gasMaxX); b.writeDouble(p.gasMaxZ);
					b.writeVarInt(p.gasSecondsToNext); b.writeVarInt(p.myRank);
				},
				b -> new MatchPayload(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readDouble(),
						b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt(), b.readVarInt()));
		@Override public Type<MatchPayload> type() { return TYPE; }
	}

	/** S2C この受け手から見えない（茂みに隠れた）実体のID一覧。core が 4tick ごとに送る。空配列=全員見える。 */
	public record HiddenPayload(int[] entityIds) implements CustomPacketPayload {
		public static final Type<HiddenPayload> TYPE = typeOf("hidden");
		public static final StreamCodec<RegistryFriendlyByteBuf, HiddenPayload> CODEC = StreamCodec.of(
				(b, p) -> b.writeVarIntArray(p.entityIds), b -> new HiddenPayload(b.readVarIntArray()));
		@Override public Type<HiddenPayload> type() { return TYPE; }
	}

	/**
	 * S2C 頭上のHPバー用。core が 2tick ごとに試合中の全プレイヤーへ送る（受け手の近く48ブロック以内の参加者）。
	 * team: 0=自分 1=敵 （ソロなので味方なし）。
	 */
	public record BarsPayload(int[] ids, int[] hp, int[] maxHp, int[] team, int[] cubes) implements CustomPacketPayload {
		public static final Type<BarsPayload> TYPE = typeOf("bars");
		public static final StreamCodec<RegistryFriendlyByteBuf, BarsPayload> CODEC = StreamCodec.of(
				(b, p) -> {
					b.writeVarIntArray(p.ids); b.writeVarIntArray(p.hp); b.writeVarIntArray(p.maxHp);
					b.writeVarIntArray(p.team); b.writeVarIntArray(p.cubes);
				},
				b -> new BarsPayload(b.readVarIntArray(), b.readVarIntArray(), b.readVarIntArray(),
						b.readVarIntArray(), b.readVarIntArray()));
		@Override public Type<BarsPayload> type() { return TYPE; }
	}

	/** S2C 見た目: 実体ID → キャラID（core が 20tick ごとに全プレイヤーへ。空=試合なし）。クライアントはこの実体をキャラのモデルで描く。 */
	public record LooksPayload(int[] ids, String[] brawlers) implements CustomPacketPayload {
		public static final Type<LooksPayload> TYPE = typeOf("looks");
		public static final StreamCodec<RegistryFriendlyByteBuf, LooksPayload> CODEC = StreamCodec.of(
				(b, p) -> {
					b.writeVarIntArray(p.ids);
					b.writeVarInt(p.brawlers.length);
					for (String s : p.brawlers) b.writeUtf(s);
				},
				b -> {
					int[] ids = b.readVarIntArray();
					String[] bs = new String[b.readVarInt()];
					for (int i = 0; i < bs.length; i++) bs[i] = b.readUtf();
					return new LooksPayload(ids, bs);
				});
		@Override public Type<LooksPayload> type() { return TYPE; }
	}

	/** C2S 通常攻撃（左クリック or 右クリック）。yaw は度。brawler が受けて Brawlers.attack を呼ぶ。 */
	public record AttackPayload(float yaw) implements CustomPacketPayload {
		public static final Type<AttackPayload> TYPE = typeOf("attack");
		public static final StreamCodec<RegistryFriendlyByteBuf, AttackPayload> CODEC = StreamCodec.of(
				(b, p) -> b.writeFloat(p.yaw), b -> new AttackPayload(b.readFloat()));
		@Override public Type<AttackPayload> type() { return TYPE; }
	}

	/** C2S 必殺技（キー既定 R）。yaw は度。brawler が受けて Brawlers.useSuper を呼ぶ。 */
	public record SuperPayload(float yaw) implements CustomPacketPayload {
		public static final Type<SuperPayload> TYPE = typeOf("super");
		public static final StreamCodec<RegistryFriendlyByteBuf, SuperPayload> CODEC = StreamCodec.of(
				(b, p) -> b.writeFloat(p.yaw), b -> new SuperPayload(b.readFloat()));
		@Override public Type<SuperPayload> type() { return TYPE; }
	}

	/** S2C キルログ（mode が脱落のたびに全プレイヤーへ）。killer="" は毒ガスなど。名前は表示名。 */
	public record KillFeedPayload(String killer, String killerBrawler, String victim, String victimBrawler)
			implements CustomPacketPayload {
		public static final Type<KillFeedPayload> TYPE = typeOf("killfeed");
		public static final StreamCodec<RegistryFriendlyByteBuf, KillFeedPayload> CODEC = StreamCodec.of(
				(b, p) -> { b.writeUtf(p.killer); b.writeUtf(p.killerBrawler); b.writeUtf(p.victim); b.writeUtf(p.victimBrawler); },
				b -> new KillFeedPayload(b.readUtf(), b.readUtf(), b.readUtf(), b.readUtf()));
		@Override public Type<KillFeedPayload> type() { return TYPE; }
	}

	/**
	 * S2C 試合結果（mode が試合終了時に参加プレイヤーへ1回）。rank=自分の順位、trophyDelta=増減。
	 * names/brawlers/ranks は全参加者（順位順）。クライアントは結果画面を出す。
	 */
	public record ResultPayload(int rank, int total, int trophyDelta, int trophies, String[] names, String[] brawlers,
			int[] ranks) implements CustomPacketPayload {
		public static final Type<ResultPayload> TYPE = typeOf("result");
		public static final StreamCodec<RegistryFriendlyByteBuf, ResultPayload> CODEC = StreamCodec.of(
				(b, p) -> {
					b.writeVarInt(p.rank); b.writeVarInt(p.total); b.writeVarInt(p.trophyDelta); b.writeVarInt(p.trophies);
					b.writeVarInt(p.names.length);
					for (int i = 0; i < p.names.length; i++) { b.writeUtf(p.names[i]); b.writeUtf(p.brawlers[i]); b.writeVarInt(p.ranks[i]); }
				},
				b -> {
					int rank = b.readVarInt(), total = b.readVarInt(), d = b.readVarInt(), tr = b.readVarInt();
					int n = b.readVarInt();
					String[] ns = new String[n], bs = new String[n]; int[] rs = new int[n];
					for (int i = 0; i < n; i++) { ns[i] = b.readUtf(); bs[i] = b.readUtf(); rs[i] = b.readVarInt(); }
					return new ResultPayload(rank, total, d, tr, ns, bs, rs);
				});
		@Override public Type<ResultPayload> type() { return TYPE; }
	}

	/**
	 * S2C ロビーの状況（lobby が10tickごとにロビーにいるプレイヤーへ。ロビー外に出たら inLobby=false を1回）。
	 * ready/total=準備完了の人数/ロビーの人数、countdown=開始までの秒（-1=待機中）、selected=選んでいるキャラ。
	 */
	public record LobbyPayload(boolean inLobby, int ready, int total, int countdown, int trophies, int wins,
			String selected, boolean meReady) implements CustomPacketPayload {
		public static final Type<LobbyPayload> TYPE = typeOf("lobby");
		public static final StreamCodec<RegistryFriendlyByteBuf, LobbyPayload> CODEC = StreamCodec.of(
				(b, p) -> {
					b.writeBoolean(p.inLobby); b.writeVarInt(p.ready); b.writeVarInt(p.total); b.writeVarInt(p.countdown + 1);
					b.writeVarInt(p.trophies); b.writeVarInt(p.wins); b.writeUtf(p.selected); b.writeBoolean(p.meReady);
				},
				b -> new LobbyPayload(b.readBoolean(), b.readVarInt(), b.readVarInt(), b.readVarInt() - 1, b.readVarInt(),
						b.readVarInt(), b.readUtf(), b.readBoolean()));
		@Override public Type<LobbyPayload> type() { return TYPE; }
	}

	/** S2C キャラ選択画面を開け（像を右クリックしたとき。focus=最初に表示するキャラ）。 */
	public record OpenSelectPayload(String focus) implements CustomPacketPayload {
		public static final Type<OpenSelectPayload> TYPE = typeOf("open_select");
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenSelectPayload> CODEC = StreamCodec.of(
				(b, p) -> b.writeUtf(p.focus), b -> new OpenSelectPayload(b.readUtf()));
		@Override public Type<OpenSelectPayload> type() { return TYPE; }
	}

	/** C2S キャラを選んだ（選択画面の「選ぶ」）。lobby が受けて保存し LobbyPayload に反映。試合中は無視。 */
	public record PickPayload(String brawler) implements CustomPacketPayload {
		public static final Type<PickPayload> TYPE = typeOf("pick");
		public static final StreamCodec<RegistryFriendlyByteBuf, PickPayload> CODEC = StreamCodec.of(
				(b, p) -> b.writeUtf(p.brawler), b -> new PickPayload(b.readUtf()));
		@Override public Type<PickPayload> type() { return TYPE; }
	}

	/** BrawlMod.onInitialize の最初に1回だけ呼ぶ（司令塔）。 */
	public static void registerTypes() {
		PayloadTypeRegistry.clientboundPlay().register(HudPayload.TYPE, HudPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MatchPayload.TYPE, MatchPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(HiddenPayload.TYPE, HiddenPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(BarsPayload.TYPE, BarsPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LooksPayload.TYPE, LooksPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(KillFeedPayload.TYPE, KillFeedPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ResultPayload.TYPE, ResultPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LobbyPayload.TYPE, LobbyPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenSelectPayload.TYPE, OpenSelectPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(PickPayload.TYPE, PickPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AttackPayload.TYPE, AttackPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SuperPayload.TYPE, SuperPayload.CODEC);
	}
}

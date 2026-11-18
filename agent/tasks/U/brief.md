# 指示書 U: 本家風のUI（試合HUD・ロビー画面・キャラ選択・結果画面）
先に agent/tasks/COMMON.md と CONTRACT.md（末尾「第2弾」も）を読む。
目的: ブロスタらしい画面にする。今の client/hud（W6作）は作り直してよい（頭上表示・茂みで隠す・入力・名前タグ消しの機能は残す）。
担当範囲: src/client/java/com/amaguri/brawl/client/hud/**, src/client/java/com/amaguri/brawl/client/select/**, src/client/resources/brawl.hud.mixins.json, src/main/resources/assets/brawl/textures/gui/**, agent/lang/U.json, agent/preview/U/**
画風: 本家のような「ゲームのUI」。太い濃色の縁取り、鮮やかな黄・青・緑の面、少し斜めの帯（パネル）、大きく太い数字、アイコン。ブロスタ公式の絵・ロゴ・フォントは使わない（MCのフォントを拡大＋縁取り。日本語は影のみ＝今の outlined の扱いを踏襲）。アイコンやパネルは PNG を Python で描いて textures/gui に置いてよい
試合中:
- 開始: 「3」「2」「1」→「ファイト！」を画面中央に大きく（拡大→縮小のアニメ、黄色に濃い縁）
- 上中央: 斜めの帯に ドクロアイコン＋「残り 7」。その下に毒ガスの残り秒（縮み中は「毒ガス接近中！」点滅）
- 左上: キルログ（KillFeedPayload。最新4件、5秒で消える。「[キャラ顔]倒した人 ✕ [キャラ顔]倒された人」、毒ガスはガスのアイコン）
- 左下: 自分のキャラ顔（テクスチャから head ボーンの正面を切り出して拡大）＋キャラ名＋キューブ数（緑の立方体アイコン）
- 下中央: HP（大きな数字）とバー、弾3本（オレンジ、溜まり途中も伸びる）。必殺が満タンのとき「R 必殺技！」
- 右下: 必殺ボタン（大きな円、溜まるにつれ黄色で満たされ、満タンで光って脈打つ）と、その左に小さめの攻撃ボタン円（クリックのアイコン）
- ダメージ数字: BarsPayload の HP 減少を差分で拾い、その実体の頭上に赤い数字（自分が受けた分は白）がふわっと上がって消える
- 被弾時に画面の縁が一瞬赤く、毒ガスの中は緑
- 頭上表示（名前・HP・バー・自分の弾）は今の作りを維持して本家寄りに整える
- 脱落: 「#5 敗退」を大きく。観戦中はその表示を小さく上に
- 結果画面（ResultPayload）: 画面全体の半透明パネルに 大きく「#1 勝利！」または「#4」、トロフィー増減（+8 など、上がる演出）、全員の順位表（順位・キャラ顔・名前）。8秒または クリック/Esc で閉じる（Screen として作る）
ロビー（HudState.lobby.inLobby が true の間。試合HUDは出さない）:
- 左上: トロフィー（金のトロフィーアイコン＋数）、勝利数
- 下中央: 選んでいるキャラの顔と名前、「B: キャラ選択」
- 上中央: 準備の状態「準備完了 2/3」、カウントダウン中は大きく「5」…。自分が未準備なら「プレイ床に乗って準備完了！」
キャラ選択画面（client/select/SelectScreen）: キー B（キー設定「キャラ選択」）または OpenSelectPayload で開く。左に5人の顔の一覧（クリックで切替）、中央に選択中のキャラの3Dモデル（ゆっくり回転）、右に名前・役割・HP・通常攻撃と必殺技の説明（数値は Brawlers.get(id) から。技名・説明文は言語ファイル）、下に大きな「選ぶ」ボタン → PickPayload を送って閉じる
- 3Dモデル: client/model パッケージ（M0作。読むだけ）の BrawlerRenderer/BrawlerLooks を読み、GUI 内で描く方法を選ぶ（例: クライアント側だけの仮の実体を作り setId(負の値) して BrawlerLooks に登録し、InventoryScreen の実体描画と同様に描く。過去の学び: GUIに描くだけの実体は setId(負の仮ID) 必須）。難しければ正面の顔を大きく出す代替でもよいが、まず3Dを試す
- payload の受信登録（Hud/Match/Hidden/Bars/KillFeed/Result/Lobby/OpenSelect）は U がまとめて行い、HudState に public static で `hud`, `match`, `lobby` を置く（V が読む）
- 見下ろし視点: `com.amaguri.brawl.client.view.TopDown.active()`（V 作、並行中）が true のときは 左/右クリックの攻撃入力を U では扱わない（R の必殺は U のまま）。このクラスはまだ無いので契約の署名どおり呼ぶだけでよい
見本: agent/preview/U/ に match.png（試合中 1280x720）、lobby.png、select.png、result.png を Python で描き、自分で Read して本家らしさ・読みやすさを確認（実装の配置と同じ座標で）
合格基準:
- [ ] 上の要素がすべて実装され、見本4枚と配置が一致
- [ ] 試合・ロビー・それ以外で出る画面が正しく切り替わる（試合外・ロビー外では何も出ない）
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内

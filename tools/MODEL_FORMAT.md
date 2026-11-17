# キャラモデル形式 (brawler model)

プレイヤーにもボットにも着せる自作キャラモデルの共通形式。確認は `tools/model_preview.py`。

## ファイル
- モデル: `src/client/resources/assets/brawl/models/brawler/<id>.json`
- テクスチャ: `src/client/resources/assets/brawl/textures/entity/brawler/<id>.png`（64x64 か 128x128、RGBA。透明部分は描画しない）
- 見た目は完全オリジナル（公式の絵・ロゴ・キャラのデザインは使わない）。

## 座標系（重要）
- 単位はピクセル。16px = 1ブロック。
- x は**キャラの右が +X**、y は上が +、**キャラの正面は -Z**（背中が +Z）。右手系。
- 足元が y=0。全高 24〜34px。頭でっかちのデフォルメ（頭の幅は体の幅の 1.2〜1.6 倍）。
- **pivot と cube.from はすべてモデル全体の絶対座標**（待機姿勢での位置。親ボーンからの相対ではない）。
- cube の範囲は `from` 〜 `from + size`（x,y,z それぞれ）。`inflate` は全方向に等しく広げる（UV は元の size のまま）。

## JSON
```
{
  "texture_size": [64, 64],
  "bones": [ { bone }, ... ]
}
```
bone:
| キー | 内容 |
|---|---|
| name | ボーン名 |
| parent | 親ボーン名。無ければ null（省略可） |
| pivot | [x,y,z] 回転の中心（絶対座標） |
| rotation | [x,y,z] 度。待機姿勢の回転。pivot まわり。X→Y→Z の順に回す（右ねじ。X を + に回すと、pivot より下にある部分が**前(-Z)へ**振れる） |
| cubes | cube の配列（飾りだけのボーンは空でよい） |

cube: `{"from":[x,y,z], "size":[w,h,d], "uv":[u,v], "inflate":0, "mirror":false}`
w = X方向の幅、h = Y方向の高さ、d = Z方向の奥行き。`inflate` と `mirror` は省略可。

親子: 子の最終位置は「親の回転 × 自分の回転（どちらも各自の pivot まわり）」。親が動けば子も付いてくる。

## 固定ボーン名（全員必須）
`body`, `head`, `right_arm`, `left_arm`, `right_leg`, `left_leg`, `weapon`
- `weapon` の parent は必ず `right_arm`。武器を持たせたい位置（手の先）に pivot を置く。武器の形そのものは cube で作ってよい（空でも可）。
- 他の5つは parent なし（または body）。
- 任意の飾り（`hat`, `tail`, `cape` 等）は自由に追加し、`parent` に付ける先（例 `head`, `body`）を指定する。

## 歩きの前提
- 腕・脚の歩きは、それぞれの pivot まわりの **X回転だけ**で付ける（右腕+/左腕−/右脚−/左脚+ のように交互）。
- したがって pivot は**肩・股の位置**（腕の付け根・脚の付け根）に置く。腕・脚の cube はその pivot から下に伸ばす。
- 確認用レンダラーの歩きポーズは右腕+35°・左腕−35°・右脚−35°・左脚+35°。

## UV（バニラの箱UV）
Minecraft の `CubeListBuilder.texOffs(u,v).addBox(..., w, h, d)` と同じ展開。uv=[u,v] はテクスチャのピクセル座標で、w,h,d の cube は次の矩形を使う（左上原点、右が +u、下が +v）:

```
         u+d       u+d+w     u+d+2w
 v     +---------+---------+
       |   上(T)  |   下(D)  |        各 w x d
 v+d   +----+----+----+----+----+
       | 右 | 前 | 左 | 後 |        右・左 = d x h、前・後 = w x h
 v+d+h +----+----+----+----+
       u   u+d  u+d+w u+2d+w  u+2d+2w
```
- 上段: 上(T) は x=u+d〜u+d+w、下(D) は x=u+d+w〜u+d+2w、y=v〜v+d。
- 下段 (y=v+d〜v+d+h): 右 = x u〜u+d、前 = u+d〜u+d+w、左 = u+d+w〜u+2d+w、後 = u+2d+w〜u+2d+2w。
- 「右」=キャラの右側の面(+X)、「前」=正面(-Z)、「左」=-X、「後」=+Z。
- 前・後・左・右の面は、絵の上が cube の上。上面は絵の下端が前側、下面は絵の上端が前側。
- 全体の占有幅 = 2d+2w、高さ = d+h。他の cube の矩形と重ならないよう配置する。
- `mirror:true` は cube を左右(X)反転して貼る（左右対称パーツで右腕の絵を左腕に流用するとき用）。使わなくてもよい（その場合は別の uv に絵を描く）。

## 例（全高26px、頭幅12/体幅8=1.5倍、帽子・しっぽ付き）
64x64 のテクスチャ上の配置: head(0,0) body(0,20) right_arm(24,20) left_arm(40,20) right_leg(0,32) left_leg(16,32) weapon(0,44) hat(24,44) tail(44,32)。
```json
{
  "texture_size": [64, 64],
  "bones": [
    {"name": "body", "parent": null, "pivot": [0, 8, 0], "rotation": [0, 0, 0],
     "cubes": [{"from": [-4, 8, -2], "size": [8, 8, 4], "uv": [0, 20]}]},
    {"name": "head", "parent": null, "pivot": [0, 16, 0], "rotation": [0, 0, 0],
     "cubes": [{"from": [-6, 16, -5], "size": [12, 10, 10], "uv": [0, 0]}]},
    {"name": "right_arm", "parent": null, "pivot": [6, 15, 0], "rotation": [0, 0, 0],
     "cubes": [{"from": [4, 7, -2], "size": [4, 8, 4], "uv": [24, 20]}]},
    {"name": "left_arm", "parent": null, "pivot": [-6, 15, 0], "rotation": [0, 0, 0],
     "cubes": [{"from": [-8, 7, -2], "size": [4, 8, 4], "uv": [40, 20]}]},
    {"name": "right_leg", "parent": null, "pivot": [2, 8, 0], "rotation": [0, 0, 0],
     "cubes": [{"from": [0, 0, -2], "size": [4, 8, 4], "uv": [0, 32]}]},
    {"name": "left_leg", "parent": null, "pivot": [-2, 8, 0], "rotation": [0, 0, 0],
     "cubes": [{"from": [-4, 0, -2], "size": [4, 8, 4], "uv": [16, 32]}]},
    {"name": "weapon", "parent": "right_arm", "pivot": [6, 8, -1], "rotation": [0, 0, 0],
     "cubes": [{"from": [5, 7, -11], "size": [2, 2, 8], "uv": [0, 44]}]},
    {"name": "hat", "parent": "head", "pivot": [0, 26, 0], "rotation": [0, 0, 0],
     "cubes": [{"from": [-3, 26, -3], "size": [6, 3, 6], "uv": [24, 44]}]},
    {"name": "tail", "parent": "body", "pivot": [0, 10, 2], "rotation": [30, 0, 0],
     "cubes": [{"from": [-1, 8, 2], "size": [2, 2, 6], "uv": [44, 32]}]}
  ]
}
```
（weapon は手の先の位置に pivot を置き、cube は最初から前(-Z)向きに書いてある。）

## 確認手順
```
python3 tools/model_preview.py <id>.json <id>.png out.png
```
正面・背面・左・右・斜め上前（遠近）・歩きポーズの6枚が1枚に並ぶ。stderr に警告（必須ボーン欠落・UVはみ出し・全高/頭身比の範囲外）が出る。正面・歩き姿を実際に見て、顔や模様が正しい面に出ているか、手足が pivot（肩・股）で回っているかを確認する。
`tools/sample/` に見本（バニラ形状 `vanilla.json`、面ラベル付き `test_tex.png`、顔付き `face_tex.png`、各プレビュー）がある。

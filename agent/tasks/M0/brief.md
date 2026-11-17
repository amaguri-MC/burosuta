# 指示書 M0: キャラモデルの形式と確認用レンダラー（第1段）
先に agent/tasks/COMMON.md を読む。
背景: 5人のキャラ（性能は本家ブロスタ準拠、見た目は完全オリジナル）を、プレイヤーにもボットにも着せる自作モデルにする。このあと5人の作業役が1人1キャラずつモデルを作るので、その共通の形式と確認ツールを先に作る。
担当範囲: tools/model_preview.py, tools/MODEL_FORMAT.md, tools/sample/（見本1体）
形式（tools/MODEL_FORMAT.md に書く）:
- ファイル: src/client/resources/assets/brawl/models/brawler/<id>.json と textures/entity/brawler/<id>.png（64x64 か 128x128）
- 単位はピクセル（16px=1ブロック）。y上向き、足元 y=0、キャラの正面は -Z。全高は 24〜34px（頭でっかちのデフォルメ。頭は体の1.2〜1.6倍幅）
- bones: 固定名 body, head, right_arm, left_arm, right_leg, left_leg, weapon（weapon は right_arm の子）＋任意の飾り（hat, tail 等。parent を指定）。各 bone: name, parent, pivot[x,y,z], rotation[x,y,z]（度、待機姿勢）, cubes[{from[x,y,z], size[w,h,d], uv[u,v], inflate, mirror}]
- UV はバニラの箱UV（uv の位置から 上・下・左右前後 を size で展開する、Minecraft の CubeListBuilder.texOffs と同じ並び）
- 腕・脚の振り（歩き）は pivot まわりの X 回転で付ける前提。pivot は肩・股の位置に置く
model_preview.py:
- `python3 tools/model_preview.py <model.json> <texture.png> <out.png>` で、正面・背面・左・右・斜め上前（遠近あり）・歩き途中ポーズ の6枚を1枚の画像に並べる（各 320x400 程度、背景は明るい灰色、面ごとに軽い陰影、テクスチャは最近傍拡大）
- 箱UVの展開と回転（親子の合成）を正しく。描画は PIL だけ（numpy があれば使ってよい、無ければ純Python）。Z順はポリゴンごとの深度ソートで可
- 自己検査: tools/sample/ に、バニラのプレイヤー形状（頭8x8x8 uv0,0 / 胴8x12x4 uv16,16 / 右腕4x12x4 uv40,16 / 左腕 uv32,48 / 右脚 uv0,16 / 左脚 uv16,48、64x64）の見本と、各部位の各面に「部位名の頭文字＋面（F前/B後/L/R/T上/D下）」の文字と別々の色を描いたテスト用テクスチャを作る（バニラ皮の箱UVの並び＝上と下が上段、下段が 右・前・左・後 の順。展開の向きは Minecraft の ModelPart と一致させる）。プレビュー tools/sample/test_preview.png を自分で Read し、正面に F、背面に B、上に T が正しい向きで見えることを確認。顔（目・口）を描いた版でも1枚出す
合格基準:
- [ ] test_preview.png で全部位の面の文字が正しい面・向きに出る
- [ ] MODEL_FORMAT.md だけを読めば別の人がモデルを作れる（例の JSON 付き）
返答: 「完了 / 成果物 / 不確かな点」を3行以内

# 指示書 W2: brawler（5キャラの通常攻撃と必殺技・武器アイテム）
先に agent/tasks/COMMON.md を読む。
目的: CONTRACT.md「W2 brawler」を実装。弾は core の Projectiles.spawn(Shot) で飛ばし、ダメージは Combat.damage / areaDamage。
担当範囲: src/main/java/com/amaguri/brawl/brawler/**, src/main/resources/assets/brawl/{items,models/item,textures/item}/**, agent/lang/W2.json, agent/preview/W2/**
成果物: brawler パッケージ一式、武器5種のアイテム（テクスチャ・モデル・items 定義）、agent/preview/W2/items.png（5つの武器を8倍に拡大して並べた確認画像）
数値（ブロスタ準拠。タイル単位は Tiles.b() でブロックへ。弾速はブロック/tick）:
- shelly シェリー HP3600 速さ2.4 / 通常: 散弾5発×300 射程7.67 扇30° 弾速1.0 / リロード1.5秒 / 必殺: 散弾9発×320 射程7.67 扇40° 当たった敵を3タイル押す・壁を壊す / ゲージ 1発命中ごと0.067
- colt コルト HP2800 速さ2.4 / 通常: 6発×360を2tick間隔で一直線（ずれ±2°）射程9.33 弾速1.2 / リロード1.6秒 / 必殺: 12発×360 射程12.67 壁を壊す / 0.042
- bull ブル HP5000 速さ2.57 / 通常: 散弾5発×400 射程5.33 扇25° / リロード1.6秒 / 必殺: 10タイル突進（約0.6秒）触れた敵に800と押し出し・壁を壊す / 0.0625
- brock ブロック HP2400 速さ2.4 / 通常: ロケット1発 射程9.67 弾速0.8 着弾で半径1タイル1040（OnEnd で areaDamage）/ リロード2.1秒 / 必殺: 前方7.67タイル先を中心に半径3タイルへロケット9発を順に降らせる 各1040 壁を壊す / 0.25
- elprimo エルプリモ HP6000 速さ2.57 / 通常: パンチ4発×380 射程3 弾速0.9 / リロード1.1秒 / 必殺: 最大6タイル先へ跳んで着地、半径1.5タイルに800と押し出し・壁を壊す（跳ぶ間は放物線で移動、ボットにも効くよう LivingEntity で） / 0.0625
- 全員 maxAmmo 3、attackCooldownTicks 8
- パーティクルと音は各キャラらしく（散弾=オレンジ火花、コルト=黄色の光線、ロケット=煙と爆発、パンチ=打撃）。バニラの ParticleTypes / SoundEvents のみ
制約:
- 攻撃はプレイヤーとボット（LivingEntity）で同じ処理。Combat.consumeAmmo / consumeSuper を通す
- AttackPayload/SuperPayload を ServerPlayNetworking で受信（試合外・脱落後は無視）
- 武器アイテムは見た目だけ（右クリック等で何も起きない）。テクスチャは一般的な散弾銃・リボルバー・大型散弾銃・ロケットランチャー・ボクシンググローブ
合格基準:
- [ ] CONTRACT の W2 署名が一字一句そのまま存在
- [ ] 5キャラの通常・必殺が上の数値どおり（数値はこのパッケージの1か所に集約）
- [ ] items.png を自分で Read して形が分かることを確認した
- [ ] 26.2 API が apidump に実在（UNVERIFIED は返答に列挙）
返答: 「完了 / 成果物 / 不確かな点」を3行以内

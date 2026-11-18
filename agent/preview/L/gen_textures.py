"""ロビー用ブロックのテクスチャ(16x16)と blockstate/model/item 定義を書く。python3 -I gen_textures.py"""
import json, os
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.normpath(os.path.join(HERE, "../../../src/main/resources/assets/brawl"))
NAMES = ["lobby_tile_a", "lobby_tile_b", "lobby_edge", "lobby_pad", "lobby_pedestal"]

def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)

def base(c):
    im = Image.new("RGB", (16, 16), c)
    return im, im.load()

def frame(p, c, dark=0.82, light=1.06):
    """縁取りは黒でなく、同じ色相を暗く/明るくした色。"""
    d, l = shade(c, dark), shade(c, light)
    for k in range(16):
        p[k, 0] = l; p[0, k] = l
        p[k, 15] = d; p[15, k] = d

def tile_a():
    c = (255, 243, 210)
    im, p = base(c)
    for y in range(16):
        for x in range(16):
            if (x * 7 + y * 13) % 11 == 0: p[x, y] = shade(c, 0.97)
    frame(p, c, 0.88, 1.0)
    for k in range(2, 14):
        p[k, 1] = (255, 250, 232); p[1, k] = (255, 250, 232)
    return im

def tile_b():
    c = (196, 228, 255)
    im, p = base(c)
    for y in range(16):
        for x in range(16):
            if (x * 5 + y * 9) % 13 == 0: p[x, y] = shade(c, 0.97)
    frame(p, c, 0.86, 1.03)
    for k in range(2, 14):
        p[k, 1] = (225, 243, 255); p[1, k] = (225, 243, 255)
    return im

def edge():
    c = (255, 150, 40)
    im, p = base(c)
    y2 = (255, 214, 70)
    for y in range(16):
        for x in range(16):
            if ((x + y) // 4) % 2 == 0: p[x, y] = y2     # 斜めの縞
    frame(p, (240, 120, 30), 0.8, 1.05)
    return im

def pad():
    c = (255, 232, 80)
    im, p = base(c)
    frame(p, (255, 190, 40), 0.85, 1.0)
    for k in range(1, 15):
        p[k, 1] = (255, 190, 40); p[k, 14] = (255, 190, 40); p[1, k] = (255, 190, 40); p[14, k] = (255, 190, 40)
    # 中央に右向き三角（プレイ）
    for y in range(4, 12):
        w = 6 - abs(y - 7.5) * 1.6
        for x in range(5, int(5 + max(0, w)) + 1):
            p[x, y] = (255, 255, 235)
    for y in range(4, 12):
        w = 6 - abs(y - 7.5) * 1.6
        p[4, y] = (255, 205, 60)
    return im

def pedestal():
    c = (122, 104, 214)
    im, p = base(c)
    for y in range(16):
        for x in range(16):
            if (x + y) % 8 == 0: p[x, y] = shade(c, 1.1)
    frame(p, c, 0.72, 1.15)
    for k in range(3, 13):  # 上辺と下辺に明るい帯
        p[k, 3] = (170, 150, 245); p[k, 12] = (92, 78, 176)
    return im

TEX = {"lobby_tile_a": tile_a, "lobby_tile_b": tile_b, "lobby_edge": edge, "lobby_pad": pad, "lobby_pedestal": pedestal}

def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False); f.write("\n")

sheet = Image.new("RGB", (16 * 5 * 6, 16 * 6), (40, 40, 48))
for n in NAMES:
    im = TEX[n]()
    os.makedirs(f"{RES}/textures/block", exist_ok=True)
    im.save(f"{RES}/textures/block/{n}.png")
    w(f"{RES}/blockstates/{n}.json", {"variants": {"": {"model": f"brawl:block/{n}"}}})
    w(f"{RES}/models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"brawl:block/{n}"}})
    w(f"{RES}/items/{n}.json", {"model": {"type": "minecraft:model", "model": f"brawl:block/{n}"}})
for k, n in enumerate(NAMES):
    im = Image.open(f"{RES}/textures/block/{n}.png").resize((96, 96), Image.NEAREST)
    sheet.paste(im, (k * 96, 0))
sheet.crop((0, 0, 480, 96)).save(os.path.join(HERE, "blocks.png"))
print("ok")

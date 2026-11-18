#!/usr/bin/env python3
"""相棒のクマ nitabear（鎧を着た大きなシロクマ）のモデルJSONとテクスチャを作る。
使い方: python3 agent/preview/NB/gen_bear.py  → models/brawler/nitabear.json, textures/entity/brawler/nitabear.png
"""
import json, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "..", "src", "client", "resources", "assets", "brawl")
TS = 128
random.seed(7)

# 毛: 白に青みの影（縁取りは隣の色を暗くした色相）
FUR = (236, 241, 246); FUR_D = (205, 216, 230); FUR_DD = (170, 186, 208)
STEEL = (136, 146, 162); STEEL_L = (178, 188, 202); STEEL_D = (88, 98, 118); STEEL_DD = (58, 66, 88)
GOLD = (196, 160, 80); GOLD_D = (140, 108, 52)
NOSE = (38, 44, 66); EYE = (28, 34, 58); PINK = (214, 150, 160)

# bone, from, size, kind
BONES = {
 "body": {"parent": None, "pivot": [0, 18, 0]},
 "head": {"parent": None, "pivot": [0, 22, -9]},
 "right_arm": {"parent": None, "pivot": [5, 12, -5]},
 "left_arm": {"parent": None, "pivot": [-5, 12, -5]},
 "right_leg": {"parent": None, "pivot": [4, 12, 7]},
 "left_leg": {"parent": None, "pivot": [-4, 12, 7]},
 "weapon": {"parent": "right_arm", "pivot": [5, 1, -9]},
 "back_armor": {"parent": "body", "pivot": [0, 25, 1]},
 "helmet": {"parent": "head", "pivot": [0, 27, -12]},
}
CUBES = [
 ("body", [-5, 12, -8], [10, 13, 18], "fur"),
 ("head", [-6, 17, -17], [12, 10, 9], "head"),
 ("head", [-3, 17, -21], [6, 5, 4], "snout"),
 ("head", [-8, 25, -13], [3, 3, 2], "ear"),
 ("head", [5, 25, -13], [3, 3, 2], "ear"),
 ("right_arm", [2, 0, -8], [6, 12, 6], "fur"),
 ("right_arm", [1, 0, -9], [8, 4, 8], "cuff"),
 ("left_arm", [-8, 0, -8], [6, 12, 6], "fur"),
 ("left_arm", [-9, 0, -9], [8, 4, 8], "cuff"),
 ("right_leg", [1, 0, 4], [5, 12, 6], "fur"),
 ("right_leg", [0, 0, 3], [7, 3, 8], "cuff"),
 ("left_leg", [-6, 0, 4], [5, 12, 6], "fur"),
 ("left_leg", [-7, 0, 3], [7, 3, 8], "cuff"),
 ("back_armor", [-7, 25, -8], [14, 2, 18], "plate"),
 ("back_armor", [-2, 27, -6], [4, 2, 14], "ridge"),
 ("helmet", [-7, 26, -17], [14, 4, 10], "plate"),
 ("helmet", [-1, 20, -22], [2, 7, 2], "nasal"),
]

def pack():
    """棚詰めで UV を割り当てる。"""
    x = y = rowh = 0
    out = []
    for b, fr, sz, kind in CUBES:
        w, h, d = sz
        w, h, d = int(round(w + 0.4)), int(round(h + 0.4)), int(round(d + 0.4))   # 端数サイズは切り上げ枠
        W, H = 2 * d + 2 * w, d + h
        if x + W > TS:
            x = 0; y += rowh; rowh = 0
        out.append((x, y))
        x += W; rowh = max(rowh, H)
    assert y + rowh <= TS, (y, rowh)
    return out

def noise(c, amt):
    n = random.randint(-amt, amt)
    return tuple(max(0, min(255, v + n)) for v in c)

def fill(img, x0, y0, w, h, base, amt=5, shade=0):
    for yy in range(h):
        for xx in range(w):
            c = noise(base, amt)
            if shade: c = tuple(max(0, min(255, v + shade)) for v in c)
            img.putpixel((x0 + xx, y0 + yy), c + (255,))

def border(img, x0, y0, w, h, col, sides="tblr"):
    for xx in range(w):
        if "t" in sides: img.putpixel((x0 + xx, y0), col + (255,))
        if "b" in sides: img.putpixel((x0 + xx, y0 + h - 1), col + (255,))
    for yy in range(h):
        if "l" in sides: img.putpixel((x0, y0 + yy), col + (255,))
        if "r" in sides: img.putpixel((x0 + w - 1, y0 + yy), col + (255,))

def faces(u, v, w, h, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}

def paint(img, uv, sz, kind):
    w, h, d = [int(round(s + 0.4)) for s in sz]
    F = faces(uv[0], uv[1], w, h, d)
    shade = {"top": 12, "bottom": -26, "front": 0, "back": -8, "right": -10, "left": -10}
    for name, (x, y, fw, fh) in F.items():
        s = shade[name]
        if kind in ("fur", "head", "snout", "ear"):
            fill(img, x, y, fw, fh, FUR, 4, s)
            # 毛のかたまり（暗い毛束）
            for _ in range(max(2, fw * fh // 14)):
                px, py = x + random.randrange(fw), y + random.randrange(fh)
                img.putpixel((px, py), noise(FUR_D, 4) + (255,))
            if name in ("bottom", "right", "left") or (name == "front" and kind == "fur"):
                border(img, x, y, fw, fh, FUR_DD, "b" if name != "bottom" else "tblr")
        elif kind == "cuff":      # 前足・後足の鉄の足甲
            fill(img, x, y, fw, fh, STEEL, 5, s)
            border(img, x, y, fw, fh, STEEL_D, "tblr")
            if fw > 4 and fh > 2: img.putpixel((x + fw // 2, y + fh // 2), STEEL_L + (255,))
        elif kind in ("plate", "ridge", "nasal"):
            fill(img, x, y, fw, fh, STEEL, 5, s)
            border(img, x, y, fw, fh, STEEL_D if name != "bottom" else STEEL_DD, "tblr")
            if name in ("top", "front") and fw > 6:      # リベット
                for rx in range(x + 2, x + fw - 1, 4):
                    img.putpixel((rx, y + (1 if fh > 2 else 0)), GOLD + (255,))
            if name == "top" and fh > 6 and fw > 6:      # 板のつなぎ目
                for yy in range(y + 1, y + fh - 1):
                    img.putpixel((x + fw // 2, yy), STEEL_D + (255,))
    # 顔・模様
    if kind == "head":
        x, y, fw, fh = F["front"]
        for ex in (x + 1, x + fw - 3):                       # 目: 2x2 の濃紺＋ハイライト
            for dx in (0, 1):
                for dy in (0, 1):
                    img.putpixel((ex + dx, y + 2 + dy), EYE + (255,))
            img.putpixel((ex, y + 2), (210, 225, 245, 255))
        for dx in range(fw):                                 # 頬の影
            img.putpixel((x + dx, y + fh - 1), FUR_DD + (255,))
    if kind == "snout":
        x, y, fw, fh = F["front"]
        for dx in range(1, fw - 1):                          # 鼻
            img.putpixel((x + dx, y), NOSE + (255,))
            img.putpixel((x + dx, y + 1), NOSE + (255,))
        img.putpixel((x + fw // 2, y + 3), PINK + (255,))    # 口
        img.putpixel((x + fw // 2 - 1, y + 3), FUR_DD + (255,))
        img.putpixel((x + fw // 2 + 1, y + 3), FUR_DD + (255,))
    if kind == "ear":
        for name in ("front",):
            x, y, fw, fh = F[name]
            fill(img, x + 1, y + 1, max(1, fw - 2), max(1, fh - 2), PINK, 4)

def main():
    uvs = pack()
    img = Image.new("RGBA", (TS, TS), (0, 0, 0, 0))
    bones = {n: dict(name=n, parent=b["parent"], pivot=b["pivot"], rotation=[0, 0, 0], cubes=[]) for n, b in BONES.items()}
    for (b, fr, sz, kind), uv in zip(CUBES, uvs):
        paint(img, uv, sz, kind)
        bones[b]["cubes"].append({"from": fr, "size": sz, "uv": list(uv), "inflate": 0})
    os.makedirs(os.path.join(ROOT, "models", "brawler"), exist_ok=True)
    os.makedirs(os.path.join(ROOT, "textures", "entity", "brawler"), exist_ok=True)
    with open(os.path.join(ROOT, "models", "brawler", "nitabear.json"), "w") as f:
        json.dump({"texture_size": [TS, TS], "bones": list(bones.values())}, f, indent=1)
    img.save(os.path.join(ROOT, "textures", "entity", "brawler", "nitabear.png"))

main()

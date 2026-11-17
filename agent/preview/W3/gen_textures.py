#!/usr/bin/env python3
"""W3 テクスチャ(16x16)を描き、blocks.png(8倍)も出す。"""
import random, colorsys, os
from PIL import Image, ImageDraw, ImageFont
BASE = "/home/claude/burosuta/src/main/resources/assets/brawl/textures/block"
PREV = "/home/claude/burosuta/agent/preview/W3"
os.makedirs(BASE, exist_ok=True)
S = 16

def clamp(v): return max(0, min(255, int(v)))
def shade(c, k): return tuple(clamp(x * k) for x in c)
def hue_shift_dark(c, k, dh=-0.02):
    h, l, s = colorsys.rgb_to_hls(*[x / 255 for x in c])
    r, g, b = colorsys.hls_to_rgb((h + dh) % 1, max(0, l * k), min(1, s * 1.05))
    return (clamp(r * 255), clamp(g * 255), clamp(b * 255))
def mix(a, b, t): return tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3))

def floor(seed, base):
    rnd = random.Random(seed)
    im = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            im.putpixel((x, y), base + (255,)) if False else im.putpixel((x, y), tuple(clamp(v + rnd.randint(-4, 4)) for v in base) + (255,))
    for _ in range(14):  # 草の短い線
        x, y = rnd.randrange(S), rnd.randrange(S)
        im.putpixel((x, y), hue_shift_dark(base, 0.82) + (255,))
        im.putpixel((x, (y - 1) % S), mix(base, (200, 240, 130), 0.5) + (255,))
    return im

def rock(seed, base, n=7, crack_k=0.62):
    rnd = random.Random(seed)
    pts = [(rnd.uniform(0, S), rnd.uniform(0, S)) for _ in range(n)]
    tone = [rnd.uniform(0.92, 1.1) for _ in pts]
    def d2(x, y, p):
        dx = min(abs(x - p[0]), S - abs(x - p[0])); dy = min(abs(y - p[1]), S - abs(y - p[1]))
        return dx * dx + dy * dy
    im = Image.new("RGBA", (S, S))
    for y in range(S):
        for x in range(S):
            ds = sorted((d2(x + .5, y + .5, p), i) for i, p in enumerate(pts))
            (a, i), (b, _) = ds[0], ds[1]
            col = shade(base, tone[i])
            gap = (b ** .5) - (a ** .5)
            if gap < 0.9: col = hue_shift_dark(base, crack_k, -0.015)
            elif gap < 1.8: col = mix(col, hue_shift_dark(base, crack_k, -0.015), .25)
            # 左上ハイライト
            px, py = pts[i]
            if x + .5 < px - 0.5 and y + .5 < py: col = mix(col, (255, 245, 220), .12)
            col = tuple(clamp(v + rnd.randint(-5, 5)) for v in col)
            im.putpixel((x, y), col + (255,))
    return im

def bush(seed):
    rnd = random.Random(seed)
    dark, mid, light = (22, 82, 40), (36, 118, 52), (70, 168, 70)
    im = Image.new("RGBA", (S, S), dark + (255,))
    px = im.load()
    for _ in range(26):  # 葉の塊
        cx, cy = rnd.randrange(S), rnd.randrange(S)
        col = rnd.choice([mid, mid, light, dark])
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0)]:
            px[((cx + dx) % S, (cy + dy) % S)] = col + (255,)
    for _ in range(14):  # ハイライト点
        px[(rnd.randrange(S), rnd.randrange(S))] = (110, 205, 90, 255)
    holes = 0
    while holes < 22:  # 透明の穴(cutout)
        x, y = rnd.randrange(S), rnd.randrange(S)
        px[(x, y)] = (0, 0, 0, 0); holes += 1
        if rnd.random() < .4: px[((x + 1) % S, y)] = (0, 0, 0, 0)
    return im

def box():
    wood, frame, edge = (168, 112, 62), (112, 70, 38), (74, 44, 28)
    rnd = random.Random(5)
    im = Image.new("RGBA", (S, S))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 15, 15], fill=wood + (255,))
    for y in range(S):  # 板目
        if y % 4 == 3: d.line([(0, y), (15, y)], fill=hue_shift_dark(wood, .86) + (255,))
    for _ in range(18):
        x, y = rnd.randrange(S), rnd.randrange(S); im.putpixel((x, y), hue_shift_dark(wood, .9) + (255,))
    d.rectangle([0, 0, 15, 15], outline=edge + (255,))
    d.rectangle([1, 1, 14, 14], outline=frame + (255,))
    d.rectangle([2, 2, 13, 13], outline=mix(frame, wood, .5) + (255,))
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]: im.putpixel((x, y), (200, 200, 190, 255))
    # 緑の光る模様(ひし形+十字)
    cx = cy = 7.5
    for y in range(S):
        for x in range(S):
            dist = abs(x - cx) + abs(y - cy)
            if 3 <= x <= 12 and 3 <= y <= 12:
                if dist <= 5.5:
                    t = 1 - dist / 5.5
                    core = mix((40, 150, 70), (200, 255, 160), t)
                    if dist > 4: core = mix(frame, (40, 170, 80), .7)
                    im.putpixel((x, y), core + (255,))
                elif dist <= 7.5:
                    im.putpixel((x, y), mix(im.getpixel((x, y))[:3], (70, 220, 100), .28) + (255,))
    for k in (7, 8):
        for y in (4, 11): im.putpixel((k, y), (150, 255, 130, 255)); 
    return im

def rope():
    im = Image.new("RGBA", (S, S))
    d = ImageDraw.Draw(im)
    rope_c = (214, 182, 112)
    for y in range(0, 3):  # ロープ(上3行・横いっぱい)
        for x in range(S):
            c = rope_c if (x + y) % 4 < 2 else hue_shift_dark(rope_c, .78)
            im.putpixel((x, y), c + (255,))
    wood = (150, 104, 60)
    for y in range(3, S):
        for x in range(S):
            c = wood if (x // 2) % 2 == 0 else hue_shift_dark(wood, .88)
            if y == 3: c = hue_shift_dark(wood, .7)
            im.putpixel((x, y), c + (255,))
    return im

def power_cube():
    im = Image.new("RGBA", (S, S))
    d = ImageDraw.Draw(im)
    out, mid, inn = (24, 134, 70), (60, 214, 96), (178, 255, 170)
    d.rectangle([0, 0, 15, 15], fill=out + (255,))
    d.rectangle([1, 1, 14, 14], fill=mid + (255,))
    d.rectangle([3, 3, 12, 12], fill=hue_shift_dark(mid, .85) + (255,))
    d.rectangle([4, 4, 11, 11], fill=inn + (255,))
    d.rectangle([6, 6, 9, 9], fill=(235, 255, 225, 255))
    for k in range(S):  # 十字の光
        for (x, y) in [(k, 7), (k, 8)]:
            if not (3 <= x <= 12):
                im.putpixel((x, y), mix(mid, (220, 255, 210), .6) + (255,))
        for (x, y) in [(7, k), (8, k)]:
            if not (3 <= y <= 12):
                im.putpixel((x, y), mix(mid, (220, 255, 210), .6) + (255,))
    for (x, y) in [(1, 1), (14, 1), (1, 14), (14, 14)]: im.putpixel((x, y), (140, 255, 150, 255))
    return im

tex = {
    "floor_a": floor(1, (126, 200, 74)),
    "floor_b": floor(2, (104, 176, 60)),
    "wall": rock(11, (204, 162, 106), n=11, crack_k=.7),
    "border": rock(12, (96, 80, 88), n=9, crack_k=.65),
    "bush": bush(3),
    "box": box(),
    "rope": rope(),
    "power_cube": power_cube(),
}
for n, im in tex.items():
    im.save(os.path.join(BASE, n + ".png"))

# blocks.png: 各テクスチャ8倍、グレー地(透明確認)、名前
K = 8; cell = S * K; pad = 10; cols = 4; rows = 2
sheet = Image.new("RGB", (cols * (cell + pad) + pad, rows * (cell + pad + 14) + pad), (60, 60, 70))
dr = ImageDraw.Draw(sheet)
for i, (n, im) in enumerate(tex.items()):
    x = pad + (i % cols) * (cell + pad); y = pad + (i // cols) * (cell + pad + 14)
    bg = Image.new("RGBA", (cell, cell), (255, 0, 255, 255))  # マゼンタ=透明部分
    big = im.resize((cell, cell), Image.NEAREST)
    bg.alpha_composite(big)
    sheet.paste(bg.convert("RGB"), (x, y))
    dr.text((x, y + cell + 1), n, fill=(240, 240, 240))
sheet.save(os.path.join(PREV, "blocks.png"))
print("ok", list(tex))

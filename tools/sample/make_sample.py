#!/usr/bin/env python3
"""見本の生成: vanilla.json(バニラ形状) / test_tex.png(面ラベル) / face_tex.png(顔つき)。
部位ラベル: H=頭 B=胴 R=右腕 L=左腕 P=右脚 S=左脚 / 面: F前 B後 L左 R右 T上 D下
実行: python3 tools/sample/make_sample.py"""
import colorsys
import json
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))

# name: (uv, size, from, pivot, letter, hue)
PARTS = {
    "head": ((0, 0), (8, 8, 8), (-4, 24, -4), (0, 24, 0), "H", 0.00),
    "body": ((16, 16), (8, 12, 4), (-4, 12, -2), (0, 24, 0), "B", 0.58),
    "right_arm": ((40, 16), (4, 12, 4), (4, 12, -2), (6, 22, 0), "R", 0.33),
    "left_arm": ((32, 48), (4, 12, 4), (-8, 12, -2), (-6, 22, 0), "L", 0.12),
    "right_leg": ((0, 16), (4, 12, 4), (0, 0, -2), (2, 12, 0), "P", 0.78),
    "left_leg": ((16, 48), (4, 12, 4), (-4, 0, -2), (-2, 12, 0), "S", 0.92),
}

G = {
    "H": ["#.#", "#.#", "###", "#.#", "#.#"],
    "B": ["##.", "#.#", "##.", "#.#", "##."],
    "R": ["##.", "#.#", "##.", "#.#", "#.#"],
    "L": ["#..", "#..", "#..", "#..", "###"],
    "P": ["##.", "#.#", "##.", "#..", "#.."],
    "S": ["###", "#..", "###", "..#", "###"],
    "F": ["###", "#..", "##.", "#..", "#.."],
    "T": ["###", ".#.", ".#.", ".#.", ".#."],
    "D": ["##.", "#.#", "#.#", "#.#", "##."],
}
G4 = {"T": ["###", ".#.", ".#.", ".#."], "D": ["##.", "#.#", "#.#", "##."]}


def regions(uv, size):
    u, v = uv
    w, h, d = size
    return {
        "T": (u + d, v, w, d), "D": (u + d + w, v, w, d),
        "R": (u, v + d, d, h), "F": (u + d, v + d, w, h),
        "L": (u + d + w, v + d, d, h), "B": (u + 2 * d + w, v + d, w, h),
    }


def glyph(img, g, x, y, col):
    for j, row in enumerate(g):
        for i, c in enumerate(row):
            if c == "#":
                img.putpixel((x + i, y + j), col)


def make_test():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    light = {"F": 0.95, "B": 0.55, "R": 0.8, "L": 0.65, "T": 1.0, "D": 0.4}
    for part, (uv, size, _f, _p, letter, hue) in PARTS.items():
        for face, (x, y, w, h) in regions(uv, size).items():
            bg = tuple(int(c * 255) for c in colorsys.hsv_to_rgb(hue, 0.55, light[face]))
            lum = 0.3 * bg[0] + 0.59 * bg[1] + 0.11 * bg[2]
            fg = (20, 20, 20, 255) if lum > 120 else (255, 255, 255, 255)
            for j in range(h):
                for i in range(w):
                    img.putpixel((x + i, y + j), bg + (255,))
            if w >= 7 and h >= 5:
                glyph(img, G[letter], x + (w - 7) // 2, y + (h - 5) // 2, fg)
                glyph(img, G[face], x + (w - 7) // 2 + 4, y + (h - 5) // 2, fg)
            elif w >= 3 and h >= 11:
                gx = x + (w - 3) // 2
                gy = y + (h - 11) // 2
                glyph(img, G[letter], gx, gy, fg)
                glyph(img, G[face], gx, gy + 6, fg)
            elif h == 4:
                glyph(img, G4[face], x + (w - 3) // 2, y, fg)
            else:
                glyph(img, G[face], x + (w - 3) // 2, y + (h - 5) // 2, fg)
    img.save(os.path.join(HERE, "test_tex.png"))


def make_face():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    skin, hair, shirt, pants = (236, 190, 150, 255), (90, 60, 40, 255), (60, 120, 200, 255), (60, 60, 110, 255)
    cols = {"head": skin, "body": shirt, "right_arm": skin, "left_arm": skin, "right_leg": pants, "left_leg": pants}
    for part, (uv, size, *_r) in PARTS.items():
        for face, (x, y, w, h) in regions(uv, size).items():
            c = cols[part]
            if part == "head" and face in "TBLRD" and face != "D":
                c = hair
            for j in range(h):
                for i in range(w):
                    img.putpixel((x + i, y + j), c)
    # 頭の前面 (8,8)-(16,16): 髪の前髪・目・口
    hx, hy = 8, 8
    for i in range(8):
        img.putpixel((hx + i, hy), hair)
    for (ex, ey) in [(1, 3), (5, 3)]:
        img.putpixel((hx + ex, hy + ey), (255, 255, 255, 255))
        img.putpixel((hx + ex + 1, hy + ey), (40, 60, 160, 255))
        img.putpixel((hx + ex, hy + ey + 1), (255, 255, 255, 255))
        img.putpixel((hx + ex + 1, hy + ey + 1), (40, 60, 160, 255))
    for i in range(3, 5):
        img.putpixel((hx + i, hy + 6), (170, 60, 60, 255))
    img.save(os.path.join(HERE, "face_tex.png"))


def make_model():
    bones = []
    for part, (uv, size, frm, piv, _l, _h) in PARTS.items():
        bones.append({"name": part, "parent": None, "pivot": list(piv), "rotation": [0, 0, 0],
                      "cubes": [{"from": list(frm), "size": list(size), "uv": list(uv), "inflate": 0, "mirror": False}]})
    bones.append({"name": "weapon", "parent": "right_arm", "pivot": [6, 12, 0], "rotation": [0, 0, 0], "cubes": []})
    with open(os.path.join(HERE, "vanilla.json"), "w", encoding="utf-8") as fp:
        json.dump({"texture_size": [64, 64], "bones": bones}, fp, indent=1, ensure_ascii=False)


if __name__ == "__main__":
    make_test()
    make_face()
    make_model()

#!/usr/bin/env python3
"""mimosa(ミモザ) モデル/テクスチャ生成。"""
import json, os
from PIL import Image

ROOT = "/home/claude/burosuta"
JSON_OUT = f"{ROOT}/src/client/resources/assets/brawl/models/brawler/mimosa.json"
PNG_OUT = f"{ROOT}/src/client/resources/assets/brawl/textures/entity/brawler/mimosa.png"

# ---------------------------------------------------------------- モデル
def bone(name, parent, pivot, cubes, rot=(0, 0, 0)):
    return {"name": name, "parent": parent, "pivot": list(pivot), "rotation": list(rot), "cubes": cubes}
def cube(frm, size, uv, inflate=0):
    c = {"from": list(frm), "size": list(size), "uv": list(uv)}
    if inflate: c["inflate"] = inflate
    return c

LEAN = -8
bones = [
    bone("body", None, (0, 5, 0), [
        cube((-4, 5, -2), (8, 8, 4), (0, 22)),
        cube((-4, 7, -2), (8, 2, 4), (32, 34), 0.5),   # 帯
    ], (LEAN, 0, 0)),
    bone("head", "body", (0, 13, 0), [cube((-6, 13, -5), (12, 11, 10), (0, 0))]),
    bone("right_arm", "body", (5.5, 12, 0), [cube((4, 6, -1.5), (3, 7, 3), (24, 22))], (80, 0, 0)),
    bone("left_arm", "body", (-5.5, 12, 0), [cube((-7, 6, -1.5), (3, 7, 3), (36, 22))]),
    bone("right_leg", None, (2, 5, 0), [cube((0, 0, -2), (4, 5, 4), (0, 34))]),
    bone("left_leg", None, (-2, 5, 0), [cube((-4, 0, -2), (4, 5, 4), (16, 34))]),
    bone("weapon", "right_arm", (5.5, 6.5, 0), [
        cube((6.5, 7.2, -1), (4, 4, 12), (32, 48)),      # 筒
        cube((5.5, 6.2, -3), (6, 6, 2), (0, 56)),       # 先端の広がり
    ], (-70, 0, 0)),
    bone("bun", "head", (0, 24, 3), [
        cube((-2, 24, 1), (4, 1, 4), (44, 0)),
        cube((-2.5, 25, 0.5), (5, 2, 5), (44, 5)),
        cube((-1.5, 27, 1.5), (3, 1, 3), (44, 41)),
    ]),
    bone("kanzashi", "head", (1, 26.5, 3), [
        cube((1, 26, 2.5), (5, 1, 1), (48, 22)),
        cube((6, 25.5, 2), (2, 2, 2), (48, 25)),
    ], (0, 0, 14)),
    bone("mimosa_a", "head", (3, 24, 2), [cube((2.5, 24, 1), (2, 2, 2), (56, 34))]),
    bone("mimosa_b", "head", (-3, 24, 2), [cube((-4.5, 24, 2), (2, 2, 2), (56, 34))]),
    bone("shell_a", "body", (-2.5, 9.5, 4.5), [cube((-5, 7, 2), (5, 5, 5), (0, 46))]),
    bone("shell_b", "body", (2.5, 8.5, 4.5), [cube((0, 6, 2), (5, 5, 5), (44, 12))]),
    bone("fuse_a", "shell_a", (-2.5, 12, 4.5), [cube((-3, 12, 4), (1, 2, 1), (22, 46))]),
    bone("fuse_b", "shell_b", (2.5, 11, 4.5), [cube((2, 11, 4), (1, 2, 1), (22, 46))]),
    bone("obi_bow", "body", (0, 7.5, 3), [cube((-2, 6, 2), (4, 3, 1), (32, 41))]),
]

# ---------------------------------------------------------------- テクスチャ
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
px = img.load()

def rects(u, v, w, h, d):
    return {
        "top": (u + d, v, u + d + w, v + d), "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "right": (u, v + d, u + d, v + d + h), "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h), "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }

def put(r, x, y, c):
    X, Y = r[0] + x, r[1] + y
    if r[0] <= X < r[2] and r[1] <= Y < r[3]:
        px[X, Y] = c if len(c) == 4 else c + (255,)

def fillr(r, c):
    for y in range(r[3] - r[1]):
        for x in range(r[2] - r[0]):
            put(r, x, y, c)

def hline(r, y, x0, x1, c):
    for x in range(x0, x1 + 1): put(r, x, y, c)
def vline(r, x, y0, y1, c):
    for y in range(y0, y1 + 1): put(r, x, y, c)

def W(r): return r[2] - r[0]
def H(r): return r[3] - r[1]

def banded(r, pal, kind, cuts=(0.3, 0.72)):
    """pal=(明,中,暗,縁)。上が明るく下が暗い。側面/背面は一段暗い。"""
    shift = {"front": 0, "right": 0, "left": 1, "back": 1}.get(kind, 0)
    if kind == "top":
        fillr(r, pal[0]); return
    if kind == "bottom":
        fillr(r, pal[3]); return
    h = H(r)
    for y in range(h):
        f = y / max(1, h - 1)
        b = 0 if f < cuts[0] else (1 if f < cuts[1] else 2)
        b = min(2, b + shift)
        hline(r, y, 0, W(r) - 1, pal[b])
    # 縁取り: 最下行は縁色（隣の色を暗くした色相）
    hline(r, h - 1, 0, W(r) - 1, pal[3])

# 配色
SK = [(255, 224, 190), (246, 200, 160), (226, 166, 132), (178, 112, 96)]
HAIR = [(255, 255, 255), (234, 238, 248), (200, 208, 232), (140, 150, 190)]
YEL = [(255, 232, 100), (248, 204, 52), (222, 164, 36), (150, 100, 40)]
NAV = [(78, 98, 168), (52, 68, 132), (36, 48, 98), (22, 28, 62)]
RED = [(250, 116, 96), (224, 56, 56), (170, 34, 52), (96, 20, 44)]
GOLD = [(255, 236, 130), (255, 206, 70), (214, 150, 40), (130, 82, 30)]
TABI = [(255, 248, 236), (240, 226, 204), (210, 190, 170), (150, 120, 110)]
ZORI = [(200, 120, 80), (170, 84, 60), (120, 54, 46), (70, 30, 34)]
BLU = [(130, 190, 255), (64, 130, 230), (40, 84, 180), (24, 40, 100)]
ROPE = [(240, 210, 150), (212, 170, 110), (170, 126, 84), (110, 76, 56)]
GL = (120, 72, 52)      # めがねフレーム
GLD = (78, 44, 40)
EYE = (52, 34, 48)
LENS = (214, 238, 252)

# ---- head (0,0) 12x11x10
R = rects(0, 0, 12, 11, 10)
for k, r in R.items():
    banded(r, HAIR, k, (0.5, 0.85))
F = R["front"]
# 顔の肌 (行3-10)
for y in range(3, 11):
    hline(F, y, 1, 10, SK[0] if y < 8 else SK[1])
hline(F, 10, 1, 10, SK[2])
for x in (0, 11): vline(F, x, 3, 8, HAIR[1]); put(F, x, 9, HAIR[2]); put(F, x, 10, HAIR[3])
# 前髪: 2行ベタ + ぎざぎざ。真ん中でふんわり分ける
hline(F, 0, 0, 11, HAIR[0]); hline(F, 1, 0, 11, HAIR[1])
for x in (5, 6): put(F, x, 0, (255, 255, 255))
for x in (0, 1, 2, 3, 4, 7, 8, 9, 10, 11): put(F, x, 1, HAIR[0] if x in (1, 3, 8, 10) else HAIR[1])
for x in (0, 2, 4, 5, 6, 7, 9, 11): put(F, x, 2, HAIR[1] if x % 2 else HAIR[2])
put(F, 0, 2, HAIR[2]); put(F, 11, 2, HAIR[2])
for x in (1, 3, 8, 10): put(F, x, 2, SK[0])
put(F, 5, 2, HAIR[2]); put(F, 6, 2, HAIR[2])
for x in (5, 6): put(F, x, 3, HAIR[2]) if False else None
for (x, y) in ((4, 3), (7, 3), (6, 3)): put(F, x, y, HAIR[1])
for x in (2, 9): put(F, x, 0, HAIR[2])
put(F, 4, 1, HAIR[2]); put(F, 7, 1, HAIR[2]); put(F, 0, 3, HAIR[3]); put(F, 11, 4, HAIR[3])
# 眉(右が高い=いたずら)
hline(F, 3, 1, 3, HAIR[3]); hline(F, 3, 7, 9, HAIR[3]); put(F, 10, 2, HAIR[3]); put(F, 4, 3, SK[1])
# 丸めがね(金のつる): 外枠5x4 を左右に
GLF = (176, 120, 70); GLH = (214, 166, 104)
def lens(ox):
    for yy in range(4, 8):
        for xx in range(ox, ox + 5):
            corner = (yy in (4, 7)) and (xx in (ox, ox + 4))
            if corner: continue
            edge = yy in (4, 7) or xx in (ox, ox + 4)
            put(F, xx, yy, (GLH if yy == 4 else GLF) if edge else LENS)
    for yy in (5, 6):
        for xx in (ox + 1, ox + 2): put(F, xx, yy, EYE)
    put(F, ox + 1, 5, (255, 255, 255)); put(F, ox + 3, 6, (150, 184, 226)); put(F, ox + 3, 5, LENS)
lens(1); lens(6)
put(F, 5, 5, GLF)     # ブリッジ
# ほっぺ
for x in (1, 2, 9, 10): put(F, x, 8, (255, 168, 150))
# にやり口（右が上がる）
MO = (176, 70, 84)
hline(F, 9, 4, 8, MO); put(F, 3, 8, MO); put(F, 9, 8, MO); put(F, 10, 8, SK[1])
hline(F, 10, 5, 7, (240, 126, 128)); put(F, 4, 10, SK[2]); put(F, 8, 10, SK[2])
put(F, 5, 9, (255, 252, 246)); put(F, 7, 9, (255, 252, 246))
# 側面: 髪は後ろ、前は肌(耳)
for kind, back_x in (("right", range(0, 6)), ("left", range(4, 10))):
    r = R[kind]
    for y in range(3, 11):
        for x in range(10):
            fr = (x >= 6) if kind == "right" else (x <= 3)
            if fr and y >= 3:
                put(r, x, y, SK[0] if y < 7 else SK[1])
    for x in range(10):
        for y in (0, 1, 2): put(r, x, y, HAIR[0] if y < 2 else HAIR[1])
    # 耳
    ex = 5 if kind == "right" else 4
    for yy in (5, 6, 7): put(r, ex, yy, SK[2]); 
    put(r, ex, 6, (240, 150, 140))
    # 髪の下端
    for x in range(10):
        fr = (x >= 6) if kind == "right" else (x <= 3)
        if not fr:
            for y in range(3, 9): put(r, x, y, HAIR[1] if y < 6 else HAIR[2])
            put(r, x, 9, HAIR[2]); put(r, x, 10, HAIR[3])
# 上面: 髪の分け目・つや
T = R["top"]
fillr(T, HAIR[0])
for i in range(12): put(T, i, 9, HAIR[1])
for i in range(0, 12, 3): put(T, i, 4, HAIR[1]); put(T, i + 1, 5, HAIR[1])
hline(T, 8, 0, 11, HAIR[1])
put(T, 4, 2, (255, 255, 255)); put(T, 5, 2, (255, 255, 255)); put(T, 6, 3, (255, 255, 255))
for y in range(10): put(T, 0, y, HAIR[1]); put(T, 11, y, HAIR[1])
# 後頭部: 髪+しわ
B = R["back"]
for y in range(11):
    hline(B, y, 0, 11, HAIR[1] if y < 6 else HAIR[2])
for x in range(0, 12, 2): put(B, x, 3, HAIR[2]); put(B, x + 1, 7, HAIR[3])
hline(B, 10, 0, 11, HAIR[3])
for x in (4, 5, 6, 7): put(B, x, 9, SK[2])   # うなじ

# ---- bun (44,0) 4x4x4 : 白髪のお団子
# お団子: 3段の丸い形 + 毛束の線 + かんざし
R1 = rects(44, 0, 4, 1, 4)
for k, r in R1.items(): banded(r, NAV, k, (0.5, 0.8))     # 髪紐
R2 = rects(44, 5, 5, 2, 5)
for k, r in R2.items():
    banded(r, HAIR, k, (0.5, 0.85))
    if k in ("front", "back", "left", "right"):
        put(r, 1, 0, HAIR[2]); put(r, 3, 0, HAIR[2]); put(r, 2, 1, HAIR[2]); put(r, 0, 1, HAIR[2]); put(r, 4, 1, HAIR[3])
    if k == "top":
        for (x, y) in ((1, 1), (2, 3), (3, 2), (0, 3), (4, 1)): put(r, x, y, HAIR[2])
R3 = rects(44, 41, 3, 1, 3)
for k, r in R3.items():
    banded(r, HAIR, k, (0.5, 0.85))
fillr(R3["top"], (255, 255, 255)); put(R3["top"], 1, 1, HAIR[1]); put(R3["top"], 2, 0, HAIR[1])
# かんざし
RK = rects(48, 22, 5, 1, 1)
for k, r in RK.items(): fillr(r, GOLD[1] if k != "bottom" else GOLD[2])
put(RK["front"], 0, 0, GOLD[0]); put(RK["top"], 0, 0, GOLD[0])
RKb = rects(48, 25, 2, 2, 2)
for k, r in RKb.items(): fillr(r, RED[1] if k != "top" else RED[0]); 
for k in ("front", "right"): put(RKb[k], 0, 0, (255, 190, 170)); put(RKb[k], 1, 1, RED[2])
fillr(RKb["bottom"], RED[3])
# ---- mimosa (44,8) 2x2x2 : ミモザの黄色い花
RM = rects(56, 34, 2, 2, 2)
for k, r in RM.items():
    fillr(r, GOLD[0] if k == "top" else GOLD[1])
    put(r, 0, 0, (255, 245, 170))
    if H(r) > 1 and k not in ("top", "bottom"): put(r, W(r) - 1, H(r) - 1, GOLD[2])

# ---- body (0,22) 8x8x4 : 法被
RBd = rects(0, 22, 8, 8, 4)
for k, r in RBd.items():
    banded(r, YEL, k, (0.3, 0.7))
Fb = RBd["front"]
# 襟(紺): 左右から中央下へ。中央は肌ののど。
for y in range(0, 4):
    put(Fb, 1 + y // 2 + 0, y, NAV[1]); put(Fb, 2 + y // 2, y, NAV[0] if y < 2 else NAV[1])
    put(Fb, 6 - y // 2, y, NAV[1]); put(Fb, 5 - y // 2, y, NAV[0] if y < 2 else NAV[1])
for x in (3, 4): put(Fb, x, 0, SK[1])
put(Fb, 3, 1, YEL[1]); put(Fb, 4, 1, YEL[1]); put(Fb, 3, 2, YEL[2]); put(Fb, 4, 2, YEL[2])
# しわ・縫い目
for (x, y) in ((1, 5), (2, 6), (5, 6), (6, 5), (0, 3), (7, 3)): put(Fb, x, y, YEL[2])
put(Fb, 2, 5, YEL[0]); put(Fb, 5, 5, YEL[0]); put(Fb, 1, 4, YEL[0])
# 裾に紺の縁と白い花火の染め抜き
hline(Fb, 7, 0, 7, YEL[3])
# 胸に小さな花火の染め抜き(白)
for (x, y) in ((6, 3),): pass
# 帯(紺)
O = rects(32, 34, 8, 2, 4)
for k, r in O.items():
    banded(r, NAV, k, (0.5, 0.8))
for k in ("front", "back", "left", "right"):
    r = O[k]
    for x in range(0, W(r), 2): put(r, x, 0, NAV[0])      # 縫い目のハイライト
    hline(r, 1, 0, W(r) - 1, NAV[2])
for x in (3, 4): put(O["front"], x, 0, (110, 128, 200))   # 結び目の光
# 背中: 紺の染め抜き(背紋) 黄色背景に白丸+花火
Bk = RBd["back"]
cx, cy = 4, 3
for (x, y) in ((3, 2), (4, 2), (2, 3), (5, 3), (3, 4), (4, 4)): put(Bk, x, y, (255, 250, 235))
for (x, y) in ((3, 3), (4, 3)): put(Bk, x, y, NAV[1])
for x in range(8): put(Bk, x, 4, YEL[1] if x not in (3, 4) else (255, 250, 235)); 
for y in range(5, 8): vline(Bk, 3, y, y, ROPE[1]); vline(Bk, 4, y, y, ROPE[1])
# 背負いひも(茶)
for y in range(0, 5): put(Bk, 1, y, ROPE[1]); put(Bk, 6, y, ROPE[1])
for y in range(0, 5): put(RBd["front"], 1, y, YEL[1]) if False else None
# 上面(襟元) / 下面
fillr(RBd["top"], YEL[0]); 
for x in range(8): put(RBd["top"], x, 3, NAV[1]); put(RBd["top"], x, 0, NAV[1]) if False else None
for x in (2, 3, 4, 5): put(RBd["top"], x, 1, SK[1]); put(RBd["top"], x, 2, SK[1])
# 側面: 袖ぐりの縫い目
for k in ("right", "left"):
    r = RBd[k]
    for y in range(0, 3): put(r, 1, y, YEL[2])
    put(r, 2, 4, YEL[2]); put(r, 3, 5, YEL[2])

# ---- bow (32,41) 4x3x1 : 帯の結び
RW = rects(32, 41, 4, 3, 1)
for k, r in RW.items():
    banded(r, NAV, k, (0.4, 0.8))
for k in ("front", "back"):
    r = RW[k]; hline(r, 0, 0, 3, NAV[0]); put(r, 1, 1, NAV[2]); put(r, 2, 1, NAV[2])

# ---- arms
def arm(u, side):
    Rr = rects(u, 22, 3, 7, 3)
    for k, r in Rr.items():
        banded(r, YEL, k, (0.3, 0.6))
        if k not in ("top", "bottom"):
            # 袖口の紺ライン(行4)、前腕は肌(行5-6)
            hline(r, 4, 0, W(r) - 1, NAV[1] if k in ("front", "right") else NAV[2])
            for y in (5, 6):
                hline(r, y, 0, W(r) - 1, SK[1] if y == 5 else SK[2])
            if k in ("left", "back"):
                hline(r, 5, 0, W(r) - 1, SK[2]); hline(r, 6, 0, W(r) - 1, SK[3])
            hline(r, 3, 0, W(r) - 1, YEL[2])
            put(r, 1, 1, YEL[0]) if k == "front" else None
    fillr(Rr["top"], YEL[0]); fillr(Rr["bottom"], SK[2])
    put(Rr["front"], 1, 6, SK[3]); put(Rr["front"], 0, 5, SK[0])
arm(24, 1); arm(36, -1)

# ---- legs : 紺の股引、足袋、草履
def leg(u):
    Rr = rects(u, 34, 4, 5, 4)
    for k, r in Rr.items():
        banded(r, NAV, k, (0.4, 0.7))
        if k in ("front", "back", "left", "right"):
            hline(r, 3, 0, W(r) - 1, TABI[1]); hline(r, 4, 0, W(r) - 1, ZORI[1])
            hline(r, 2, 0, W(r) - 1, TABI[0] if k in ("front", "right") else TABI[1])
            put(r, 0, 1, NAV[0]) if k == "front" else None
            put(r, 2, 1, NAV[2]) if k == "front" else None
    fillr(Rr["top"], NAV[1]); fillr(Rr["bottom"], ZORI[2])
    f = Rr["front"]; put(f, 1, 3, TABI[2]); put(f, 2, 3, TABI[2]); hline(f, 4, 0, 3, ZORI[0])
leg(0); leg(16)

# ---- shells (0,46) 5x5x5 : 花火玉
RS = rects(0, 46, 5, 5, 5)
for k, r in RS.items():
    banded(r, RED, k, (0.35, 0.7))
    if k in ("front", "right", "left", "back"):
        hline(r, 2, 0, W(r) - 1, GOLD[1]); hline(r, 3, 0, W(r) - 1, GOLD[2])
        put(r, 1, 0, RED[0]); put(r, 3, 1, (255, 190, 170))
        for x in range(0, W(r), 2): put(r, x, 2, GOLD[0])
    if k == "front":
        for (x, y) in ((2, 0), (1, 1), (3, 1)): put(r, x, y, GOLD[1])
fillr(RS["top"], RED[0]); put(RS["top"], 2, 2, ROPE[2]); fillr(RS["bottom"], RED[3])
for (x, y) in ((1, 1),): put(RS["top"], x, y, (255, 170, 150))
RS2 = rects(44, 12, 5, 5, 5)
for k, r in RS2.items():
    banded(r, BLU, k, (0.35, 0.7))
    if k in ("front", "right", "left", "back"):
        hline(r, 2, 0, W(r) - 1, (255, 250, 235)); hline(r, 3, 0, W(r) - 1, TABI[2])
        put(r, 1, 0, BLU[0]); put(r, 3, 1, (190, 225, 255))
        for x in range(1, W(r), 2): put(r, x, 2, GOLD[1])
    if k == "front":
        for (x, y) in ((2, 0), (1, 1), (3, 1)): put(r, x, y, (255, 250, 235))
fillr(RS2["top"], BLU[0]); put(RS2["top"], 2, 2, ROPE[2]); fillr(RS2["bottom"], BLU[3]); put(RS2["top"], 1, 1, (200, 230, 255))
# ---- fuse (22,46) 1x2x1
RF = rects(22, 46, 1, 2, 1)
for k, r in RF.items():
    for y in range(H(r)): hline(r, y, 0, W(r) - 1, ROPE[0] if y == 0 else ROPE[2])

# ---- weapon tube (32,48) 4x4x12
RT = rects(32, 48, 4, 4, 12)
for k, r in RT.items():
    banded(r, RED, k, (0.35, 0.7))
for k in ("right", "left"):
    r = RT[k]       # 12 x 4: 横が筒の長さ
    for x in (2, 3): vline(r, x, 0, 3, GOLD[1])
    for x in (8, 9): vline(r, x, 0, 3, GOLD[1])
    vline(r, 2, 3, 3, GOLD[2]); vline(r, 3, 3, 3, GOLD[2]); vline(r, 8, 3, 3, GOLD[2]); vline(r, 9, 3, 3, GOLD[2])
    put(r, 5, 1, RED[0]); put(r, 6, 1, RED[0]); put(r, 6, 2, RED[2]) 
T2 = RT["top"]      # 4 x 12 (縦がZ方向)
fillr(T2, RED[0])
for y in (2, 3, 8, 9): hline(T2, y, 0, 3, GOLD[0])
for y in (5, 6): put(T2, 1, y, (255, 170, 150))
B2 = RT["bottom"]; fillr(B2, RED[3])
for y in (2, 3, 8, 9): hline(B2, y, 0, 3, GOLD[3])
hline(RT["front"], 0, 0, 3, RED[2]); fillr(RT["front"], (60, 28, 40)); put(RT["front"], 1, 1, (30, 14, 24)); put(RT["front"], 2, 2, (30, 14, 24))
put(RT["back"], 1, 1, GOLD[1])

# ---- muzzle (0,56) 6x6x2 金
RMz = rects(0, 56, 6, 6, 2)
for k, r in RMz.items():
    banded(r, GOLD, k, (0.35, 0.7))
for k in ("right", "left", "back"):
    r = RMz[k]
    for y in range(H(r)): put(r, 0, y, GOLD[0]) if False else None
    put(r, 0, 1, GOLD[0])
fm = RMz["front"]
fillr(fm, GOLD[1]); 
for y in range(6): hline(fm, y, 0, 5, GOLD[0] if y == 0 else GOLD[1] if y < 4 else GOLD[2])
for y in range(1, 5): hline(fm, y, 1, 4, (70, 30, 40))
hline(fm, 1, 1, 4, (90, 40, 46)); put(fm, 2, 2, (40, 20, 30)); put(fm, 3, 3, (40, 20, 30))

# ---------------------------------------------------------------- 出力
os.makedirs(os.path.dirname(PNG_OUT), exist_ok=True)
os.makedirs(os.path.dirname(JSON_OUT), exist_ok=True)
img.save(PNG_OUT)
with open(JSON_OUT, "w") as f:
    json.dump({"texture_size": [64, 64], "bones": bones}, f, ensure_ascii=False, indent=1)

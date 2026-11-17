#!/usr/bin/env python3
import json, random, os
from PIL import Image

ROOT = "/home/claude/burosuta"
OUT_J = ROOT + "/src/client/resources/assets/brawl/models/brawler/takomaru.json"
OUT_P = ROOT + "/src/client/resources/assets/brawl/textures/entity/brawler/takomaru.png"
S = 128
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
P = img.load()
rnd = random.Random(7)

def H(s):
    s = s.lstrip('#'); return (int(s[0:2],16), int(s[2:4],16), int(s[4:6],16), 255)
# 赤ダコ
R_HI=H('ff8c6e'); R_L=H('ec5a42'); R_M=H('d43c36'); R_D=H('ab2b32'); R_OL=H('7c1e2e')
# 吸盤
S_L=H('ffc8b4'); S_M=H('f09a8a'); S_D=H('bc4a4a')
# まわし
W_L=H('faf4e8'); W_M=H('dcd4c6'); W_D=H('b0a8a8'); W_OL=H('7f7888')
# 鉢巻 藍
B_L=H('5c78c8'); B_M=H('3f56a6'); B_D=H('2c3a7c'); B_OL=H('1e2756')
INK=H('3a1224'); WHITE=H('fffaf0'); GOLD=H('f2c14e')

cubes = []  # (bone, name, from, size, painter)
cur = [0, 0, 0]  # shelf packer
def alloc(w, h):
    tw, th = 2*h + 0, 0
    raise

shelf = {"x":0,"y":0,"rowh":0}
def pack(w, h, d):
    W_, H_ = 2*d+2*w, d+h
    if shelf["x"] + W_ > S:
        shelf["x"] = 0; shelf["y"] += shelf["rowh"]; shelf["rowh"] = 0
    u, v = shelf["x"], shelf["y"]
    shelf["x"] += W_; shelf["rowh"] = max(shelf["rowh"], H_)
    assert shelf["y"] + H_ <= S, "texture overflow"
    return u, v

bones = {}
def bone(name, parent, pivot, rot=(0,0,0)):
    bones[name] = {"name": name, "parent": parent, "pivot": list(pivot), "rotation": list(rot), "cubes": []}

class Face:
    def __init__(s, x0, y0, w, h): s.x0, s.y0, s.w, s.h = x0, y0, w, h
    def px(s, x, y, c):
        if 0 <= x < s.w and 0 <= y < s.h: P[s.x0+x, s.y0+y] = c
    def get(s, x, y): return P[s.x0+x, s.y0+y]
    def rect(s, x, y, w, h, c):
        for j in range(h):
            for i in range(w): s.px(x+i, y+j, c)

def add(bn, frm, size, painter=None, pal='red', inflate=0):
    w, h, d = size
    u, v = pack(w, h, d)
    bones[bn]["cubes"].append({"from": list(frm), "size": list(size), "uv": [u, v], "inflate": inflate})
    faces = {
        'R': Face(u, v+d, d, h), 'F': Face(u+d, v+d, w, h), 'L': Face(u+d+w, v+d, d, h), 'B': Face(u+2*d+w, v+d, w, h),
        'T': Face(u+d, v, w, d), 'D': Face(u+d+w, v, w, d)}
    base(faces, pal)
    if painter: painter(faces, w, h, d)
    return faces

PALS = {
 'red': (R_L, R_M, R_D, R_OL, R_HI),
 'white': (W_L, W_M, W_D, W_OL, W_L),
 'blue': (B_L, B_M, B_D, B_OL, B_L),
}
def base(faces, pal):
    L, M, D, OL, HI = PALS[pal]
    for k, f in faces.items():
        for y in range(f.h):
            t = y / max(1, f.h-1)
            if k == 'T': c = L
            elif k == 'D': c = D
            else:
                if f.h <= 3: c = M
                elif t < 0.34: c = L
                elif t < 0.72: c = M
                else: c = D
                # ディザ境界
                if f.h > 3:
                    for edge, lo, hi in ((0.34, L, M), (0.72, M, D)):
                        if abs(t-edge) < 0.5/f.h+0.04: pass
            for x in range(f.w):
                cc = c
                if k not in 'TD' and f.h > 5:
                    # 境界ディザ
                    if abs(t-0.34) < 0.12 and (x+y) % 2 == 0: cc = M if c == L else L
                    if abs(t-0.72) < 0.12 and (x+y) % 2 == 0: cc = D if c == M else M
                f.px(x, y, cc)
        # 縁取り(隣色を暗く)
        for y in range(f.h):
            for x in range(f.w):
                if x == f.w-1 or y == f.h-1 or (k in 'RL' and x == 0):
                    if k in 'TD': continue
                    f.px(x, y, OL if y == f.h-1 else D)
        # 点描の揺らぎ
        if pal == 'red' and f.w*f.h > 20:
            for _ in range(f.w*f.h//40):
                x, y = rnd.randrange(f.w), rnd.randrange(f.h)
                f.px(x, y, HI if k == 'T' or y < f.h*0.5 else R_D)

def sucker(f, cx, cy, big=False):
    f.px(cx, cy, S_L); f.px(cx+1, cy, S_L); f.px(cx, cy+1, S_M); f.px(cx+1, cy+1, S_D if big else S_M)
    if big: f.px(cx-1, cy, S_M); f.px(cx+2, cy+1, S_D) if False else None

# ------------------------------------------------------------ ボーン
bone("body", None, (0, 8, 0))
bone("head", None, (0, 16, 0))
bone("right_arm", None, (8, 15, 0))
bone("left_arm", None, (-8, 15, 0))
bone("right_leg", None, (3, 6, 0))
bone("left_leg", None, (-3, 6, 0))
bone("weapon", "right_arm", (9, 6, -1))
bone("tentacle_r", "body", (2, 13, 3), (0, 0, 0))
bone("tentacle_l", "body", (-2, 13, 3), (0, 0, 0))

# ---------- 頭 (16x14x12)
def ring(f, cx, cy):
    for dx, dy in ((0,-1),(1,-1),(-1,0),(2,0),(-1,1),(2,1),(0,2),(1,2)): f.px(cx+dx, cy+dy, S_L)
    f.px(cx, cy, S_M); f.px(cx+1, cy, S_M); f.px(cx, cy+1, S_M); f.px(cx+1, cy+1, S_D)
def head_p(F, w, h, d):
    f = F['F']
    for (x0, sgn) in ((1, 1), (14, -1)):
        for dx in range(0, 4): f.px(x0+sgn*dx, 6, INK)
        for dx in range(2, 6): f.px(x0+sgn*dx, 7, INK)
        f.px(x0, 7, R_OL)
    for ex, px_, hl in ((2, 3, 3), (10, 10, 11)):
        f.rect(ex, 8, 4, 4, WHITE)
        f.rect(ex, 11, 4, 1, H('e8d6cc'))
        f.rect(px_, 9, 2, 3, INK)
        f.px(hl, 9, WHITE)
    for cx in (1, 13): f.rect(cx, 12, 2, 1, H('f4806e'))
    f.rect(6, 13, 4, 1, INK)
    f.px(5, 12, R_OL); f.px(10, 12, R_OL)
    f.rect(2, 0, 4, 1, R_HI); f.px(1, 1, R_HI)
    sucker(F['L'], 3, 8); sucker(F['R'], 8, 8)
    ring(F['B'], 3, 1); ring(F['B'], 11, 2); sucker(F['B'], 7, 8)
    ring(F['T'], 2, 3); ring(F['T'], 11, 6); ring(F['T'], 12, 2)
    F['T'].rect(5, 1, 3, 1, R_HI); F['T'].px(4, 2, R_HI)
add("head", (-8, 16, -6), (16, 14, 12), head_p)

def d1(F, w, h, d):
    ring(F['T'], 2, 2); ring(F['T'], 9, 5); F['T'].rect(5, 1, 3, 1, R_HI); F['F'].rect(2, 0, 3, 1, R_HI)
add("head", (-7, 30, -5), (14, 1, 10), d1)
def d2(F, w, h, d):
    ring(F['T'], 7, 3); F['T'].rect(2, 1, 3, 1, R_HI); F['F'].rect(1, 0, 2, 1, R_HI)
add("head", (-6, 31, -4), (12, 1, 8), d2)
def d3(F, w, h, d):
    F['T'].rect(0, 0, w, d, R_L); F['T'].rect(1, 1, 2, 1, WHITE); F['T'].px(4, 2, R_HI)
    F['F'].px(1, 0, R_HI)
add("head", (-4, 32, -2), (8, 1, 4), d3)

# 鉢巻 藍
B_L=H('5c78c8'); B_M=H('3f56a6'); B_D=H('2c3a7c'); B_OL=H('1e2756')
INK=H('3a1224'); WHITE=H('fffaf0'); GOLD=H('f2c14e')

cubes = []  # (bone, name, from, size, painter)
cur = [0, 0, 0]  # shelf packer
def alloc(w, h):
    tw, th = 2*h + 0, 0
    raise

shelf = {"x":0,"y":0,"rowh":0}
def pack(w, h, d):
    W_, H_ = 2*d+2*w, d+h
    if shelf["x"] + W_ > S:
        shelf["x"] = 0; shelf["y"] += shelf["rowh"]; shelf["rowh"] = 0
    u, v = shelf["x"], shelf["y"]
    shelf["x"] += W_; shelf["rowh"] = max(shelf["rowh"], H_)
    assert shelf["y"] + H_ <= S, "texture overflow"
    return u, v

bones = {}
def bone(name, parent, pivot, rot=(0,0,0)):
    bones[name] = {"name": name, "parent": parent, "pivot": list(pivot), "rotation": list(rot), "cubes": []}

class Face:
    def __init__(s, x0, y0, w, h): s.x0, s.y0, s.w, s.h = x0, y0, w, h
    def px(s, x, y, c):
        if 0 <= x < s.w and 0 <= y < s.h: P[s.x0+x, s.y0+y] = c
    def get(s, x, y): return P[s.x0+x, s.y0+y]
    def rect(s, x, y, w, h, c):
        for j in range(h):
            for i in range(w): s.px(x+i, y+j, c)

def add(bn, frm, size, painter=None, pal='red', inflate=0):
    w, h, d = size
    u, v = pack(w, h, d)
    bones[bn]["cubes"].append({"from": list(frm), "size": list(size), "uv": [u, v], "inflate": inflate})
    faces = {
        'R': Face(u, v+d, d, h), 'F': Face(u+d, v+d, w, h), 'L': Face(u+d+w, v+d, d, h), 'B': Face(u+2*d+w, v+d, w, h),
        'T': Face(u+d, v, w, d), 'D': Face(u+d+w, v, w, d)}
    base(faces, pal)
    if painter: painter(faces, w, h, d)
    return faces

PALS = {
 'red': (R_L, R_M, R_D, R_OL, R_HI),
 'white': (W_L, W_M, W_D, W_OL, W_L),
 'blue': (B_L, B_M, B_D, B_OL, B_L),
}
def base(faces, pal):
    L, M, D, OL, HI = PALS[pal]
    for k, f in faces.items():
        for y in range(f.h):
            t = y / max(1, f.h-1)
            if k == 'T': c = L
            elif k == 'D': c = D
            else:
                if f.h <= 3: c = M
                elif t < 0.34: c = L
                elif t < 0.72: c = M
                else: c = D
                # ディザ境界
                if f.h > 3:
                    for edge, lo, hi in ((0.34, L, M), (0.72, M, D)):
                        if abs(t-edge) < 0.5/f.h+0.04: pass
            for x in range(f.w):
                cc = c
                if k not in 'TD' and f.h > 5:
                    # 境界ディザ
                    if abs(t-0.34) < 0.12 and (x+y) % 2 == 0: cc = M if c == L else L
                    if abs(t-0.72) < 0.12 and (x+y) % 2 == 0: cc = D if c == M else M
                f.px(x, y, cc)
        # 縁取り(隣色を暗く)
        for y in range(f.h):
            for x in range(f.w):
                if x == f.w-1 or y == f.h-1 or (k in 'RL' and x == 0):
                    if k in 'TD': continue
                    f.px(x, y, OL if y == f.h-1 else D)
        # 点描の揺らぎ
        if pal == 'red' and f.w*f.h > 20:
            for _ in range(f.w*f.h//40):
                x, y = rnd.randrange(f.w), rnd.randrange(f.h)
                f.px(x, y, HI if k == 'T' or y < f.h*0.5 else R_D)

def sucker(f, cx, cy, big=False):
    f.px(cx, cy, S_L); f.px(cx+1, cy, S_L); f.px(cx, cy+1, S_M); f.px(cx+1, cy+1, S_D if big else S_M)
    if big: f.px(cx-1, cy, S_M); f.px(cx+2, cy+1, S_D) if False else None

# ------------------------------------------------------------ ボーン
bone("body", None, (0, 8, 0))
bone("head", None, (0, 16, 0))
bone("right_arm", None, (8, 15, 0))
bone("left_arm", None, (-8, 15, 0))
bone("right_leg", None, (3, 6, 0))
bone("left_leg", None, (-3, 6, 0))
bone("weapon", "right_arm", (9, 6, -1))
bone("tentacle_r", "body", (2, 13, 3), (0, 0, 0))
bone("tentacle_l", "body", (-2, 13, 3), (0, 0, 0))

# ---------- 頭 (16x14x12)
def head_p(F, w, h, d):
    f = F['F']
    for (x0, sgn) in ((1, 1), (14, -1)):
        for dx in range(0, 4): f.px(x0+sgn*dx, 5, INK)
        for dx in range(2, 6): f.px(x0+sgn*dx, 6, INK)
        f.px(x0, 6, R_OL)
    for ex, px_, hl in ((2, 3, 3), (10, 10, 11)):
        f.rect(ex, 7, 4, 4, WHITE)
        f.rect(ex, 10, 4, 1, H('e8d6cc'))
        f.rect(px_, 8, 2, 3, INK)
        f.px(hl, 8, WHITE)
    for cx in (1, 13): f.rect(cx, 11, 2, 1, H('f4806e'))
    f.rect(6, 12, 4, 1, INK)
    f.px(5, 11, R_OL); f.px(10, 11, R_OL)
    f.rect(2, 0, 4, 1, R_HI); f.px(1, 1, R_HI)
    sucker(F['L'], 3, 1); sucker(F['R'], 8, 1)
    sucker(F['B'], 3, 2); sucker(F['B'], 11, 3); sucker(F['B'], 7, 8)
    sucker(F['T'], 2, 3, True); sucker(F['T'], 11, 6, True); sucker(F['T'], 12, 2)
add("head", (-8, 16, -6), (16, 14, 12), head_p)

def dome_p(F, w, h, d):
    sucker(F['T'], 2, 2, True); sucker(F['T'], 8, 5)
    F['F'].rect(1, 0, 3, 1, R_HI)
add("head", (-7, 30, -5), (14, 2, 10), dome_p)
def dome2_p(F, w, h, d):
    F['T'].rect(0, 0, w, d, R_L); sucker(F['T'], 2, 1)
    F['F'].rect(1, 0, 2, 1, R_HI)
add("head", (-5, 32, -3), (10, 1, 6), dome2_p)

# 鉢巻 藍
B_L=H('5c78c8'); B_M=H('3f56a6'); B_D=H('2c3a7c'); B_OL=H('1e2756')
INK=H('3a1224'); WHITE=H('fffaf0'); GOLD=H('f2c14e')

cubes = []  # (bone, name, from, size, painter)
cur = [0, 0, 0]  # shelf packer
def alloc(w, h):
    tw, th = 2*h + 0, 0
    raise

shelf = {"x":0,"y":0,"rowh":0}
def pack(w, h, d):
    W_, H_ = 2*d+2*w, d+h
    if shelf["x"] + W_ > S:
        shelf["x"] = 0; shelf["y"] += shelf["rowh"]; shelf["rowh"] = 0
    u, v = shelf["x"], shelf["y"]
    shelf["x"] += W_; shelf["rowh"] = max(shelf["rowh"], H_)
    assert shelf["y"] + H_ <= S, "texture overflow"
    return u, v

bones = {}
def bone(name, parent, pivot, rot=(0,0,0)):
    bones[name] = {"name": name, "parent": parent, "pivot": list(pivot), "rotation": list(rot), "cubes": []}

class Face:
    def __init__(s, x0, y0, w, h): s.x0, s.y0, s.w, s.h = x0, y0, w, h
    def px(s, x, y, c):
        if 0 <= x < s.w and 0 <= y < s.h: P[s.x0+x, s.y0+y] = c
    def get(s, x, y): return P[s.x0+x, s.y0+y]
    def rect(s, x, y, w, h, c):
        for j in range(h):
            for i in range(w): s.px(x+i, y+j, c)

def add(bn, frm, size, painter=None, pal='red', inflate=0):
    w, h, d = size
    u, v = pack(w, h, d)
    bones[bn]["cubes"].append({"from": list(frm), "size": list(size), "uv": [u, v], "inflate": inflate})
    faces = {
        'R': Face(u, v+d, d, h), 'F': Face(u+d, v+d, w, h), 'L': Face(u+d+w, v+d, d, h), 'B': Face(u+2*d+w, v+d, w, h),
        'T': Face(u+d, v, w, d), 'D': Face(u+d+w, v, w, d)}
    base(faces, pal)
    if painter: painter(faces, w, h, d)
    return faces

PALS = {
 'red': (R_L, R_M, R_D, R_OL, R_HI),
 'white': (W_L, W_M, W_D, W_OL, W_L),
 'blue': (B_L, B_M, B_D, B_OL, B_L),
}
def base(faces, pal):
    L, M, D, OL, HI = PALS[pal]
    for k, f in faces.items():
        for y in range(f.h):
            t = y / max(1, f.h-1)
            if k == 'T': c = L
            elif k == 'D': c = D
            else:
                if f.h <= 3: c = M
                elif t < 0.34: c = L
                elif t < 0.72: c = M
                else: c = D
                # ディザ境界
                if f.h > 3:
                    for edge, lo, hi in ((0.34, L, M), (0.72, M, D)):
                        if abs(t-edge) < 0.5/f.h+0.04: pass
            for x in range(f.w):
                cc = c
                if k not in 'TD' and f.h > 5:
                    # 境界ディザ
                    if abs(t-0.34) < 0.12 and (x+y) % 2 == 0: cc = M if c == L else L
                    if abs(t-0.72) < 0.12 and (x+y) % 2 == 0: cc = D if c == M else M
                f.px(x, y, cc)
        # 縁取り(隣色を暗く)
        for y in range(f.h):
            for x in range(f.w):
                if x == f.w-1 or y == f.h-1 or (k in 'RL' and x == 0):
                    if k in 'TD': continue
                    f.px(x, y, OL if y == f.h-1 else D)
        # 点描の揺らぎ
        if pal == 'red' and f.w*f.h > 20:
            for _ in range(f.w*f.h//40):
                x, y = rnd.randrange(f.w), rnd.randrange(f.h)
                f.px(x, y, HI if k == 'T' or y < f.h*0.5 else R_D)

def sucker(f, cx, cy, big=False):
    f.px(cx, cy, S_L); f.px(cx+1, cy, S_L); f.px(cx, cy+1, S_M); f.px(cx+1, cy+1, S_D if big else S_M)
    if big: f.px(cx-1, cy, S_M); f.px(cx+2, cy+1, S_D) if False else None

# ------------------------------------------------------------ ボーン
bone("body", None, (0, 8, 0))
bone("head", None, (0, 16, 0))
bone("right_arm", None, (8, 15, 0))
bone("left_arm", None, (-8, 15, 0))
bone("right_leg", None, (3, 6, 0))
bone("left_leg", None, (-3, 6, 0))
bone("weapon", "right_arm", (9, 6, -1))
bone("tentacle_r", "body", (2, 13, 3), (0, 0, 0))
bone("tentacle_l", "body", (-2, 13, 3), (0, 0, 0))

# ---------- 頭 (14x13x12)
def head_p(F, w, h, d):
    f = F['F']
    # 太い怒り眉(内側が下がる) 行5-6
    for (x0, sgn) in ((1, 1), (12, -1)):
        pts = [(0,5),(1,5),(2,5),(2,6),(3,6),(4,6)]
        for dx, y in pts: f.px(x0+sgn*dx, y, INK)
        f.px(x0+sgn*1, 6, R_OL)
    # 目 3x3 (行7-9) 内寄りに黒目
    for ex, px_ in ((2, 3), (9, 9)):
        f.rect(ex, 7, 3, 3, WHITE)
        f.rect(ex, 9, 3, 1, H('e8d6cc'))
        f.rect(px_, 8, 2, 2, INK)
        f.px(px_, 8, WHITE) if px_ == 3 else f.px(px_+1, 8, WHITE)
        f.rect(ex, 7, 3, 1, H('e8d6cc')) if False else None
    # 頬紅
    for cx in (1, 11): f.rect(cx, 10, 2, 1, H('f4806e'))
    # への字の口
    f.rect(5, 11, 4, 1, INK)
    f.px(4, 10, R_OL); f.px(9, 10, R_OL)
    # 艶
    f.rect(2, 0, 3, 1, R_HI)
    sucker(F['L'], 3, 1); sucker(F['R'], 8, 1)
    sucker(F['B'], 3, 2); sucker(F['B'], 9, 3); sucker(F['B'], 6, 7)
    sucker(F['T'], 2, 3, True); sucker(F['T'], 9, 6, True); sucker(F['T'], 10, 2)
add("head", (-7, 16, -6), (14, 13, 12), head_p)

def dome_p(F, w, h, d):
    sucker(F['T'], 2, 2, True); sucker(F['T'], 7, 4)
    F['F'].rect(1, 0, 3, 1, R_HI)
    for fc in (F['L'], F['R']): sucker(fc, 3, 0) if False else None
add("head", (-6, 29, -5), (12, 2, 10), dome_p)
def dome2_p(F, w, h, d):
    F['T'].rect(0, 0, w, d, R_L); sucker(F['T'], 1, 1)
    F['F'].rect(1, 0, 2, 1, R_HI)
add("head", (-4, 31, -3), (8, 1, 6), dome2_p)

# 鉢巻 藍
B_L=H('5c78c8'); B_M=H('3f56a6'); B_D=H('2c3a7c'); B_OL=H('1e2756')
INK=H('3a1224'); WHITE=H('fffaf0'); GOLD=H('f2c14e')

cubes = []  # (bone, name, from, size, painter)
cur = [0, 0, 0]  # shelf packer
def alloc(w, h):
    tw, th = 2*h + 0, 0
    raise

shelf = {"x":0,"y":0,"rowh":0}
def pack(w, h, d):
    W_, H_ = 2*d+2*w, d+h
    if shelf["x"] + W_ > S:
        shelf["x"] = 0; shelf["y"] += shelf["rowh"]; shelf["rowh"] = 0
    u, v = shelf["x"], shelf["y"]
    shelf["x"] += W_; shelf["rowh"] = max(shelf["rowh"], H_)
    assert shelf["y"] + H_ <= S, "texture overflow"
    return u, v

bones = {}
def bone(name, parent, pivot, rot=(0,0,0)):
    bones[name] = {"name": name, "parent": parent, "pivot": list(pivot), "rotation": list(rot), "cubes": []}

class Face:
    def __init__(s, x0, y0, w, h): s.x0, s.y0, s.w, s.h = x0, y0, w, h
    def px(s, x, y, c):
        if 0 <= x < s.w and 0 <= y < s.h: P[s.x0+x, s.y0+y] = c
    def get(s, x, y): return P[s.x0+x, s.y0+y]
    def rect(s, x, y, w, h, c):
        for j in range(h):
            for i in range(w): s.px(x+i, y+j, c)

def add(bn, frm, size, painter=None, pal='red', inflate=0):
    w, h, d = size
    u, v = pack(w, h, d)
    bones[bn]["cubes"].append({"from": list(frm), "size": list(size), "uv": [u, v], "inflate": inflate})
    faces = {
        'R': Face(u, v+d, d, h), 'F': Face(u+d, v+d, w, h), 'L': Face(u+d+w, v+d, d, h), 'B': Face(u+2*d+w, v+d, w, h),
        'T': Face(u+d, v, w, d), 'D': Face(u+d+w, v, w, d)}
    base(faces, pal)
    if painter: painter(faces, w, h, d)
    return faces

PALS = {
 'red': (R_L, R_M, R_D, R_OL, R_HI),
 'white': (W_L, W_M, W_D, W_OL, W_L),
 'blue': (B_L, B_M, B_D, B_OL, B_L),
}
def base(faces, pal):
    L, M, D, OL, HI = PALS[pal]
    for k, f in faces.items():
        for y in range(f.h):
            t = y / max(1, f.h-1)
            if k == 'T': c = L
            elif k == 'D': c = D
            else:
                if f.h <= 3: c = M
                elif t < 0.34: c = L
                elif t < 0.72: c = M
                else: c = D
                # ディザ境界
                if f.h > 3:
                    for edge, lo, hi in ((0.34, L, M), (0.72, M, D)):
                        if abs(t-edge) < 0.5/f.h+0.04: pass
            for x in range(f.w):
                cc = c
                if k not in 'TD' and f.h > 5:
                    # 境界ディザ
                    if abs(t-0.34) < 0.12 and (x+y) % 2 == 0: cc = M if c == L else L
                    if abs(t-0.72) < 0.12 and (x+y) % 2 == 0: cc = D if c == M else M
                f.px(x, y, cc)
        # 縁取り(隣色を暗く)
        for y in range(f.h):
            for x in range(f.w):
                if x == f.w-1 or y == f.h-1 or (k in 'RL' and x == 0):
                    if k in 'TD': continue
                    f.px(x, y, OL if y == f.h-1 else D)
        # 点描の揺らぎ
        if pal == 'red' and f.w*f.h > 20:
            for _ in range(f.w*f.h//40):
                x, y = rnd.randrange(f.w), rnd.randrange(f.h)
                f.px(x, y, HI if k == 'T' or y < f.h*0.5 else R_D)

def sucker(f, cx, cy, big=False):
    f.px(cx, cy, S_L); f.px(cx+1, cy, S_L); f.px(cx, cy+1, S_M); f.px(cx+1, cy+1, S_D if big else S_M)
    if big: f.px(cx-1, cy, S_M); f.px(cx+2, cy+1, S_D) if False else None

# ------------------------------------------------------------ ボーン
bone("body", None, (0, 8, 0))
bone("head", None, (0, 16, 0))
bone("right_arm", None, (8, 15, 0))
bone("left_arm", None, (-8, 15, 0))
bone("right_leg", None, (3, 6, 0))
bone("left_leg", None, (-3, 6, 0))
bone("weapon", "right_arm", (9, 6, -1))
bone("tentacle_r", "body", (2, 13, 3), (0, 0, 0))
bone("tentacle_l", "body", (-2, 13, 3), (0, 0, 0))

# ---------- 頭 (14x12x12) 本体
def head_p(F, w, h, d):
    f = F['F']
    # 顔 (行0が頭頂 y=28)。鉢巻は別cubeで上に被さる
    # 眉: 太く、内側が下がった怒り眉
    for i in range(5):
        # 向かって左の眉(キャラの右)
        f.rect(1+i, 4+(i*0 if i<2 else 0)+ (i//2), 1, 2, INK)
        f.rect(12-i, 4+(i//2), 1, 2, INK)
    # 眉の上端ハイライトなし。目
    for ex in (2, 9):
        f.rect(ex, 6, 3, 4, WHITE)
        f.rect(ex, 9, 3, 1, H('e8d6cc'))
        f.rect(ex+(1 if ex == 2 else 0), 7, 2, 3, INK)   # 瞳は内寄り
        f.px(ex+(1 if ex==2 else 0), 7, WHITE)           # ハイライト
        f.rect(ex-1 if False else ex, 5, 3, 1, R_OL)      # まぶた影
    # 頬紅
    for cx in (1, 11):
        f.rect(cx, 9, 2, 1, H('f4806e'))
    # 口: への字
    f.rect(5, 10, 4, 1, INK)
    f.px(4, 11, R_OL) if False else None
    f.px(5, 11, R_D); f.px(8, 11, R_D)
    # つるっと艶ハイライト
    f.rect(2, 1, 2, 1, WHITE); f.px(1, 2, R_HI); f.px(4, 1, R_HI)
    # 吸盤斑点 (頭頂側面)
    for fc in (F['L'], F['R']):
        sucker(fc, 3, 2); sucker(fc, 8, 3)
    sucker(F['B'], 3, 3); sucker(F['B'], 8, 2); sucker(F['B'], 11, 5, ); sucker(F['B'], 6, 7)
    sucker(F['T'], 3, 3, True); sucker(F['T'], 9, 5, True); sucker(F['T'], 11, 2); sucker(F['T'], 6, 8)
add("head", (-7, 16, -6), (14, 12, 12), head_p)

def dome_p(F, w, h, d):
    sucker(F['T'], 3, 3, True); sucker(F['T'], 7, 5)
    F['T'].px(1, 1, WHITE); F['T'].px(2, 1, R_HI)
    F['F'].px(2, 0, WHITE); F['F'].px(3, 0, WHITE); F['F'].px(2, 1, R_HI)
    for fc in (F['L'], F['R']): sucker(fc, 3, 1)
add("head", (-5, 28, -4), (10, 2, 8), dome_p)

# 鉢巻 (inflate 0.5) 前面は藍にねじり模様
def band_p(F, w, h, d):
    for k in 'FBLR':
        f = F[k]
        for x in range(f.w):
            for y in range(f.h):
                # ねじり: 斜めストライプ
                c = B_M if (x + y) % 4 in (0, 1) else B_L
                if y == f.h-1: c = B_D
                f.px(x, y, c)
        for x in range(f.w):
            f.px(x, f.h-1, B_OL if x % 2 == 0 else B_D)
            if f.h > 1: f.px(x, 0, B_L)
    for k in 'TD':
        F[k].rect(0, 0, F[k].w, F[k].h, (0,0,0,0))
add("head", (-8, 25, -6), (16, 3, 12), band_p, inflate=0.5)

# 結び目と垂れ
def knot_p(F, w, h, d):
    for k, f in F.items():
        f.rect(0, 0, f.w, f.h, B_M)
        f.rect(0, 0, f.w, 1, B_L); f.rect(0, f.h-1, f.w, 1, B_D)
        f.px(0, f.h//2, B_D)
add("head", (8, 25, 2), (3, 3, 3), knot_p)
def tail_p(F, w, h, d):
    for k, f in F.items():
        for y in range(f.h):
            for x in range(f.w):
                f.px(x, y, B_M if (x+y) % 3 else B_L)
        f.rect(0, f.h-1, f.w, 1, B_OL)
add("head", (9, 20, 3), (2, 5, 2), tail_p)
add("head", (9, 21, 5), (2, 4, 2), tail_p)

# ---------- 胴 (10x10x6)
def body_p(F, w, h, d):
    f = F['F']
    # 腹の艶と吸盤ふうの斑
    f.rect(3, 1, 2, 1, R_HI)
    # 腹のくぼみ/おへそ
    f.px(4, 5, R_OL); f.px(5, 5, R_D)
    # 大胸筋ライン
    f.rect(1, 3, 3, 1, R_D); f.rect(6, 3, 3, 1, R_D)
    sucker(F['B'], 3, 3); sucker(F['B'], 7, 5)
    for c in (F['L'], F['R']): c.px(2, 4, R_D)
add("body", (-5, 6, -3), (10, 10, 6), body_p)

# まわし帯
def maw_p(F, w, h, d):
    for k in 'FBLR':
        f = F[k]
        f.rect(0, 0, f.w, 1, W_L)
        f.rect(0, 1, f.w, 1, W_M)
        f.rect(0, 2, f.w, 1, W_D)
        # 縫い目
        for x in range(0, f.w, 4): f.px(x, 1, W_D)
        for x in range(f.w): f.px(x, 2, W_OL if x % 2 == 0 else W_D)
    for k in 'TD': F[k].rect(0, 0, F[k].w, F[k].h, W_M)
add("body", (-6, 6, -4), (12, 3, 8), maw_p, pal='white')
def flap_p(F, w, h, d):
    f = F['F']
    for y in range(f.h):
        for x in range(f.w):
            c = W_L if y < 2 else (W_M if y < 4 else W_D)
            f.px(x, y, c)
        if y % 2 == 1:
            for x in range(1, f.w-1, 2): f.px(x, y, W_M if y < 4 else W_D)
    f.rect(0, f.h-1, f.w, 1, W_OL)
    f.px(0, 0, W_M); f.px(f.w-1, 0, W_M)
    for k in 'RLBT': 
        F[k].rect(0, 0, F[k].w, F[k].h, W_M)
add("body", (-2, 3, -5), (4, 5, 1), flap_p, pal='white')
add("body", (-2, 6, 4), (4, 3, 1), flap_p, pal='white')

# ---------- 腕 (5段 ブロックでうねる)
def arm_parts(bn, sx):
    # sx=+1 右(+X), -1 左
    def mk(x0, w): return x0 if sx > 0 else -x0 - w
    def seg1_p(F, w, h, d):
        sucker(F['F'], 2, 3); F['F'].rect(1, 1, 2, 1, R_HI)
        sucker(F['R' if sx > 0 else 'L'], 3, 3)
    add(bn, (mk(5, 5), 10, -3), (5, 6, 6), seg1_p)
    def seg2_p(F, w, h, d):
        sucker(F['F'], 2, 1); F['F'].px(1, 0, R_HI)
        sucker(F['L' if sx > 0 else 'R'], 3, 1)
    add(bn, (mk(6, 5), 7, -3), (5, 3, 5), seg2_p)
    def seg3_p(F, w, h, d):
        sucker(F['F'], 2, 1)
    if sx < 0: add(bn, (mk(5, 4), 5, -3), (4, 2, 5), seg3_p)
    else: add(bn, (mk(5, 4), 5, -3), (4, 2, 5), seg3_p)
arm_parts("right_arm", 1)
arm_parts("left_arm", -1)

# ---------- 武器: 張り手
def palm_p(F, w, h, d):
    f = F['F']  # 掌 7x8
    # 掌パッド: 明るい肌色
    for y in range(f.h):
        for x in range(f.w):
            c = S_L if y < 3 else (S_M if y < 6 else H('e07c72'))
            f.px(x, y, c)
    for x in range(f.w):
        f.px(x, f.h-1, R_OL); 
    for y in range(f.h): f.px(0, y, R_D); f.px(f.w-1, y, R_D)
    f.rect(0, 0, f.w, 1, R_M)
    # 掌の吸盤
    sucker(f, 2, 2, True); sucker(f, 5, 2, True); sucker(f, 2, 5, True); sucker(f, 5, 5, True)
    f.px(3, 3, WHITE)
add("weapon", (6, 3, -6), (7, 8, 3), palm_p)
def fing_p(F, w, h, d):
    F['F'].rect(0, 0, w, h, R_L); F['F'].rect(0, h-1, w, 1, R_D)
    sucker(F['F'], 1, 0)
    F['F'].px(0, 0, R_HI)
add("weapon", (6, 11, -6), (3, 2, 3), fing_p)
add("weapon", (10, 11, -6), (3, 2, 3), fing_p)
add("weapon", (13, 5, -5), (2, 3, 2), fing_p)  # 親指

# ---------- 脚
def leg_p(sx):
    def f_(F, w, h, d):
        sucker(F['F'], 2, 2)
        F['F'].rect(1, 0, 2, 1, R_L)
    return f_
add("right_leg", (0, 0, -3), (5, 6, 5), leg_p(1))
add("left_leg", (-5, 0, -3), (5, 6, 5), leg_p(-1))
def foot_p(F, w, h, d):
    sucker(F['F'], 1, 0); sucker(F['F'], 3, 0)
    F['T'].px(1, 1, S_M) 
add("right_leg", (0, 0, -5), (5, 2, 2), foot_p)
add("left_leg", (-5, 0, -5), (5, 2, 2), foot_p)

# ---------- 背中のタコ足
def tent_p(F, w, h, d):
    for k in ('F','B','L','R'):
        sucker(F[k], F[k].w//2-1, F[k].h//2-1) if k in 'FB' else None
    for y in range(F['T'].h): pass
def tentacle(bn, sx):
    def mk(x0, w): return x0 if sx > 0 else -x0 - w
    add(bn, (mk(1, 4), 11, 3), (4, 4, 3), tent_p)
    add(bn, (mk(2, 4), 9, 5), (4, 4, 3), tent_p)
    add(bn, (mk(3, 4), 7, 7), (4, 4, 3), tent_p)
    add(bn, (mk(3, 3), 5, 9), (3, 3, 3), tent_p)
tentacle("tentacle_r", 1); tentacle("tentacle_l", -1)

order = ["body","head","right_arm","left_arm","right_leg","left_leg","weapon","tentacle_r","tentacle_l"]
model = {"texture_size": [S, S], "bones": [bones[n] for n in order]}
os.makedirs(os.path.dirname(OUT_J), exist_ok=True); os.makedirs(os.path.dirname(OUT_P), exist_ok=True)
json.dump(model, open(OUT_J, "w"), ensure_ascii=False, indent=1)
img.save(OUT_P)
print("ok")

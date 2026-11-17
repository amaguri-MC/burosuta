#!/usr/bin/env python3
"""showdown.txt を生成する（4回回転対称 + 少し崩す）。実行: python3 gen_map.py"""
import os
N = 50
Q = 25
OUT = "/home/claude/burosuta/src/main/resources/data/brawl/maps/showdown.txt"

q = [['.'] * Q for _ in range(Q)]
def rect(ch, r, c, h, w):
    for i in range(r, r + h):
        for j in range(c, c + w):
            if 0 < i < Q and 0 < j < Q:
                q[i][j] = ch
def put(ch, pts):
    for r, c in pts:
        q[r][c] = ch

# ---- 茂み（帯とかたまり）
rect('B', 3, 3, 3, 4)      # 角のかたまり
rect('B', 8, 2, 2, 8)      # 横の帯
rect('B', 2, 17, 3, 5)     # 上辺寄り
rect('B', 13, 12, 3, 3)
rect('B', 15, 3, 7, 2)     # 縦の帯
rect('B', 19, 11, 2, 6)
rect('B', 10, 17, 4, 2)
rect('B', 6, 21, 5, 3)
rect('B', 22, 6, 2, 4)
rect('B', 17, 20, 2, 4)
# ---- 壁（L字・短い列）
put('#', [(5, c) for c in range(11, 16)] + [(r, 11) for r in range(6, 9)])      # L
put('#', [(r, 6) for r in range(12, 16)] + [(15, c) for c in range(7, 9)] )     # L
put('#', [(12, 15), (12, 16), (12, 17)])
put('#', [(17, c) for c in range(15, 19)] + [(r, 18) for r in range(18, 20)])   # L
put('#', [(9, 12), (9, 13), (10, 13)])
put('#', [(21, 20), (22, 20)])
put('#', [(3, 10), (4, 10)])
put('#', [(20, 5), (21, 5)])
put('#', [(23, 15), (23, 16), (23, 17)])
# ---- 箱（中央寄り多め）
put('X', [(20, 21), (22, 23), (23, 19), (11, 10)])
# ---- ロープ
put('R', [(14, 21), (14, 22), (14, 23)])
# 外壁
for i in range(Q):
    q[0][i] = '='; q[i][0] = '='

def rot(r, c):
    while not (r < Q and c < Q):
        r, c = c, N - 1 - r
    return r, c

g = [[q[rot(r, c)[0]][rot(r, c)[1]] for c in range(N)] for r in range(N)]
for i in range(N):
    for ch in (0, N - 1):
        g[ch][i] = '='; g[i][ch] = '='

# ---- 少し崩す
def setc(r, c, ch): g[r][c] = ch
# 1) 一枚だけ壁を茂みに / 茂みを壁に
for r, c in [(5, 12), (5, 13)]: setc(r, c, 'B')
for r, c in [(33, 40), (33, 41)]: setc(r, c, '#')
# 2) ロープを2か所だけ消して他は残す
for c in (21, 22, 23): setc(14, c, '.')     # 左上象限のロープを無くす
for r in (27, 28, 29): setc(r, 14, '.') if False else None
# 3) 茂みを少し足す・削る
for r, c in [(30, 30), (30, 31), (31, 30)]: setc(r, c, 'B')
for r, c in [(44, 20), (44, 21)]: setc(r, c, 'B')
for r, c in [(8, 3), (8, 4)]: setc(r, c, '.')
# 4) 箱: 1個動かす
for r, c in [(11, 10)]: setc(r, c, '.')
setc(12, 11, 'X')

# ---- スタート10個（外周を約等間隔） 外周の内側リング(オフセット2)の周長で18おき
ring = []
lo, hi = 2, 47
for c in range(lo, hi): ring.append((lo, c))
for r in range(lo, hi): ring.append((r, hi))
for c in range(hi, lo, -1): ring.append((hi, c))
for r in range(hi, lo, -1): ring.append((r, lo))
step = len(ring) / 10
starts = [ring[int(round(i * step)) % len(ring)] for i in range(10)]
for (r, c) in starts:
    for dr in range(-1, 2):
        for dc in range(-1, 2):
            rr, cc = r + dr, c + dc
            if 0 < rr < N - 1 and 0 < cc < N - 1:
                g[rr][cc] = '.'
    g[r][c] = 'S'
# 中央 2x2 は空ける
for r in (24, 25):
    for c in (24, 25): g[r][c] = '.'

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w") as f:
    f.write("\n".join("".join(row) for row in g) + "\n")
print("written", OUT, "starts", starts)

"""ロビー広場の文字マップ(44x44)を作り、data/brawl/lobby/plaza.txt に書き、真上図 lobby_top.png を描く。
実行: python3 -I gen_plaza.py   （Pillow が要る）
文字の意味は下の LEGEND。Java 側(LobbyMap)も同じ文字を読む。"""
import math, os
from PIL import Image, ImageDraw, ImageFont

N = 44
HERE = os.path.dirname(os.path.abspath(__file__))
OUT_TXT = os.path.join(HERE, "../../../src/main/resources/data/brawl/lobby/plaza.txt")
OUT_PNG = os.path.join(HERE, "lobby_top.png")

g = [["." for _ in range(N)] for _ in range(N)]
def put(i, j, c):
    if 0 <= i < N and 0 <= j < N: g[j][i] = c

# 外周: 壁(W) / 縁(e)2マス / 水色の線(c)
for j in range(N):
    for i in range(N):
        d = min(i, j, N - 1 - i, N - 1 - j)
        if d == 0: put(i, j, "W")
        elif d <= 2: put(i, j, "e")
        elif d == 3: put(i, j, "c")

# 中央の円形模様
cx, cz = 22, 23
for j in range(N):
    for i in range(N):
        r = math.hypot(i - cx, j - cz)
        if g[j][i] != ".": continue
        if 6.0 <= r < 7.6: put(i, j, "y")
        elif 3.4 <= r < 4.6: put(i, j, "c")
        elif r < 2.0: put(i, j, "e")

# 像の台座(3x3, 中心がN)
STATUE_X = [6, 14, 22, 30, 38]
for sx in STATUE_X:
    for dj in (-1, 0, 1):
        for di in (-1, 0, 1):
            put(sx + di, 10 + dj, "N" if (di == 0 and dj == 0) else "T")

# 旗(ポールの東に2マスの旗がなびく)
for fx, col in zip([10, 18, 26, 34], "RYGU"):
    put(fx, 6, col)

# 練習場: 緑のマット + カカシ3体 + 射撃ライン
for j in range(17, 32):
    for i in range(4, 9):
        put(i, j, "g")
for dj in (19, 24, 29):
    for di in (-1, 0, 1):
        for dd in (-1, 0, 1):
            put(6 + di, dj + dd, "d")
    put(6, dj, "D")
for j in range(17, 32):
    put(12, j, "e")

# プレイ床 4x4 と縁
for j in range(22, 26):
    for i in range(34, 38):
        put(i, j, "P")
for j in range(21, 27):
    for i in range(33, 39):
        if g[j][i] != "P": put(i, j, "p")

# 掲示板(東)
for j in range(28, 35):
    for i in (40, 41):
        put(i, j, "K")

# 植え込み(南の隅)
for pcx in (6, 38):
    for dj in range(-2, 3):
        for di in range(-2, 3):
            edge = max(abs(di), abs(dj)) == 2
            put(pcx + di, 38 + dj, "q" if edge else "m")
    put(pcx, 38, "V")

# ベンチ(北向き)
for bx in (14, 28):
    for di in range(3): put(bx + di, 36, "B")

# スポーン(中央手前)
for j in range(N):
    for i in range(N):
        if math.hypot(i - 22, j - 38) <= 2.3 and g[j][i] == ".": put(i, j, "c")
put(22, 38, "S")

# 街灯
for lx, lz in [(10, 14), (34, 14), (10, 33), (22, 31), (34, 36), (10, 24), (22, 4), (16, 22), (28, 22)]:
    put(lx, lz, "M")

rows = ["".join(r) for r in g]
with open(OUT_TXT, "w", encoding="utf-8") as f:
    f.write("\n".join(rows) + "\n")

# 明かりの届き具合(近似): 光源から 14 マス以内か
SRC = []
for j in range(N):
    for i in range(N):
        if g[j][i] in "MPK" or (g[j][i] in "TN"): SRC.append((i, j))
dark = [(i, j) for j in range(3, N - 3) for i in range(3, N - 3)
        if min(abs(i - a) + abs(j - b) for a, b in SRC) > 14]
print("暗いマス:", len(dark), dark[:10])

# ---- 真上図 ----
S = 8
COL = {
    "a": (255, 244, 214), "b": (200, 232, 255), ".": None,
    "e": (255, 150, 40), "c": (110, 200, 245), "y": (255, 215, 60), "g": (120, 205, 90),
    "P": (255, 235, 90), "p": (255, 255, 255), "W": (225, 110, 25),
    "T": (110, 90, 200), "N": (140, 120, 235), "D": (230, 60, 60), "d": (255, 255, 255),
    "q": (240, 130, 40), "m": (110, 160, 80), "V": (50, 140, 60),
    "B": (150, 100, 60), "K": (90, 75, 170), "M": (255, 250, 200), "S": (255, 90, 160),
    "R": (230, 60, 60), "Y": (250, 210, 40), "G": (80, 200, 90), "U": (60, 120, 230),
}
FLAGCOL = {"R": (230, 60, 60), "Y": (250, 210, 40), "G": (80, 200, 90), "U": (60, 120, 230)}
LEG_H = 190
img = Image.new("RGB", (N * S + 230, max(N * S, 420)), (40, 40, 48))
d = ImageDraw.Draw(img)
for j in range(N):
    for i in range(N):
        c = g[j][i]
        col = COL.get(c)
        if c == ".":
            col = (255, 244, 214) if ((i // 2 + j // 2) % 2 == 0) else (200, 232, 255)
        if c in "RYGU":
            col = (255, 244, 214) if ((i // 2 + j // 2) % 2 == 0) else (200, 232, 255)
        if col is None: col = (255, 0, 255)
        d.rectangle([i * S, j * S, i * S + S - 1, j * S + S - 1], fill=col)
# 旗・ポール
for j in range(N):
    for i in range(N):
        c = g[j][i]
        if c in FLAGCOL:
            for k in (1, 2):
                d.rectangle([(i + k) * S, j * S + 1, (i + k) * S + S - 1, j * S + S - 2], fill=FLAGCOL[c])
            d.rectangle([i * S + 2, j * S + 2, i * S + S - 3, j * S + S - 3], fill=(120, 80, 40))
        if c == "M":
            d.ellipse([i * S + 1, j * S + 1, i * S + S - 2, j * S + S - 2], fill=(255, 250, 200), outline=(220, 170, 40))
        if c == "N":
            d.ellipse([i * S + 1, j * S + 1, i * S + S - 2, j * S + S - 2], fill=(255, 255, 255), outline=(80, 60, 160))
        if c == "D":
            d.ellipse([i * S + 1, j * S + 1, i * S + S - 2, j * S + S - 2], fill=(230, 60, 60), outline=(255, 255, 255))
        if c == "S":
            d.polygon([(i * S + S // 2, j * S), (i * S + S - 1, j * S + S - 1), (i * S, j * S + S - 1)], fill=(255, 90, 160))
        if c == "V":
            d.ellipse([i * S, j * S, i * S + S - 1, j * S + S - 1], fill=(50, 140, 60))
# 格子の薄線は描かない(1ブロック=8px)
try:
    font = ImageFont.truetype("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc", 11)
except Exception:
    font = ImageFont.load_default()
LEG = [("W", "外壁(縁色+見えない壁)"), ("e", "縁の床"), ("c", "水色の線"), ("y", "金色の輪"), ("a", "タイル(市松)"),
       ("N", "像の台座(上に像)"), ("T", "台座"), ("D", "カカシ(3体)"), ("d", "的の輪"), ("g", "練習マット"),
       ("P", "プレイ床 4x4"), ("p", "プレイ床の縁"), ("K", "掲示板(上位5人)"), ("S", "スポーン"),
       ("M", "街灯"), ("R", "旗(東へ2マス)"), ("q", "植え込みの縁"), ("m", "苔と花"), ("V", "茂み"), ("B", "ベンチ")]
x0 = N * S + 10
d.text((x0, 4), "ロビー 44x44 (1マス=8px)", fill=(255, 255, 255), font=font)
for k, (c, t) in enumerate(LEG):
    y = 22 + k * 17
    col = COL.get(c) or (255, 244, 214)
    d.rectangle([x0, y, x0 + 11, y + 11], fill=col, outline=(0, 0, 0))
    d.text((x0 + 17, y - 1), f"{c}  {t}", fill=(255, 255, 255), font=font)
d.text((x0, 22 + len(LEG) * 17 + 4), "上=北  下=南(入口)", fill=(200, 200, 200), font=font)
img.save(OUT_PNG)
print("ok", OUT_TXT, OUT_PNG)

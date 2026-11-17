#!/usr/bin/env python3
"""showdown.txt の検査: 寸法・個数・到達性(BFS)・茂み率。実行: python3 check.py [map.txt]
通れる: . B S X以外 ... (# = X R は不可)。全スタート→中央(24..25,24..25)が歩いてつながることを確認。"""
import sys
from collections import deque
P = sys.argv[1] if len(sys.argv) > 1 else "/home/claude/burosuta/src/main/resources/data/brawl/maps/showdown.txt"
rows = [l.rstrip("\n") for l in open(P) if l.strip()]
N = len(rows)
assert N == 50 and all(len(r) == 50 for r in rows), "50x50 ではない: %d行 %s" % (N, {len(r) for r in rows})
cnt = {}
for r in rows:
    for ch in r: cnt[ch] = cnt.get(ch, 0) + 1
assert set(cnt) <= set(".#=BXRS"), "不明な文字: %s" % set(cnt)
walk = lambda ch: ch in ".BS"
starts = [(r, c) for r in range(N) for c in range(N) if rows[r][c] == 'S']
center = [(r, c) for r in (24, 25) for c in (24, 25)]
def bfs(src):
    seen = {src}; dq = deque([src])
    while dq:
        r, c = dq.popleft()
        for dr, dc in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            rr, cc = r + dr, c + dc
            if 0 <= rr < N and 0 <= cc < N and (rr, cc) not in seen and walk(rows[rr][cc]):
                seen.add((rr, cc)); dq.append((rr, cc))
    return seen
reach = bfs(center[0])
ok = all(p in reach for p in center) and all(s in reach for s in starts)
free = sum(1 for r in range(N) for c in range(N) if walk(rows[r][c]))
print("S=%d X=%d R=%d 壁#=%d 外壁==%d 茂みB=%d (全体の%.1f%%)" % (cnt.get('S', 0), cnt.get('X', 0), cnt.get('R', 0), cnt.get('#', 0), cnt.get('=', 0), cnt.get('B', 0), 100 * cnt.get('B', 0) / 2500))
print("歩ける%d 中央から到達%d / 孤立した歩ける床 %d" % (free, len(reach), free - len(reach)))
for s in starts:
    print("  S(%2d,%2d) -> 中央: %s" % (s[0], s[1], "到達可" if s in reach else "到達不可"))
checks = [("S=10", cnt.get('S', 0) == 10), ("X 14-18", 14 <= cnt.get('X', 0) <= 18),
          ("全スタート→中央 到達", ok), ("茂み 15-20%", 15 <= 100 * cnt.get('B', 0) / 2500 <= 20)]
for n, v in checks: print(("OK  " if v else "NG  ") + n)
sys.exit(0 if all(v for _, v in checks) else 1)

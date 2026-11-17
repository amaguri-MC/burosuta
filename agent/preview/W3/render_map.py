#!/usr/bin/env python3
from PIL import Image, ImageDraw
P = "/home/claude/burosuta/src/main/resources/data/brawl/maps/showdown.txt"
OUT = "/home/claude/burosuta/agent/preview/W3/map.png"
T = 8
rows = [l.rstrip("\n") for l in open(P) if l.strip()]
im = Image.new("RGB", (50 * T, 50 * T))
d = ImageDraw.Draw(im)
FA, FB = (126, 200, 74), (108, 178, 62)
COL = {'#': (206, 164, 108), '=': (84, 70, 78), 'B': (28, 112, 48), 'X': (150, 100, 55), 'R': (222, 196, 120)}
for z, row in enumerate(rows):
    for x, ch in enumerate(row):
        x0, y0 = x * T, z * T
        d.rectangle([x0, y0, x0 + T - 1, y0 + T - 1], fill=FA if (x + z) % 2 == 0 else FB)
        if ch in COL:
            d.rectangle([x0, y0, x0 + T - 1, y0 + T - 1], fill=COL[ch])
        if ch == 'X':
            d.rectangle([x0 + 2, y0 + 2, x0 + 5, y0 + 5], fill=(90, 240, 110))
        if ch == 'R':
            d.rectangle([x0, y0 + 3, x0 + T - 1, y0 + 4], fill=(150, 112, 60))
        if ch == 'S':
            d.ellipse([x0 + 1, y0 + 1, x0 + 6, y0 + 6], fill=(240, 90, 60), outline=(150, 40, 30))
im.save(OUT)
print("saved", OUT, im.size)

# U: 見本用の描画ライブラリ。Java の Ui.java / Faces.java と同じ式（GUI座標 → S 倍で描く）。
import math, json, os
from PIL import Image, ImageDraw, ImageFont

S = 3
ROOT = "/home/claude/burosuta"
ICON = ROOT + "/src/main/resources/assets/brawl/textures/gui/"
BR = ROOT + "/src/client/resources/assets/brawl/"
FONT = "/usr/share/fonts/opentype/noto/NotoSansCJK-Black.ttc"

NAVY=0xFF1E2B52; NAVY_EDGE=0xFF0B1330; NAVY_LIGHT=0xFF34457F
YELLOW=0xFFFFD21F; YELLOW_EDGE=0xFF7A4A00; YELLOW_DARK=0xFFE0A100
BLUE=0xFF2F8CFF; BLUE_EDGE=0xFF123C8A
GREEN=0xFF4CD04A; GREEN_EDGE=0xFF1B6B24; GREEN_BG=0xFF14301A
RED=0xFFFF4A3D; RED_EDGE=0xFF7A1410; RED_BG=0xFF3A1412
ORANGE=0xFFFF9A1F; ORANGE_EDGE=0xFF8A4A0A; ORANGE_BG=0xFF3A2410
WHITE=0xFFFFFFFF; GREY=0xFFB8C2DC
GAS=0xFF6DE04A; GAS_EDGE=0xFF1E5A14
LEFT, CENTER, RIGHT = -1, 0, 1

def clamp(v, a, b): return max(a, min(b, v))
def lighten(c, n):
    a = c & 0xFF000000
    r = clamp(((c>>16)&255)+n,0,255); g = clamp(((c>>8)&255)+n,0,255); b = clamp((c&255)+n,0,255)
    return a | (r<<16) | (g<<8) | b
def darken(c, n): return lighten(c, -n)
def alpha(c, a): return (clamp(a,0,255)<<24) | (c & 0xFFFFFF)
def rgba(c): return ((c>>16)&255, (c>>8)&255, c&255, (c>>24)&255)
def ease(t):
    t = clamp(t, 0, 1); return 1-(1-t)*(1-t)
def jround(v):  # Java Math.round
    return int(math.floor(v + 0.5))

_fonts = {}
def pfont(px):
    px = max(4, int(round(px)))
    if px not in _fonts: _fonts[px] = ImageFont.truetype(FONT, px)
    return _fonts[px]
def wide(s): return any(ord(ch) >= 0x2000 for ch in s)
def cw(ch): return 9 if ord(ch) >= 0x2000 else (4 if ch == ' ' else 6)
def fw(s): return sum(cw(ch) for ch in s)   # MC の font.width の近似

class Ctx:
    def __init__(self, W, H, bg=None):
        self.W, self.H = W, H
        self.img = Image.new("RGB", (int(math.ceil(W*S)), int(math.ceil(H*S))), (0,0,0)) if bg is None else bg
        self.d = ImageDraw.Draw(self.img, "RGBA")
    def fill(self, x0, y0, x1, y1, c):
        if x1 <= x0 or y1 <= y0: return
        self.d.rectangle([x0*S, y0*S, x1*S-1, y1*S-1], fill=rgba(c))
    # ---- 図形 ----
    def panel(self, x, y, w, h, skew, fill, edge, bw):
        k = skew/(h-1) if h > 1 else 0
        for r in range(-bw, h+bw):
            off = jround(k*(h-1-r)); self.fill(x+off-bw, y+r+2, x+off+w+bw, y+r+3, 0x55000000)
        for r in range(-bw, h+bw):
            off = jround(k*(h-1-r)); self.fill(x+off-bw, y+r, x+off+w+bw, y+r+1, edge)
        for r in range(h):
            off = jround(k*(h-1-r))
            c = lighten(fill,28) if r < 2 else (darken(fill,22) if r >= h-2 else fill)
            self.fill(x+off, y+r, x+off+w, y+r+1, c)
    def box(self, x, y, w, h, fill, edge, bw):
        self.fill(x-bw, y-bw, x+w+bw, y+h+bw, edge); self.fill(x, y, x+w, y+h, fill)
        if h >= 4: self.fill(x, y, x+w, y+1, lighten(fill,28))
    def bar(self, x, y, w, h, frac, fill, bg, edge):
        self.fill(x-1, y-1, x+w+1, y+h+1, edge); self.fill(x, y, x+w, y+h, bg)
        fwid = jround(w*clamp(frac,0,1))
        if fwid > 0:
            self.fill(x, y, x+fwid, y+h, fill)
            if h >= 4:
                self.fill(x, y, x+fwid, y+1, lighten(fill,50)); self.fill(x, y+h-1, x+fwid, y+h, darken(fill,30))
    def fatbar(self, x, y, w, h, frac, fill, bg, edge, bw):
        self.fill(x-bw, y-bw, x+w+bw, y+h+bw, edge); self.bar(x, y, w, h, frac, fill, bg, bg)
    def disc(self, cx, cy, r, c):
        for dy in range(-r, r+1):
            half = int(math.floor(math.sqrt(max(0, r*r + r - dy*dy))))
            self.fill(cx-half, cy+dy, cx+half+1, cy+dy+1, c)
    def discfill(self, cx, cy, r, level, c, ctop):
        top = jround(cy + r - (2*r+1)*clamp(level,0,1))
        for dy in range(-r, r+1):
            y = cy+dy
            if y < top: continue
            half = int(math.floor(math.sqrt(max(0, r*r + r - dy*dy))))
            self.fill(cx-half, y, cx+half+1, y+1, ctop if y <= top+1 else c)
    def vignette(self, rgb, strength):
        W, H = self.W, self.H
        e = max(14, min(W, H)//6)
        for i in range(e):
            t = 1 - i/e; a = int(0xB0*t*t*strength)
            if a <= 0: continue
            col = (a<<24) | (rgb & 0xFFFFFF)
            self.fill(i, i, W-i, i+1, col); self.fill(i, H-i-1, W-i, H-i, col)
            self.fill(i, i+1, i+1, H-i-1, col); self.fill(W-i-1, i+1, W-i, H-i-1, col)
    def icon(self, name, x, y, size, tint=0xFFFFFFFF):
        im = Image.open(ICON+name+".png").convert("RGBA").resize((max(1,int(size*S)),)*2, Image.NEAREST)
        if tint != 0xFFFFFFFF:
            r,g,b,a = rgba(tint); px = im.load()
            for yy in range(im.size[1]):
                for xx in range(im.size[0]):
                    p = px[xx,yy]; px[xx,yy] = (p[0]*r//255, p[1]*g//255, p[2]*b//255, p[3]*a//255)
        self.img.paste(im, (int(x*S), int(y*S)), im)
    # ---- 文字（Ui.text と同じ: 縁取り・太字・日本語は2倍以上で細い縁） ----
    def text(self, s, x, y, scale, col, edge, align=LEFT, bold=False):
        tw = fw(s)*scale
        ax = x - tw/2 if align == CENTER else (x - tw if align == RIGHT else x)
        jp = wide(s)
        t = (1.0 if scale < 3 else 0.75) if not jp else (0.6 if scale >= 2 else 0.0)
        cx = ax
        for ch in s:
            self._glyph(ch, cx, y, scale, col, edge, bold and not jp, t)
            cx += cw(ch)*scale
    def _glyph(self, ch, x, y, scale, col, edge, bold, t):
        if ch == ' ': return
        jp = ord(ch) >= 0x2000
        f = pfont((8.2 if jp else 9.6)*scale*S)
        px, py = x*S, (y+7*scale)*S  # MC のベースラインは上端から7
        er = rgba(edge); cr = rgba(col)
        def put(dx, dy, c, stroke=0):
            self.d.text((px+dx*S*scale, py+dy*S*scale), ch, font=f, fill=c, anchor='ls')
        if t <= 0:
            put(1/scale, 1/scale, er)
        else:
            for dx, dy in [(-t,0),(t,0),(0,-t),(0,t),(-t,-t),(t,-t),(-t,t),(t,t)]: put(dx, dy, er)
            if bold: put(0.5, 0, er)
        put(0, 0, cr)
        if bold: put(0.5, 0, cr)

# ---- Faces ----
_heads = {}
def face_info(bid):
    if bid in _heads: return _heads[bid]
    p = BR+"models/brawler/%s.json" % bid
    if not os.path.exists(p): _heads[bid] = None; return None
    j = json.load(open(p)); tw, th = j.get("texture_size", [64,64])
    top = 0; face = None
    for b in j["bones"]:
        for c in b.get("cubes", []): top = max(top, c["from"][1]+c["size"][1])
        if face is None and b["name"] == "head" and b.get("cubes"):
            c = b["cubes"][0]; w,h,d = [jround(v) for v in c["size"]]
            face = (c["uv"][0]+d, c["uv"][1]+d, w, h)
    _heads[bid] = (face, tw, th, top/16.0); return _heads[bid]
def tex_img(bid): return Image.open(BR+"textures/entity/brawler/%s.png" % bid).convert("RGBA")
def face_draw(c, bid, x, y, size):
    fi = face_info(bid)
    if not fi or not fi[0]:
        c.fill(x, y, x+size, y+size, 0xFF6B7391); return
    (u,v,w,h), tw, th, _ = fi
    s = min(size/w, size/h); dw = max(1, jround(w*s)); dh = max(1, jround(h*s))
    im = tex_img(bid).crop((u,v,u+w,v+h)).resize((dw*S, dh*S), Image.NEAREST)
    c.img.paste(im, (int((x+(size-dw)//2)*S), int((y+(size-dh)//2)*S)), im)
def face_framed(c, bid, x, y, size, bg, edge, bw):
    c.box(x, y, size, size, bg, edge, bw); face_draw(c, bid, x, y, size)

# ---- 3D の代用: 正面から見た紙人形（各 cube の正面をそのまま貼る） ----
def paper_doll(bid, px_per_block):
    j = json.load(open(BR+"models/brawler/%s.json" % bid)); tex = tex_img(bid)
    items = []
    for b in j["bones"]:
        for cu in b.get("cubes", []):
            fx, fy, fz = cu["from"]; w,h,d = [jround(v) for v in cu["size"]]
            sx, sy, sz = cu["size"]; u,v = cu["uv"]
            items.append((fz, fx, fy, sx, sy, jround(sz), u, v))
    items.sort(key=lambda t: -t[0])  # 奥から
    xs = [i[1] for i in items]+[i[1]+i[3] for i in items]; ys = [i[2]+i[4] for i in items]
    minx, maxx = min(xs), max(xs); maxy = max(ys)
    sc = px_per_block/16.0
    W = int((maxx-minx)*sc)+2; H = int(maxy*sc)+2
    im = Image.new("RGBA", (W, H), (0,0,0,0))
    for fz, fx, fy, sx, sy, d, u, v in items:
        w, h = jround(sx), jround(sy)
        face = tex.crop((u+d, v+d, u+d+w, v+d+h))
        face = face.resize((max(1,int(sx*sc)), max(1,int(sy*sc))), Image.NEAREST)
        # 形式は x+=右・正面を向く側から見ると左右反転
        px = int((maxx-(fx+sx))*sc); py = int((maxy-(fy+sy))*sc)
        im.paste(face, (px, py), face)
    return im, maxy/16.0

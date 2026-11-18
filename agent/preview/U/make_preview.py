# U: 見本4枚（match / lobby / select / result、1280x720 = GUI倍率3 → 427x240）。Java の MatchHud / LobbyHud / SelectScreen / ResultScreen と同じ座標。
import math, random, sys
sys.path.insert(0, "/home/claude/burosuta/agent/preview/U")
from ui_lib import *
from PIL import Image, ImageDraw

W, H = 427, 240
OUT = "/home/claude/burosuta/agent/preview/U/"
DEFS = {  # BrawlerStats の値（見本用）
 "rusty":("ラスティ","ショットガン使い",3600,2.4,7.67,1.5,0.067),
 "kaden":("ケイデン","ガンマン",2800,2.4,9.33,1.6,0.042),
 "gouen":("ゴウエン","タンク",5000,2.57,5.33,1.6,0.0625),
 "mimosa":("ミモザ","花火砲手",2400,2.4,9.67,2.1,0.25),
 "takomaru":("タコまる","格闘家",6000,2.57,3.0,1.1,0.0625),
}
SK = {
 "rusty":(("スクラップ散弾","前方に5発の散弾を扇状に撃つ。近いほど全部当たって痛い。"),("鉱山ダイナマイト弾","広い扇状に9発の爆裂弾を放ち、当てた敵を大きく吹き飛ばす。")),
 "kaden":(("ツインレイ","光線弾を6連射する。まっすぐ飛んで遠くまで届く。"),("レイストーム","12発の光線弾を一気に連射する。射程も伸びる。")),
 "gouen":(("岩石つぶて","岩のつぶてを5発、扇状に投げる。射程は短いが重い。"),("ゴーレムタックル","前へ一気にダッシュして、ぶつかった敵にダメージを与え押し出す。")),
 "mimosa":(("打ち上げ花火","遠くへ花火を1発。当たると爆発して周りの敵にも当たる。"),("大輪スターマイン","狙った場所に9発の花火を降らせる。範囲がとても広い。")),
 "takomaru":(("張り手連打","目の前に4発の張り手を連打する。射程は短いが重い。"),("ボディプレス","高く跳んで着地し、周りの敵にダメージを与えて吹き飛ばす。")),
}
SK["takomaru"]=(("張り手連打","目の前に4発の張り手を連打する。射程は短いがタフ。"),SK["takomaru"][1])

def game_bg(seed=3):
    random.seed(seed)
    c = Ctx(W, H)
    for ty in range(0, H, 10):
        for tx in range(0, W, 10):
            v = random.randint(0, 14); col = (86+v, 158+v, 74+v//2) if (tx//10+ty//10) % 2 else (92+v, 166+v, 80+v//2)
            c.d.rectangle([tx*S, ty*S, (tx+10)*S-1, (ty+10)*S-1], fill=col+(255,))
    return c
def wall(c, x, y, w, h):
    c.d.rectangle([x*S, y*S, (x+w)*S, (y+h)*S], fill=(70,74,92,255)); c.d.rectangle([x*S, y*S, (x+w)*S, (y+3)*S], fill=(120,126,150,255))
def bush(c, x, y, w, h):
    c.d.rectangle([x*S, y*S, (x+w)*S, (y+h)*S], fill=(46,128,52,255))
    for i in range(0, w, 6): c.d.ellipse([(x+i)*S, (y-3)*S, (x+i+8)*S, (y+5)*S], fill=(60,150,66,255))
def doll(c, bid, cx, foot, pxb=34):
    im, hb = paper_doll(bid, pxb*S)
    c.img.paste(im, (int(cx*S - im.size[0]/2), int(foot*S - im.size[1])), im)
    return hb
def scene(c):
    wall(c, 60, 40, 60, 14); wall(c, 300, 100, 14, 70); bush(c, 250, 40, 70, 22); bush(c, 40, 120, 40, 34)
    c.d.ellipse([196*S, 138*S, 236*S, 148*S], fill=(0,0,0,70))

# ---- 頭上表示 ----
def overhead(c, sx, sy, name, hp, mx, self_, cubes=0, ammo=None):
    fc, bg, edge = (GREEN, GREEN_BG, GREEN_EDGE) if self_ else (RED, RED_BG, RED_EDGE)
    c.fatbar(sx-25, sy, 50, 6, hp/mx, fc, bg, NAVY_EDGE, 2)
    c.text(str(hp), sx, sy-14, 1.25, WHITE, edge, CENTER, True)
    c.text(name, sx, sy-25, 1.0, WHITE if self_ else 0xFFFFE4E0, NAVY_EDGE, CENTER)
    if cubes:
        cs = str(cubes); ix = sx-25-4-fw(cs)-11
        c.icon("cube", ix, sy-3, 11); c.text(cs, ix+12, sy-1, 1.0, WHITE, GREEN_EDGE, LEFT)
    if ammo is not None:
        gap = 3; sw = (50-gap*2)//3; x0 = sx-(3*sw+gap*2)//2
        for k in range(3): c.fatbar(x0+k*(sw+gap), sy+10, sw, 3, clamp(ammo-k,0,1), ORANGE, ORANGE_BG, NAVY_EDGE, 1)
def pop(c, sx, sy, amount, self_, age, dx):
    life = 28; a = int(255*clamp((life-age)/8,0,1)); rise = 24*ease(age/life)
    scale = 1.6+0.9*(1-ease(age/5))
    c.text(str(amount), sx+dx, int(sy-8-rise), scale, alpha(WHITE if self_ else 0xFFFF4A3D, a), alpha(NAVY_EDGE if self_ else RED_EDGE, a), CENTER, True)

# ---- MatchHud ----
def big_centered(c, s, cx, cy, scale, a=255):
    ty = jround(cy-4*scale)
    c.text(s, cx, ty+jround(scale*0.8), scale, alpha(NAVY_EDGE, a//2), alpha(NAVY_EDGE, a//2), CENTER, True)
    c.text(s, cx, ty, scale, alpha(YELLOW, a), alpha(YELLOW_EDGE, a), CENTER, True)

def match_hud(c, st):
    w, h = W, H; cx = w//2
    # 上中央
    c.panel(cx-53, 4, 100, 22, 6, NAVY, NAVY_EDGE, 2)
    label, num = "残り", str(st["alive"])
    lw = fw(label); nw = jround(fw(num)*2); total = 16+3+lw+3+nw; x = cx-total//2
    c.icon("skull", x, 7, 16); c.text(label, x+19, 12, 1.0, GREY, NAVY_EDGE, LEFT); c.text(num, x+19+lw+3, 7, 2.0, WHITE, NAVY_EDGE, LEFT, True)
    if st["phase"] == 2:
        coming = st["gas"] <= 0
        s = "毒ガス接近中！" if coming else "毒ガスまで %d秒" % st["gas"]
        a = 255
        if coming and (st["tick"]//6) % 2 == 1: a = 80
        pw = fw(s)+28; px = cx-(pw+4)//2
        c.panel(px, 31, pw, 12, 4, RED_BG if coming else 0xFF16401C, RED_EDGE if coming else GAS_EDGE, 2)
        c.icon("gas", px+4, 30, 14, alpha(WHITE, a))
        c.text(s, px+20, 33, 1.0, alpha(YELLOW if coming else GAS, a), alpha(RED_EDGE if coming else GAS_EDGE, a), LEFT)
    # キルログ（最新4件）
    row = 0
    for (kn, kb, vn, vb, age) in st["kills"][-4:]:
        slide = 0
        if age < 4: slide = (4-age)/4
        elif age > 92: slide = (age-92)/8
        gas = kn == ""; fs = 11
        kw = 12 if gas else fs+3+fw(kn); vw = fs+3+fw(vn); xw = fw("×")
        total = kw+5+xw+5+vw; pw = total+10; y = 6+row*17; x = 6-int(slide*(pw+12)); row += 1
        c.panel(x, y, pw, 14, 4, NAVY, NAVY_EDGE, 2)
        cx2 = x+6
        if gas: c.icon("gas", cx2, y+1, 12)
        else:
            face_framed(c, kb, cx2, y+1, fs, NAVY_LIGHT, NAVY_EDGE, 1); c.text(kn, cx2+fs+3, y+3, 1.0, WHITE, NAVY_EDGE, LEFT)
        cx2 += kw+5; c.text("×", cx2, y+3, 1.0, RED, RED_EDGE, LEFT); cx2 += xw+5
        face_framed(c, vb, cx2, y+1, fs, NAVY_LIGHT, NAVY_EDGE, 1); c.text(vn, cx2+fs+3, y+3, 1.0, 0xFFFFC8C0, NAVY_EDGE, LEFT)
    if st["alive_me"]:
        hud = st
        # 左下
        c.panel(44, h-26, 64, 14, 4, BLUE, BLUE_EDGE, 2); c.text(DEFS[hud["brawler"]][0], 54, h-23, 1.0, WHITE, BLUE_EDGE, LEFT)
        c.panel(44, h-42, 38, 13, 4, 0xFF2A9A36, GREEN_EDGE, 2); c.icon("cube", 49, h-42, 12)
        c.text(str(hud["cubes"]), 64, h-41, 1.25, WHITE, GREEN_EDGE, LEFT, True)
        c.box(8-2, h-44-2, 40, 40, YELLOW, NAVY_EDGE, 2); c.fill(8, h-44, 44, h-8, NAVY); face_draw(c, hud["brawler"], 8, h-44, 36)
        # 下中央
        frac = clamp(hud["hp"]/hud["maxhp"], 0, 1); low = frac < 0.3
        c.text(str(hud["hp"]), cx, h-57, 2.5, WHITE, RED_EDGE if low else GREEN_EDGE, CENTER, True)
        bw = 132
        c.fatbar(cx-bw//2, h-30, bw, 12, frac, RED if low else GREEN, GREEN_BG, NAVY_EDGE, 2)
        n = 3; sw = 40; gap = 6; x0 = cx-(n*sw+(n-1)*gap)//2
        for i in range(n): c.fatbar(x0+i*(sw+gap), h-14, sw, 7, clamp(hud["ammo"]-i,0,1), ORANGE, ORANGE_BG, NAVY_EDGE, 2)
        if hud["super"] >= 0.999:
            pulse = 0.5+0.5*math.sin(st["tick"]*0.4); pw = 92+jround(pulse*4)
            c.panel(cx-(pw+5)//2, h-80, pw, 15, 5, YELLOW_DARK, YELLOW_EDGE, 2)
            c.text("R 必殺技！", cx, h-77, 1.25, WHITE, YELLOW_EDGE, CENTER, True)
        # 右下ボタン
        cc = clamp(hud["super"], 0, 1); full = cc >= 0.999; r = 26; bx, by = w-38, h-38
        pulse = 0.5+0.5*math.sin(st["tick"]*0.45)
        if full: c.disc(bx, by, r+5+jround(pulse*3), alpha(YELLOW, 70+int(pulse*70)))
        c.disc(bx, by, r+3, NAVY_EDGE); c.disc(bx, by, r+1, (YELLOW if pulse > 0.5 else 0xFFFFF0A0) if full else NAVY_LIGHT)
        c.disc(bx, by, r-2, NAVY); c.discfill(bx, by, r-2, cc, YELLOW_DARK, YELLOW)
        c.icon("star", bx-15, by-15, 30, 0xFFFFFFFF if full else 0xB0FFFFFF)
        c.box(bx-r+1, by-r+1, 13, 13, NAVY, NAVY_EDGE, 1); c.text("R", bx-r+7, by-r+4, 1.0, WHITE, NAVY_EDGE, CENTER)
        r2 = 18; ax = bx-r-r2-8; ay = h-30; ready = hud["ammo"] >= 1
        c.disc(ax, ay, r2+3, NAVY_EDGE); c.disc(ax, ay, r2+1, BLUE_EDGE if ready else NAVY_LIGHT)
        c.disc(ax, ay, r2-1, BLUE if ready else 0xFF4A5578)
        c.disc(ax, ay-r2//2-1, r2//2, lighten(BLUE,40) if ready else 0xFF5A6588)
        c.disc(ax, ay+3, r2-5, BLUE if ready else 0xFF4A5578)
        c.icon("click", ax-11, ay-11, 22)
    # 中央
    if st["phase"] == 1:
        scale = 7*(1+0.6*(1-ease(st["cd_age"]/10)))
        big_centered(c, str(st["countdown"]), cx, h//2-8, scale)
    if st.get("fight_age") is not None:
        fa = st["fight_age"]; scale = 4.5*(1+0.8*(1-ease(fa/8))); a = int(255*clamp((34-fa)/8, 0, 1))
        big_centered(c, "ファイト！", cx, h//2-8, scale, a)
    if st.get("rank"):
        win = st["rank"] == 1; s = "#1 勝利！" if win else "#%d 敗退" % st["rank"]
        col, edge = (YELLOW, YELLOW_EDGE) if win else (WHITE, RED_EDGE)
        if st.get("rank_big", True):
            pop_ = 1+0.5*(1-ease(st["rank_age"]/8)); c.text(s, cx, h//3-int(2*4*pop_), 4*pop_, col, edge, CENTER, True)
        else:
            c.panel(cx-38, 48, 70, 18, 5, NAVY, NAVY_EDGE, 2); c.text(s, cx, 51, 1.75, col, edge, CENTER, True)

def lobby_hud(c, st):
    w, h = W, H; cx = w//2
    c.panel(12, 7, 66, 18, 5, NAVY, NAVY_EDGE, 2); c.icon("trophy", 4, 3, 24)
    c.text(str(st["trophies"]), 32, 10, 1.5, YELLOW, YELLOW_EDGE, LEFT, True)
    c.panel(12, 30, 56, 14, 4, NAVY, NAVY_EDGE, 2); c.icon("crown", 6, 28, 16); c.text("勝利 %d" % st["wins"], 26, 33, 1.0, WHITE, NAVY_EDGE, LEFT)
    ready = "準備完了 %d/%d" % (st["ready"], st["total"]); rw = fw(ready)*5//4+24
    c.panel(cx-(rw+5)//2, 5, rw, 18, 5, 0xFF2A9A36 if st["me_ready"] else NAVY, GREEN_EDGE if st["me_ready"] else NAVY_EDGE, 2)
    c.text(ready, cx, 10, 1.25, WHITE, GREEN_EDGE if st["me_ready"] else NAVY_EDGE, CENTER, True)
    if st["countdown"] >= 0:
        c.text("試合開始まで", cx, 28, 1.0, WHITE, NAVY_EDGE, CENTER)
        scale = 6*(1+0.5*(1-ease(st["cd_age"]/10))); big_centered(c, str(st["countdown"]), cx, 44+24, scale)
    elif not st["me_ready"]:
        s = "プレイ床に乗って準備完了！"; bob = jround(math.sin(st["tick"]*0.2)*1.5); pw = fw(s)+16
        c.panel(cx-(pw+4)//2, 28+bob, pw, 14, 4, BLUE, BLUE_EDGE, 2); c.text(s, cx, 31+bob, 1.0, WHITE, BLUE_EDGE, CENTER)
    bid = st["selected"]; name = DEFS[bid][0]; fx, fy = cx-62, h-46
    c.panel(cx-28, h-42, 92, 16, 5, BLUE, BLUE_EDGE, 2); c.text(name, cx-18, h-38, 1.25, WHITE, BLUE_EDGE, LEFT, True)
    c.panel(cx-28, h-24, 92, 14, 4, NAVY, NAVY_EDGE, 2); c.box(cx-22, h-22, 12, 11, YELLOW, YELLOW_EDGE, 1)
    c.text("B", cx-16, h-20, 1.0, NAVY_EDGE, YELLOW, CENTER); c.text("キャラ選択", cx-6, h-21, 1.0, WHITE, NAVY_EDGE, LEFT)
    c.box(fx-2, fy-2, 38, 38, YELLOW, NAVY_EDGE, 2); c.fill(fx, fy, fx+34, fy+34, NAVY); face_draw(c, bid, fx, fy, 34)

def wrap(s, width):
    lines, cur = [], ""
    for ch in s:
        if fw(cur+ch) > width: lines.append(cur); cur = ch
        else: cur += ch
    if cur: lines.append(cur)
    return lines

def select_screen(c, sel, current, age=40):
    w, h = W, H; order = list(DEFS)
    ts = clamp((h-36)//5-4, 20, 36); lx, ly = 10, 26; pw = 164; px = w-pw-8
    mx0 = lx+ts+14; mx1 = px-8; btnw, btnh = 112, 26; btnx = (mx0+mx1)//2-btnw//2; btny = h-34
    # 背景
    for y in range(h):
        t = y/(h-1); col = (int(0x24+(0x0C-0x24)*t), int(0x40+(0x15-0x40)*t), int(0x8C+(0x35-0x8C)*t))
        c.d.rectangle([0, y*S, w*S, (y+1)*S-1], fill=col+(255,))
    def slant(x, y, ww, hh, skew, col):
        for r in range(hh):
            off = jround(skew*(hh-1-r)/max(1, hh-1)); c.fill(x+off, y+r, x+off+ww, y+r+1, col)
    slant(mx0-6, 0, 70, h, 40, 0x18FFFFFF); slant(mx0+80, 0, 26, h, 40, 0x12FFFFFF); slant(mx1-60, 0, 40, h, 40, 0x10FFFFFF)
    bid = order[sel]; dname, role, hp, spd, rng, rel, sph = DEFS[bid]
    c.panel(10, 5, 84, 14, 5, YELLOW_DARK, YELLOW_EDGE, 2); c.text("キャラ選択", 18, 8, 1.0, WHITE, YELLOW_EDGE, LEFT)
    for i, b in enumerate(order):
        y = ly+i*(ts+4); on = i == sel
        if on: c.box(lx-3, y-3, ts+6, ts+6, YELLOW, NAVY_EDGE, 2)
        else: c.box(lx-2, y-2, ts+4, ts+4, NAVY, NAVY_EDGE, 2)
        c.fill(lx, y, lx+ts, y+ts, NAVY_LIGHT if on else NAVY); face_draw(c, b, lx, y, ts)
        if b == current: c.icon("star", lx+ts-9, y-4, 12)
    mcx = (mx0+mx1)//2; top, bot = 16, h-52
    a_, b_ = min(60, (mx1-mx0)//2-6), 9; cy = bot-2
    for dy in range(-b_, b_+1):
        half = int(a_*math.sqrt(1-dy*dy/(b_*b_))); c.fill(mcx-half, cy+dy, mcx+half+1, cy+dy+1, 0xFF0B1330 if abs(dy) >= b_-2 else 0xFF2C3A6B)
    for dy in range(-b_+3, b_-2):
        half = int((a_-3)*math.sqrt(1-dy*dy/((b_-3)**2))); c.fill(mcx-half, cy+dy, mcx+half+1, cy+dy+1, 0xFF3C4F94)
    # 3D の代用（紙人形）
    fi = face_info(bid); hb = fi[3]; pxb = 0.9*(bot-top)/hb
    im, _ = paper_doll(bid, pxb*S)
    c.img.paste(im, (int(mcx*S-im.size[0]/2), int(bot*S-6*S-im.size[1]+ (0.0))), im)
    # 右パネル
    py, ph = 8, h-16
    c.panel(px, py, pw, ph, 0, 0xE0182454, NAVY_EDGE, 2)
    x = px+8; iw = pw-16; y = py+7
    c.text(dname, x, y, 2.0, WHITE, NAVY_EDGE, LEFT, True); y += 21
    rw = fw(role)+14; c.panel(x, y, rw, 11, 3, YELLOW_DARK, YELLOW_EDGE, 1); c.text(role, x+6, y+2, 1.0, WHITE, YELLOW_EDGE, LEFT); y += 16
    mxall = max(d[2] for d in DEFS.values())
    c.fatbar(x, y, iw, 10, hp/mxall, GREEN, GREEN_BG, NAVY_EDGE, 2); c.text("HP %d" % hp, x+4, y+1, 1.0, WHITE, GREEN_EDGE, LEFT, True); y += 16
    cwid = iw//2-2
    stats = ["速さ %.1f" % spd, "射程 %.1f" % rng, "装填 %.1f秒" % rel, "必殺 %dヒット" % max(1, jround(1/max(0.01, sph)))]
    for i in range(4):
        sx = x+(i % 2)*(cwid+4); sy = y+(i//2)*14
        c.panel(sx, sy, cwid-3, 11, 3, NAVY_LIGHT, NAVY_EDGE, 1); c.text(stats[i], sx+5, sy+2, 1.0, WHITE, NAVY_EDGE, LEFT)
    y += 32
    def skill(icon, kind, nm, desc, y, col):
        c.panel(x, y, iw-4, 12, 4, col, darken(col, 80), 1); c.icon(icon, x+2, y-2, 14)
        c.text(kind, x+20, y+2, 1.0, WHITE, darken(col, 80), LEFT); c.text(nm, x+22+fw(kind), y+2, 1.0, WHITE if col == YELLOW else 0xFFFFF0A0, darken(col, 80), LEFT)
        y += 15
        for ln in wrap(desc, iw):
            c.text(ln, x, y, 1.0, 0xFFE6ECFF, 0xFF05080F, LEFT); y += 9   # 影つき（見本では縁）
        return y
    y = skill("click", "通常攻撃", SK[bid][0][0], SK[bid][0][1], y, BLUE); y += 3
    skill("star", "必殺技", SK[bid][1][0], SK[bid][1][1], y, YELLOW_DARK)
    c.panel(btnx, btny, btnw, btnh, 7, GREEN, GREEN_EDGE, 3)
    c.text("選ぶ", btnx+btnw//2+3, btny+5, 2.0, WHITE, GREEN_EDGE, CENTER, True)

def result_screen(c, rank, total, delta, trophies, rows, t=60, me=None):
    w, h = W, H; win = rank == 1
    c.fill(0, 0, w, h, 0xC0101A38)
    c.panel(-10, h//2-56, w//2+40, 64, 16, 0xFF6A4A00 if win else 0xFF1F4FA8, YELLOW_EDGE if win else BLUE_EDGE, 2)
    lx = (w//2+24)//2-6+8
    head = "#1 勝利！" if win else "#%d" % rank; base = 5 if win else 7
    pop_ = 1+0.8*(1-ease(t/8)); scale = base*pop_; ty = h//2-24-jround(4*scale)
    c.text(head, lx, ty+jround(scale*0.8), scale, alpha(NAVY_EDGE, 120), alpha(NAVY_EDGE, 120), CENTER, True)
    c.text(head, lx, ty, scale, YELLOW if win else WHITE, YELLOW_EDGE if win else BLUE_EDGE, CENTER, True)
    c.text("%d人中" % total, lx, h//2+16, 1.0, GREY, NAVY_EDGE, CENTER)
    tt = clamp((t-14)/26, 0, 1); shown = jround(delta*ease(tt)); ds = ("+" if delta > 0 else "")+str(shown)
    dcol = 0xFF7CF070 if delta > 0 else (0xFFFF6A5E if delta < 0 else GREY); dedge = GREEN_EDGE if delta > 0 else (RED_EDGE if delta < 0 else NAVY_EDGE)
    rise = jround(10*(1-ease(tt))); dy = h//2+38-rise; dw = jround(fw(ds)*3); total_w = 28+4+dw; dx = lx-total_w//2
    if t > 8:
        c.panel(dx-10, dy-4, total_w+16, 30, 6, NAVY, NAVY_EDGE, 2); c.icon("trophy", dx, dy-1, 28)
        c.text(ds, dx+32, dy+1, 3.0, dcol, dedge, LEFT, True); c.text("トロフィー %d" % trophies, lx, dy+33, 1.0, WHITE, NAVY_EDGE, CENTER)
    tx = w//2+30; tw = w//2-44; n = len(rows); rh = max(11, min(18, (h-50)//max(1, n))); ty0 = 22
    c.panel(tx, 6, tw, 12, 4, NAVY, NAVY_EDGE, 2); c.text("順位", tx+8, 8, 1.0, GREY, NAVY_EDGE, LEFT)
    for i, (nm, bid, rk) in enumerate(rows):
        ap = clamp((t-4-i*2)/6, 0, 1)
        if ap <= 0: continue
        y = ty0+i*(rh+2); x = tx-jround((1-ease(ap))*40); mine = rk == rank
        c.panel(x, y, tw, rh, 4, 0xFF2F6FD8 if mine else NAVY, YELLOW if mine else NAVY_EDGE, 2)
        rc = YELLOW if rk == 1 else 0xFFD8DEEE if rk == 2 else 0xFFE0A070 if rk == 3 else WHITE
        c.text("#%d" % rk, x+6, y+(rh-8)//2+1, 1.0, rc, NAVY_EDGE, LEFT, True)
        fs = rh-3; face_framed(c, bid, x+30, y+1, fs, NAVY_LIGHT, NAVY_EDGE, 1); c.text(nm, x+34+fs, y+(rh-8)//2+1, 1.0, WHITE, NAVY_EDGE, LEFT)
    c.text("クリックで閉じる", w//2, h-12, 1.0, GREY, NAVY_EDGE, CENTER)

def save(c, name):
    im = c.img.crop((0, 0, 1280, 720)); im.save(OUT+name)

if __name__ == "__main__":
    # 試合中（必殺満タン・キルログ・ダメージ数字）
    c = game_bg(); scene(c)
    doll(c, "kaden", 270, 118); overhead(c, 270, 52, "ミモザ", 2400, 4000, False, 2)
    doll(c, "rusty", 150, 190); overhead(c, 150, 120, "Steve", 3200, 4400, True, 1, 2.45)
    pop(c, 270, 52, 1040, False, 7, 12); pop(c, 150, 120, 320, True, 14, -10)
    doll(c, "gouen", 345, 210); overhead(c, 345, 140, "タコまる", 5000, 5000, False, 0)
    st = dict(phase=2, alive=7, gas=32, tick=7, countdown=0, cd_age=0, alive_me=True, brawler="rusty", hp=3200, maxhp=4400, ammo=2.45, super=1.0, cubes=1,
              kills=[("ケイデン","kaden","ミモザ","mimosa",20),("ゴウエン","gouen","ラスティ","rusty",40),("","","タコまる","takomaru",60)])
    match_hud(c, st); save(c, "match.png")
    # 試合開始のカウントダウン・毒ガス中・被弾
    c = game_bg(5); scene(c); doll(c, "rusty", 213, 150)
    c.vignette(0x50E030, 0.85); c.vignette(0xFF3A28, 0.5)
    st = dict(phase=1, alive=10, gas=40, tick=3, countdown=3, cd_age=2, alive_me=True, brawler="takomaru", hp=6000, maxhp=6000, ammo=3, super=0.45, cubes=0, kills=[])
    match_hud(c, st); save(c, "match_b.png")
    c = game_bg(7); scene(c); doll(c, "rusty", 213, 150, 30)
    c.vignette(0x50E030, 0.7)
    st = dict(phase=2, alive=4, gas=0, tick=0, countdown=0, cd_age=0, alive_me=False, brawler="rusty", hp=0, maxhp=3600, ammo=0, super=0, cubes=0,
              kills=[("ゴウエン","gouen","Steve","rusty",10)], rank=5, rank_big=True, rank_age=20, fight_age=None)
    match_hud(c, st); save(c, "match_c.png")
    c = game_bg(9); scene(c); doll(c, "kaden", 213, 150)
    st = dict(phase=2, alive=9, gas=0, tick=0, countdown=0, cd_age=0, alive_me=True, brawler="kaden", hp=2800, maxhp=2800, ammo=3, super=0.2, cubes=0,
              kills=[], fight_age=5)
    match_hud(c, st); save(c, "match_d.png")
    # ロビー
    c = game_bg(11); wall(c, 20, 150, 90, 12); doll(c, "rusty", 213, 160, 38)
    lobby_hud(c, dict(trophies=420, wins=12, ready=2, total=3, me_ready=False, countdown=-1, selected="rusty", cd_age=0, tick=5)); save(c, "lobby.png")
    c = game_bg(11); doll(c, "kaden", 213, 160, 38)
    lobby_hud(c, dict(trophies=420, wins=12, ready=3, total=3, me_ready=True, countdown=5, selected="kaden", cd_age=2, tick=5)); save(c, "lobby_b.png")
    # キャラ選択
    c = Ctx(W, H); select_screen(c, 0, "rusty"); save(c, "select.png")
    c = Ctx(W, H); select_screen(c, 3, "rusty"); save(c, "select_b.png")
    # 結果
    rows = [("ケイデンBot","kaden",1),("Steve","rusty",2),("ミモザBot","mimosa",3),("タコまるBot","takomaru",4),("ゴウエンBot","gouen",5),
            ("ラスティBot","rusty",6),("ケイデンBot2","kaden",7),("ゴウエンBot2","gouen",8),("ミモザBot2","mimosa",9),("タコまるBot2","takomaru",10)]
    c = game_bg(2); result_screen(c, 2, 10, 5, 425, rows, 70); save(c, "result.png")
    rows2 = [("Steve","rusty",1)]+[(n,b,i+2) for i,(n,b,_) in enumerate(rows[2:]+rows[:1])]
    c = game_bg(2); result_screen(c, 1, 10, 8, 428, rows2, 70); save(c, "result_win.png")
    print("ok")

# U: GUIアイコン（32x32）。8倍で描いて縮小。縁は黒でなく隣の色相を暗くした紺・濃緑・濃琥珀。
import math, os
from PIL import Image, ImageDraw
OUT = "/home/claude/burosuta/src/main/resources/assets/brawl/textures/gui"
S = 8; N = 32
def canvas(): 
    im = Image.new("RGBA", (N*S, N*S), (0,0,0,0)); return im, ImageDraw.Draw(im)
def fin(im, name):
    im.resize((N, N), Image.LANCZOS).save(os.path.join(OUT, name + ".png"))
def P(pts): return [(x*S, y*S) for x, y in pts]
def poly(d, pts, fill, edge, w=1.4):
    d.polygon(P(pts), fill=fill, outline=None)
    d.line(P(pts + [pts[0]]), fill=edge, width=int(w*S), joint="curve")
def ell(d, box, fill, edge, w=1.4):
    x0,y0,x1,y1 = [v*S for v in box]
    d.ellipse([x0-w*S/2,y0-w*S/2,x1+w*S/2,y1+w*S/2], fill=edge)
    d.ellipse([x0+w*S/2,y0+w*S/2,x1-w*S/2,y1-w*S/2], fill=fill)
def rr(d, box, r, fill, edge, w=1.4):
    x0,y0,x1,y1 = [v*S for v in box]
    d.rounded_rectangle([x0,y0,x1,y1], r*S, fill=edge)
    d.rounded_rectangle([x0+w*S,y0+w*S,x1-w*S,y1-w*S], max(1,(r-w))*S, fill=fill)

NAVY=(26,37,71,255); WHITE=(250,250,255,255); GREY=(205,212,230,255)
# skull
im,d = canvas()
ell(d,(5,3,27,23),WHITE,NAVY,1.6)
rr(d,(10,19,22,28),2.4,WHITE,NAVY,1.6)
d.rectangle([11*S,19*S,21*S,21*S],fill=WHITE)
ell(d,(8.5,10,14.5,17),NAVY,NAVY,0.1); ell(d,(17.5,10,23.5,17),NAVY,NAVY,0.1)
poly(d,[(15,17.5),(17,17.5),(16,20)],NAVY,NAVY,0.2)
for x in (13.3,16,18.7): d.line(P([(x,23.5),(x,27)]),fill=NAVY,width=int(0.9*S))
fin(im,"skull")
# gas (緑の雲)
im,d = canvas()
G=(112,224,74,255); GE=(30,90,20,255); GL=(176,245,120,255)
for (cx,cy,r) in [(9,19,6),(16,13,8),(23,19,6.5),(16,22,7)]:
    ell(d,(cx-r,cy-r,cx+r,cy+r),G,GE,1.6)
for (cx,cy,r) in [(9,19,6),(16,13,8),(23,19,6.5),(16,22,7)]:
    ell(d,(cx-r+1.3,cy-r+1.3,cx+r-1.3,cy+r-1.3),G,G,0.01)
ell(d,(11,9,17,13),GL,GL,0.01)
d.ellipse([12*S,16*S,15.5*S,20*S],fill=GE); d.ellipse([18*S,16*S,21.5*S,20*S],fill=GE)
d.line(P([(13,24),(19,24)]),fill=GE,width=int(1.3*S))
fin(im,"gas")
# cube（緑の立方体）
im,d = canvas()
top=(120,232,96,255); left=(62,186,62,255); right=(38,140,52,255); ed=(18,84,30,255)
poly(d,[(16,3),(28,9.5),(16,16),(4,9.5)],top,ed)
poly(d,[(4,9.5),(16,16),(16,29),(4,22.5)],left,ed)
poly(d,[(16,16),(28,9.5),(28,22.5),(16,29)],right,ed)
d.line(P([(9,8.2),(15,5.2)]),fill=(214,255,190,255),width=int(1.1*S))
fin(im,"cube")
# trophy
im,d = canvas()
Y=(255,210,31,255); YE=(138,82,0,255); YL=(255,243,150,255); YD=(224,161,0,255)
for sx in (1,-1):
    pts=[(16+sx*8,7),(16+sx*13,7),(16+sx*13,11),(16+sx*9,16.5),(16+sx*7.5,15)]
    ell(d,(16+sx*13-3.5 if sx>0 else 16-13-0.0,6,16+sx*13+0 if sx>0 else 16-13+3.5,15),(0,0,0,0),YE,1.5)
poly(d,[(8,4),(24,4),(23,13),(20,19),(12,19),(9,13)],Y,YE,1.6)
d.polygon(P([(10,5.5),(13,5.5),(13.8,13),(11.5,10.5)]),fill=YL)
rr(d,(13.5,19,18.5,24),0.5,YD,YE,1.3)
rr(d,(9,24,23,29),1.8,Y,YE,1.5)
d.line(P([(11,26.5),(21,26.5)]),fill=YL,width=int(0.9*S))
fin(im,"trophy")
# click（マウスの絵。左ボタンが光る）
im,d = canvas()
rr(d,(8,3,24,29),7.5,(236,242,255,255),NAVY,1.6)
d.pieslice([8.8*S,3.8*S,23.2*S,18*S],180,270,fill=(255,200,40,255)) if False else None
poly(d,[(9.6,12.5),(9.6,9),(11,6),(15,4.6),(15,12.5)],(255,196,38,255),YE,0.8)
d.line(P([(16,4),(16,13.3)]),fill=NAVY,width=int(1.3*S)); d.line(P([(9,13.3),(23,13.3)]),fill=NAVY,width=int(1.3*S))
fin(im,"click")
# star（必殺技）
im,d = canvas()
pts=[]
for i in range(10):
    a=-math.pi/2+i*math.pi/5; r=14 if i%2==0 else 6.2
    pts.append((16+r*math.cos(a),16.8+r*math.sin(a)))
poly(d,pts,Y,YE,1.8)
d.polygon(P([(16,6),(18.3,13),(16,14),(13.8,13)]),fill=YL)
fin(im,"star")
# crown（勝利数）
im,d = canvas()
poly(d,[(4,24),(3,9),(10,15),(16,5),(22,15),(29,9),(28,24)],Y,YE,1.7)
d.rectangle([4*S,24*S,28*S,28*S],fill=YE); d.rectangle([5*S,24*S,27*S,27*S],fill=YD)
for (x,y) in [(3,8.5),(16,4.5),(29,8.5)]: ell(d,(x-1.9,y-1.9,x+1.9,y+1.9),YL,YE,1.2)
fin(im,"crown")
# 青いパネル用の菱形ダイヤ（区切り）
im,d = canvas()
poly(d,[(16,4),(26,16),(16,28),(6,16)],(80,170,255,255),(18,60,138,255),1.6)
fin(im,"gem")
print("ok")

import math, random
from PIL import Image, ImageDraw, ImageFont
S=2; W,H=640,360
img=Image.new("RGB",(W*S,H*S),(96,150,70))
d=ImageDraw.Draw(img)
random.seed(3)
for y in range(0,H*S,32):
    for x in range(0,W*S,32):
        c=(92+random.randint(0,12),146+random.randint(0,12),66+random.randint(0,10)); d.rectangle([x,y,x+31,y+31],fill=c)
d.rectangle([300*S,150*S,340*S,190*S],fill=(60,60,70))   # stand-in wall
F="/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"
def font(px): return ImageFont.truetype(F,int(px*S))
def R(x0,y0,x1,y1,c): d.rectangle([x0*S,y0*S,x1*S-1,y1*S-1],fill=c)
def text(s,cx,y,scale,col,edge,left=False):
    f=font(8*scale); w=d.textlength(s,font=f)
    x=cx*S if left else cx*S-w/2
    for ox in (-1,0,1):
        for oy in (-1,0,1):
            if ox or oy: d.text((x+ox*S*(scale/1 if scale<2 else 1)*0.9,y*S+oy*S*0.9),s,font=f,fill=edge)
    d.text((x,y*S),s,font=f,fill=col)
def bar(x,y,w,h,f,col,bg,edge):
    R(x-1,y-1,x+w+1,y+h+1,edge); R(x,y,x+w,y+h,bg)
    fw=round(w*f)
    if fw>0:
        R(x,y,x+fw,y+h,col)
        if h>=4: R(x,y,x+fw,y+1,tuple(min(255,c+40) for c in col))
def cube(cx,cy):
    for dy in range(-4,5):
        h=4-abs(dy); R(cx-h,cy+dy,cx+h+1,cy+dy+1,(92,42,142))
    for dy in range(-3,4):
        h=3-abs(dy); R(cx-h,cy+dy,cx+h+1,cy+dy+1,(184,100,242))
GREEN=(76,208,74);GB=(23,48,26);GE=(27,107,36);RED=(229,69,59);RB=(56,22,20);RE=(138,30,26)
# gas vignette
ov=Image.new("RGBA",img.size,(0,0,0,0)); od=ImageDraw.Draw(ov)
e=36
for i in range(e):
    t=1-i/e; a=int(0x90*t*t*0.9); c=(0x50,0xE0,0x30,a)
    od.rectangle([i*S,i*S,(W-i)*S,(i+1)*S],fill=c); od.rectangle([i*S,(H-i-1)*S,(W-i)*S,(H-i)*S],fill=c)
    od.rectangle([i*S,(i+1)*S,(i+1)*S,(H-i-1)*S],fill=c); od.rectangle([(W-i-1)*S,(i+1)*S,(W-i)*S,(H-i-1)*S],fill=c)
img.paste(ov,(0,0),ov); d=ImageDraw.Draw(img)
# overhead bars
def over(sx,sy,hp,mx,own,cubes):
    col,bg,ed=(GREEN,GB,GE) if own else (RED,RB,RE)
    bar(sx-20,sy-4,40,4,hp/mx,col,bg,ed); text(str(hp),sx,sy-15,1,(255,255,255),ed)
    if cubes:
        cube(sx-20-5-6-4,sy-3); text(str(cubes),sx-20-2-8,sy-8,1,(184,100,242),(92,42,142),left=True)
over(250,120,2800,5200,False,2); over(470,200,5000,5000,False,0)
d.rectangle([318*S,215*S,338*S,255*S],fill=(220,170,120)); over(328,205,3600,4400,True,1)
# hotbar stand-in
R(W/2-91,H-22,W/2+91,H,(30,30,30)); 
for i in range(9): R(W/2-91+i*20+1,H-21,W/2-91+i*20+19,H-1,(70,70,70))
# self hp
text("3600",W/2,H-63,2,(255,255,255),GE)
bar(W//2-60,H-44,120,7,0.82,GREEN,GB,GE)
for i,f in enumerate((1,1,0.45)):
    bar(W//2-60-3+i*42 if False else (W//2-(3*36+12)//2)+i*42,H-34,36,5,f,(244,168,28) if f>=1 else (201,138,24),(51,36,16),(138,90,12))
# super ring
def ring(cx,cy,o,i,frac,fill,edge,back):
    for dy in range(-o,o+1):
        for dx in range(-o,o+1):
            r2=dx*dx+dy*dy
            if i*i<=r2<=o*o:
                if r2>(o-1)**2 or r2<(i+1)**2: c=edge
                else:
                    a=math.atan2(dx,-dy); a+=2*math.pi if a<0 else 0
                    c=fill if a/(2*math.pi)<=frac else back
                R(cx+dx,cy+dy,cx+dx+1,cy+dy+1,c)
cx,cy=W-40,H-46
ring(cx,cy,26,23,2,(255,216,58),(255,216,58),(255,216,58))
ring(cx,cy,22,16,1,(255,216,58),(138,106,10),(42,36,18))
text("R",cx,cy-9,2,(255,255,255),(138,106,10)); text("必殺技",cx,cy+26,1,(255,216,58),(138,106,10))
# top
text("残り 7",W/2,6,2,(255,255,255),(20,20,20))
cube(W//2-14,30); text("3",W/2-6,26,1,(184,100,242),(92,42,142),left=True)
text("毒ガスまで 23秒",W/2,38,1,(109,224,74),(30,90,20))
# countdown / rank samples (small corner insets)
text("3",100,100,7,(255,216,58),(138,106,10)); text("#3",W-100,60,4,(255,255,255),(20,20,20))
d.text((8*S,H*S-24*S),"1280x720 (GUI x2) / sample: countdown 3 (left), rank #3 (right), gas vignette on",font=ImageFont.truetype(F,10*S) ,fill=(255,255,255))
img.save("/home/claude/burosuta/agent/preview/W6/hud.png")

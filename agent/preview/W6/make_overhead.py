from PIL import Image, ImageDraw, ImageFont
S=4; W,H=200,120
img=Image.new("RGB",(W*S,H*S),(98,150,72)); d=ImageDraw.Draw(img)
F="/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc"
def R(x0,y0,x1,y1,c): d.rectangle([x0*S,y0*S,x1*S-1,y1*S-1],fill=c)
def text(s,cx,y,sc,col,edge,left=False):
    f=ImageFont.truetype(F,int(8*sc*S)); w=d.textlength(s,font=f); x=cx*S if left else cx*S-w/2
    for ox in (-1,0,1):
        for oy in (-1,0,1):
            if ox or oy: d.text((x+ox*S,y*S+oy*S),s,font=f,fill=edge)
    d.text((x,y*S),s,font=f,fill=col)
def bar(x,y,w,h,f,col,bg,edge):
    R(x-1,y-1,x+w+1,y+h+1,edge); R(x,y,x+w,y+h,bg); fw=round(w*f)
    if fw>0:
        R(x,y,x+fw,y+h,col)
        if h>=4: R(x,y,x+fw,y+1,tuple(min(255,c+40) for c in col))
def cube(cx,cy,f,e):
    for dy in range(-4,5): h=4-abs(dy); R(cx-h,cy+dy,cx+h+1,cy+dy+1,e)
    for dy in range(-3,4): h=3-abs(dy); R(cx-h,cy+dy,cx+h+1,cy+dy+1,f)
def mob(sx,sy,name,hp,frac,own,cubes,ammo=None,skill=None):
    col,bg,ed=((76,208,74),(23,48,26),(27,107,36)) if own else ((229,69,59),(56,22,20),(138,30,26))
    bw=46; bar(sx-bw//2,sy,bw,5,frac,col,bg,ed)
    text(str(hp),sx,sy-13,1.25,(255,255,255),(20,20,20))
    text(name,sx,sy-24,1,(255,255,255) if own else (255,228,224),(20,20,20))
    if cubes:
        tw=d.textlength(str(cubes),font=ImageFont.truetype(F,8*S))/S
        text(str(cubes),sx-bw//2-4-tw,sy-2,1,(88,216,78),(20,20,20),left=True); cube(int(sx-bw//2-4-tw-6),sy+2,(88,216,78),(30,106,28))
    if ammo is not None:
        sw=(bw-4)//3; x0=sx-(3*sw+4)//2
        for k in range(3): bar(x0+k*(sw+2),sy+8,sw,3,max(0,min(1,ammo-k)),(244,168,28),(51,36,16),(138,90,12))
    if skill: text(skill,sx,sy+19,1.25,(255,216,58),(138,106,10))
mob(55,60,"ボット・ミナト",2800,0.6,False,2)
mob(145,60,"amaguri",3600,0.82,True,1,2.45,"スーパーシェル")
R(135,75,155,110,(220,170,120)); R(45,75,65,110,(180,120,90))
img.save("/home/claude/burosuta/agent/preview/W6/overhead.png")

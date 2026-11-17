import json, os
from PIL import Image
ROOT="/home/claude/burosuta"
JS=ROOT+"/src/client/resources/assets/brawl/models/brawler/kaden.json"
PNG=ROOT+"/src/client/resources/assets/brawl/textures/entity/brawler/kaden.png"
img=Image.new("RGBA",(128,128),(0,0,0,0))
def hx(s,a=255):
    s=s.lstrip('#'); return (int(s[0:2],16),int(s[2:4],16),int(s[4:6],16),a)
def P(x,y,c):
    if isinstance(c,str): c=hx(c)
    img.putpixel((x,y),c)
def R(x,y,w,h,c):
    for j in range(h):
        for i in range(w): P(x+i,y+j,c)
def faces(u,v,w,h,d):
    return dict(top=(u+d,v,w,d),bottom=(u+d+w,v,w,d),right=(u,v+d,d,h),front=(u+d,v+d,w,h),left=(u+d+w,v+d,d,h),back=(u+2*d+w,v+d,w,h))
def ramp_fill(r,ramp,shift=0,weights=None):
    x,y,w,h=r; n=len(ramp)
    for j in range(h):
        k=min(n-1,int(j*n/h)+shift) if weights is None else weights[min(j,len(weights)-1)]
        k=min(n-1,k)
        for i in range(w): P(x+i,y+j,ramp[k])
def box(f,ramp,top=None,bottom=None,shifts=(0,1,1)):
    ramp_fill(f['front'],ramp)
    ramp_fill(f['right'],ramp,shifts[0]); ramp_fill(f['left'],ramp,shifts[1]); ramp_fill(f['back'],ramp,shifts[2])
    R(*f['top'],top or ramp[0]); R(*f['bottom'],bottom or ramp[-1])
# palette
SL,SM,SD,SO="#eef3f9","#c3d0e0","#93a5c0","#667a9d"   # silver light/mid/dark/outline(blue shift)
CL,CM,CD,CO="#9bf0ff","#45c6ee","#2193c8","#14679a"   # cyan
GL,GM,GD="#ffe27a","#ffc030","#c98216"                  # gold
SK=["#ffe0c4","#f6c4a0","#dd9a7a"]
HR=["#f7aa60","#dd7a3c","#a9502c","#7c3622"]  # hair hi, mid, dark, deep
FACE=faces(0,0,13,12,11); HELM=faces(48,0,13,12,11)
BODY=faces(0,24,9,8,5); RA=faces(30,24,4,7,4); LA=faces(46,24,4,7,4)
RL=faces(0,38,4,7,4); LL=faces(16,38,4,7,4)
GUN=faces(64,24,3,4,8); MUZ=faces(88,24,4,4,2); TIP=faces(88,32,2,2,1); FIN=faces(88,36,1,2,5)
ANT=faces(100,32,1,2,1); BALL=faces(88,46,2,2,2)
TANK=faces(0,50,4,6,3); NOZ=faces(16,50,2,2,2)

# ---------- HEAD ----------
def hair_px(x,y,w,h,seed):
    for j in range(h):
        for i in range(w):
            k=1
            if (i+j*2+seed)%8 in (0,1) and j<h-2: k=0
            if j>=h-2: k=2
            P(x+i,y+j,HR[k])
f=FACE
# top
tx,ty,tw,th=f['top']; hair_px(tx,ty,tw,th,2)
for i in range(tw): P(tx+i,ty+th-1,HR[2])
for j in range(th-3): P(tx+6,ty+1+j,HR[2])   # parting swirl
P(tx+4,ty+2,HR[0]);P(tx+8,ty+2,HR[0]);P(tx+5,ty+1,HR[0]);P(tx+7,ty+1,HR[0])
R(*f['bottom'],SK[2])
# back
bx,by,bw,bh=f['back']; hair_px(bx,by,bw,bh,5)
R(bx,by+bh-3,bw,3,HR[2])
for i in range(0,bw,2): P(bx+i,by+bh-1,HR[3])
for i in range(1,bw,3): P(bx+i,by+bh-3,HR[1])
R(bx+3,by+bh-3,7,2,HR[2])
# sides
def side(fr,front_on_right):
    x,y,w,h=fr
    hair_px(x,y,w,h,3)
    # skin face area near front
    if front_on_right: sx=x+w-4
    else: sx=x
    for j in range(4,h):
        for i in range(4): P(sx+i,y+j,SK[0] if j<h-2 else SK[1])
    for i in range(4): P(sx+i,y+h-1,SK[2])
    # sideburn
    sb = sx+(3 if front_on_right else 0)
    # hairline step
    for j in range(0,4):
        for i in range(4): P(sx+i,y+j,HR[1] if (i+j)%4 else HR[0])
    for i in range(4): P(sx+i,y+3,HR[2])
    # ear
    ex = x+w-8 if front_on_right else x+4
    R(ex,y+6,3,3,SK[1]); P(ex+1,y+7,SK[2]); P(ex+(0 if front_on_right else 2),y+6,SK[0])
    # hair behind ear down
    bxs = x if front_on_right else x+w-6
    R(bxs,y+h-4,6,3,HR[2])
    for i in range(6): P(bxs+i,y+h-1,HR[3] if front_on_right else HR[3])
side(f['right'],True); side(f['left'],False)
# front face
fx,fy,fw,fh=f['front']
for j in range(fh):
    for i in range(fw):
        P(fx+i,fy+j,SK[0] if j<8 else (SK[1] if j<11 else SK[2]))
# hair + bangs
for i in range(fw):
    bang={0:5,1:4,2:3,3:3,4:2,5:3,6:5,7:4,8:2,9:2,10:2,11:4,12:5}[i]
    for j in range(bang):
        k=1
        if (i+j*2)%6==0 and j<bang-1: k=0
        if j==bang-1 and bang>2: k=2
        P(fx+i,fy+j,HR[k])
for i in range(fw): P(fx+i,fy,HR[2])
P(fx+0,fy+5,HR[3]);P(fx+12,fy+5,HR[3])
for i in (3,4): P(fx+i,fy+1,HR[0])
P(fx+7,fy+1,HR[0]);P(fx+6,fy+2,HR[0]);P(fx+11,fy+2,HR[0])
# brows
for i in (2,3,4): P(fx+i,fy+4,"#8a4326")
for i in (8,9,10): P(fx+i,fy+3,"#8a4326")
# eyes (3x4)  lash row, iris
def eye(ex,mirror):
    ey=fy+5
    R(ex,ey,3,1,"#3a2145")
    P(ex+(2 if mirror else 0)+ (1 if mirror else -1),ey,"#3a2145") if 0<=ex+(2 if mirror else 0)+(1 if mirror else -1)<fx+13 else None
    R(ex,ey+1,3,3,"#1f4aa8")
    R(ex,ey+2,3,2,"#3fa8ee")
    P(ex+1,ey+3,"#8ee6ff");P(ex+2 if not mirror else ex,ey+3,"#6fd2fa")
    P(ex+1,ey+2,"#12195c")            # pupil
    P(ex+1,ey+1,"#12195c")
    P(ex+(0 if not mirror else 2),ey+1,"#ffffff")   # highlight
    P(ex+(2 if not mirror else 0),ey+3,"#d5fbff")
eye(fx+2,False); eye(fx+8,True)
P(fx+1,fy+5,"#3a2145");P(fx+11,fy+5,"#3a2145")
# nose
P(fx+6,fy+8,SK[1])
# blush
for xx in (1,2,10,11): P(fx+xx,fy+9,"#ffa09c")
# smirk
R(fx+4,fy+9,5,1,"#7a2a3e")
P(fx+9,fy+9,"#7a2a3e");P(fx+10,fy+8,"#7a2a3e")
R(fx+5,fy+10,3,1,"#ffffff");P(fx+4,fy+10,"#7a2a3e") if 0 else None
P(fx+8,fy+10,"#e8b0a0")
# ---------- HELMET (glass) ----------
h=HELM
# front: transparent with rim
fx2,fy2,fw2,fh2=h['front']
R(fx2,fy2,fw2,fh2,(0,0,0,0))
for i in range(fw2):
    P(fx2+i,fy2,SL)
    P(fx2+i,fy2+fh2-1,SD)
for j in range(fh2):
    P(fx2,fy2+j,SM); P(fx2+fw2-1,fy2+j,SD)
P(fx2,fy2,SM);P(fx2+fw2-1,fy2,SM);P(fx2,fy2+fh2-1,SD)
# glints (on hair side of glass)
for k in range(2): P(fx2+10+k,fy2+2-k,"#ffffff") if 0 else None
P(fx2+11,fy2+2,"#ffffff");P(fx2+11,fy2+3,"#ffffff");P(fx2+12,fy2+1,"#ffffff") if 0 else None
P(fx2+2,fy2+10,"#d6fcff");P(fx2+3,fy2+9,"#d6fcff")
# top cap
t=h['top']; ramp_fill(t,[SL,SM],0)
for i in range(t[2]): P(t[0]+i,t[1],SL); P(t[0]+i,t[1]+t[3]-1,SM)
R(t[0]+3,t[1]+3,7,5,SM); R(t[0]+4,t[1]+4,5,3,SL)
P(t[0]+5,t[1]+4,"#ffffff");P(t[0]+6,t[1]+4,"#ffffff");P(t[0]+4,t[1]+5,"#ffffff")
R(t[0]+6,t[1]+5,1,1,CM)
R(*h['bottom'],SD)
# sides/back: glass with rim
for nm,sh in (('right',0),('left',1),('back',1)):
    x,y,w,hh=h[nm]
    R(x,y,w,hh,(0,0,0,0))
    for i in range(w): P(x+i,y,SL if sh==0 else SM); P(x+i,y+1,SM if sh==0 else SD)
    for j in range(hh): P(x,y+j,SD)
    for i in range(w): P(x+i,y+hh-1,SO); P(x+i,y+hh-2,SD); P(x+i,y+hh-3,SM if sh==0 else SD)
    # glass lines
    for k in range(3): P(x+2+k,y+5-k,"#ffffff") if nm=='right' else None
for nm in ('right','left','back'):
    x,y,w,hh=h[nm]
    P(x+w-1,y+2,SD);P(x+w-1,y+3,SD) if False else None
# right face glass edge columns
for nm in ('right','left','back','front'):
    x,y,w,hh=h[nm]
    for j in range(hh): P(x+w-1,y+j,SD)

for nm in ('front','right','left','back'):
    x,y,w,hh=HELM[nm]
    for (cx,cy) in ((x,y),(x+w-1,y)):
        P(cx,cy,(0,0,0,0))
    for j in range(2,hh-2):
        P(x,y+j,SM); P(x+w-1,y+j,SM)
# ---------- BODY ----------
box(BODY,[SL,SM,SD])
fx,fy,fw,fh=BODY['front']
R(fx+2,fy+1,5,3,CD); R(fx+2,fy+1,5,1,CM); R(fx+3,fy+2,3,1,CL)   # chest panel
P(fx+3,fy+3,GM);P(fx+5,fy+3,CL);P(fx+4,fy+3,SO)
R(fx,fy+5,fw,1,CD); R(fx,fy+6,fw,2,CO)       # belt
R(fx+3,fy+5,3,3,GM); P(fx+4,fy+6,GD); P(fx+3,fy+5,GL);P(fx+4,fy+5,GL)
for i in range(fw): P(fx+i,fy+4,SD)
P(fx,fy,SL);P(fx+fw-1,fy,SM)
P(fx+1,fy+3,SO) ; P(fx+7,fy+3,SO)
for nm in ('right','left'):
    x,y,w,hh=BODY[nm]
    R(x,y+5,w,1,CD); R(x,y+6,w,2,CO); P(x+1,y+2,SO);P(x+3,y+3,SO)
bx,by,bw,bh=BODY['back']
R(bx,by+5,bw,1,CD); R(bx,by+6,bw,2,CO)
for i in (1,2,6,7): R(bx+i,by,1,5,SD)   # straps
R(bx+4,by+1,1,3,SO)
# ---------- ARMS ----------
def arm(F,outer,patch):
    box(F,[SL,SM,SD])
    for nm in ('front','back','right','left'):
        x,y,w,hh=F[nm]
        R(x,y+4,w,1,CD); R(x,y+5,w,2,"#eaf8ff"); 
        for i in range(w): P(x+i,y+6,"#aebfd4")
        P(x,y+4,CO);P(x+w-1,y+4,CO) if False else None
        P(x+1,y+2,SD)
    R(*F['top'],SL)
    R(*F['bottom'],"#aebfd4")
    x,y,w,hh=F[outer]
    if patch:
        R(x,y,w,4,GD); R(x+1,y+1,2,2,GM)
        # star 4x4 pattern gold on dark
        pat=["0110","1111","0110","1001"] if False else ["0100","1110","0100","1010"] 
        R(x,y,w,4,"#2f3f7a")
        star=[(1,0),(0,1),(1,1),(2,1),(3,1),(1,2),(2,2),(0,3),(3,3)]
        for (sx,sy) in [(1,0),(1,1),(0,1),(2,1),(1,2),(0,2),(2,2),(0,3),(2,3)]: pass
        S=["..y.","yyyy",".yy.","y..y"] if False else [".yy.","yyyy",".yy.","y..y"]
        for sy,row in enumerate(S):
            for sx,ch in enumerate(row):
                if ch=='y': P(x+sx,y+sy,GM if sy<2 else GD)
        P(x+1,y+1,GL)
        for i in range(w): P(x+i,y+4,"#1b2756")
    else:
        R(x,y,w,1,CM); R(x,y+1,w,1,CD)
arm(RA,'right',False); arm(LA,'left',True)
# ---------- LEGS ----------
def leg(F):
    box(F,[SL,SM,SD],shifts=(0,1,1))
    for nm in ('front','back','right','left'):
        x,y,w,hh=F[nm]
        R(x,y+4,w,1,CL); R(x,y+5,w,2,CM) if nm in('front','right') else R(x,y+5,w,2,CD)
        R(x,y+6,w,1,CO)
        R(x,y+2,w,1,SD) if nm in ('front','right') else None
    x,y,w,hh=F['front']
    R(x+1,y+4,w-2,1,"#ffffff"); P(x+1,y+5,CL)
    R(*F['bottom'],CO)
leg(RL); leg(LL)
# ---------- GUN ----------
IN=["#5b79d6","#3d56a8","#27387a"]; INO="#1a2557"
g=GUN
box(g,IN,shifts=(0,1,1))
for nm in ('right','left'):
    x,y,w,hh=g[nm]
    R(x,y,w,1,"#7e98e8"); R(x+1,y+1,w-2,2,CM); R(x+2,y+1,w-4,1,"#d6fcff"); R(x,y+3,w,1,INO)
    P(x,y+1,IN[1]);P(x+w-1,y+1,IN[1]);P(x,y+2,IN[2]);P(x+w-1,y+2,IN[2])
    P(x+1,y+3,"#ffc030");P(x+w-2,y+3,"#ffc030")
x,y,w,hh=g['front']; R(x,y,w,hh,IN[1]); R(x+1,y+1,1,2,CL); P(x,y,IN[0]);P(x+2,y+3,INO)
x,y,w,hh=g['back']; R(x,y,w,hh,IN[2]); R(x+1,y+1,1,2,CM)
x,y,w,hh=g['top']; R(x,y,w,hh,IN[0]); R(x+2,y+3,4,2,IN[1]); R(x+1,y,6,1,"#9db3f2")
R(*g['bottom'],INO)
mz=MUZ
for nm in ('right','left','back','top','bottom'): R(*mz[nm],GD)
R(*mz['top'],GL)
x,y,w,hh=mz['front']; R(x,y,w,hh,GM); R(x+1,y+1,2,2,"#ffffff"); P(x,y,GL);P(x+1,y,GL);P(x+3,y+3,GD);P(x+3,y+2,GD);P(x,y+3,GD);P(x+3,y,GD)
for nm in ('right','left'):
    x,y,w,hh=mz[nm]; R(x,y,w,hh,GM); R(x,y,w,1,GL); R(x,y+3,w,1,GD); P(x+1,y+1,GD) 
t=TIP
for nm in t: R(*t[nm],CL)
R(*t['front'],"#ffffff")
fn=FIN
for nm in fn: R(*fn[nm],CM)
R(*fn['top'],CL); R(*fn['front'],CL)
for nm in ('right','left'):
    x,y,w,hh=fn[nm]; R(x,y,w,hh,CM); R(x,y,w,1,CL); R(x,y+1,w,1,CD); P(x+3,y+1,CL)
ant=ANT
for nm in ant: R(*ant[nm],SM)
R(*ant['top'],SL); R(*ant['front'],SL); R(*ant['bottom'],SD)
bl=BALL
for nm in bl: R(*bl[nm],CM)
R(*bl['top'],"#ffffff"); R(*bl['front'],CL); R(*bl['bottom'],CD)
for nm in ('right','left','back'): 
    x,y,w,hh=bl[nm]; P(x,y,CL); P(x+1,y+1,CD)
# ---------- JETPACK ----------
TK=["#6fd6f5","#2aa0d6","#1a6fa8"]
tk=TANK
box(tk,TK,shifts=(0,1,1))
x,y,w,hh=tk['front']
R(x,y,w,1,SL); R(x,y+2,w,1,SM); R(x,y+5,w,1,CO)
for j in (3,4): P(x+1,y+j,"#e6fdff")
for nm in ('right','left','back'):
    x,y,w,hh=tk[nm]; R(x,y,w,1,SM); R(x,y+2,w,1,SD); R(x,y+5,w,1,CO)
    P(x+1,y+3,CL)
R(*tk['top'],SL); R(*tk['bottom'],SO)
nz=NOZ
for nm in nz: R(*nz[nm],SD)
R(*nz['bottom'],"#ffb347"); R(*nz['top'],SM)
for nm in ('front','back','right','left'):
    x,y,w,hh=nz[nm]; R(x,y,w,1,SM); R(x,y+1,w,1,"#ff9a3c")
img.save(PNG)
img.save(os.path.dirname(__file__)+"/kaden_tex.png")

# ---------- MODEL ----------
def cube(fr,size,uv,infl=0): 
    c={"from":fr,"size":size,"uv":uv}
    if infl: c["inflate"]=infl
    return c
bones=[]
def bone(name,parent,pivot,cubes,rot=(0,0,0)):
    bones.append({"name":name,"parent":parent,"pivot":list(pivot),"rotation":list(rot),"cubes":cubes})
bone("body",None,[0,7,0],[cube([-4.5,7,-2.5],[9,8,5],[0,24])])
bone("head",None,[0,15,0],[cube([-6.5,15,-5.5],[13,12,11],[0,0])])
bone("helmet","head",[0,15,0],[cube([-6.5,15,-5.5],[13,12,11],[48,0],0.9)])
bone("antenna","head",[0,28,0],[cube([-0.5,27.9,-0.5],[1,2,1],[100,32]),cube([-1,29.5,-1],[2,2,2],[88,46])])
bone("right_arm",None,[6.5,14,0],[cube([4.5,7,-2],[4,7,4],[30,24])])
bone("left_arm",None,[-6.5,14,0],[cube([-8.5,7,-2],[4,7,4],[46,24])])
bone("right_leg",None,[2.3,7,0],[cube([0.3,0,-2],[4,7,4],[0,38])])
bone("left_leg",None,[-2.3,7,0],[cube([-4.3,0,-2],[4,7,4],[16,38])])
def gun(sx):
    cx=6.5*sx
    return [cube([cx-1.5,8,-9],[3,4,8],[64,24]),
            cube([cx-2,8,-11],[4,4,2],[88,24]),
            cube([cx-1,9,-12],[2,2,1],[88,32]),
            cube([cx-0.5,12,-8],[1,2,5],[88,36])]
bone("weapon","right_arm",[6.5,8.5,-1],gun(1))
bone("left_gun","left_arm",[-6.5,8.5,-1],gun(-1))
bone("jetpack","body",[0,10,2.5],[cube([-4.5,8,2.5],[4,6,3],[0,50]),cube([0.5,8,2.5],[4,6,3],[0,50]),
     cube([-3.5,6,3],[2,2,2],[16,50]),cube([1.5,6,3],[2,2,2],[16,50])])
json.dump({"texture_size":[128,128],"bones":bones},open(JS,"w"),indent=1)

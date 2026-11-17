import json, random, os
from PIL import Image
ROOT="/home/claude/burosuta"
OUTJ=ROOT+"/src/client/resources/assets/brawl/models/brawler/gouen.json"
OUTP=ROOT+"/src/client/resources/assets/brawl/textures/entity/brawler/gouen.png"
TS=128
# ---------- palettes
STONE=[(58,64,70),(84,92,92),(112,121,114),(142,151,138),(170,178,158)]
EDGE=(40,46,54)
MOSS=[(36,64,44),(56,98,50),(88,136,60),(128,172,78)]
MOSSEDGE=(30,52,40)
AMB=[(150,70,16),(226,128,24),(255,184,52),(255,232,150)]
# ---------- cubes: (bone, name, from, size, kind)
C=[]
def cube(bone,name,f,s,kind="stone"): C.append(dict(bone=bone,name=name,f=f,s=s,kind=kind))
cube("body","belly",[-4,6,-3],[8,6,6])
cube("body","chest",[-8,12,-4],[16,8,8])
cube("body","backmoss",[-5,13,4],[10,6,2],"moss")
cube("body","rsp_base",[7,20,-2],[5,2,4])
cube("body","rsp_tip",[8.5,22,-1],[2,3,2])
cube("body","lsp_base",[-12,20,-2],[5,2,4])
cube("body","lsp_tip",[-10.5,22,-1],[2,3,2])
cube("body","bstem1",[-3,19,4],[1,2,1],"stem")
cube("body","bbloom1",[-4,21,3.5],[3,1,3],"flower")
cube("body","bstem2",[2,17,4],[1,2,1],"stem")
cube("body","bbloom2",[1,19,3.5],[3,1,3],"flower2")
cube("head","head",[-5.5,18,-5],[11,9,9])
cube("head","head2",[-6,19,-4],[12,6,8])
cube("head","brow",[-5.5,23,-6],[11,2,1],"brow")
cube("head","hmoss",[-4,27,-3],[8,2,6],"moss")
cube("head","hstem1",[-2,29,-1],[1,2,1],"stem")
cube("head","hbloom1",[-3,31,-1.5],[3,1,3],"flower")
cube("head","hstem2",[2,29,1],[1,1,1],"stem")
cube("head","hbloom2",[1,30,0.5],[3,1,3],"flower2")
cube("right_arm","rarm",[8,8,-2.5],[5,12,5])
cube("right_arm","rfist",[7,2,-3.5],[7,6,7])
cube("left_arm","larm",[-13,8,-2.5],[5,12,5])
cube("left_arm","lfist",[-14,2,-3.5],[7,6,7])
cube("right_leg","rleg",[0.5,0,-2.5],[5,6,5])
cube("right_leg","rtoe",[0.5,0,-3.5],[5,2,1])
cube("left_leg","lleg",[-5.5,0,-2.5],[5,6,5])
cube("left_leg","ltoe",[-5.5,0,-3.5],[5,2,1])
cube("weapon","gcuff",[6.5,7,-4],[8,4,8],"gauntlet")
cube("weapon","gs1",[7,3.5,-4.5],[2,2,1],"gauntlet")
cube("weapon","gs2",[9.5,3.5,-4.5],[2,2,1],"gauntlet")
cube("weapon","gs3",[12,3.5,-4.5],[2,2,1],"gauntlet")

BONES=dict(
 body=dict(pivot=[0,12,0]),
 head=dict(pivot=[0,19,0]),
 right_arm=dict(pivot=[10.5,19,0]),
 left_arm=dict(pivot=[-10.5,19,0]),
 right_leg=dict(pivot=[3,6,0]),
 left_leg=dict(pivot=[-3,6,0]),
 weapon=dict(pivot=[10.5,5,0],parent="right_arm"))
# ---------- pack UVs
def rsz(c):
    w,h,d=c["s"]; return int(2*d+2*w+0.999), int(d+h+0.999)
import math
def dims(c):
    w,h,d=[math.ceil(x) for x in c["s"]]; return w,h,d
def pack():
    items=sorted(C,key=lambda c:-(2*dims(c)[2]+2*dims(c)[0]))
    x=y=0; rowh=0
    # shelf by height
    items=sorted(C,key=lambda c:-(dims(c)[2]+dims(c)[1]))
    for c in items:
        w,h,d=dims(c); W=2*d+2*w; H=d+h
        if x+W>TS: x=0; y+=rowh; rowh=0
        c["uv"]=[x,y]; x+=W; rowh=max(rowh,H)
    assert y+rowh<=TS,(y+rowh)
pack()
img=Image.new("RGBA",(TS,TS),(0,0,0,0))
px=img.load()
def hsh(a,b,c=0):
    return (a*73856093 ^ b*19349663 ^ c*83492791)&0xffff
def rnd(a,b,c=0): return hsh(a,b,c)%100
def put(x,y,col):
    if 0<=x<TS and 0<=y<TS: px[x,y]=col+(255,) if len(col)==3 else col
def faces(c):
    w,h,d=dims(c); u,v=c["uv"]
    return {"top":(u+d,v,w,d),"bottom":(u+d+w,v,w,d),"right":(u,v+d,d,h),"front":(u+d,v+d,w,h),
            "left":(u+d+w,v+d,d,h),"back":(u+2*d+w,v+d,w,h)}
SEED={}
def stone_px(c,face,i,j,w,h,seed):
    # i,j pixel inside face (j=0 top row)
    if face=="top": lvl=3.2
    elif face=="bottom": lvl=0.4
    else:
        t=j/max(1,h-1); lvl=2.9-1.9*t
        if face in("right",): lvl+=0.3   # light from +x
        if face in("left",): lvl-=0.35
        if face=="back": lvl-=0.4
    # block seams
    sh=(hsh(seed,j//4,7)%5)
    if face not in("top","bottom"):
        if (j%4)==3 and w>3: lvl-=0.9
        elif (j%4)==0 and j>0: lvl+=0.35
        if ((i+sh)%6)==0 and (j%4)!=3: lvl-=0.55
    n=rnd(i+seed,j,seed)
    if n<9: lvl-=1
    elif n>92: lvl+=1
    # edges
    if i==0 and face not in("top","bottom"): lvl+=0.5
    if j==0: lvl+=0.5
    if i==w-1: lvl-=0.5
    if j==h-1: lvl-=0.8
    return STONE[max(0,min(4,int(round(lvl))))]
def moss_px(i,j,w,h,seed,edge=True):
    lvl=1.2+1.6*(1-j/max(1,h-1))
    n=rnd(i,j,seed+3)
    if n<22: lvl-=1
    elif n>78: lvl+=1
    if (i*3+j*2+seed)%7==0: lvl+=1
    return MOSS[max(0,min(3,int(round(lvl))))]
def moss_cover(face,i,j,w,h,seed,depth):
    # returns True if pixel is mossy at top of face
    if face=="top": return rnd(i,j,seed+9)<depth*30+10
    if face=="bottom": return False
    if rnd(i//2,2,seed+8)<45: return False
    d=1+rnd(i,0,seed+5)%depth
    return j<d
def paint(c):
    seed=hsh(len(c["name"]),sum(map(ord,c["name"])))%97+1
    for face,(x0,y0,w,h) in faces(c).items():
        k=c["kind"]
        for j in range(h):
            for i in range(w):
                col=None
                if k in("stone","brow"):
                    col=stone_px(c,face,i,j,w,h,seed)
                    mdepth={"head":1,"head2":1,"chest":2,"belly":1,"rarm":1,"larm":2,"rsp_base":1,"lsp_base":2,"rleg":0,"lleg":1,"rfist":0,"lfist":1,"rsp_tip":0,"lsp_tip":0,"brow":0}.get(c["name"],0)
                    if mdepth and face!="bottom":
                        if c["name"] in("chest","belly") and face in("front","back","left","right") and not moss_cover_ok(c,face,i,j,w,h): pass
                        elif moss_cover(face,i,j,w,h,seed,mdepth) and not(c["name"] in("head","brow") and face=="front"):
                            col=moss_px(i,j,w,h,seed)
                elif k=="moss":
                    col=moss_px(i,j,w,h,seed)
                    if face=="top" and (i+j)%3==0: col=MOSS[3]
                    if j==h-1 and face!="top": col=MOSS[0]
                elif k=="stem":
                    col=MOSS[1] if face!="bottom" else MOSS[0]
                    if i==0 and w>1: col=MOSS[2]
                elif k in("flower","flower2"):
                    pet=(250,236,226) if k=="flower" else (255,170,150)
                    pet2=(214,170,190) if k=="flower" else (214,106,106)
                    if face=="top":
                        col=(255,200,60) if (i==w//2 and j==h//2) else pet
                    elif face=="bottom": col=pet2
                    else: col=pet if j==0 else pet2
                elif k=="gauntlet":
                    col=stone_px(c,face,i,j,w,h,seed+11)
                    # darker, bluer iron-stone tone
                    col=tuple(max(0,int(a*0.93)) for a in col)
                    col=(col[0]-2,col[1],min(255,col[2]+6))
                px[x0+i,y0+j]=col+(255,)
def moss_cover_ok(c,face,i,j,w,h): return True
for c in C: paint(c)
# outlines: darker hue-shifted edge on bottom-row / right col of side faces already shaded by ramp.
def F(name,face):
    c=[c for c in C if c["name"]==name][0]; return faces(c)[face]
def setp(r,i,j,col): px[r[0]+i,r[1]+j]=col+(255,)
# ---------- head front: face with amber eyes
r=F("head","front"); w,h=r[2],r[3]
for j in range(h):
    for i in range(w):
        col=stone_px(None,"front",i,j,w,h,5)
        if j>=h-2: col=STONE[max(0,min(4,1 if j==h-2 else 0))]  # buried into chest: dark
        if j<2 and rnd(i,j,3)<60: pass
        setp(r,i,j,col)
# slight moss on top row edges
for i in range(w):
    if rnd(i,0,44)<55: setp(r,i,0,moss_px(i,0,w,h,4))
    if rnd(i,1,45)<28: setp(r,i,1,moss_px(i,1,w,h,4))
# eyes: left eye (image left = char right? front face seen from front: image left = char's left(-X)? front view looks at -Z toward viewer;
# image x increases toward +X as per format (front face u+d..u+d+w = x -..+). keep symmetric anyway.
def eye(x0,y0,slant):
    # sockets (dark ring)
    for dj in range(-1,4):
        for di in range(-1,4):
            ii,jj=x0+di,y0+dj
            if 0<=ii<w and 0<=jj<h:
                d=abs(di-1)+abs(dj-1)
                if d<=2: setp(r,ii,jj,STONE[0] if d==2 else (30,34,42))
    pat=[[1,2,2],[2,3,2],[1,2,1]]
    for dj in range(3):
        for di in range(3):
            v=pat[dj][di]
            col=AMB[v] if v<3 else AMB[3]
            setp(r,x0+di,y0+dj,col)
    # angry slant: inner top corner dark
    ci=x0+2 if slant>0 else x0
    setp(r,ci,y0,(60,34,20))
    # glow halo pixels
    for (di,dj) in((-1,1),(3,1)):
        ii=x0+di
        if 0<=ii<w: setp(r,ii,y0+dj,(130,96,60))
eye(1,4,-1)   # image-left eye: inner corner is right side
eye(7,4,1)
# nose-bridge crack & mouth-less jaw cracks
for (i,j) in((5,3),(5,4),(4,5)): setp(r,i,j,STONE[0])
for (i,j) in((2,7),(3,7),(7,7),(8,8)): setp(r,i,j,STONE[0])
# brow front: dark underside
for nm,fc in (("brow","front"),("brow","bottom")):
    rr=F(nm,fc)
    for j in range(rr[3]):
        for i in range(rr[2]):
            base=stone_px(None,fc,i,j,rr[2],rr[3],21)
            if fc=="front" and j==rr[3]-1: base=STONE[0]
            if fc=="bottom": base=(34,38,46)
            px[rr[0]+i,rr[1]+j]=base+(255,)
# ---------- chest front: rune + cracks
r=F("chest","front"); w,h=r[2],r[3]
for j in range(h):
    for i in range(w):
        col=stone_px(None,"front",i,j,w,h,9)
        if j<3 and rnd(i//2,1,31)<(60 if j<2 else 25) and (i<7 or i>11): col=moss_px(i,j,w,h,9)
        setp(r,i,j,col)
rune=[(8,3),(7,4),(9,4),(8,4),(8,5),(7,6),(9,6),(6,5),(10,5)]
for (i,j) in rune: setp(r,i,j,AMB[1])
setp(r,8,4,AMB[3]); setp(r,8,3,AMB[2]); setp(r,8,5,AMB[2])
for (i,j) in((8,2),(7,3),(9,3),(7,5),(9,5),(8,6)): 
    setp(r,i,j,(120,100,70))
# cracks
for (i,j) in((2,4),(3,5),(3,6),(4,7),(12,3),(13,4),(13,5),(12,6)): setp(r,i,j,STONE[0])
# belly front: seam + moss
r=F("belly","front"); w,h=r[2],r[3]
for (i,j) in((3,2),(4,3),(4,4),(5,5)): setp(r,i,j,STONE[0])
# fists: knuckle shading on front
for nm in("rfist","lfist"):
    r=F(nm,"front"); w,h=r[2],r[3]
    for i in range(w): setp(r,i,h-1,STONE[0]); setp(r,i,h-2,STONE[1])
    for (i,j) in((1,2),(3,2),(5,2)): setp(r,i,j,STONE[0])
# gauntlet studs: highlights
for nm in("gs1","gs2","gs3"):
    for fc in("front",):
        r=F(nm,fc)
        setp(r,0,0,(190,200,190)); setp(r,1,1,(70,80,86)); setp(r,1,0,(150,160,152)); setp(r,0,1,(110,122,118))
# cuff: rim lines
for fc in("front","left","right","back"):
    r=F("gcuff",fc)
    for i in range(r[2]):
        setp(r,i,0,(170,180,172)); setp(r,i,r[3]-1,(38,44,56))
    for i in range(1,r[2],3): setp(r,i,1,(54,62,74))
# amber gem on cuff front
r=F("gcuff","front"); setp(r,3,1,AMB[1]); setp(r,4,1,AMB[2])
img.save(OUTP)
# ---------- json
bones=[]
for name in ["body","head","right_arm","left_arm","right_leg","left_leg","weapon"]:
    b=dict(name=name,parent=BONES[name].get("parent"),pivot=BONES[name]["pivot"],rotation=[0,0,0],cubes=[])
    for c in C:
        if c["bone"]==name:
            b["cubes"].append(dict({"from":c["f"],"size":c["s"],"uv":c["uv"]}))
    bones.append(b)
json.dump(dict(texture_size=[TS,TS],bones=bones),open(OUTJ,"w"),indent=1)
print("ok")

import json, random
from PIL import Image
ROOT='/home/claude/burosuta/'
MJ=ROOT+'src/client/resources/assets/brawl/models/brawler/rusty.json'
TP=ROOT+'src/client/resources/assets/brawl/textures/entity/brawler/rusty.png'
random.seed(7)
# ---------- model
def cube(f,s,uv,inf=0): 
    d={"from":f,"size":s,"uv":uv}
    if inf: d["inflate"]=inf
    return d
def bone(n,p,pv,cubes,rot=(0,0,0)): return {"name":n,"parent":p,"pivot":pv,"rotation":list(rot),"cubes":cubes}
UV=dict(head=(0,0),helmet=(50,0),body=(0,24),rarm=(30,24),larm=(44,24),rleg=(0,40),lleg=(18,40),
 bA=(0,56),bB=(28,56),muz=(56,56),rec=(76,56),hand=(96,56),stock=(104,72),
 hairR=(0,80),hairL=(12,80),lamp=(24,80),lensR=(36,80),lensL=(48,80),bridge=(60,80),brim=(66,80),ridge=(0,92),pack=(30,92))
bones=[
 bone("body",None,[0,7,0],[cube([-4.5,7,-2.5],[9,8,5],UV['body'])]),
 bone("head",None,[0,15,0],[cube([-6.5,15,-5.5],[13,11,11],UV['head'])]),
 bone("right_arm",None,[6,14,0],[cube([4.5,7,-1.5],[3,7,3],UV['rarm'])]),
 bone("left_arm",None,[-6,14,0],[cube([-7.5,7,-1.5],[3,7,3],UV['larm'])]),
 bone("right_leg",None,[2.5,7,0],[cube([0.5,0,-2],[4,7,4],UV['rleg'])]),
 bone("left_leg",None,[-2.5,7,0],[cube([-4.5,0,-2],[4,7,4],UV['lleg'])]),
 bone("weapon","right_arm",[6,9,-2],[
   cube([3,7.5,-15],[3,4,10],UV['bA']),cube([6,7.5,-15],[3,4,10],UV['bB']),
   cube([2.5,7,-16],[7,5,2],UV['muz']),cube([3,6,-5],[6,7,3],UV['rec']),
   cube([3,5,-11],[6,2,6],UV['hand']),cube([4.5,6.5,1.5],[3,5,4],UV['stock'])]),
 bone("helmet","head",[0,24,0],[cube([-7,24,-6.5],[14,6,13],UV['helmet']),cube([-1,30,-6],[2,1,12],UV['ridge']),
   cube([-7,24,-8.5],[14,1,2],UV['brim']),cube([-1.5,27,-8.5],[3,3,2],UV['lamp'])]),
 bone("goggles","head",[0,26,-7],[cube([-6,25,-7.5],[4,3,1],UV['lensR']),cube([2,25,-7.5],[4,3,1],UV['lensL']),cube([-2,26,-7.5],[4,1,1],UV['bridge'])]),
 bone("hair_r","head",[6.5,24,0],[cube([6.5,17,-1],[2,7,3],UV['hairR'])]),
 bone("hair_l","head",[-6.5,24,0],[cube([-8.5,17,-1],[2,7,3],UV['hairL'])]),
 bone("pack","body",[0,15,2.5],[cube([-3,8,2.5],[6,5,3],UV['pack'])]),
]
json.dump({"texture_size":[128,128],"bones":bones},open(MJ,'w'),indent=1)
# ---------- texture
img=Image.new('RGBA',(128,128),(0,0,0,0)); px=img.load()
def rects(u,v,w,h,d):
    return {'top':(u+d,v,w,d),'bot':(u+d+w,v,w,d),'R':(u,v+d,d,h),'F':(u+d,v+d,w,h),'L':(u+d+w,v+d,d,h),'B':(u+2*d+w,v+d,w,h)}
def paint(uvk,size,fn,faces=None):
    w,h,d=size; r=rects(*UV[uvk],w,h,d)
    for k,(x,y,fw,fh) in r.items():
        if faces and k not in faces: continue
        for j in range(fh):
            for i in range(fw):
                c=fn(k,i,j,fw,fh)
                if c: px[x+i,y+j]=c+(255,) if len(c)==3 else c
def dk(c,f=.72): return (int(c[0]*f),int(c[1]*f*.93),int(c[2]*f*.85))   # darker + hue shift to warm
def lt(c,f=1.15): return (min(255,int(c[0]*f+6)),min(255,int(c[1]*f+4)),min(255,int(c[2]*f)))
# palette
O1=(236,142,66);O2=(200,106,48);O3=(156,76,38);O4=(112,54,32)   # rust orange ramp
Y1=(255,224,86);Y2=(238,184,48);Y3=(190,130,34);Y4=(124,80,30)  # helmet yellow
S1=(255,214,170);S2=(240,180,138);S3=(206,140,108)
H1=(168,90,52);H2=(122,60,40);H3=(84,40,32)
G1=(178,130,84);G2=(134,94,60);G3=(94,64,46)
B1=(104,70,52);B2=(76,50,40);B3=(52,34,30)
M1=(150,150,158);M2=(104,104,114);M3=(66,66,78)   # scrap metal
def ramp(r,rows,row): 
    t=row/max(1,rows-1); return r[0] if t<.34 else (r[1] if t<.7 else r[2])
def noise(c,p=.06,key=None):
    return c
def speck(x,y,k=0): return (x*7+y*13+k*5)%11
# ----- head
EYEW=(255,255,255)
def head(k,i,j,w,h):
    if k=='top': return H2 if (i+j)%3 else H1
    if k=='bot': return S3
    if k=='B':
        c=ramp((H1,H2,H3),h,j)
        if (i*3+j*5)%7==0: c=lt(c,1.2)
        if j==7 and i in(5,6,7): c=(200,90,40)   # hair tie
        if j>=8 and i in (0,12): c=H3
        return c
    if k in('R','L'):
        front=(w-1-i) if k=='R' else i
        if j<=2: return H2 if j>0 else H1
        if front<=1: return S1 if j<8 else S2
        if 4<=front<=5 and 5<=j<=7: return S2 if not(front==4 and j==6) else S3   # ear
        c=ramp((H1,H2,H3),h,j)
        if (i*5+j*3)%9==0: c=lt(c,1.2)
        if j>=9: c=H3
        return c
    # front
    if j<=1: return H1
    if j==2:  # bangs
        if i in (0,1,5,6,7,11,12): return H2
        if i in(3,9): return H1
        return S1
    skin=S1 if j<8 else (S2 if j<10 else S3)
    # brows
    if j==3 and i in(2,3,4): return H3 if i<4 else H2
    if j==3 and i in(8,9,10): return H3 if i>8 else H2
    if j==2 and i in(4,8): return H3
    for c0 in (2,8):
        if c0<=i<=c0+2 and 5<=j<=8:
            ci=i-c0;rj=j-5
            if rj==0: return B3
            if c0==8: ci=2-ci        # mirror: white outside, iris toward centre
            if rj==1: return (150,82,44) if ci==2 else (255,255,255)
            if rj==2: return (255,255,255) if ci==0 else (46,26,30)
            return (206,118,60) if ci else (150,84,52)
    # eye shine already; blush & freckles
    if j==9 and (i in(1,2,3) or i in(9,10,11)):
        return (240,150,128) if i in(2,10) else ((196,110,84) if (i+j)%2 else S2)
    if j==7 and i in(0,12): return (204,124,92)
    if j==8 and i in(1,3,9,11): return (204,124,92)
    if j==10 and i in (5,6,7):  # grin
        return (255,250,240) if i==6 else (142,48,50)
    if j==9 and i in (4,8): return (170,60,56)
    if j==9 and i in (5,6,7): return (142,48,50) if i!=6 else (255,250,240)
    if j==8 and i==6: return S3   # nose
    if i==0 or i==12: return dk(skin,.9)
    return skin
paint('head',(13,11,11),head)
# ----- helmet
def helmet(k,i,j,w,h):
    if k=='top':
        c=Y1 if (i>2 and i<w-3 and j>1 and j<h-2) else Y2
        if i in(6,7) : c=Y2
        return c
    if k=='bot': return Y4
    if j==5:  # rim: hazard stripes on front, dark elsewhere
        if k=='F': return Y4 if (i//2)%2==0 else Y2
        return Y3
    c=ramp((Y1,Y2,Y3),h-1,j)
    if j==0: c=lt(Y1,1.05)
    if k=='F':
        if j==2 and i in (0,13): return Y4
        if i in(1,12) and j in(1,3): return M2 if j==1 else M3     # rivets
        if (i*7+j*3)%13==0: return Y3
        if j==3 and 2<=i<=4: return Y3  # scuff
    if k in('R','L'):
        if i in(w//2-1,w//2) and j in(2,3): return M3 if j==3 else M1   # ear bolt
        if (i*5+j*7)%11==0: return Y4
    if k=='B':
        if j==3: return (230,230,200) if i%2==0 else Y3  # reflective band
    if (i==0 or i==w-1): c=dk(c,.85)
    return c
paint('helmet',(14,6,13),helmet)
paint('ridge',(2,1,12),lambda k,i,j,w,h: Y3 if (i+j)%2 else Y2)
paint('brim',(14,1,2),lambda k,i,j,w,h: (Y4 if (i//2)%2 else Y3) if k in('F','top') else Y4)
def lamp(k,i,j,w,h):
    if k=='F':
        if i==1 and j==1: return (255,255,230)
        return (255,244,170) if j<2 and 0<i<2 or (i,j)==(1,1) else (255,226,120)
    if k=='top': return M1
    return M2 if j<2 else M3
paint('lamp',(3,3,2),lamp)
def lens(k,i,j,w,h):
    if k=='F':
        if i==w-1 or j==h-1: return M3
        if i==0 or j==0: return M2
        if (i,j)==(1,1): return (230,250,255)
        return (120,196,220) if j==1 else (74,140,176)
    return B2
paint('lensR',(4,3,1),lens);paint('lensL',(4,3,1),lens);paint('bridge',(4,1,1),lambda *a:B2)
# ----- hair pigtails
def hair(k,i,j,w,h):
    c=ramp((H1,H2,H3),h,j)
    if (i+j*2)%4==0: c=lt(c,1.2)
    if k=='bot' : return H3
    if k in('F','B','R','L') and j==0: return (200,90,40)  # tie
    return c
paint('hairR',(2,7,3),hair);paint('hairL',(2,7,3),hair)
# ----- body
def body(k,i,j,w,h):
    c=ramp((O1,O2,O3),h,j)
    if k=='top': return O1
    if k=='bot': return O4
    if j==6: return B2 if (k!='F' or i!=4) else (240,200,80)   # belt + buckle
    if j==7: c=O3 if k!='F' else ((O3,O4)[ (i%4==0) ])
    if k=='F':
        if 1<=i<=3 and 2<=j<=4:  # chest pocket
            if j==2: return O4
            if i==1 or i==3: return O3
            return lt(O2,1.05) if j==3 else O2
        if i in(0,8): c=dk(c,.85)
        if (i,j)==(6,2) or (i,j)==(7,3): c=O3
        if (i,j) in ((5,4),(6,5)): c=O3   # wrinkle
        if (i,j)==(6,3): return (110,110,120)  # rivet
        if i==5 and 0<=j<=1: return (110,100,70) # strap
    if k=='B':
        if (i in (1,7)) and j<6: c=O3        # straps
        if (i,j)==(4,3): c=O3
    if k in('R','L'):
        if i in(0,4): c=dk(c,.88)
        if j==2 and i==2: c=O3
    return c
paint('body',(9,8,5),body)
def pack(k,i,j,w,h):
    c=ramp((M1,M2,M3),h,j)
    if k=='B' :
        if j==1 and 0<i<w-1: return (230,60,50) if i in(2,3) else (240,200,80)
        if i in(0,w-1): return M3
    if (i*3+j)%7==0: c=lt(c,1.15)
    return c
paint('pack',(6,5,3),pack)
# ----- arms
def arm(mirror):
    def f(k,i,j,w,h):
        if k=='top': return O1
        if k=='bot': return G3
        if j<=2:
            c=ramp((O1,O2,O3),3,j)
            if k!='top' and i==(w-1 if mirror else 0): c=dk(c,.88)
            if j==1 and i==1 and k in('F','B'): c=O3
            return c
        if j==3: return O4   # cuff seam
        # glove rows 4..6
        c=(G1,G2,G3)[j-4]
        if j==4 and i==1: c=lt(G1,1.1)
        if k=='F' and j==5 and i==1: c=G3
        if i==0 or i==w-1: c=dk(c,.88) if j>4 else c
        return c
    return f
paint('rarm',(3,7,3),arm(False));paint('larm',(3,7,3),arm(True))
# ----- legs
def leg(k,i,j,w,h):
    if k=='top': return O3
    if k=='bot': return B3
    if j<=3:
        c=ramp((O2,O3,O4),4,j)
        if k=='F' and j==1 and 1<=i<=2: c=O4 if i==1 else O2      # knee patch edge
        if k=='F' and j==2 and 1<=i<=2: c=O2 if i==2 else O3
        if i==0 or i==w-1: c=dk(c,.88)
        return c
    c=(B1,B2,B3)[j-4]
    if j==4: c=Y3 if i%2==0 else Y4 if False else B1
    if k=='F' and j==5 and 1<=i<=2: c=lt(B2,1.15)
    if j==6: c=B3
    return c
paint('rleg',(4,7,4),leg);paint('lleg',(4,7,4),leg)
# ----- weapon (scrap shotgun)
def barrel(k,i,j,w,h):
    if k=='F': return (22,18,22) if (1<=i<=w-2 and 1<=j<=h-2) else M2
    c=ramp((M1,M2,M3),h,j) if k!='top' else M1
    if k in('R','L'):
        if i%5==4: return M3         # weld seam
        if (i*3+j*7)%9==0: return (170,96,52)   # rust
    if k=='top' and (j%4==0 or (i,j)==(1,5)): c=M2
    if k=='bot': return M3
    return c
paint('bA',(3,4,10),barrel);paint('bB',(3,4,10),barrel)
def muz(k,i,j,w,h):
    if k=='F':
        if 1<=j<=3 and i in(1,2,4,5): return (22,18,22)
        if j==0: return M1 if i%2 else M2
        if j==h-1: return M3
        return (170,96,52) if i==3 else M2
    c=(170,96,52) if (i+j)%3 else (130,70,42)
    if k=='top': c=M1 if i%2 else M2
    return c
paint('muz',(7,5,2),muz)
def rec(k,i,j,w,h):
    c=ramp((O2,O3,O4),h,j)
    if k in('R','L'):
        if (i,j) in((1,1),(1,5)): return M1      # rivets
        if j==3: return O4
        if i==2 and j in(2,4): return M2
    if k=='F':
        if j in (1,5) and i in(1,4): return M1
        if j==3 and 1<=i<=4: return M3    # slot
    if k=='top': return M2 if i%2 else M1
    if (i+j)%4==0: c=(170,96,52)
    return c
paint('rec',(6,7,3),rec)
def hand(k,i,j,w,h):
    if k=='top': return G1 if (i+j)%3 else G2
    if k in('F','B') and i in(1,4) and j==0: return M1
    if k=='F' and j==1 : return G3
    return G2 if j==0 else G3
paint('hand',(6,2,6),hand)
def stock(k,i,j,w,h):
    c=ramp((G1,G2,G3),h,j)
    if k in('R','L') and j in(1,3) : c=G3
    if k=='B':
        if (i,j) in((1,1),(1,3)): return M1
    if (i*3+j*5)%7==0: c=lt(c,1.12)
    return c
paint('stock',(3,5,4),stock)
img.save(TP)

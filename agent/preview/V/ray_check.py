"""TopDown.rayDir / intersectPlane / rotateInput と同じ式の数値確認。"""
import math

YAW, PITCH, DIST = 180.0, 58.0, 15.0

def sub(a,b): return tuple(x-y for x,y in zip(a,b))
def add(a,b): return tuple(x+y for x,y in zip(a,b))
def mul(a,k): return tuple(x*k for x in a)
def cross(a,b): return (a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0])
def norm(a):
    n = math.sqrt(sum(x*x for x in a)); return mul(a, 1/n)

def forward(yaw, pitch):
    y, p = math.radians(yaw), math.radians(pitch)
    return (-math.sin(y)*math.cos(p), -math.sin(p), math.cos(y)*math.cos(p))
def left(yaw):
    y = math.radians(yaw); return (math.cos(y), 0.0, math.sin(y))
def ray_dir(yaw, pitch, fov, aspect, nx, ny):
    f = forward(yaw, pitch); l = left(yaw); up = cross(f, l)
    t = math.tan(math.radians(fov)/2)
    sx = (2*nx-1)*t*aspect; sy = (1-2*ny)*t
    return norm(add(add(f, mul(l, -sx)), mul(up, sy)))
def plane(o, d, y):
    if d[1] > -1e-4: return None
    k = (y-o[1])/d[1]
    return None if k < 0 else add(o, mul(d, k))
def rotate_input(s, f, yaw):
    dx, dz = -s, -f
    y = math.radians(yaw); sn, c = math.sin(y), math.cos(y)
    return (dx*c+dz*sn, dx*-sn+dz*c)  # (left, forward)

feet = (100.0, 64.0, 200.0)
focus = (feet[0], feet[1]+1.0, feet[2])
cam = sub(focus, mul(forward(YAW, PITCH), DIST))
print("camera", cam)
fov, aspect = 70.0, 16/9
hit = plane(cam, ray_dir(YAW, PITCH, fov, aspect, 0.5, 0.5), feet[1]+1.0)
print("center ->", hit)
assert max(abs(a-b) for a, b in zip(hit, focus)) < 1e-9, "center must hit focus"
# 画面の上(ny=0.25)は北(z が小さい)、右(nx=0.75)は東(x が大きい)
up_hit = plane(cam, ray_dir(YAW, PITCH, fov, aspect, 0.5, 0.25), feet[1]+1.0)
rt_hit = plane(cam, ray_dir(YAW, PITCH, fov, aspect, 0.75, 0.5), feet[1]+1.0)
print("up ->", up_hit, "right ->", rt_hit)
assert up_hit[2] < focus[2] and rt_hit[0] > focus[0]
# aimYaw: 北=180, 東=-90 (MC: yaw0=南+z, 90=西)
def aim_yaw(h): return math.degrees(math.atan2(-(h[0]-feet[0]), h[2]-feet[2]))
print("aimYaw north-ish", aim_yaw(up_hit), "east-ish", aim_yaw(rt_hit))
# 移動: プレイヤーがどの向きでも W=北(-z) になる。MC の移動: world = f*(-sin,cos) + l*(cos,sin)
for yaw in (0, 37, 90, 180, -120):
    for key, want in (("W",(0,-1)),("S",(0,1)),("A",(-1,0)),("D",(1,0))):
        s = {"A":1,"D":-1}.get(key,0); f = {"W":1,"S":-1}.get(key,0)
        l, fw = rotate_input(s, f, yaw)
        y = math.radians(yaw)
        wx = fw*-math.sin(y) + l*math.cos(y); wz = fw*math.cos(y) + l*math.sin(y)
        assert abs(wx-want[0]) < 1e-9 and abs(wz-want[1]) < 1e-9, (yaw, key, wx, wz)
print("OK")

#!/usr/bin/env python3
"""キャラモデル確認用レンダラー。

使い方: python3 tools/model_preview.py <model.json> <texture.png> <out.png>
正面・背面・左・右・斜め上前(遠近)・歩き途中ポーズ の6枚を1枚に並べる。
形式は tools/MODEL_FORMAT.md を参照。PIL + numpy のみ使用。
"""
import json
import math
import sys

import numpy as np
from PIL import Image, ImageDraw

PANEL_W, PANEL_H = 320, 400
SS = 2  # スーパーサンプリング倍率
BG = (214, 214, 214)
FIXED_BONES = ["body", "head", "right_arm", "left_arm", "right_leg", "left_leg", "weapon"]


# ---------------------------------------------------------------- モデル読み込み
def rot_matrix(rx, ry, rz):
    """度。X → Y → Z の順に回す (R = Rz*Ry*Rx)。右手系、y上向き。"""
    ax, ay, az = map(math.radians, (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def bone_local(pivot, rot):
    """pivot まわりに rot 回すアフィン(4x4)。"""
    M = np.eye(4)
    R = rot_matrix(*rot)
    p = np.array(pivot, float)
    M[:3, :3] = R
    M[:3, 3] = p - R @ p
    return M


def cube_faces(cube):
    """箱UV展開。戻り値: [(corners[4][3], uvs[4][2], normal, (umin,umax,vmin,vmax))]
    座標は x+ = キャラの右、y+ = 上、正面 = -Z。"""
    fx, fy, fz = cube["from"]
    w, h, d = cube["size"]
    inf = cube.get("inflate", 0.0)
    u, v = cube["uv"]
    x0, y0, z0 = fx - inf, fy - inf, fz - inf
    x1, y1, z1 = fx + w + inf, fy + h + inf, fz + d + inf
    # UV矩形 (ModelPart と同じ並び: 上段 上・下 / 下段 右・前・左・後)
    ru = (u, u + d)                 # 右(+X)
    fr = (u + d, u + d + w)         # 前(-Z)
    le = (u + d + w, u + 2 * d + w)  # 左(-X)
    ba = (u + 2 * d + w, u + 2 * d + 2 * w)  # 後(+Z)
    to = (u + d, u + d + w)
    do = (u + d + w, u + d + 2 * w)
    vt = (v, v + d)
    vs = (v + d, v + d + h)
    faces = []

    def add(c, uv, n, ur, vr):
        faces.append([c, uv, n, (ur[0], ur[1], vr[0], vr[1])])

    # 前 (-Z): 左上(視点)=x1側
    add([(x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0)],
        [(fr[0], vs[0]), (fr[1], vs[0]), (fr[1], vs[1]), (fr[0], vs[1])], (0, 0, -1), fr, vs)
    # 後 (+Z)
    add([(x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1)],
        [(ba[0], vs[0]), (ba[1], vs[0]), (ba[1], vs[1]), (ba[0], vs[1])], (0, 0, 1), ba, vs)
    # 右 (+X): 右端(u1)が前(z0)
    add([(x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1)],
        [(ru[0], vs[0]), (ru[1], vs[0]), (ru[1], vs[1]), (ru[0], vs[1])], (1, 0, 0), ru, vs)
    # 左 (-X): 左端(u0)が前(z0)
    add([(x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0)],
        [(le[0], vs[0]), (le[1], vs[0]), (le[1], vs[1]), (le[0], vs[1])], (-1, 0, 0), le, vs)
    # 上 (+Y): 下端(v1)が前(z0)
    add([(x1, y1, z1), (x0, y1, z1), (x0, y1, z0), (x1, y1, z0)],
        [(to[0], vt[0]), (to[1], vt[0]), (to[1], vt[1]), (to[0], vt[1])], (0, 1, 0), to, vt)
    # 下 (-Y): 上端(v0)が前(z0)
    add([(x1, y0, z0), (x0, y0, z0), (x0, y0, z1), (x1, y0, z1)],
        [(do[0], vt[0]), (do[1], vt[0]), (do[1], vt[1]), (do[0], vt[1])], (0, -1, 0), do, vt)
    if cube.get("mirror"):
        cx = (x0 + x1)
        for f in faces:
            f[0] = [(cx - p[0], p[1], p[2]) for p in f[0]]
            f[2] = (-f[2][0], f[2][1], f[2][2])
    return faces


class Model:
    def __init__(self, path):
        with open(path, encoding="utf-8") as fp:
            data = json.load(fp)
        self.bones = {}
        self.order = []
        for b in data["bones"]:
            self.bones[b["name"]] = b
            self.order.append(b["name"])
        self.faces = {n: [f for c in self.bones[n].get("cubes", []) for f in cube_faces(c)] for n in self.order}
        self.cubes = {n: self.bones[n].get("cubes", []) for n in self.order}

    def check(self, tex_size):
        warns = []
        for n in FIXED_BONES:
            if n not in self.bones:
                warns.append("必須ボーン %s がありません" % n)
        for n, b in self.bones.items():
            p = b.get("parent")
            if p and p not in self.bones:
                warns.append("%s の parent %s が存在しません" % (n, p))
            for c in self.cubes[n]:
                w, h, d = c["size"]
                u, v = c["uv"]
                if u + 2 * d + 2 * w > tex_size[0] or v + d + h > tex_size[1]:
                    warns.append("%s の cube uv %s がテクスチャ(%dx%d)からはみ出します" % (n, c["uv"], *tex_size))
        ys = [c["from"][1] + c["size"][1] + c.get("inflate", 0) for n in self.order if n != "weapon" for c in self.cubes[n]]
        y0 = [c["from"][1] - c.get("inflate", 0) for n in self.order for c in self.cubes[n]]
        if ys:
            H = max(ys)
            if not 24 <= H <= 34:
                warns.append("全高 %.1fpx は 24〜34 の範囲外" % H)
            if min(y0) < -0.01:
                warns.append("足元 y=0 より下に %.1fpx はみ出し" % -min(y0))
        if "head" in self.bones and "body" in self.bones and self.cubes["head"] and self.cubes["body"]:
            hw = self.cubes["head"][0]["size"][0]
            bw = self.cubes["body"][0]["size"][0]
            if bw and not 1.2 <= hw / bw <= 1.6:
                warns.append("頭幅/体幅 = %.2f は 1.2〜1.6 の範囲外" % (hw / bw))
        return warns

    def world_matrices(self, pose=None):
        pose = pose or {}
        cache = {}

        def get(name):
            if name in cache:
                return cache[name]
            b = self.bones[name]
            rot = list(b.get("rotation", [0, 0, 0]))
            dx = pose.get(name, 0.0)
            rot[0] += dx
            M = bone_local(b.get("pivot", [0, 0, 0]), rot)
            p = b.get("parent")
            if p and p in self.bones:
                M = get(p) @ M
            cache[name] = M
            return M

        return {n: get(n) for n in self.order}


# ---------------------------------------------------------------- 描画
class Canvas:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.color = np.zeros((h, w, 3), np.float32)
        self.color[:] = BG
        self.depth = np.full((h, w), np.inf, np.float32)


def raster_tri(cv, P, W, UV, shade, tex, rect, Z=None):
    xs, ys = P[:, 0], P[:, 1]
    minx = max(int(math.floor(xs.min())), 0)
    maxx = min(int(math.ceil(xs.max())), cv.w - 1)
    miny = max(int(math.floor(ys.min())), 0)
    maxy = min(int(math.ceil(ys.max())), cv.h - 1)
    if minx > maxx or miny > maxy:
        return
    (x0, y0), (x1, y1), (x2, y2) = P
    area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
    if abs(area) < 1e-9:
        return
    gx, gy = np.meshgrid(np.arange(minx, maxx + 1) + 0.5, np.arange(miny, maxy + 1) + 0.5)
    l1 = ((gx - x0) * (y2 - y0) - (x2 - x0) * (gy - y0)) / area
    l2 = ((x1 - x0) * (gy - y0) - (gx - x0) * (y1 - y0)) / area
    l0 = 1 - l1 - l2
    eps = -1e-6
    mask = (l0 >= eps) & (l1 >= eps) & (l2 >= eps)
    if not mask.any():
        return
    w = l0 * W[0] + l1 * W[1] + l2 * W[2]
    uu = (l0 * UV[0][0] * W[0] + l1 * UV[1][0] * W[1] + l2 * UV[2][0] * W[2]) / w
    vv = (l0 * UV[0][1] * W[0] + l1 * UV[1][1] * W[1] + l2 * UV[2][1] * W[2]) / w
    depth = (l0 * Z[0] + l1 * Z[1] + l2 * Z[2]) if Z is not None else 1.0 / w
    sub = cv.depth[miny:maxy + 1, minx:maxx + 1]
    mask &= depth < sub
    if not mask.any():
        return
    umin, umax, vmin, vmax = rect
    uu = np.clip(uu, umin, umax - 1e-3)
    vv = np.clip(vv, vmin, vmax - 1e-3)
    th, tw = tex.shape[:2]
    tx = np.clip(np.floor(uu).astype(int), 0, tw - 1)
    ty = np.clip(np.floor(vv).astype(int), 0, th - 1)
    texel = tex[ty, tx]  # (h,w,4)
    mask &= texel[..., 3] >= 128
    if not mask.any():
        return
    col = texel[..., :3].astype(np.float32) * shade
    csub = cv.color[miny:maxy + 1, minx:maxx + 1]
    csub[mask] = col[mask]
    sub[mask] = depth[mask]


def render_view(model, tex, pose, yaw, pitch, persp, center, R):
    cv = Canvas(PANEL_W * SS, PANEL_H * SS)
    ya, pa = math.radians(yaw), math.radians(pitch)
    cam_dir = np.array([math.sin(ya) * math.cos(pa), math.sin(pa), -math.cos(ya) * math.cos(pa)])
    f = -cam_dir
    up0 = np.array([0, 1.0, 0])
    r = np.cross(f, up0)
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    dist = 3.2 * R
    scale = min(PANEL_W * 0.46, PANEL_H * 0.46) * SS / R
    K = scale * dist
    cx, cy = cv.w / 2, cv.h / 2 + 4 * SS
    L = np.array([-0.3, 0.5, -0.8])
    L /= np.linalg.norm(L)
    mats = model.world_matrices(pose)

    def to_view(p):
        q = p - center
        xc, yc, zc = q @ r, q @ u, q @ f + dist
        return xc, yc, zc

    for name in model.order:
        M = mats[name]
        Rm = M[:3, :3]
        for corners, uvs, n, rect in model.faces[name]:
            pts = np.array(corners, float)
            wp = pts @ Rm.T + M[:3, 3]
            nw = Rm @ np.array(n, float)
            nv = np.array([nw @ r, nw @ u, nw @ f])
            shade = 0.55 + 0.45 * max(0.0, float(nv @ L))
            sp, ws, zs = [], [], []
            for p in wp:
                xc, yc, zc = to_view(p)
                if persp:
                    wv = 1.0 / zc
                    sx, sy = cx + xc * wv * K, cy - yc * wv * K
                else:
                    wv = 1.0 / dist
                    sx, sy = cx + xc * scale, cy - yc * scale
                sp.append((sx, sy))
                zs.append(zc)
                ws.append(wv)
            sp = np.array(sp)
            ws = np.array(ws)
            zs = np.array(zs)
            uvs_a = np.array(uvs, float)
            for tri in ((0, 1, 2), (0, 2, 3)):
                raster_tri(cv, sp[list(tri)], ws[list(tri)], uvs_a[list(tri)], shade, tex, rect,
                            None if persp else zs[list(tri)])
    img = Image.fromarray(np.clip(cv.color, 0, 255).astype(np.uint8), "RGB")
    return img.resize((PANEL_W, PANEL_H), Image.BOX)


def main(argv):
    if len(argv) != 4:
        print("使い方: python3 tools/model_preview.py <model.json> <texture.png> <out.png>", file=sys.stderr)
        return 2
    model_path, tex_path, out_path = argv[1:]
    model = Model(model_path)
    timg = Image.open(tex_path).convert("RGBA")
    tex = np.array(timg)
    for w in model.check(timg.size):
        print("警告:", w, file=sys.stderr)

    # 全頂点(待機姿勢)から中心と半径
    allp = []
    for n in model.order:
        for corners, *_ in model.faces[n]:
            allp.extend(corners)
    allp = np.array(allp, float)
    lo, hi = allp.min(0), allp.max(0)
    center = (lo + hi) / 2
    R = float(np.linalg.norm(allp - center, axis=1).max()) * 1.05

    walk = {"right_arm": 35, "left_arm": -35, "right_leg": -35, "left_leg": 35}
    views = [
        ("front", 0, 0, False, None),
        ("back", 180, 0, False, None),
        ("left", -90, 0, False, None),
        ("right", 90, 0, False, None),
        ("oblique (persp)", 35, 28, True, None),
        ("walk", 55, 8, False, walk),
    ]
    sheet = Image.new("RGB", (PANEL_W * 3, PANEL_H * 2), BG)
    dr = ImageDraw.Draw(sheet)
    for i, (label, yaw, pitch, persp, pose) in enumerate(views):
        im = render_view(model, tex, pose, yaw, pitch, persp, center, R)
        ox, oy = (i % 3) * PANEL_W, (i // 3) * PANEL_H
        sheet.paste(im, (ox, oy))
        dr.rectangle([ox, oy, ox + PANEL_W - 1, oy + PANEL_H - 1], outline=(180, 180, 180))
        dr.text((ox + 8, oy + 6), label, fill=(60, 60, 60))
    sheet.save(out_path)
    print("saved", out_path)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

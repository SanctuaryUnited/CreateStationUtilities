"""Expanded inventory preview from a native client catalog export and shipped models.

This is a software render, not a screenshot. Run the inventoryData validation run
first so the list of items and divider rows comes from the registered tabs.
"""
from pathlib import Path
from functools import lru_cache
import json
import math
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont

from lrt_geometry import cube, faces, quad, atlas_uv

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets"
BACKGROUND = (198, 198, 198)
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


@lru_cache(None)
def model(identifier):
    namespace, path = identifier.split(":")
    doc = json.loads((ASSETS / namespace / "models" / (path + ".json")).read_text())
    parent = doc.get("parent", "")
    inherited = model(parent) if parent.startswith("createtransit:") else {}
    return {**inherited, **doc,
            "textures": {**inherited.get("textures", {}), **doc.get("textures", {})},
            "display": {**inherited.get("display", {}), **doc.get("display", {})}}


def material(doc, key):
    while key.startswith("#"):
        key = doc["textures"][key[1:]]
    return key


@lru_cache(None)
def texture(identifier):
    namespace, path = identifier.split(":")
    return np.asarray(Image.open(ASSETS / namespace / "textures" / (path + ".png")).convert("RGBA"), dtype=float)


def geometry(doc):
    result = []
    for entry in doc.get("quads", []):
        result.append(quad(entry["vertices"], material(doc, entry["texture"]),
                           entry.get("tint", -1), entry.get("uv")))
    for entry in doc.get("elements", []):
        for direction, face in entry["faces"].items():
            material_id = material(doc, face["texture"])
            mesh = faces([cube(entry["from"], entry["to"], material_id,
                               face.get("tintindex", -1), [direction], entry.get("rotation"))])[0]
            u, v, U, V = face.get("uv", [0, 0, 16, 16])
            uv = [[u, V], [U, V], [U, v], [u, v]]
            turns = face.get("rotation", 0) // 90
            mesh["uv"] = uv[turns:] + uv[:turns]
            result.append(mesh)
    return result


def thumbnail(identifier, size=128):
    doc = model("createtransit:item/" + identifier.split(":")[1])
    if "layer0" in doc.get("textures", {}):
        ns, path = material(doc, "#layer0").split(":")
        sprite = Image.open(ASSETS / ns / "textures" / (path + ".png")).convert("RGBA")
        sprite.thumbnail((size - 18, size - 18), Image.Resampling.NEAREST)
        image = Image.new("RGB", (size, size), BACKGROUND)
        image.paste(sprite, ((size-sprite.width)//2, (size-sprite.height)//2), sprite)
        return image
    mesh = geometry(doc)
    if not mesh:
        raise ValueError("No shipped geometry for " + identifier)
    image = np.empty((size, size, 3), dtype=float)
    image[:] = BACKGROUND
    depth = np.full((size, size), -np.inf)
    gui = doc.get("display", {}).get("gui", {})
    pitch, yaw = [math.radians(v) for v in gui.get("rotation", [30, 225, 0])[:2]]
    yaw -= math.pi
    right = -np.array([math.cos(yaw), 0, math.sin(yaw)])
    forward = np.array([math.sin(yaw)*math.cos(pitch), math.sin(pitch), -math.cos(yaw)*math.cos(pitch)])
    up = np.cross(forward, right)
    points = np.array([p for entry in mesh for p in entry["_quad"]])
    center = (points.min(0) + points.max(0)) / 2
    xx, yy = (points - center) @ right, (points - center) @ up
    scale = min((size - 26) / max(1, np.ptp(xx)), (size - 26) / max(1, np.ptp(yy)))
    center += right * (xx.min() + xx.max()) / 2 + up * (yy.min() + yy.max()) / 2

    def triangle(vertices, uv, tex, brightness):
        screen = np.stack(((vertices-center) @ right * scale + size/2,
                           size/2 - (vertices-center) @ up * scale,
                           (vertices-center) @ forward), axis=1)
        lo = np.maximum(0, np.floor(screen[:, :2].min(0)).astype(int))
        hi = np.minimum(size-1, np.ceil(screen[:, :2].max(0)).astype(int))
        if np.any(hi < lo):
            return
        x0, y0 = lo; x1, y1 = hi
        x, y = np.meshgrid(np.arange(x0, x1+1)+.5, np.arange(y0, y1+1)+.5)
        a, b, c = screen
        den = (b[1]-c[1])*(a[0]-c[0]) + (c[0]-b[0])*(a[1]-c[1])
        if abs(den) < 1e-8:
            return
        q = ((b[1]-c[1])*(x-c[0])+(c[0]-b[0])*(y-c[1]))/den
        r = ((c[1]-a[1])*(x-c[0])+(a[0]-c[0])*(y-c[1]))/den
        s = 1-q-r
        distance = q*a[2]+r*b[2]+s*c[2]
        u = (q*uv[0,0]+r*uv[1,0]+s*uv[2,0])/16
        v = (q*uv[0,1]+r*uv[1,1]+s*uv[2,1])/16
        tx = (u*tex.shape[1]).astype(int).clip(0, tex.shape[1]-1)
        ty = (v*tex.shape[0]).astype(int).clip(0, tex.shape[0]-1)
        rgba = tex[ty, tx]
        region = image[y0:y1+1, x0:x1+1]
        region_depth = depth[y0:y1+1, x0:x1+1]
        mask = (q >= -1e-8) & (r >= -1e-8) & (s >= -1e-8) & (distance > region_depth + 1e-7) & (rgba[:,:,3] > 0)
        alpha = rgba[:,:,3:4]/255
        color = rgba[:,:,:3]*brightness*alpha + region*(1-alpha)
        region[mask] = color[mask]
        region_depth[mask] = distance[mask]

    # Translucent glass is sorted after the opaque casing, as in the model pass.
    mesh.sort(key=lambda e: ("glass" in e["texture"], np.mean(np.array(e["_quad"]) @ forward)))
    for entry in mesh:
        v = np.array(entry["_quad"], dtype=float)
        normal = np.cross(v[1]-v[0], v[2]-v[0])
        length = np.linalg.norm(normal)
        if length < 1e-8:
            continue
        normal /= length
        if normal @ forward <= 0:
            continue
        name = entry["texture"]
        brightness = 1 if "led" in name or "arrow" in name else .7 + .3*max(0, normal @ np.array([.35,.75,-.55]))
        tex = texture(name)
        uv = np.array(atlas_uv(entry))
        for indices in ([0,1,2], [0,2,3]):
            triangle(v[indices], uv[indices], tex, brightness)
    return Image.fromarray(np.clip(image, 0, 255).astype("uint8"))


def main():
    catalog = json.loads((ROOT / "build/inventory-validation/inventory-layout.json").read_text())
    assert catalog["client_mixins_verified"]
    lang = json.loads((ASSETS / "createtransit/lang/en_us.json").read_text())
    out = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "preview/Create-Transit-Inventory-Preview.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet = Image.new("RGB", (1500, 1120), (27, 38, 53))
    draw = ImageDraw.Draw(sheet)
    def font(n, bold=False):
        return ImageFont.truetype(BOLD if bold else FONT, n)
    draw.text((40, 27), "CREATE TRANSIT  /  INVENTORY REWORK", font=font(34, True), fill="#edf5fc")
    draw.text((42, 84), "Two tabs. Named dividers. All current building parts in one place.", font=font(22), fill="#91cee9")
    cell = 72
    for panel, tab in enumerate(catalog["tabs"]):
        x, y = 42 + panel*742, 221
        title = lang[tab["label_key"]].split("—")[-1].strip().upper()
        draw.text((x, 147), title, font=font(28, True), fill="#edf5fc")
        draw.text((x, 187), f"{tab['real_items']} items  /  single white, dyeable block entries", font=font(18), fill="#adc8e3")
        headers = {entry["row"]: lang[entry["label_key"]] for entry in tab["headers"]}
        draw.rectangle((x-9, y-9, x+9*cell+9, y+tab["rows"]*cell+9), fill="#c6c6c6", outline="#607487", width=2)
        for row in range(tab["rows"]):
            yy = y + row*cell
            if row in headers:
                # Positions are the same ratios as the 162-by-18 native row.
                draw.rectangle((x+12, yy+52, x+9*cell-12, yy+56), fill="#353b40")
                draw.rectangle((x+12, yy+56, x+9*cell-12, yy+64), fill="#52c6df")
                draw.text((x+16, yy+8), headers[row], font=font(27, True), fill="#353b40")
                continue
            for column in range(9):
                xx = x + column*cell
                draw.rectangle((xx, yy, xx+cell-1, yy+cell-1), fill="#8b8b8b")
                draw.line((xx, yy, xx+cell-1, yy, xx+cell-1, yy+cell-1), fill="#ffffff", width=4)
                draw.line((xx, yy+cell-1, xx, yy, xx+cell-1, yy), fill="#373737", width=4)
                draw.rectangle((xx+4, yy+4, xx+cell-5, yy+cell-5), fill=BACKGROUND)
                item = tab["cells"][row*9+column]
                if item:
                    icon = thumbnail(item).resize((cell-8, cell-8), Image.Resampling.LANCZOS)
                    sheet.paste(icon, (xx+4, yy+4))
    draw.text((42, 1052), "Expanded catalog preview — the in-game inventory uses a standard five-row scrolling grid.", font=font(19), fill="#bdd2e5")
    draw.text((42, 1083), "Actual registered items and shipped models • software render • Alpha 16.1", font=font(17), fill="#89a9c2")
    sheet.save(out)
    print(f"Rendered {sum(tab['real_items'] for tab in catalog['tabs'])} real item models to {out}")


if __name__ == "__main__":
    main()

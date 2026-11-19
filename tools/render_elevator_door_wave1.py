"""Software preview of exported production meshes and the exact shipped Wave 1 PNGs.

No concept images, no new meshes, no simulated PBR. Dynamic display text is not
baked into door textures and is intentionally omitted in these asset renders.
Requires ExportDoorStyles output and MINECRAFT_CLIENT_JAR for selected oak only.
"""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import json
import os
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import render_lrt_preview as raster

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'build/door-previews'
OUT = ROOT / 'build/elevator-wave1'
OUT.mkdir(parents=True, exist_ok=True)
mc = ZipFile(os.environ['MINECRAFT_CLIENT_JAR'])
styles = ['SOLID', 'GLASS', 'WOOD_TRIM', 'SLIM_WINDOW', 'DIVIDED_GLASS', 'INDUSTRIAL']


def texture(id, alpha=1):
    key = id + str(alpha)
    if key not in raster.TEXTURES:
        namespace, path = id.split(':')
        file = 'assets/' + namespace + '/textures/' + path + '.png'
        im = Image.open(ROOT / 'src/main/resources' / file) if namespace == 'createtransit' else Image.open(BytesIO(mc.read(file)))
        values = np.array(im.convert('RGBA')).astype(float)
        values[:, :, 3] *= alpha
        raster.TEXTURES[key] = values
    return key


def geometry(doc, pose):
    out = []
    for index, face in enumerate(doc[pose]):
        material = doc['materials'][face[0]]
        verts = np.array(face[1:]).reshape((4, 3)) * 16
        id = doc['sprites'].get(material, 'minecraft:block/oak_planks' if material == 'DOOR_WOOD' else 'createtransit:block/elevator_metal')
        tex = texture(id, .75 if material == 'DOOR_GLASS' else 1)
        if material == 'DOOR_GLASS':
            raster.TEXTURES['lrt_glass'] = raster.TEXTURES[tex]
            tex = 'lrt_glass'
        if material == 'DOOR_LED':
            raster.TEXTURES['gondola_led'] = raster.TEXTURES[tex]
            tex = 'gondola_led'
        u0, v0, u1, v1 = doc[pose + 'UV'][index]
        n = np.cross(verts[1]-verts[0], verts[2]-verts[0])
        n /= max(np.linalg.norm(n), 1e-9)
        natural = .7 + .3 * max(0, n @ np.array([.35, .75, -.55]))
        shade = 1 if material == 'DOOR_LED' else (.72+.28*max(0, n[1]))/natural
        uv = [[u0*16, v1*16], [u0*16, v0*16], [u1*16, v0*16], [u1*16, v1*16]]
        if material == 'DOOR_WOOD' and abs(verts[1][1]-verts[0][1]) > abs(verts[2][0]-verts[0][0]):
            uv = [[v, 16-u] for u,v in uv]
        out.append({'texture': tex, 'tint': 1 if material == 'DOOR_LED' else -1,
                    '_shade': shade, '_quad': verts.tolist(), 'from': verts.min(0).tolist(),
                    'to': verts.max(0).tolist(), 'visible': ['north'],
                    'uv': uv})
    return out


fontpath = '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
title = ImageFont.truetype(fontpath, 29)
label = ImageFont.truetype(fontpath, 22)
small = ImageFont.truetype(fontpath, 16)
sheet = Image.new('RGB', (1800, 1460), '#172536')
d = ImageDraw.Draw(sheet)
d.text((28, 20), 'CREATE: TRANSIT  /  ELEVATOR TEXTURES — WAVE 1', font=title, fill='white')
d.text((28, 64), 'Actual production geometry + packaged textures • software asset preview • matching cabin / landing designs', font=small, fill='#a7c5d7')
for index, style in enumerate(styles):
    doc = json.loads((DATA / (style + '.json')).read_text())
    closed, opened = geometry(doc, 'closed'), geometry(doc, 'open')
    # Preserve the same bounds/scale when comparing the two poses.
    a = raster.render(closed, (580, 355), yaw=180, pitch=0, paint=(249,249,249), accent=(94,190,236), fit=closed+opened)
    b = raster.render(opened, (580, 255), yaw=180, pitch=0, paint=(249,249,249), accent=(94,190,236), fit=closed+opened)
    x, y = (index % 3)*600, 112 + (index//3)*650
    # Large closed view plus shorter, clearly labelled open view below.
    sheet.paste(a, (x+10, y))
    sheet.paste(b, (x+10, y+370))
    d.text((x+22, y-24), f'{index+1:02}  {style.replace("_", " ").title()}', font=label, fill='white')
    d.text((x+22, y+345), 'CLOSED', font=small, fill='#a7c5d7')
    d.text((x+22, y+604), 'OPEN — panels visible along shaft-facing side', font=small, fill='#a7c5d7')
    a.save(OUT / (f'{index+1:02}-' + style.lower() + '.png'))
d.text((28, 1430), 'Live floor labels/arrows, world lighting, shaders and gameplay require a graphical Minecraft check.', font=small, fill='#a7c5d7')
sheet.save(OUT / 'Create-Transit-Elevator-Wave-1-Preview.png')
print(OUT / 'Create-Transit-Elevator-Wave-1-Preview.png')

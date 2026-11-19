"""Original pixel tiles for approved elevator styles 01–06; no concept-sheet crops.

Deterministic generator, RGBA PNGs, local nearest-neighbor UVs. The production
DoorGeometry mesh supplies windows, dividers, wood inserts, LEDs and safety bands.
Run from any directory. Only this elevator_door/ namespace and its atlas entries
are updated. Existing station, vehicle, cabin body and Copycat assets are retained.
"""
from pathlib import Path
import hashlib
import json
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/createtransit/textures/block/elevator_door'
OUT.mkdir(parents=True, exist_ok=True)


def steel(base, width=32, height=32, ribbed=False):
    im = Image.new('RGBA', (width, height))
    for y in range(height):
        for x in range(width):
            # Short horizontal brush strokes, not random photographic noise.
            grain = ((x // 4 * 13 + y * 7) % 7) - 3
            line = (0, 3, 1, -2, 0, 2, -1, -3)[y % 8]
            bevel = (3, 0, -4, -6, -1, 4, 8, 0)[x % 8] if ribbed else 0
            im.putpixel((x, y), tuple(max(0, min(255, c + grain + line + bevel)) for c in base) + (255,))
    return im


def bevel(draw, rect, light, dark):
    x, y, X, Y = rect
    draw.line((x, y, X, y), fill=dark)
    draw.line((x, y, x, Y), fill=dark)
    draw.line((x, Y, X, Y), fill=light)
    draw.line((X, y, X, Y), fill=light)


tiles = {
    'brushed_steel': steel((186, 189, 196)),
    'edge_steel': steel((151, 156, 165)),
    'charcoal_steel': steel((59, 62, 69)),
    'dark_trim': steel((36, 42, 49)),
    'ribbed_steel': steel((58, 61, 67), 32, 64, True),
}
solid = steel((184, 188, 194), 32, 64)
d = ImageDraw.Draw(solid)
for rect in [(3, 3, 28, 29), (3, 34, 28, 60)]:
    bevel(d, rect, (224, 227, 232, 255), (107, 112, 122, 255))
tiles['solid_leaf'] = solid

glass = Image.new('RGBA', (32, 64), (121, 167, 182, 35))
d = ImageDraw.Draw(glass)
for x, y in [(4, 12), (19, 48)]:
    for i in range(7):
        d.rectangle((x + i, y - i, x + i + 1, y - i + 1), fill=(195, 230, 238, 150))
    for i in range(4):
        d.point((x + 4 + i, y + 5 - i), fill=(175, 213, 226, 95))
tiles['clear_glass'] = glass

hazard = Image.new('RGBA', (16, 32))
for y in range(32):
    for x in range(16):
        hazard.putpixel((x, y), (237, 185, 34, 255) if (x + y) % 16 < 8 else (34, 39, 45, 255))
tiles['safety_stripe'] = hazard
tiles['safety_edge'] = Image.new('RGBA', (8, 8), (238, 185, 35, 255))
led = Image.new('RGBA', (8, 32))
for y in range(32):
    for x in range(8):
        value = (175, 215, 245, 255, 255, 245, 215, 175)[x]
        led.putpixel((x, y), (value, value, value, 255))
tiles['led'] = led

for name, im in tiles.items():
    im.save(OUT / (name + '.png'), optimize=True)
    # Preserve the existing kit's LabPBR companion-map convention. Shader packs
    # discover these companions from each base sprite; no extra atlas entries.
    Image.new('RGB', im.size, (128, 128, 255)).save(OUT / (name + '_n.png'), optimize=True)
    metal = name in {'brushed_steel', 'edge_steel', 'solid_leaf', 'ribbed_steel'}
    smoothness = 235 if name in {'edge_steel', 'clear_glass'} else 210 if metal else 100
    Image.new('RGBA', im.size, (smoothness, 230 if metal else 0, 0, 254 if name == 'led' else 0)).save(OUT / (name + '_s.png'), optimize=True)

atlas = ROOT / 'src/main/resources/assets/minecraft/atlases/blocks.json'
doc = json.loads(atlas.read_text())
doc['sources'] = [entry for entry in doc['sources'] if not entry.get('resource', '').startswith('createtransit:block/elevator_door/')]
for name in sorted(tiles):
    key = 'createtransit:block/elevator_door/' + name
    doc['sources'].append({'type': 'minecraft:single', 'resource': key, 'sprite': key})
atlas.write_text(json.dumps(doc, indent=2) + '\n')

manifest = {
    'wave': 1,
    'styles': ['SOLID', 'GLASS', 'WOOD_TRIM', 'SLIM_WINDOW', 'DIVIDED_GLASS', 'INDUSTRIAL'],
    'provenance': 'Original deterministic pixel artwork authored for Create: Transit from the approved visual directions; no vanilla or concept image pixels copied.',
    'glass_layout': 'Divided Glass: exactly two panes per leaf, one central horizontal divider.',
    'sampling': 'Sprite-local UVs. Continuous full-leaf crops across metal window cutouts. Flat normal and LabPBR specular/emission companions follow the existing kit convention; live shader appearance is unverified.',
    'textures': [dict(name=name, width=im.width, height=im.height,
                      sha256=hashlib.sha256((OUT / (name + '.png')).read_bytes()).hexdigest(),
                      companions={suffix: hashlib.sha256((OUT / (name + suffix + '.png')).read_bytes()).hexdigest() for suffix in ['_n','_s']})
                 for name, im in sorted(tiles.items())],
}
(OUT / 'wave1-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
print(f'Generated {len(tiles)} original Wave 1 tiles and explicit block-atlas sources.')

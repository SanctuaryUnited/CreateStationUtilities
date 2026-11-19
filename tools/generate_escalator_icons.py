"""Flat pixel icons with separate paint, LED and untinted detail layers."""
from pathlib import Path
import json
from PIL import Image, ImageDraw


def sprite(kind, glass, powered=False):
    layers = [Image.new('RGBA', (64, 64)) for _ in range(3)]
    frame, trim, detail = [ImageDraw.Draw(im) for im in layers]
    white = (239, 243, 247, 255)
    dark = (22, 29, 35, 255)
    grey = (122, 139, 153, 255)
    if kind == 'incline':
        body = [(5, 48), (19, 48), (43, 24), (58, 24), (58, 30), (46, 30), (22, 54), (5, 54)]
        pane = [(9, 37), (20, 37), (44, 13), (53, 13), (53, 23), (45, 23), (21, 47), (9, 47)]
        rail = [(8, 45), (8, 38), (12, 34), (19, 34), (43, 10), (51, 10), (55, 14), (55, 23)]
        line = [(6, 50), (20, 50), (44, 26), (57, 26)]
        detail.line([(5, 44), (18, 44), (18, 40), (22, 40), (22, 36), (26, 36),
                     (26, 32), (30, 32), (30, 28), (34, 28), (34, 24),
                     (38, 24), (38, 20), (58, 20)], fill=grey, width=3)
    else:
        body = [(5, 44), (58, 44), (58, 52), (5, 52)]
        pane = [(10, 25), (53, 25), (53, 44), (10, 44)]
        rail = [(8, 42), (8, 27), (12, 22), (51, 22), (55, 27), (55, 42)]
        if kind == 'lower_landing':
            rail = [(8, 42), (8, 27), (12, 22), (58, 22)]
            pane = [(10, 25), (58, 25), (58, 44), (10, 44)]
        elif kind == 'upper_landing':
            rail = [(5, 22), (51, 22), (55, 27), (55, 42)]
            pane = [(5, 25), (53, 25), (53, 44), (5, 44)]
        line = [(6, 47), (57, 47)]
        detail.line((6, 42, 57, 42), fill=grey, width=3)
    if glass:
        detail.polygon(pane, fill=(132, 190, 215, 235))
        detail.line(pane[:2], fill=(204, 238, 251, 255), width=2)
    else:
        frame.polygon(pane, fill=white)
    frame.polygon(body, fill=white)
    detail.line(rail, fill=dark, width=5, joint='curve')
    detail.line(line, fill=(76, 93, 108, 255), width=4, joint='curve')
    trim.line(line, fill=(255, 255, 255, 255), width=2, joint='curve')
    detail.rectangle((5, 51, 11, 55), fill=dark)
    detail.rectangle((51, 27 if kind == 'incline' else 51, 58, 31 if kind == 'incline' else 55), fill=dark)
    if powered:
        detail.polygon([(50, 41), (60, 51), (50, 61), (40, 51)], fill=(75, 15, 17, 255))
        detail.polygon([(50, 44), (57, 51), (50, 58), (43, 51)], fill=(221, 48, 46, 255))
        detail.line((47, 49, 51, 47, 49, 52, 53, 50), fill=(255, 190, 153, 255), width=2)
    return layers


def generate(assets):
    for glass in (True, False):
        skin = 'glass' if glass else 'solid'
        entries = [(f'escalator_{kind}_{skin}', kind, False)
                   for kind in ('lower_landing', 'incline', 'flat', 'upper_landing')]
        entries += [(f'escalator_{skin}' + ('_redstone' if powered else ''), 'incline', powered)
                    for powered in (False, True)]
        for name, kind, powered in entries:
            textures = {}
            for i, (suffix, image) in enumerate(zip(('frame', 'trim', 'detail'), sprite(kind, glass, powered))):
                path = assets / 'textures/item' / f'{name}_{suffix}.png'
                path.parent.mkdir(parents=True, exist_ok=True)
                image.save(path)
                textures[f'layer{i}'] = f'createtransit:item/{name}_{suffix}'
            model = {'parent': 'minecraft:item/generated', 'textures': textures,
                     'gui_light': 'front'}
            (assets / 'models/item' / f'{name}.json').write_text(json.dumps(model, indent=2) + '\n')


if __name__ == '__main__':
    generate(Path(__file__).resolve().parents[1] / 'src/main/resources/assets/createtransit')

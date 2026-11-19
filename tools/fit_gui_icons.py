"""Fit and centre every 3D item icon in its inventory slot. Run after all generate_*_assets.py scripts.

Only display.gui changes, i.e. how the item draws in inventories, creative tabs and JEI. Placed blocks
(blockstate models) and the hand, ground and item-frame transforms are left untouched.

Each icon keeps its authored rotation. Its scale is chosen so the projected bounds take up the same
on-screen space as a vanilla block icon (a full cube at rotation [30,225,0], scale 0.625, which fits a
16 px slot), capped at MAX_SCALE so tiny parts do not balloon. Its translation moves the geometry's
centre to the centre of the slot, so edge-mounted strips and panels no longer draw along the slot edge.

Usage: python tools/fit_gui_icons.py [--check]   (--check reports drift and exits 1 without writing)
"""
import itertools, json, math, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/createtransit'
MAX_SCALE = 1.0
VANILLA = ([30, 225, 0], 0.625)


def rotate(v, rotation):
    """ItemTransform.apply: rotationXYZ(x, y, z), i.e. v' = Rx * Ry * Rz * v."""
    x, y, z = v
    rx, ry, rz = (math.radians(a) for a in rotation)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    return x, y, z


def extent(lo, hi, rotation):
    """Projected screen width and height (block units) of a box after rotation."""
    pts = [rotate(c, rotation) for c in itertools.product(*zip(lo, hi))]
    return max(p[0] for p in pts) - min(p[0] for p in pts), max(p[1] for p in pts) - min(p[1] for p in pts)


def load(ref):
    ns, _, path = ref.partition(':') if ':' in ref else ('minecraft', '', ref)
    if ns != 'createtransit':
        return None
    f = ASSETS / 'models' / f'{path}.json'
    return json.loads(f.read_text()) if f.exists() else None


def elements(model):
    """Own elements, or the nearest createtransit parent's (vanilla parents carry none)."""
    seen = 0
    while model is not None and seen < 8:
        if model.get('elements'):
            return model['elements']
        model = load(model.get('parent', ''))
        seen += 1
    return None


def gui_of(model):
    seen = 0
    while model is not None and seen < 8:
        gui = (model.get('display') or {}).get('gui')
        if gui:
            return gui
        model = load(model.get('parent', ''))
        seen += 1
    return None


def fit(model):
    elems = elements(model)
    if not elems:
        return None  # flat item/generated icons and builtin renderers
    lo = [min(e['from'][i] for e in elems) / 16 - .5 for i in range(3)]
    hi = [max(e['to'][i] for e in elems) / 16 - .5 for i in range(3)]
    rotation = list((gui_of(model) or {}).get('rotation', VANILLA[0]))
    cube_w, cube_h = extent([-.5] * 3, [.5] * 3, VANILLA[0])
    w, h = extent(lo, hi, rotation)
    scale = min(MAX_SCALE, VANILLA[1] * min(cube_w / max(w, 1e-6), cube_h / max(h, 1e-6)))
    centre = rotate([(a + b) / 2 * scale for a, b in zip(lo, hi)], rotation)
    translation = [round(-c * 16, 3) + 0.0 for c in centre]
    return {'rotation': rotation, 'translation': translation, 'scale': [round(scale, 3)] * 3}


def main():
    check = '--check' in sys.argv
    changed = []
    for f in sorted((ASSETS / 'models/item').glob('*.json')):
        model = json.loads(f.read_text())
        gui = fit(model)
        if gui is None:
            continue
        old = (model.get('display') or {}).get('gui')
        if old == gui:
            continue
        changed.append((f.stem, (old or {}).get('scale', ['inherited'])[0], gui['scale'][0]))
        if not check:
            model.setdefault('display', {})['gui'] = gui
            f.write_text(json.dumps(model, separators=(',', ':')))
    for name, before, after in changed:
        print(f'{name}: scale {before} -> {after}')
    print(f'{len(changed)} icon(s) {"need fitting" if check else "fitted"}')
    return 1 if check and changed else 0


if __name__ == '__main__':
    sys.exit(main())

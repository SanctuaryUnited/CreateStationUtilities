"""Check actual selected deck surfaces: complete coverage, no hidden overlaps."""
from pathlib import Path
import itertools
import json
from PIL import Image
from escalator_geometry import moving

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/createtransit'
checked=0
for material,powered in itertools.product(('glass','solid'),(False,True)):
    name='escalator_'+material+('_redstone' if powered else '')
    rules=json.loads((A/'blockstates'/f'{name}.json').read_text())['multipart']
    for kind,back,front,left,right in itertools.product(('flat','lower_landing','upper_landing'),(False,True),(False,True),(False,True),(False,True)):
        props={'facing':'north','shape':kind,'flat_join':str(back).lower(),'slope_join':str(front).lower(),
               'left':str(not left).lower(),'right':str(not right).lower(),'stopped':'false'}
        selected=[r['apply']['model'] for r in rules if all(props[k]==v for k,v in r['when'].items())]
        static=[q for model in selected for q in json.loads((A/'models'/(model.split(':')[1]+'.json')).read_text())['quads']]
        plates=[q for q in static if q['texture']=='#comb' and all(abs(v[1]-2.06)<1e-8 for v in q['vertices'])]
        if kind in ('lower_landing','upper_landing') and back:assert not plates,'Internal landings must not have a fixed end plate'
        for phase in [n/32 for n in range(32)]+[.999999]:
            animation=moving(kind,left,right,phase,flat_join=back,slope_join=front)
            belt=[q for q in animation if q['texture']=='#steps' and all(abs(v[1]-2)<1e-8 for v in q['vertices'])]
            spans=sorted((min(v[2] for v in q['vertices']),max(v[2] for v in q['vertices'])) for q in plates+belt)
            end=0
            for z,Z in spans:
                assert abs(z-end)<1e-8,('Deck gap or overlap',name,props,phase,end,z)
                assert Z>z
                end=Z
            assert abs(end-16)<1e-8,('Deck must reach both block edges',name,props,phase,end)
            for q in animation:
                if q['texture'] not in ('#steps','#rail','#edge'):continue
                z,Z=min(v[2] for v in q['vertices']),max(v[2] for v in q['vertices'])
                for plate in plates:
                    a,b=min(v[2] for v in plate['vertices']),max(v[2] for v in plate['vertices'])
                    assert min(b,Z)-max(a,z)<=1e-8,'No moving deck stripes beneath the fixed plate'
            checked+=1

icons=0
for model in (A/'models/item').glob('escalator_*.json'):
    doc=json.loads(model.read_text())
    assert doc['parent']=='minecraft:item/generated' and doc['gui_light']=='front'
    assert len(doc['textures'])==3
    for name in doc['textures'].values():
        image=Image.open(A/'textures'/(name.split(':')[1]+'.png'))
        assert image.mode=='RGBA' and image.size==(64,64)
        assert image.getchannel('A').getextrema()==(0,255),'Icons require transparent backgrounds'
    icons+=1
assert icons==12
print(f'PASS: {checked} landing/travelator deck phases cover each block exactly once; no moving surface beneath a fixed plate; {icons} three-layer flat item icons.')

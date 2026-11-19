"""Check cabin model coverage, original materials and sliding clearance."""
from pathlib import Path
from PIL import Image
import json,itertools
from gondola_geometry import *
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/createtransit'
def overlaps(a,b):return all(min(a['to'][i],b['to'][i])-max(a['from'][i],b['from'][i])>1e-6 for i in range(3))
for kind in KINDS:
    name='gondola_'+kind
    geom=mesh(kind)
    for a,b in itertools.combinations(geom,2):assert not overlaps(a,b),(kind,a,b)
    bb=json.loads((ROOT/f'preview/{name}.bbmodel').read_text());assert bb['elements']and bb['textures']
    item=json.loads((A/f'models/item/{name}.json').read_text());assert item['elements']
    states=json.loads((A/f'blockstates/{name}.json').read_text())['multipart']
    for part in states:
        p=A/('models/'+part['apply']['model'].split(':')[1]+'.json');assert p.is_file(),p
        data=json.loads(p.read_text())
        for e in data['elements']:
            assert all(-16<=v<=32 for v in e['from']+e['to'])
            for f in e['faces'].values():assert f['texture'][1:]in data['textures']
    for f in ['north','east','south','west']:
        keys=['left','right','top','bottom']if kind=='window'else['top','bottom']if kind=='window_corner'else[]
        flags=list(itertools.product([False,True],repeat=len(keys)))
        for values in flags:
            state={'facing':f,**dict(zip(keys,[str(v).lower()for v in values]))}
            cases=[dict(state,half=half,paired=paired,hinge=hinge)for half,paired,hinge in itertools.product(['lower','upper'],['false','true'],['left','right'])]if kind=='door'else[state]
            if kind=='roof_edge':cases=[dict(state,half=half,shape=shape)for half,shape in itertools.product(['bottom','top'],ROOF_SHAPES)]
            if kind in LED_KINDS:cases=[dict(state,half=half)for half in ['bottom','top']]
            if kind=='double_door':cases=[dict(state,tall=str(height==48).lower(),row=str(row),half='lower'if row==0 else'upper',column=str(col))for height in [32,48]for row,col in itertools.product(range(height//16),range(3))]
            for case in cases:
                selected=[part for part in states if all(case[k]==v for k,v in part['when'].items())]
                assert len(selected)==(2 if kind in ['window','window_corner']else 1),(kind,case,len(selected))
    loot=json.loads((R/f'data/createtransit/loot_table/blocks/{name}.json').read_text())
    assert len(loot['pools'])==1 and len(loot['pools'][0]['entries'])==1
    ops=loot['pools'][0]['entries'][0]['functions'][0]['ops'];assert {op['source']for op in ops}=={'paint','accent'}
    if kind=='double_door':assert loot['pools'][0]['conditions'][0]['properties']=={'half':'lower','column':'0'}
    if kind=='door':assert loot['pools'][0]['conditions'][0]['properties']=={'half':'lower'}
    for e in geom:
        if e['texture']=='gondola_glass':assert e['tint']==-1
        if e['texture']in ['gondola_accent','gondola_led','gondola_fabric']:assert e['tint']==1
# No new cabin recipe shadows another registered transit recipe (including mirrored layouts).
seen={}
for p in (R/'data/createtransit/recipe').glob('*.json'):
    r=json.loads(p.read_text())
    if r['type']!='minecraft:crafting_shaped':continue
    matrix=tuple(tuple(r['key'].get(c,{}).get('item','air')for c in row)for row in r['pattern'])
    mirrored=tuple(tuple(reversed(row))for row in matrix);canonical=min(str(matrix),str(mirrored))
    if canonical in seen:assert not p.stem.startswith('gondola_')and not seen[canonical].startswith('gondola_'),(p.stem,seen[canonical])
    seen[canonical]=p.stem
for values in itertools.product([False,True],repeat=4):
    pane=next(e for e in window(*values)if e['texture']=='gondola_glass')
    for i,edge in enumerate(['left','right','top','bottom']):
        if values[i]:assert {'left':pane['from'][0],'right':16-pane['to'][0],'top':16-pane['to'][1],'bottom':pane['from'][1]}[edge]==0
# Check both outward slide directions at every 1% against frames, side walls and fixed LEDs.
fixed=door_fixed()+door_lights()+door_status()
neighbors=[transformed(e,offset)for offset in [-16,16]for e in mesh('wall')+mesh('window')]
for direction in [-1,1]:
    for i in range(101):
        progress=i/100;slide=max(0,(progress-.2)/.8);pop=min(1,progress*5)
        moving=[transformed(e,direction*slide*slide*16,0,-pop*3.2)for e in door_leaf()]
        for a in moving:
            for b in fixed+neighbors:assert not overlaps(a,b),(direction,progress,a,b)
# Paired frames leave the center of a two-block doorway open.
left=[e for e in door_fixed()if e['from'][1]>=31 or e['from'][0]<8]
right=[transformed(e,16)for e in door_fixed()if e['from'][1]>=31 or e['from'][0]>8]
assert all(e['from'][1]>=31 or e['to'][0]<=1 or e['from'][0]>=31 for e in left+right)
assert all(e['from'][1]>=31 for e in door_status())and all(e['tint']==1 for e in door_lights())
atlas={s['sprite']for s in json.loads((R/'assets/minecraft/atlases/blocks.json').read_text())['sources']}
for p in (A/'textures/block').glob('gondola_*.png'):
    if p.stem.endswith(('_n','_s','_e')):continue
    im=Image.open(p);assert p.stem.startswith('gondola_')and'createtransit:block/'+p.stem in atlas
    for suffix in ['_n','_s']:assert Image.open(p.with_stem(p.stem+suffix)).size==im.size
spec=Image.open(A/'textures/block/gondola_metal_s.png');assert spec.getpixel((0,0))[1]==230
spec=Image.open(A/'textures/block/gondola_led_s.png');assert spec.getpixel((0,0))[3]==254
assert 'createtransit:gondola_seat'in json.loads((R/'data/create/tags/block/seats.json').read_text())['values']
# Wide leaves have a precisely centered two-block opening and never cross fixed trim.
fixed=wide_fixed()+wide_lights()+wide_status()
assert min(e['from'][0]for e in wide_leaf())==8 and max(e['to'][0]for e in wide_leaf(True))==40
for height in [32,48]:
    fixed=wide_fixed(height)+wide_lights(height)+wide_status(height)
    assert max(e['to'][1]for e in wide_fixed(height))==height
    assert all(e['from'][1]>=height-1 or e['to'][0]<=8 or e['from'][0]>=40 for e in wide_fixed(height))
    assert all(e['from'][1]>=height-1 for e in wide_status(height))
    for i in range(101):
        progress=i/100
        moving=wide_moving(progress,height,center=True)
        closed=wide_moving(0,height,center=True)
        slide,retract=wide_motion(progress)
        assert 0<=retract<=3 and(slide==0 or retract==3),('three-pixel retraction must finish before sliding',height,progress)
        for a,b in zip(moving,closed):
            assert a['from'][1]==b['from'][1]and a['to'][1]==b['to'][1]
            assert abs(a['from'][2]-b['from'][2]-retract)<1e-9 and abs(a['to'][2]-b['to'][2]-retract)<1e-9,('leaf depth travel is exactly three pixels',height,progress)
        for a,b in itertools.combinations(moving,2):assert not overlaps(a,b),(progress,a,b)
        neighbors=[transformed(e,offset)for offset in [-16,48]for e in mesh('wall')+mesh('window')]
        for a in moving:
            for b in fixed+neighbors:assert not overlaps(a,b),(height,progress,a,b)
for shape,half in itertools.product(ROOF_SHAPES,['bottom','top']):
    geo=roof_edge(shape,half)
    for a,b in itertools.combinations(geo,2):assert not overlaps(a,b),(shape,half)
    assert all(0<=v<=16 for e in geo for v in e['from']+e['to'])
for kind in LED_KINDS:
    for half in ['top','bottom']:
        for a in led_lights(kind,half):
            assert a['tint']==1 and a['texture']=='gondola_led'
            for b in led_body(kind,half):assert not overlaps(a,b)
# A vertical corner ends at the next horizontal corner's little vertical bridge.
# The horizontal arms meet straight trims on exactly the same emissive planes.
for half in ['bottom','top']:
    vertical=led_lights('led_vertical_outer_corner')
    horizontal=led_lights('led_horizontal_outer_corner',half)
    assert {tuple(e['visible'])for e in vertical}=={('south',),('east',)}
    assert {tuple(e['visible'])for e in horizontal}=={('south',),('east',)}
    for face,axis in [('south',0),('east',2)]:
        planes=[e for e in horizontal if e['visible']==[face]]
        assert min(e['from'][axis]for e in planes)==13 and max(e['to'][axis]for e in planes)==16.045
        bridge=next(e for e in planes if e['from'][axis]==14.55)
        assert (bridge['from'][1]==0 if half=='bottom'else bridge['to'][1]==16)
        # No overlapping coplanar quads at T junctions in either piece.
        for group in [planes,[e for e in vertical if e['visible']==[face]]]:
            for a,b in itertools.combinations(group,2):assert not overlaps(a,b)
        # Rotate the standard trim 180 degrees: its front LED is on z=16.045.
        straight=next(e for e in led_lights('led_horizontal',half)if e['visible']==['north'])
        assert abs(16-straight['from'][2]-planes[0]['to'][2 if face=='south'else 0])<1e-9
assert all(13<=e['from'][i]<e['to'][i]<=16 for e in mesh('outer_corner')for i in [0,2])
# The example is a block-by-block build; the double door alone reserves nine cells.
from inset_cabin_layout import layout,geometry,oriented
occupied=set()
for c in layout():
    cells=[(c['x'],c['y'],c['z'])]
    if c['kind']=='double_door':cells=[(c['x']-col,c['y']+row,c['z'])for col,row in itertools.product(range(3),range(3))]
    for pos in cells:
        assert pos not in occupied,('example block overlap',pos)
        occupied.add(pos)
    if c['kind']=='roof_edge':continue
    # Other static solids cannot poke outside the intended inset shell.
    for e in mesh(c['kind']):
        if e['texture']=='gondola_led'or e['texture'].startswith('led_'):continue
        q=oriented(e,c['turn'],c['x']*16,c['y']*16,c['z']*16)
        # Closed wide leaves and center signals stay inside the narrow shell.
        assert all(13<=q['from'][axis]<=q['to'][axis]<=67 for axis in [0,2]),(c,q)
assert len(occupied)==104 and min(p[0]for p in occupied)==0 and max(p[0]for p in occupied)==4
solids=[e for e in geometry()if e['texture']!='gondola_led'and not e['texture'].startswith('led_')]
assert min(e['from'][0]for e in solids)==13 and max(e['to'][0]for e in solids)<=67
for a,b in itertools.combinations(solids,2):assert not overlaps(a,b),('inset cabin clearance',a,b)
# The three-pixel retraction and sliding path also clear the complete cabin interior.
door=next(c for c in layout()if c['kind']=='double_door')
def cabin_leaves(progress):return [oriented(e,door['turn'],door['x']*16,door['y']*16,door['z']*16)for e in wide_moving(progress,center=True)]
closed=cabin_leaves(0);stationary=[e for e in solids if not any(e['from']==c['from']and e['to']==c['to']for c in closed)]
for i in range(101):
    for a in cabin_leaves(i/100):
        for b in stationary:assert not overlaps(a,b),('inset cabin sliding clearance',i,a,b)
print('PASS: 23 cabin parts; 3x3/legacy 3x2 models; recipes; connected glass; LED junctions; labPBR; top and center status; roofs; three-pixel retraction before sliding, with clearance at every 1% including the full inset cabin.')

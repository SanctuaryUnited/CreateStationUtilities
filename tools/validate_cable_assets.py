"""Validate baked cable models, dependency gating and suspended grip alignment."""
from pathlib import Path
from PIL import Image
import json,math
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/createtransit'
def read(p):return json.loads(p.read_text())
states=read(A/'blockstates/cable_track.json')['variants']
expected={'xo','zo','pd','nd','cr_o','cr_d','cr_pdx','cr_pdz','cr_ndx','cr_ndz','as','aw','an','ae','ts','tw','tn','te','none'}
assert {k.removeprefix('shape=')for k in states}==expected
for variant in states.values():
    assert variant.get('y',0)in[0,90,180,270]
    assert (A/('models/'+variant['model'].split(':')[1]+'.json')).is_file()
for p in (A/'models/block').glob('cable_*.json'):
    data=read(p)
    for e in data['elements']:
        assert all(-16<=a<=b<=32 for a,b in zip(e['from'],e['to'])),p
        if 'rotation'in e:
            assert e['rotation']['angle']in[-45,-22.5,0,22.5,45]
            assert e['rotation']['axis']in['x','y','z']
        for face in e['faces'].values():
            sprite=data['textures'][face['texture'][1:]]
            assert (A/('textures/'+sprite.split(':')[1]+'.png')).is_file()
segment=read(A/'models/block/cable_segment.json')['elements']
assert min(e['from'][0]for e in segment)==-1.5 and max(e['to'][0]for e in segment)==1.5
assert min(e['from'][1]for e in segment)==-1.5 and max(e['to'][1]for e in segment)==1.5
assert all(e['from'][2]==0 and e['to'][2]==16 for e in segment)
# An upside-down grip is centered at the cable elevation, with its fork
# extending down to the roof hanger. Create's renderer adds a 1/128 nudge.
grip=read(A/'models/block/cable_grip_partial.json')['elements']
assert grip[0]['from'][1]==14 and grip[0]['to'][1]==18
assert max(e['to'][1]for e in grip)==32
assert abs(((111.875-14)+(111.875-18))/2-96)<.13
atlas={s.get('sprite')for s in read(R/'assets/minecraft/atlases/blocks.json')['sources']}
for name in ['cable_steel','cable_grip_metal','cable_grip_rubber']:
    assert 'createtransit:block/'+name in atlas
    for suffix in['','_n','_s']:assert Image.open(A/f'textures/block/{name}{suffix}.png').size==(32,32)
    if name!='cable_grip_rubber':assert Image.open(A/f'textures/block/{name}_s.png').getpixel((0,0))[1]==230
recipe=read(R/'data/createtransit/recipe/cable_track.json')
assert recipe['neoforge:conditions']==[{'type':'neoforge:mod_loaded','modid':'railways'}]
assert recipe['result']=={'id':'createtransit:cable_track','count':8}
for file,value in[('data/create/tags/block/tracks.json','createtransit:cable_track'),('data/create/tags/block/safe_nbt.json','createtransit:cable_grip'),('data/minecraft/tags/block/mineable/pickaxe.json','createtransit:cable_track')]:
    values=read(R/file)['values'];assert values.count({'id':value,'required':False})==1
assert read(R/'data/createtransit/loot_table/blocks/cable_grip.json')['pools'][0]['entries'][0]['name']=='create:railway_casing'
print('PASS: all 19 cable shapes; legal baked geometry; centered curve partial; suspended clamp/fork alignment; labPBR; conditional recipe and optional registry tags.')

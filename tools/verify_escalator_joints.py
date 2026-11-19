"""Check joint edge coordinates in generated meshes and smart blockstate coverage."""
from pathlib import Path
import itertools,json,math
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/createtransit'

def vertices(name,center,offset=(0,0,0)):
    document=json.loads((A/'models/block'/f'escalator_{name}.json').read_text())
    selected=set()
    for q in document['quads']:
        if q['texture']!='#rubber':continue
        for x,y,z in q['vertices']:
            if abs(z-center[0])<.5 and abs(y-center[1])<=1.001:
                selected.add(tuple(round(v+o,6) for v,o in zip((x,y,z),offset)))
    assert len(selected)==4,(name,len(selected))
    return selected

checked=0
for material,side in itertools.product(('glass','solid'),('left','right')):
    lower=vertices(f'lower_landing_{material}_{side}_slope_join',(0,18),(0,0,16))
    low_slope=vertices(f'incline_{material}_{side}',(16,18))
    upper=vertices(f'upper_landing_{material}_{side}_slope_join',(16,18),(0,16,-16))
    high_slope=vertices(f'incline_{material}_{side}',(0,34))
    assert lower==low_slope,('lower mismatch',material,side,lower,low_slope)
    assert upper==high_slope,('upper mismatch',material,side,upper,high_slope)
    # Ordinary travelator landing joints stay flat.
    flat=vertices(f'flat_{material}_{side}',(0,18),(0,0,16))
    upper_flat=vertices(f'upper_landing_{material}_{side}',(16,18))
    assert flat==upper_flat,('flat mismatch',material,side)
    checked+=3
    lower_through=vertices(f'lower_landing_{material}_{side}_through_slope_join',(0,18),(0,0,16))
    upper_through=vertices(f'upper_landing_{material}_{side}_through_slope_join',(16,18),(0,16,-16))
    assert lower_through==low_slope,('continuous lower slope mismatch',material,side)
    assert upper_through==high_slope,('continuous upper slope mismatch',material,side)
    assert vertices(f'lower_landing_{material}_{side}_through_slope_join',(16,18))==flat,('continuous lower flat mismatch',material,side)
    assert vertices(f'upper_landing_{material}_{side}_through_slope_join',(0,18))==vertices(f'flat_{material}_{side}',(16,18),(0,0,-16)),('continuous upper flat mismatch',material,side)
    checked+=4
    assert vertices(f'flat_{material}_{side}_start',(0,18),(0,0,16))==vertices(f'flat_{material}_{side}',(16,18)),('flat entrance body joint',material,side)
    assert vertices(f'flat_{material}_{side}_end',(16,18))==flat,('flat exit body joint',material,side)
    for suffix in ('_start','_end','_both'):
        doc=json.loads((A/'models/block'/f'escalator_flat_{material}_{side}{suffix}.json').read_text())
        points=[v for q in doc['quads'] if q['texture']=='#rubber' for v in q['vertices']]
        if suffix in ('_start','_both'):assert any(z>13 and y<=3 for x,y,z in points),'Entrance rail must curve down into its return housing'
        if suffix in ('_end','_both'):assert any(z<3 and y<=3 for x,y,z in points),'Exit rail must curve down into its return housing'
    checked+=2
    if material=='glass':
        for kind,extra,ends in [('flat','',(0,16)),('incline','',(0,16)),('lower_landing','_through',(0,16)),('upper_landing','_through',(0,16))]:
            pane=json.loads((A/'models/block'/f'escalator_{kind}_{material}_{side}{extra}_pane.json').read_text())
            assert not any(all(abs(v[2]-z)<1e-8 for v in q['vertices']) for q in pane['quads'] for z in ends),('No overlapping glass face at an internal join',kind,side)

states=0
for material,powered in itertools.product(('glass','solid'),(False,True)):
    name=f'escalator_{material}'+('_redstone' if powered else '')
    document=json.loads((A/'blockstates'/f'{name}.json').read_text())
    for facing,shape,left,right,joined,flat_join,stopped in itertools.product(('north','east','south','west'),('lower_landing','incline','upper_landing','flat'),('false','true'),('false','true'),('false','true'),('false','true'),('false','true')):
        props=dict(facing=facing,shape=shape,left=left,right=right,slope_join=joined,flat_join=flat_join,stopped=stopped)
        models=[rule['apply']['model'] for rule in document['multipart'] if all(props[k]==v for k,v in rule['when'].items())]
        documents={m:json.loads((A/'models'/(m.split(':')[1]+'.json')).read_text()) for m in models}
        def selected(source):
            expected=json.loads((A/'models/block'/f'escalator_{source}.json').read_text())
            return [m for m in models if documents[m]==expected]
        assert len(selected(shape+'_base'))==1,(name,props,models)
        for side in ('left','right'):
            wanted=0 if props[side]=='true' else 1
            terminal=shape in ('lower_landing','upper_landing')
            through='_through' if terminal and flat_join=='true' else ''
            if shape=='flat':through={('true','true'):'',('false','true'):'_start',('true','false'):'_end',('false','false'):'_both'}[(flat_join,joined)]
            slope='_slope_join' if terminal and joined=='true' else ''
            off=powered and stopped=='true'
            casing=selected(f'{shape}_{material}_{side}'+through+slope+('_off' if off else ''))
            assert len(casing)==wanted,(name,props,side,casing)
            if casing:
                assert casing[0].endswith('_off')==off,(name,props,casing)
                path=A/'models'/(casing[0].split(':')[1]+'.json')
                doc=json.loads(path.read_text());light_texture='#trim_off' if off else '#led'
                trim=[q for q in doc['quads'] if q['texture']==light_texture]
                assert 8<=len(trim)<=12 and all(q['tint']==1 for q in trim),(name,props,'inner and outer trim',trim)
                if off:assert not any(q['texture']=='#led' for q in doc['quads'])
                if off:
                    lit=json.loads(path.with_name(path.stem.removesuffix('_off')+'.json').read_text())
                    assert doc['quads']==[{**q,'texture':'#trim_off' if q['texture']=='#led' else q['texture']} for q in lit['quads']],('off state must preserve geometry',path)
            if material=='glass':assert len(selected(f'{shape}_{material}_{side}'+through+'_pane'))==wanted
        for m in models:assert (A/'models'/(m.split(':')[1]+'.json')).is_file()
        states+=1
assert Image.open(A/'textures/block/escalator_trim_off_s.png').getchannel('A').getextrema()==(0,0),'Unlit trim must have zero PBR emission'
assert Image.open(A/'textures/block/escalator_led_s.png').getchannel('A').getextrema()==(255,255),'Lit trim retains PBR emission'
print(f'PASS: {checked} handrail joint edge sets match exactly; {states} smart visual states select correct lit/unlit casing and continuous panes; unlit PBR emission is zero.')

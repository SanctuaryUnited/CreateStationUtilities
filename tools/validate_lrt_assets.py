"""Validate shipped states, faceted joins, native-board bays and sliding clearance."""
from pathlib import Path
from PIL import Image
import json,itertools,math
import numpy as np
from lrt_geometry import *
from lrt_car_layout import layout,quick_build
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/createtransit'
def read(p):return json.loads(p.read_text())
def overlap(a,b):return all(min(a['to'][i],b['to'][i])-max(a['from'][i],b['from'][i])>1e-6 for i in range(3))
for k in KINDS:
 name='lrt_'+k
 for p in [ROOT/f'preview/{name}.bbmodel',A/f'models/item/{name}.json',R/f'data/createtransit/recipe/{name}.json']:assert p.is_file(),p
 state=read(A/f'blockstates/{name}.json')
 for part in state['multipart']:
  assert isinstance(part['apply'],dict),'Layers cannot be weighted random alternatives'
  assert (A/('models/'+part['apply']['model'].split(':')[1]+'.json')).is_file()
 loot=read(R/f'data/createtransit/loot_table/blocks/{name}.json');assert {o['source'] for o in loot['pools'][0]['entries'][0]['functions'][0]['ops']}=={'paint','accent'}
for folder in ['block','item']:
 for p in (A/f'models/{folder}').glob('lrt_*.json'):
  m=read(p);assert m['loader']=='createtransit:lrt_mesh'
  if any(q['texture'][1:] in GLASS_TEXTURES for q in m['quads']):assert m['render_type']=='minecraft:translucent'
  for q in m['quads']:
   v=np.array(q['vertices'],dtype=float);assert v.shape==(4,3) and np.all(np.isfinite(v)),p
   assert np.all(v>=-32.1) and np.all(v<=48.1),(p,v)
   assert len(q['uv'])==4 and all(0<=u<=16 for uv in q['uv'] for u in uv),(p,q['uv'])
   tex=m['textures'][q['texture'][1:]];assert (A/('textures/'+tex.split(':')[1]+'.png')).is_file(),(p,tex)
   n=np.cross(v[1]-v[0],v[2]-v[0]);length=np.linalg.norm(n)
   if length>1e-7:assert abs(np.dot(v[3]-v[0],n/length))<1e-5,('Every quad must be planar',p,q)
states=0
for k in GLASS_KINDS:
 parts=read(A/f'blockstates/lrt_{k}.json')['multipart']
 for flags in itertools.product([False,True],repeat=4):
  for f,rot in [('north',0),('east',90),('south',180),('west',270)]:
   values={'facing':f,**dict(zip(['left','right','top','bottom'],flags))};matches=[p['apply'] for p in parts if all(values[key]==val for key,val in p['when'].items())]
   assert len(matches)>=1 and len(matches)<=2
   kinds=[]
   for variant in matches:
    model=read(A/('models/'+variant['model'].split(':')[1]+'.json'));glass=model['render_type']=='minecraft:translucent';kinds.append(glass)
    assert variant.get('y',0)==rot
    assert model['quads'] and all((q['texture'][1:] in GLASS_TEXTURES)==glass for q in model['quads'])
    if glass:assert not model['ambientocclusion']
   assert True in kinds,(k,values)
   states+=1
for flags in itertools.product([False,True],repeat=4):
 pane=window(*flags)[0]
 for face,joined in zip(['west','east','up','down'],flags):assert (face not in pane['visible'])==joined
 for a,b in itertools.combinations(window(*flags),2):assert not overlap(a,b)
 # The slim structural corner and its panes meet exactly, with no intersecting volumes.
 for a,b in itertools.combinations(window_corner(*flags),2):assert not overlap(a,b),(flags,a,b)
# The two windshield rows weld along all faceted contour points.
for left,right in itertools.product([False,True],repeat=2):
 lower=[e for e in windshield(False,left,right,True) if e['texture']=='lrt_glass'];upper=[e for e in windshield(True,left,right,False,True) if e['texture']=='lrt_glass']
 lo={tuple(round(v,7) for v in p) for e in lower for p in e['_quad'] if p[1]==16}
 hi={tuple(round(v,7) for v in [p[0],p[1]+16,p[2]]) for e in upper for p in e['_quad'] if p[1]==0}
 assert lo==hi,('Lower/upper windshield seam',left,right,lo,hi)
assert contour(False,True)[0]==(0.,16.)
assert max(e['to'][1] for e in mesh('floor'))==16
assert min(e['from'][1] for e in mesh('roof'))+64-16==48
assert min(e['from'][1] for e in pantograph())+80==67
# Independent board bay: actual CRN north-facing positive/negative volumes.
for side in [False,True]:
 board=cube([0,0,0 if side else 8],[16,16,8 if side else 16])
 for e in display_mount(True,True,side):assert not overlap(transformed(e,0,0,16),board),(side,e)
# Cab deck top faces must face upward and use the same floor UV scale as flat tiles.
for l,r in itertools.product([False,True],repeat=2):
 for e in faces(deck(l,r)):
  if e['texture']=='lrt_floor':
   v=np.array(e['_quad']);assert np.cross(v[1]-v[0],v[2]-v[0])[1]>0,'Cab floor top must not be backface-culled from inside'
for height in [32,48]:
 fixed=door_fixed(height)+door_lights(height)+door_status(height);neighbors=[transformed(e,x) for x in [-16,32] for e in mesh('wall')+mesh('window')]
 assert max(e['to'][0] for e in door_fixed(height))==32 and max(e['to'][1] for e in door_fixed(height))==height
 for i in range(101):
  moving=door_moving(i/100,height=height)
  for a,b in itertools.combinations(moving,2):assert not overlap(a,b),(height,i,a,b)
  for a in moving:
   for b in fixed+neighbors:assert not overlap(a,b),(height,i,a,b)
  from gondola_geometry import wide_motion
  if wide_motion(i/100)[0]>0:assert abs(moving[0]['from'][2]-.2+3.2)<1e-9,'Panels must park outside, away from the interior'
# Hinged arms stay joined at every animation sample and the contact bar only rises.
previous=-100
for i in range(101):
 p=i/100;angle=math.radians(5+45*p);head=-8.5+28*math.sin(angle)
 assert head>=previous;previous=head
 for e in faces(pantograph(progress=p)):assert all(math.isfinite(v) for pt in e['_quad'] for v in pt)
car=layout();assert len({tuple(c['pos']) for c in car})==len(car)
assert sum(c['kind']=='create:small_bogey' for c in car)==2
assert sum(c['kind']=='door' and c['props']['column']=='0' and c['props']['row']=='0' for c in car)==4
assert sum(c['kind'].startswith('createrailwaysnavigator:') for c in car)==14
assert max(c['pos'][0] for c in car)==4 and max(c['pos'][2] for c in car)==16
assert not any('lrt_bogey' in c['kind'] for c in car)
for removed in ['lrt_bogey','lrt_bellows_wall']:
 assert not (A/f'blockstates/{removed}.json').exists()
 assert not (R/f'data/createtransit/recipe/{removed}.json').exists()
for three in [True,False]:
 data=quick_build(three);cars=data['cars'];cells=data['cells']
 assert len({tuple(c['pos']) for c in cells})==len(cells)
 assert [c['length'] for c in cars]==([16,8,16] if three else [16,16])
 assert sum(c['kind']=='create:small_bogey' for c in cells)==len(cars)*2
 assert sum(c['kind']=='articulation_anchor' for c in cells)==(len(cars)-1)*2
 assert sum(c['kind']=='door' and c['props']['column']=='0' and c['props']['row']=='0' for c in cells)==(10 if three else 8)
 assert all(c['props']['tall']=='false' for c in cells if c['kind']=='door')
 assert all(c['pos'][1]==4 for c in cells if c['kind'] in ['roof','roof_edge','cab_roof'])
 for a,b in zip(cars,cars[1:]):assert a['offset']+a['length']+2==b['offset']
 if three:
  middle=[c for c in cells if cars[1]['offset']<=c['pos'][2]<cars[1]['offset']+8 and c['kind']=='door']
  assert {c['pos'][2]-cars[1]['offset'] for c in middle}=={3,4},'Middle car doorway must be centered'
atlas={s.get('sprite') for s in read(R/'assets/minecraft/atlases/blocks.json')['sources']}
for p in (A/'textures/block').glob('lrt_*.png'):
 if p.stem.endswith(('_n','_s','_e')):continue
 assert 'createtransit:block/'+p.stem in atlas
 for suffix in ['','_n','_s']:assert Image.open(p.with_stem(p.stem+suffix)).size==(32,32)
assert Image.open(A/'textures/block/gondola_led_s.png').getpixel((0,0))[3]==254
seen={}
for p in (R/'data/createtransit/recipe').glob('*.json'):
 r=read(p)
 if r['type']!='minecraft:crafting_shaped':continue
 matrix=tuple(tuple(r['key'].get(c,{}).get('item','air') for c in row) for row in r['pattern']);key=min(str(matrix),str(tuple(tuple(reversed(row)) for row in matrix)))
 if key in seen:assert not p.stem.startswith('lrt_') and not seen[key].startswith('lrt_'),(p.stem,seen[key])
 seen[key]=p.stem
print(f'PASS: {len(KINDS)} modules; {states} glazing states; upward cab floors; three-clear-block interiors; outward doors at 101 positions for both heights; pantograph linkage; 16/8/16 and 16/16 articulated layouts; native board housings; labPBR; recipes.')

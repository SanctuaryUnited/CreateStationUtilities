from pathlib import Path
from PIL import Image
import json,itertools,subprocess
R=Path(__file__).resolve().parents[1]/'src/main/resources';A=R/'assets/createtransit'
for p in R.rglob('*.json'):json.loads(p.read_text())
for block in ['platform_screen_door','platform_gate','platform_wall','platform_barrier','compact_screen_door','compact_wall']:
 data=json.loads((A/f'blockstates/{block}.json').read_text());parts=data['multipart']
 assert len(parts)=={'platform_screen_door':24,'platform_gate':16,'compact_screen_door':16,'platform_wall':356,'platform_barrier':240,'compact_wall':240}[block],(block,len(parts))
 for part in parts:
  assert (A/'models'/Path(part['apply']['model'].split(':')[1]+'.json')).is_file(),part
  assert set(part['when']) in [{'facing','column','row','left','right','panel'},{'facing','row','corner','panel'},{'facing','column','row'}]

for p in (A/'models').rglob('*.json'):
 if not p.stem.startswith(('platform_','compact_','display_')):continue
 data=json.loads(p.read_text());textures=data.get('textures',{})
 for name,loc in textures.items():
  if loc.startswith('createtransit:'):assert (A/'textures'/Path(loc.split(':')[1]+'.png')).is_file(),(p,loc)
 for e in data.get('elements',[]):
  assert all(a<=b for a,b in zip(e['from'],e['to'])),p
  assert all(-16<=v<=32 for v in e['from']+e['to']),p
  for f in e['faces'].values():
   if f['texture'].startswith('#'):assert f['texture'][1:]in textures,(p,f)
for name in ['frame','accent','glass','led_accent','led_white','led_green','led_amber','led_off']:
 files=[A/f'textures/block/{name}{suffix}.png'for suffix in ['', '_n','_s']]
 images=[Image.open(p)for p in files];assert all(im.size==(32,32)for im in images)
 assert all(im.mode=='RGBA'for im in images)
 alpha=images[2].getchannel('A').getextrema();assert alpha==((254,254)if name in ['led_accent','led_white','led_green','led_amber']else(0,0)),(name,alpha)
 normal=images[1].getpixel((0,0));assert normal==(128,128,255,255)
 if name=='frame':assert images[2].getpixel((0,0))[1]==232
atlas=json.loads((R/'assets/minecraft/atlases/blocks.json').read_text())
assert {'createtransit:block/'+n for n in ['led_accent','led_white','led_green','led_amber','led_off']}<={e['sprite']for e in atlas['sources']}
glass=Image.open(A/'textures/block/glass.png')
assert all(glass.getpixel((31,y))==glass.getpixel((0,(y+1)%32))for y in range(32))
p=A/'sounds/door_close_chime.ogg';assert p.stat().st_size>5000
r=subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','default=noprint_wrappers=1:nokey=1',str(p)],text=True);assert 1.3<float(r)<1.4
for name in ['full_height','half_height','two_block_height']:
 bb=json.loads((R.parents[2]/f'preview/{name}.bbmodel').read_text());assert bb['animations']and any(isinstance(x,dict)and x['name']=='left_leaf'for x in bb['outliner'])
from panel_geometry import stationary,leaf,lights,element,JAMB,SLIDE,size,transition_trim
# Verify all closed meshes and the moving/fixed channel throughout travel.
def overlap(a,b):return all(min(a['to'][i],b['to'][i])-max(a['from'][i],b['from'][i])>1e-7 for i in range(3))
def translated(e,x,y=0):
 return {**e,'from':[e['from'][0]+x,e['from'][1]+y,e['from'][2]],'to':[e['to'][0]+x,e['to'][1]+y,e['to'][2]]}
for full,door,col,row,left,right in itertools.product([False,True,32],[False,True],range(4),range(3),[False,True],[False,True]):
 mesh=stationary(full,door,col,row,left,right,leds=True)
 if door and col>1:assert not mesh
 for a,b in itertools.combinations(mesh,2):assert not overlap(a,b),(full,door,col,row,a,b)
 for e in mesh:
  assert 0<=e['from'][2]<e['to'][2]<=3,(full,door,e)
  if e['texture']=='glass':
   assert e['visible']==['north','south']
   for f in element(e)['faces'].values():assert all(0<=u<=16 for u in f['uv'])
for full in [False,True,32]:
 moving=leaf(full);assert not any(e['texture'].startswith('led_')for e in moving)
 for a,b in itertools.combinations(moving,2):assert not overlap(a,b),(full,a,b)
 lamps=lights(full,True);accent=[e for e in lamps if e['texture']=='led_accent'];status=[e for e in lamps if e['texture']=='led_white']
 assert len(accent)==4 and all(e['tint']==1 and e['from'][1]==5 and e['to'][1]==size(full)[1]-2 for e in accent)
 assert all(e['from'][1]>=(size(full)[1]if size(full)[0]>=32 else 22)and e['tint']==-1 for e in status)
 assert {e['from'][0]for e in accent}=={.45,30.95}
 fixed=[]
 for col in [-1,0,1,2]:
  isdoor=col in [0,1]
  for row in range((size(full)[0]+15)//16):
   for e in stationary(full,isdoor,col if isdoor else 0,row,left=col==2,right=col==-1,leds=True):fixed.append(translated(e,col*16,row*16))
 for t in [0,.1,.25,.5,.75,.9,1]:
  panes=[translated(e,(JAMB-t*SLIDE)if side==0 else (16+t*SLIDE))for side in [0,1]for e in moving]
  for a,b in itertools.combinations(panes,2):assert not overlap(a,b),(full,t,a,b)
  for a in panes:
   for b in fixed:assert not overlap(a,b),(full,t,a,b)
 # Connected wall glass meets the block boundary with a seamless repeating UV phase.
 a=next(e for e in stationary(full,False,0,0,right=True)if e['texture']=='glass')
 b=next(e for e in stationary(full,False,0,0,left=True)if e['texture']=='glass')
 assert a['to'][0]==16 and b['from'][0]==0
 for name in ['compact_screen_door' if full==32 else 'platform_screen_door' if full else 'platform_gate']:
  data=json.loads((A/f'models/item/{name}.json').read_text());assert data['elements']
# Compact resources never enter the display cell (y >= 32).
for door in [False,True]:
 mesh=[translated(e,col*16,row*16)for col in range(2 if door else 1)for row in range(2)for e in stationary(32,door,col,row,leds=True)]
 assert mesh and all(e['to'][1]<=32 for e in mesh)
 for e in lights(32,door)+leaf(32):assert e['to'][1]<=32
# Shared glass joins up to y=28; exposed tall trim starts exactly at that height.
for side in ['left','right']:
 trim=transition_trim(side);assert all(e['from'][1]==28 and e['to'][1]==32 for e in trim)
 for row in range(3):
  for e in stationary(True,False,0,row,left=side=='left' and row<2,right=side=='right' and row<2):
   for t in trim:assert not overlap(translated(e,0,row*16),t)
print('PASS: three heights, compact display clearance, mixed-height transition trim, two-column footprints, 0..3px edge alignment, fixed accent-tinted side LEDs, top-only status lights, sliding-channel clearance, non-overlapping meshes, connected wall UVs, PBR/atlas assets and piano audio.')
# Custom panels retain the existing leaf channel in every height and animation position.
from panel_geometry import corner_return
for full,panel in itertools.product([24,32,48],range(3)):
 moving=leaf(full,panel)
 assert any(e['texture']=='glass' for e in moving)==(panel!=2)
 for a,b in itertools.combinations(moving,2):assert not overlap(a,b),(full,panel,'moving',a,b)
 for row in range((full+15)//16):
  front=stationary(full,False,0,row,panel=panel)
  assert any(e['texture']=='glass' for e in front)==(panel!=2 and row*16<size(full)[1]-2)
  for side in [1,2]:
   wing=corner_return(full,row,side,panel)
   for e in wing:assert 3<=e['from'][2]<e['to'][2]<=16
   for a,b in itertools.combinations(front+wing,2):assert not overlap(a,b),(full,panel,'corner',a,b)
 fixed=[translated(e,col*16,row*16) for col in [-1,0,1,2] for row in range((full+15)//16) for e in stationary(full,col in [0,1],col if col in [0,1] else 0,row,left=col==2,right=col==-1,panel=panel)]
 for t in [0,.25,.5,.75,1]:
  for side in [0,1]:
   for e in moving:
    a=translated(e,(JAMB-t*SLIDE) if side==0 else (16+t*SLIDE))
    for b in fixed:assert not overlap(a,b),(full,panel,t,a,b)
print('PASS: clear/divided/solid meshes at all heights; both corner returns, opaque solid panels and sliding-channel clearance.')

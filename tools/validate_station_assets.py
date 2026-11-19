from pathlib import Path
from PIL import Image
import json,math,itertools,subprocess
import numpy as np
from station_geometry import *
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
def overlap(a,b):return all(min(a['to'][i],b['to'][i])-max(a['from'][i],b['from'][i])>1e-6 for i in range(3))
for mesh in [gate_fixed(),gate_leaf(),kiosk_fixed()]:
 for a,b in itertools.combinations(mesh,2):assert not overlap(a,b),(a,b)
def rect(e,pivot,angle,mirror=False):
 coords=np.array([[e[k][0],e[k2][2]-8]for k,k2 in [('from','from'),('to','from'),('to','to'),('from','to')]])
 if mirror:coords[:,0]*=-1
 c,s=math.cos(angle),math.sin(angle);coords=coords@np.array([[c,s],[-s,c]])
 coords+=np.array([pivot,8]);return coords
def intersects(a,b):
 for polygon in [a,b]:
  for i in range(4):
   edge=polygon[(i+1)%4]-polygon[i];axis=np.array([-edge[1],edge[0]])
   if np.linalg.norm(axis)<1e-9:continue
   pa,pb=a@axis,b@axis
   if min(pa.max(),pb.max())-max(pa.min(),pb.min())<=1e-6:return False
 return True
for degrees in range(0,91,3):
 moving=[(e,rect(e,16,math.radians(degrees)))for e in gate_leaf()]
 for e,r in moving:
  for f in gate_fixed()+[transformed(e,32)for e in gate_fixed()]:
   if min(e['to'][1],f['to'][1])-max(e['from'][1],f['from'][1])<=1e-6:continue
   a,b=f['from'],f['to'];fixed=np.array([[a[0],a[2]],[b[0],a[2]],[b[0],b[2]],[a[0],b[2]]]);assert not intersects(r,fixed),(degrees,e,f)
for name,width in [('fare_gate',3),('fare_reader',1),('ticket_kiosk',1)]:
 states=json.loads((A/f'blockstates/{name}.json').read_text());assert len(states['multipart'])==4*width*2
 for p in states['multipart']:assert(A/('models/'+p['apply']['model'].split(':')[1]+'.json')).is_file()
 bb=json.loads((ROOT/f'preview/{name}.bbmodel').read_text());assert bb['elements']and bb['textures']
 if name=='fare_gate':assert all(e['to'][0]<=32 for e in bb['elements']),"New gate geometry occupies only two columns"
 if name=='fare_reader':assert all(e['to'][0]<=16 for e in bb['elements']),"End reader geometry occupies one column"
 for p in (A/'models/block').glob(name+'*.json'):
  data=json.loads(p.read_text())
  for e in data.get('elements',[]):
   assert all(-16<=v<=32 for v in e['from']+e['to'])
   if 'rotation'in e:assert e['rotation']['angle']in [-45,-22.5,0,22.5,45],(p,e)
   for face in e['faces'].values():assert face['texture'][1:]in data['textures']
 for texture in ['led_red','fare_arrow','fare_stop','station_dark','kiosk_screen','kiosk_sign','kiosk_reader','ticket_paper']:
  im=Image.open(A/f'textures/block/{texture}.png')
  for suffix in ['_n','_s']:assert Image.open(A/f'textures/block/{texture}{suffix}.png').size==im.size
assert all(e['tint']==-1 for e in gate_lights()if e['texture']in ['fare_arrow','fare_stop','led_green','led_red'])
assert all(e['tint']==1 for e in gate_lights()+kiosk_lights()if e['texture']=='led_accent')
duration=float(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration','-of','default=noprint_wrappers=1:nokey=1',str(A/'sounds/fare_gate_chime.ogg')],text=True));assert .6<duration<.7
print('PASS: fare/kiosk model references, legal Minecraft rotations, original textures/PBR companions, separate dye channels, fixed direction colors, non-overlapping bodies and swing clearance at every 3 degrees, 0.65-second chime.')

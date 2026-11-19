"""Native structure NBT for independent glass / solid three-stop modular elevators."""
from pathlib import Path
import gzip,json
from export_lrt_circuit import nbt,string
ROOT=Path(__file__).resolve().parents[1]
DIRS=['north','east','south','west']
def cabin(glass=True,through=False):
 cells={}
 def add(x,y,z,name,f='north',extra={}):cells[x,y,z]={'name':'createtransit:elevator_'+name,'props':{'facing':f,**extra}}
 for x in range(3):
  for z in range(3):add(x,0,z,'floor');add(x,4,z,'ceiling_light' if (x,z)==(1,1) else 'roof')
 for y in range(1,4):
  for x,z,f in [(0,0,'north'),(2,0,'east'),(0,2,'west'),(2,2,'south')]:add(x,y,z,'glass_corner' if glass else 'solid_corner',f)
  for x,z,f in [(1,0,'north'),(0,1,'west'),(2,1,'east')]:add(x,y,z,'glass_wall' if glass else 'solid_wall',f)
  if y<=2:add(1,y,2,'glass_door' if glass else 'solid_door','south',{'row':str(y-1),'open':'false'})
  else:add(1,y,2,'door_header','south')
 if through:
  for row in range(2):add(1,1+row,0,'glass_door' if glass else 'solid_door','north',{'row':str(row),'open':'false'})
  add(1,3,0,'door_header','north')
  add(2,2,1,'control_panel','east')
 else:add(1,2,0,'control_panel','north')
 for (x,y,z),c in cells.items():
  if 'glass_wall' not in c['name'] and 'glass_corner' not in c['name']:continue
  f=c['props']['facing'];i=DIRS.index(f);delta=[(0,-1),(1,0),(0,1),(-1,0)]
  def plane(q):
   n=cells.get(q)
   return n is not None and ('glass_wall' in n['name']or 'glass_corner'in n['name'])and(n['props']['facing']==f or 'glass_corner'in n['name']and DIRS[(DIRS.index(n['props']['facing'])-1)%4]==f)
  for k,d in [('left',delta[(i-1)%4]),('right',delta[(i+1)%4])]:c['props'][k]=str(plane((x+d[0],y,z+d[1]))).lower()
  for k,dy in [('up',1),('down',-1)]:n=cells.get((x,y+dy,z));c['props'][k]=str(n is not None and n['name']==c['name'] and n['props']['facing']==f).lower()
 solid={'createtransit:elevator_'+n for n in ['solid_wall','solid_corner','control_panel','door_header']}
 for (x,y,z),c in cells.items():
  if c['name'] not in solid:continue
  for key,dy in [('up',1),('down',-1)]:
   n=cells.get((x,y+dy,z));c['props'][key]=str(n is not None and n['props']['facing']==c['props']['facing'] and (n['name'] in solid or key=='down' and n['name'] in ['createtransit:elevator_glass_door','createtransit:elevator_solid_door'])).lower()
 return cells

def demo(glass=True,through=False):
 cells={}
 def add(p,name,props={}):cells[p]={'name':name,'props':props}
 for x in range(9):
  for z in range(10):add((x,0,z),'minecraft:gray_concrete')
 for p,c in cabin(glass,through).items():cells[tuple(p[i]+[2,2,2][i]for i in range(3))]=c
 drive=(1,2,3) if through else (3,2,1)
 facing='east' if through else 'south'
 add(drive,'createtransit:elevator_drive',{'facing':facing})
 for y in range(3,31):add((drive[0],y,drive[2]),'createtransit:elevator_guide_rail',{'facing':facing})
 for y in [2,14,26]:
  rear=through and y==14
  for x in range(9):
   for z in (range(2) if rear else range(5,10)):add((x,y,z),'minecraft:smooth_stone')
  z=1 if rear else 5
  for row in range(2):add((3,y+1+row,z),'createtransit:elevator_'+('glass'if glass else 'solid')+'_landing_door',{'facing':'south' if rear else 'north','row':str(row),'open':'false'})
  add((5,y+1,z),'createtransit:elevator_floor_call',{'facing':'south' if rear else 'north','powered':'false'})
  add((5,y+1,z+(-1 if rear else 1)),'minecraft:gray_concrete')
 return cells

def write(glass,through=False):
 cells=demo(glass,through);pal=[];indexes={};blocks=[]
 for p,c in sorted(cells.items()):
  key=(c['name'],tuple(sorted(c['props'].items())))
  if key not in indexes:
   indexes[key]=len(pal);state={'Name':(8,c['name'])}
   if c['props']:state['Properties']=(10,{k:(8,v)for k,v in c['props'].items()})
   pal.append(state)
  block={'pos':(9,(3,list(p))),'state':(3,indexes[key])}
  if c['name'].startswith('createtransit:elevator_'):block['nbt']=(10,{'id':(8,'createtransit:elevator_drive' if c['name'].endswith('_drive')else 'createtransit:elevator_part'),'x':(3,p[0]),'y':(3,p[1]),'z':(3,p[2]),'paint':(3,0),'accent':(3,3)})
  blocks.append(block)
 data={'DataVersion':(3,3955),'size':(9,(3,[9,32,10])),'palette':(9,(10,pal)),'blocks':(9,(10,blocks)),'entities':(9,(10,[]))}
 raw=b'\x0a'+string('')+nbt(10,data);name='Elevator-'+('Glass'if glass else 'Solid')+('-Through' if through else '')+'-3x3-Three-Floors';path=ROOT/'schematics'/f'{name}.nbt';path.parent.mkdir(exist_ok=True);tmp=path.with_suffix('.tmp');tmp.write_bytes(gzip.compress(raw,mtime=0));tmp.replace(path)
 (ROOT/'schematics'/f'{name}.json').write_text(json.dumps({'size':[9,32,10],'cabin_size':[3,5,3],'floors':[2,14,26],'blocks':len(blocks),'cabin_blocks':len(cabin(glass,through)),'drive':[1,2,3] if through else [3,2,1],'landing_sides':['front','rear','front'] if through else ['front']*3,'note':'Own drive; no Create pulley or rotational power. Right click drive then select a floor.'},indent=2)+'\n')
 print(name,len(blocks),'blocks')
if __name__=='__main__':
 write(True);write(False);write(True,True);write(False,True)

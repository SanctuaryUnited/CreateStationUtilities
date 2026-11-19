"""Three enabled rail-marker stops; legacy QuickBuilds remain untouched."""
from pathlib import Path
import gzip,json
from export_elevator_kit import demo,cabin
from export_lrt_circuit import nbt,string
ROOT=Path(__file__).resolve().parents[1]
for glass in [True,False]:
 cells=demo(glass,True);drive=(1,2,3);pal=[];indexes={};blocks=[]
 for floor,label in [(2,'F1'),(14,'F2'),(26,'F3')]:
  cells[(drive[0],floor+1,drive[2])]={'name':'createtransit:elevator_floor_marker_rail','props':{'facing':'east'},'floor':label}
 for pos,cell in sorted(cells.items()):
  name=cell['name'];props=cell['props'].copy()
  if name in ['createtransit:elevator_guide_rail','createtransit:elevator_floor_marker_rail']:
   props['up']=str((pos[0],pos[1]+1,pos[2])in cells and cells[pos[0],pos[1]+1,pos[2]]['name'] in ['createtransit:elevator_guide_rail','createtransit:elevator_floor_marker_rail']).lower()
   props['down']='true'
  if name=='createtransit:elevator_floor_call':props['requested']='false'
  key=(name,tuple(sorted(props.items())))
  if key not in indexes:
   indexes[key]=len(pal);state={'Name':(8,name)}
   if props:state['Properties']=(10,{k:(8,v)for k,v in props.items()})
   pal.append(state)
  block={'pos':(9,(3,list(pos))),'state':(3,indexes[key])}
  if name.startswith('createtransit:elevator_'):
   tag={'id':(8,'createtransit:elevator_drive'if name.endswith('_drive')else'createtransit:elevator_part'),'x':(3,pos[0]),'y':(3,pos[1]),'z':(3,pos[2]),'paint':(3,0),'accent':(3,3)}
   if 'floor' in cell:tag.update({'FloorId':(8,cell['floor']),'FloorName':(8,'Floor '+cell['floor'][1:]),'FloorEnabled':(1,1)})
   block['nbt']=(10,tag)
  blocks.append(block)
 data={'DataVersion':(3,3955),'size':(9,(3,[9,32,10])),'palette':(9,(10,pal)),'blocks':(9,(10,blocks)),'entities':(9,(10,[]))}
 name='Elevator2-'+('Glass'if glass else'Solid')+'-F1-F2-F3'
 (ROOT/'schematics'/(name+'.nbt')).write_bytes(gzip.compress(b'\x0a'+string('')+nbt(10,data),mtime=0))
 (ROOT/'schematics'/(name+'.json')).write_text(json.dumps({'size':[9,32,10],'cabin_size':[3,5,3],'drive':list(drive),'floors':[{'id':label,'floor_y':y,'marker_y':y+1,'enabled':True}for y,label in[(2,'F1'),(14,'F2'),(26,'F3')]],'landing_sides':['front','rear','front'],'note':'Dedicated guide rail markers. Right-click the drive to assemble/open the controller; passenger panel requests a destination.'},indent=2)+'\n')
 print(name,len(blocks),'blocks')
# Bounded native test area for a 33-marker limit test and close neighboring shafts.
data={'DataVersion':(3,3955),'size':(9,(3,[28,70,24])),'palette':(9,(10,[{'Name':(8,'minecraft:air')}])),'blocks':(9,(10,[{'pos':(9,(3,[0,0,0])),'state':(3,0)}])),'entities':(9,(10,[]))}
p=ROOT/'src/gametest/resources/data/createtransit/structure/elevator2_shaft.nbt';p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(gzip.compress(b'\x0a'+string('')+nbt(10,data),mtime=0))

"""Portable native structure schematics and a blank native GameTest arena."""
from pathlib import Path
import gzip
from export_lrt_car import string,payload
ROOT=Path(__file__).resolve().parents[1]
def write(p,size,cells):
 palette=[];indices={};blocks=[]
 for pos,name,props,data in cells:
  key=(name,tuple(sorted(props.items())))
  if key not in indices:
   indices[key]=len(palette);v={'Name':(8,name)}
   if props:v['Properties']=(10,{k:(8,v)for k,v in props.items()})
   palette.append(v)
  v={'pos':(9,(3,pos)),'state':(3,indices[key])}
  if data:v['nbt']=(10,data)
  blocks.append(v)
 root={'DataVersion':(3,3955),'size':(9,(3,size)),'palette':(9,(10,palette)),'blocks':(9,(10,blocks)),'entities':(9,(10,[]))}
 p.parent.mkdir(parents=True,exist_ok=True)
 with p.open('wb')as f:
  with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0)as z:z.write(b'\x0a'+string('')+payload(10,root))
for glass in [True,False]:
 cells=[];material='glass' if glass else 'solid'
 for lane in range(2):
  for kind,y,z in [('lower_landing',1,8)]+[('incline',1+i,7-i)for i in range(4)]+[('upper_landing',5,3)]:
   cells.append(([lane+1,y,z],'createtransit:escalator_'+kind+'_'+material,{'facing':'north','reversed':'false','stopped':'false','left':str(lane==1).lower(),'right':str(lane==0).lower()},{'id':(8,'createtransit:escalator_section'),'paint':(3,0),'accent':(3,9)}))
   cells.append(([lane+1,y-1,z],'minecraft:polished_andesite',{},None))
 for lane in range(1,3):
  for y,z in [(0,9),(4,2),(4,1)]:cells.append(([lane,y,z],'minecraft:polished_andesite',{},None))
 write(ROOT/'schematics'/f'Escalator-QuickBuild-2-Wide-{material.title()}.nbt',[4,8,11],cells)
cells=[]
for lane in range(3):
 for i in range(10):
  kind='lower_landing' if i==0 else 'upper_landing' if i==9 else 'flat'
  cells.append(([lane+1,1,11-i],'createtransit:escalator_'+kind+'_glass',{'facing':'north','reversed':'false','stopped':'false','left':str(lane>0).lower(),'right':str(lane<2).lower()},{'id':(8,'createtransit:escalator_section'),'paint':(3,0),'accent':(3,9)}))
  cells.append(([lane+1,0,11-i],'minecraft:polished_andesite',{},None))
write(ROOT/'schematics/Travelator-QuickBuild-3-Wide.nbt',[5,4,13],cells)
write(ROOT/'src/gametest/resources/data/createtransit/structure/escalator_strip.nbt',[24,12,24],[([x,0,z],'minecraft:stone',{},None)for x in range(24)for z in range(24)])
print('Exported two escalator builds, travelator build, native test arena.')

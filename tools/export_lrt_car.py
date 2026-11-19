"""Portable Create NBT schematics with native bogeys, CRN boards and separate glue groups."""
from pathlib import Path
import gzip,struct
from lrt_car_layout import layout,quick_build
ROOT=Path(__file__).resolve().parents[1]
def string(s):
 b=s.encode('utf-8');return struct.pack('>H',len(b))+b
def payload(kind,value):
 if kind==1:return struct.pack('>b',value)
 if kind==3:return struct.pack('>i',value)
 if kind==5:return struct.pack('>f',value)
 if kind==6:return struct.pack('>d',value)
 if kind==8:return string(value)
 if kind==9:
  subtype,items=value;return bytes([subtype])+struct.pack('>i',len(items))+b''.join(payload(subtype,v) for v in items)
 if kind==10:return b''.join(bytes([t])+string(k)+payload(t,v) for k,(t,v) in value.items())+b'\0'
 raise ValueError(kind)
def make(data):
 palette=[];indices={};blocks=[];entities=[]
 for c in data['cells']:
  k=c['kind'];name=k if ':' in k else 'createtransit:lrt_'+k;props=dict(c['props'])
  if k!='create:small_bogey':props['facing']=c['facing']
  key=(name,tuple(sorted(props.items())))
  if key not in indices:
   indices[key]=len(palette);p={'Name':(8,name)}
   if props:p['Properties']=(10,{k:(8,v) for k,v in props.items()})
   palette.append(p)
  b={'pos':(9,(3,c['pos'])),'state':(3,indices[key])}
  door=name=='createtransit:lrt_door'
  if name.startswith('createtransit:') and (not door or c['props']['row']=='0'):
   entity=name if door else 'createtransit:lrt_part'
   b['nbt']=(10,{'id':(8,entity),'paint':(3,c.get('paint',0)),'accent':(3,c.get('accent',11))})
  blocks.append(b)
 for car in data['cars']:
  lo,hi=car['glue'];hi=[v+1 for v in hi];center=[(a+b)/2 for a,b in zip(lo,hi)]
  nbt={'id':(8,'create:super_glue'),'Pos':(9,(6,center)),'Motion':(9,(6,[0.,0.,0.])),'Rotation':(9,(5,[0.,0.])),
   'From':(9,(6,[a-b for a,b in zip(lo,center)])),'To':(9,(6,[a-b for a,b in zip(hi,center)]))}
  entities.append({'pos':(9,(6,center)),'blockPos':(9,(3,[int(v) for v in center])),'nbt':(10,nbt)})
 root={'DataVersion':(3,3955),'size':(9,(3,data['size'])),'palette':(9,(10,palette)),'blocks':(9,(10,blocks)),'entities':(9,(10,entities))}
 if 'body_origin' in data:root['CreateTransit']=(10,{'BodyOrigin':(9,(3,data['body_origin'])),'CabExtension':(3,4)})
 raw=b'\x0a'+string('')+payload(10,root);p=ROOT/'schematics'/ (data['name']+'.nbt');p.parent.mkdir(parents=True,exist_ok=True)
 with p.open('wb') as f:
  with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0) as z:z.write(raw)
 print(p.name,len(blocks),'blocks;',len(palette),'states;',len(entities),'separate glue groups;',data['size'])
if __name__=='__main__':
 make({'name':'LRT-B-Single-Car','size':[5,6,17],'cells':layout(),'cars':[{'glue':[[0,0,0],[4,5,16]]}]})
 for three in [True,False]:make(quick_build(three))

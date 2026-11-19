"""Independent structure reader checks topology, serialized targets, support and boarding clearances."""
from pathlib import Path
import json,math,collections
from nbt_read import read
ROOT=Path(__file__).resolve().parents[1]
for name in ['LRT-Test-Circuit-512','LRT-Test-Circuit-512-NoCRN','LRT-Test-Circuit-512-Track-and-Stations']:
 tag=read(ROOT/'schematics'/f'{name}.nbt');meta=json.loads((ROOT/'schematics'/f'{name}.json').read_text());assert tag['size']==[512,16,512]
 cells={tuple(b['pos']):{**tag['palette'][b['state']],'nbt':b.get('nbt',{})} for b in tag['blocks']};assert len(cells)==len(tag['blocks'])==meta['blocks']
 assert all(all(0<=v<limit for v,limit in zip(p,tag['size'])) for p in cells)
 for p,c in cells.items():
  if c['Name']=='create:track':assert cells[p[0],p[1]-1,p[2]]['Name'] in ['minecraft:gray_concrete','minecraft:stone_bricks']
 reciprocal=0
 for p,c in cells.items():
  if c['Name']!='create:track' or c.get('Properties',{}).get('turn')!='true':continue
  curves=c['nbt']['Connections'];assert len(curves)==1
  q=curves[0];ends=[tuple(a+b for a,b in zip(p,entry['Pos'])) for entry in q['Positions']];assert ends[0]==p
  other=cells[ends[1]]['nbt']['Connections'][0];otherends=[tuple(a+b for a,b in zip(ends[1],entry['Pos'])) for entry in other['Positions']]
  assert otherends==ends[::-1] and q['Primary']!=other['Primary'];assert q['Material']=='create:andesite'
  starts=[tuple(a+b for a,b in zip(p,v['V'])) for v in q['Starts']];otherstarts=[tuple(a+b for a,b in zip(ends[1],v['V'])) for v in other['Starts']]
  assert starts==otherstarts[::-1];assert q['Axes']==other['Axes'][::-1];reciprocal+=1
 assert reciprocal==16
 for group,block in [('stations','track_station'),('signals','track_signal'),('observers','track_observer')]:
  for endpoint in meta[group]:
   c=cells[tuple(endpoint['pos'])];assert c['Name']=='create:'+block
   actual=[a+b for a,b in zip(endpoint['pos'],c['nbt']['TargetTrack'])];assert actual==endpoint['target'] and cells[tuple(actual)]['Name']=='create:track'
   assert 'Id' not in c['nbt'],'Bindings must acquire a fresh identity when pasted'
 assert len(meta['stations'])==4 and len(meta['signals'])==16 and len(meta['observers'])==4
 # Exactly two cells above the ground top at y=1; carriage floor top is also y=3.
 for x in [75,78,81,420,422,425,443,446,450]:assert cells[x,2,260]['Name'] in ['minecraft:smooth_stone','minecraft:yellow_concrete']
 for p,c in cells.items():
  if c['Name']=='createtransit:compact_screen_door':
   s=c['Properties'];assert p[1]-int(s['row'])==3
   dz=1 if s['facing']=='east' else -1;master=(p[0],3,p[2]-dz*int(s['column']))
   assert cells[master]['Name']==c['Name']
   for col in range(2):
    for row in range(2):assert cells[master[0],3+row,master[2]+col*dz]['Name']==c['Name']
 # Straight route separation and static collision envelope for five-wide bodies.
 for loop,c in meta['track_centers'].items():
  for z in range(144,369):
   for side in ['west','east']:
    x=c[side]
    for xx in range(x-2,x+3):
     for yy in range(3,9):
      a=cells.get((xx,yy,z),{})
      assert not a or a['Name'].startswith(('createtransit:lrt_','createrailwaysnavigator:')) or a['Name'] in ['create:controls','create:blaze_burner'],(name,(xx,yy,z),a)
 assert meta['track_centers']['inner']['west']-meta['track_centers']['outer']['west']==12
 assert meta['track_centers']['outer']['east']-meta['track_centers']['inner']['east']==12
 # Track graph combinatorics: every straight segment/corner makes a single closed cycle.
 for loop,cs in meta['track_centers'].items():
  graph=collections.defaultdict(set)
  def edge(a,b):graph[a].add(b);graph[b].add(a)
  for z in range(144,368):
   for side in ['west','east']:edge((cs[side],1,z),(cs[side],1,z+1))
  for x in range(144,368):
   for side in ['north','south']:edge((x,1,cs[side]),(x+1,1,cs[side]))
  for c in meta['curves']:
   if c['loop']==loop:edge(tuple(c['ends'][0]),tuple(c['ends'][1]))
  assert all(len(n)==2 for n in graph.values())
  seen=set();pending=[next(iter(graph))]
  while pending:
   q=pending.pop()
   if q in seen:continue
   seen.add(q);pending.extend(graph[q]-seen)
  assert len(seen)==len(graph)==900
 assert len(tag['entities'])== (3 if meta['train'] else 0)
 if not meta['crn']:assert not any(p['Name'].startswith('createrailwaysnavigator:') for p in tag['palette'])
 print('PASS',name,len(cells),'blocks; reciprocal curves, closed loops, fresh target bindings, level boarding, complete doors and five-wide clearance')

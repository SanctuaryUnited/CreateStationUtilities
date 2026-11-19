"""512-square native Create double-track LRT proving circuit; reproducible structure NBT."""
from pathlib import Path
import gzip,struct,json,math,copy
from export_lrt_car import string,payload as old_payload
from lrt_car_layout import quick_build
ROOT=Path(__file__).resolve().parents[1]
S=['north','east','south','west'];F={'north':(0,-1),'east':(1,0),'south':(0,1),'west':(-1,0)}
META={'size':[512,16,512],'track_y':1,'floor_y':2,'platform_top':3,'track_centers':{'outer':{'west':72,'east':440,'north':72,'south':440},'inner':{'west':84,'east':428,'north':84,'south':428}},'stations':[],'signals':[],'observers':[],'curves':[],'train_origin':[70,2,215]}
def nbt(kind,v):
 if kind==11:return struct.pack('>i',len(v))+b''.join(struct.pack('>i',x) for x in v)
 if kind==9:
  sub,items=v;return bytes([sub])+struct.pack('>i',len(items))+b''.join(nbt(sub,x) for x in items)
 if kind==10:return b''.join(bytes([t])+string(k)+nbt(t,v) for k,(t,v) in v.items())+b'\0'
 return old_payload(kind,v)
def vec(v):return {'V':(9,(6,list(map(float,v))))}
def pos(v):return (11,list(map(int,v)))
def curve_nbt(c,local,primary):
 points=c['ends'] if primary else c['ends'][::-1];starts=c['starts'] if primary else c['starts'][::-1];axes=c['axes'] if primary else c['axes'][::-1]
 return {'Positions':(9,(10,[{'Pos':pos([a-b for a,b in zip(q,local)])} for q in points])),
  'Starts':(9,(10,[vec([a-b for a,b in zip(q,local)]) for q in starts])),
  'Axes':(9,(10,[vec(q) for q in axes])),'Normals':(9,(10,[vec([0,1,0]),vec([0,1,0])])),
  'Primary':(1,int(primary)),'Girder':(1,0),'Material':(8,'create:andesite')}
def curve_points(c,n=180):
 a,b=c['starts'];h=c['radius']*4*(math.sqrt(2)-1)/3
 u=[a[i]+c['axes'][0][i]*h for i in range(3)];v=[b[i]+c['axes'][1][i]*h for i in range(3)]
 return [[(1-t)**3*a[i]+3*(1-t)**2*t*u[i]+3*(1-t)*t*t*v[i]+t**3*b[i] for i in range(3)] for t in [j/n for j in range(n+1)]]
def schedule(stops):
 entries=[]
 for name in stops:
  instruction={'Id':(8,'create:destination'),'Data':(10,{'Text':(8,name)})}
  delay={'Id':(8,'create:delay'),'Data':(10,{'Value':(3,12),'TimeUnit':(3,1)})}
  entries.append({'Instruction':(10,instruction),'Conditions':(9,(9,[(10,[delay])]))})
 return {'Cyclic':(1,1),'Entries':(9,(10,entries))}
class Circuit:
 def __init__(self,train=True,crn=True):self.cells={};self.glue=[];self.train=train;self.crn=crn;self.meta=copy.deepcopy(META)
 def add(self,x,y,z,name,props=None,data=None):
  assert 0<=x<512 and 0<=y<16 and 0<=z<512,(x,y,z,name)
  self.cells[x,y,z]={'Name':name,'Properties':props or {},'nbt':data}
 def fill(self,x1,y1,z1,x2,y2,z2,name,props=None):
  for x in range(x1,x2+1):
   for y in range(y1,y2+1):
    for z in range(z1,z2+1):self.add(x,y,z,name,props)
 def sign(self,x,y,z,lines,facing='north'):
  text={'messages':(9,(8,[json.dumps({'text':v,'color':'aqua' if i==0 else 'white'},separators=(',',':')) for i,v in enumerate((lines+['']*4)[:4])])),'color':(8,'white'),'has_glowing_text':(1,1)}
  dx,dz=F[facing]
  if (x-dx,y,z-dz) not in self.cells:self.add(x-dx,y,z-dz,'minecraft:polished_andesite')
  self.add(x,y,z,'minecraft:oak_wall_sign',{'facing':facing,'waterlogged':'false'},{'id':(8,'minecraft:sign'),'front_text':(10,text),'back_text':(10,text),'is_waxed':(1,1)})
 def rail(self,x,z,axis):
  self.fill(x-3,0,z-3,x+3,0,z+3,'minecraft:gray_concrete')
  self.add(x,1,z,'create:track',{'shape':axis+'o','turn':'false','waterlogged':'false'})
 def track(self):
  for loop,cs in self.meta['track_centers'].items():
   for z in range(144,369):
    for side in ['west','east']:self.rail(cs[side],z,'z')
   for x in range(144,369):
    for side in ['north','south']:self.rail(x,cs[side],'x')
   curves=[([(cs['west'],1,144),(144,1,cs['north'])],[(0,0,-1),(-1,0,0)]),
    ([(368,1,cs['north']),(cs['east'],1,144)],[(1,0,0),(0,0,-1)]),
    ([(cs['east'],1,368),(368,1,cs['south'])],[(0,0,1),(1,0,0)]),
    ([(144,1,cs['south']),(cs['west'],1,368)],[(-1,0,0),(0,0,1)])]
   for ends,axes in curves:
    starts=[[p[i]+(.5 if i!=1 else 0)+axis[i]*.5 for i in range(3)] for p,axis in zip(ends,axes)]
    c={'loop':loop,'ends':ends,'axes':axes,'starts':starts,'radius':144-cs['west']-.5};self.meta['curves'].append(c)
    for point in curve_points(c):
     x,z=math.floor(point[0]),math.floor(point[2]);self.fill(x-3,0,z-3,x+3,0,z+3,'minecraft:gray_concrete')
    for j,end in enumerate(ends):
     state=self.cells[tuple(end)]['Properties'].copy();state['turn']='true';self.add(*end,'create:track',state,{'id':(8,'create:track'),'Connections':(9,(10,[curve_nbt(c,end,j==0)]))})
 def wires(self):
  # Block-built visual catenary only: no electrical simulation or train movement requirement.
  wires=set()
  for cs in self.meta['track_centers'].values():
   for t in range(144,369):
    wires.update([(cs['west'],11,t),(cs['east'],11,t),(t,11,cs['north']),(t,11,cs['south'])])
  for c in self.meta['curves']:
   pts=curve_points(c,500)
   last=None
   for v in pts:
    q=(math.floor(v[0]),11,math.floor(v[2]));wires.add(q)
    if last and abs(q[0]-last[0])+abs(q[2]-last[2])>1:wires.add((last[0],11,q[2]))
    last=q
  for x,y,z in wires:self.add(x,y,z,'minecraft:iron_bars',{'waterlogged':'false',**{k:str((x+dx,y,z+dz) in wires).lower() for k,(dx,dz) in F.items()}})
  for t in range(144,369,32):
   for x,lo,hi in [(65,65,88),(447,424,447)]:
    self.fill(x,1,t,x,12,t,'minecraft:polished_andesite');self.fill(lo,12,t,hi,12,t,'minecraft:iron_bars');self.add(x,13,t,'minecraft:lantern',{'hanging':'false','waterlogged':'false'})
   for z,lo,hi in [(65,65,88),(447,424,447)]:
    self.fill(t,1,z,t,12,z,'minecraft:polished_andesite');self.fill(t,12,lo,t,12,hi,'minecraft:iron_bars');self.add(t,13,z,'minecraft:lantern',{'hanging':'false','waterlogged':'false'})
 def screen_bank(self,x,zlo,zhi,facing,origin):
  windows=set();doors={}
  data=quick_build(True,False)
  for c in data['cells']:
   if c['kind']=='door' and c['props']['row']=='0':windows.add(c['pos'][2]+origin)
  # Door pairs are the physical two-cell openings, independent of frame handedness.
  covered=set()
  for zz in sorted(windows):
   if zz in covered or not zlo<=zz<=zhi:continue
   start=zz+1 if F[facing][0]<0 else zz
   doors[start]=True;covered.update([zz,zz+1])
  for z in range(zlo,zhi+1):
   if z in covered:continue
   for row in range(2):self.add(x,3+row,z,'createtransit:compact_wall',{'facing':facing,'row':str(row),'column':'0','status':'0','panel':'0','corner':'0','left':'true','right':'true'},{'id':(8,'createtransit:platform'),'layoutVersion':(3,2),'paint':(3,0),'accent':(3,3)})
  dz=F[facing][0] # east's clockwise is south; west's is north
  for base in doors:
   for col in range(2):
    for row in range(2):self.add(x,3+row,base+dz*col,'createtransit:compact_screen_door',{'facing':facing,'row':str(row),'column':str(col),'status':'0','panel':'0','corner':'0','left':'true','right':'true'},{'id':(8,'createtransit:platform'),'layoutVersion':(3,2),'paint':(3,0),'accent':(3,3)})
  for z in [zlo,zhi]:
   for row in range(2):
    cell=self.cells[x,3+row,z];cell['Properties']['left']='false';cell['Properties']['right']='false'
 def platform(self,xlo,xhi,zlo,zhi,label):
  self.fill(xlo,0,zlo,xhi,0,zhi,'minecraft:light_gray_concrete');self.fill(xlo,1,zlo,xhi,2,zhi,'minecraft:smooth_stone')
  for x in [xlo,xhi]:self.fill(x,2,zlo,x,2,zhi,'minecraft:yellow_concrete')
  # Shared white canopy, cyan fascia, bright fittings and back-to-back benches.
  self.fill(xlo,9,zlo+12,xhi,9,zhi-6,'minecraft:white_concrete')
  for x in [xlo,xhi]:self.fill(x,9,zlo+12,x,9,zhi-6,'minecraft:cyan_concrete')
  center=(xlo+xhi)//2
  for z in range(zlo+16,zhi-8,24):
   self.fill(center,3,z,center,8,z,'minecraft:polished_andesite');self.add(center,8,z+1,'minecraft:sea_lantern')
   for x in range(xlo+1,xhi):
    if x!=center:self.add(x,3,z+3,'minecraft:quartz_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
  # Staircase from platform height (top y=3) to the bridge deck (top y=12).
  for step in range(1,10):
   z=zlo+9-step
   for x in range(center-1,center+2):
    self.fill(x,3,z,x,step+1,z,'minecraft:smooth_stone') if step>1 else None
    self.add(x,step+2,z,'minecraft:smooth_quartz_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
  self.sign(center,6,zlo+17,[label,'TEST PLATFORM','Floor height: 2','Boarding: level'])
 def endpoint(self,name,block,p,target,positive,extra=None):
  tag={'id':(8,'create:'+block),'TargetTrack':pos([a-b for a,b in zip(target,p)]),'TargetDirection':(1,int(positive)),'Ortho':(1,1)}
  if block=='track_station':tag['Migrate']=(10,{'Name':(8,name),'Assembling':(1,0)})
  if extra:tag.update(extra)
  props={'assembling':'false','waterlogged':'false'} if block=='track_station' else {'powered':'false','type':'entry_signal'} if block=='track_signal' else {'powered':'false'}
  self.add(*p,'create:'+block,props,tag)
  self.meta['stations' if block=='track_station' else 'signals' if block=='track_signal' else 'observers'].append({'name':name,'pos':p,'target':target,'positive':positive})
 def station_sets(self):
  self.platform(75,81,198,313,'A / CENTER');self.platform(420,425,198,313,'B / SIDE 2');self.platform(443,450,198,313,'B / SIDE 1')
  for x,f,origin in [(75,'west',215),(81,'east',253),(425,'east',215),(443,'west',253)]:self.screen_bank(x,210,313,f,origin)
  specs=[('Sanctuary Test A - Outer',[77,3,217],[72,1,217],True,76,72,244),('Sanctuary Test A - Inner',[79,3,294],[84,1,294],False,80,84,278),('Sanctuary Test B - Inner',[423,3,217],[428,1,217],True,424,428,244),('Sanctuary Test B - Outer',[446,3,294],[440,1,294],False,444,440,278)]
  for name,p,target,direction,lx,tx,tz in specs:
   self.endpoint(name,'track_station',p,target,direction)
   self.add(lx,3,306,'minecraft:lever',{'face':'floor','facing':'north','powered':'false'})
   self.endpoint(name+' occupancy','track_observer',[lx,3,308],[tx,1,tz],direction)
   self.sign(lx,4,305,['SCREEN DOORS','Lever: hold open','Detector: berth','Redstone control'])
  # Pedestrian bridges cross above the catenary, with guarded decks and ground access stairs.
  for xlo,xhi,z in [(50,95,198),(413,458,198)]:
   self.fill(xlo,11,z-3,xhi,11,z,'minecraft:smooth_quartz')
   for zz in [z-3,z]:self.fill(xlo,12,zz,xhi,12,zz,'minecraft:glass_pane',{'waterlogged':'false','east':'true','west':'true','north':'false','south':'false'})
   for xx in [xlo+1,xhi-1]:
    for step in range(1,12):
     self.fill(xx-1,0,z+12-step,xx+1,step-1,z+12-step,'minecraft:light_gray_concrete')
     for x in range(xx-1,xx+2):self.add(x,step,z+12-step,'minecraft:smooth_quartz_stairs',{'facing':'north','half':'bottom','shape':'straight','waterlogged':'false'})
  # Open the bridge guard at each stair landing.
  for center in [51,78,94,414,422,446,457]:
   for x in range(center-1,center+2):self.cells.pop((x,12,198),None)
  # Storage gives a configured 12-second cyclic schedule for each running loop.
  for x,z,outer in [(77,282,True),(446,280,False)]:
   stops=['Sanctuary Test A - Outer','Sanctuary Test B - Outer'] if outer else ['Sanctuary Test A - Inner','Sanctuary Test B - Inner']
   item={'Slot':(1,0),'id':(8,'create:schedule'),'count':(3,1),'components':(10,{'create:train_schedule':(10,schedule(stops))})}
   self.add(x,3,z,'minecraft:barrel',{'facing':'up','open':'false'},{'id':(8,'minecraft:barrel'),'Items':(9,(10,[item]))})
   self.sign(x,4,z-1,['LRT SCHEDULE','OUTER LOOP' if outer else 'INNER LOOP','2 stops / 12 sec','Give to conductor'])
 def signals(self):
  for loop,c in self.meta['track_centers'].items():
   clockwise=loop=='outer'
   for side in ['west','east']:
    track=c[side];x=track-4 if side=='west' and clockwise else track+4 if side=='west' else track+4 if clockwise else track-4
    for z in [180,324]:
     self.fill(x,0,z,x,3,z,'minecraft:polished_andesite');self.endpoint(f'{loop} {side} {z}','track_signal',[x,4,z],[track,1,z],clockwise!=(side=='east'))
   for side in ['north','south']:
    track=c[side];z=track-4 if side=='north' else track+4
    for x in [224,288]:
     self.fill(x,0,z,x,3,z,'minecraft:polished_andesite');self.endpoint(f'{loop} {side} {x}','track_signal',[x,4,z],[x,1,track],clockwise!=(side=='north'))
 def train_build(self):
  if not self.train:return
  data=quick_build(True,self.crn);o=self.meta['train_origin']
  for c in data['cells']:
   kind=c['kind'];name=kind if ':' in kind else 'createtransit:lrt_'+kind;props=dict(c['props'])
   if kind!='create:small_bogey':props['facing']=c['facing']
   door=kind=='door';tag=None
   if name.startswith('createtransit:') and (not door or props['row']=='0'):
    tag={'id':(8,'createtransit:lrt_door' if door else 'createtransit:lrt_part'),'paint':(3,0),'accent':(3,3)}
   self.add(*[a+b for a,b in zip(o,c['pos'])],name,props,tag)
  # Exact Create journey fixture's proven conductor/control arrangement.
  self.add(o[0]+2,o[1]+1,o[2]+8,'create:blaze_burner',{'blaze':'smouldering'})
  for offset,face in [(7,'south'),(9,'north')]:self.add(o[0]+2,o[1]+1,o[2]+offset,'create:controls',{'facing':face})
  for car in data['cars']:
   low=[a+b for a,b in zip(o,car['glue'][0])];high=[a+b+1 for a,b in zip(o,car['glue'][1])];center=[(a+b)/2 for a,b in zip(low,high)]
   tag={'id':(8,'create:super_glue'),'Pos':(9,(6,center)),'Motion':(9,(6,[0.,0.,0.])),'Rotation':(9,(5,[0.,0.])),
    'From':(9,(6,[a-b for a,b in zip(low,center)])),'To':(9,(6,[a-b for a,b in zip(high,center)]))}
   self.glue.append({'pos':(9,(6,center)),'blockPos':(9,(3,[int(v) for v in center])),'nbt':(10,tag)})
 def perimeter(self):
  for v in range(512):
   for x,z in [(0,v),(511,v),(v,0),(v,511)]:self.add(x,0,z,'minecraft:cyan_concrete' if v%16==0 else 'minecraft:light_gray_concrete')
  for x,z in [(0,0),(511,0),(511,511),(0,511)]:
   self.add(x,1,z,'minecraft:sea_lantern');self.add(x,2,z,'minecraft:light_blue_stained_glass')
  # Sparse service paths, leaving the rest of the test site open for scenery and stress testing.
  self.fill(36,0,195,56,0,320,'minecraft:stone_bricks');self.fill(407,0,195,473,0,320,'minecraft:stone_bricks')
  self.fill(48,0,320,463,0,324,'minecraft:stone_bricks')
 def finish(self,name):
  self.perimeter();self.track();self.wires();self.station_sets();self.signals();self.train_build()
  palette=[];index={};blocks=[]
  for q,c in sorted(self.cells.items(),key=lambda v:(v[0][1],v[0][2],v[0][0])):
   key=(c['Name'],tuple(sorted(c['Properties'].items())))
   if key not in index:
    index[key]=len(palette);entry={'Name':(8,c['Name'])}
    if c['Properties']:entry['Properties']=(10,{k:(8,v) for k,v in c['Properties'].items()})
    palette.append(entry)
   b={'pos':(9,(3,list(q))),'state':(3,index[key])}
   if c['nbt']:b['nbt']=(10,c['nbt'])
   blocks.append(b)
  root={'DataVersion':(3,3955),'size':(9,(3,[512,16,512])),'palette':(9,(10,palette)),'blocks':(9,(10,blocks)),'entities':(9,(10,self.glue))}
  path=ROOT/'schematics'/f'{name}.nbt'
  temporary=path.with_suffix('.nbt.tmp')
  with temporary.open('wb') as f:
   with gzip.GzipFile(filename='',mode='wb',fileobj=f,mtime=0) as gz:gz.write(b'\x0a'+string('')+nbt(10,root))
  temporary.replace(path)
  self.meta.update(name=name,blocks=len(blocks),palette=len(palette),glue_groups=len(self.glue),crn=self.crn,train=self.train)
  (ROOT/'schematics'/f'{name}.json').write_text(json.dumps(self.meta,indent=2)+'\n')
  print(name,len(blocks),'blocks',len(palette),'states',len(self.glue),'glue groups',path.stat().st_size,'bytes')
  return self
if __name__=='__main__':
 for name,train,crn in [('LRT-Test-Circuit-512',True,True),('LRT-Test-Circuit-512-NoCRN',True,False),('LRT-Test-Circuit-512-Track-and-Stations',False,False)]:Circuit(train,crn).finish(name)

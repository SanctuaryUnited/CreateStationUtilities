"""Three-clear-block LRT layouts shared by models, schematics and journey tests."""
import json
from pathlib import Path
FACING={'north':(0,-1),'east':(1,0),'south':(0,1),'west':(-1,0)}
def glass_end(other,plane,toward):
 if not other:return False
 f=FACING[other['facing']];right=(-f[1],f[0]);opposite=(-f[0],-f[1]);k=other['kind']
 if k in ['cab_side_windshield_lower','cab_side_windshield_upper']:return f==plane and toward==((-right[0],-right[1]) if other['props'].get('right')=='true' else right)
 if k in ['window','window_panorama','window_divided','window_tinted','window_frosted']:return f==plane and (toward[0]==0)!=(plane[0]==0)
 if k in ['window_corner','window_rounded_corner']:return (plane==f and toward==right) or (plane==(f[1],-f[0]) and toward==opposite)
 return False
def connections(cells):
 for (x,y,z),c in cells.items():
  k=c['kind'];dx,dz=FACING[c['facing']];right=(-dz,dx)
  def neighbor(sign):return cells.get((x+right[0]*sign,y,z+right[1]*sign),{})
  if k in ['cab_lower','cab_windshield_lower','cab_windshield_upper','display_mount','side_display_mount']:
   c['props'].update(left=str(neighbor(-1).get('kind')==k and neighbor(-1).get('facing')==c['facing']).lower(),right=str(neighbor(1).get('kind')==k and neighbor(1).get('facing')==c['facing']).lower())
  elif k.startswith('cab_side_'):
   front='cab_'+k[len('cab_side_'):];c['props'].update(left=str(neighbor(-1).get('kind')==front).lower(),right=str(neighbor(1).get('kind')==front).lower())
 # Copy end profiles after their parent nose/glass columns have been resolved.
 for (x,y,z),c in cells.items():
  if c['kind'] in ['cab_floor','cab_skirt','cab_roof','cab_upper']:
   yy=y+1 if c['kind'] in ['cab_floor','cab_skirt'] else y-2 if c['kind']=='cab_roof' else y-1
   c['props'].update({f:cells.get((x,yy,z),{}).get('props',{}).get(f,'false') for f in ['left','right']})
  if c['kind'] in ['window','window_panorama','window_divided','window_tinted','window_frosted']:
   dx,dz=FACING[c['facing']];right=(-dz,dx)
   def joins(sign):
    n=cells.get((x+right[0]*sign,y,z+right[1]*sign),{});return glass_end(n,(dx,dz),(-right[0]*sign,-right[1]*sign))
   c['props'].update(left=str(joins(-1)).lower(),right=str(joins(1)).lower(),top=str(cells.get((x,y+1,z),{}).get('kind')==c['kind']).lower(),bottom=str(cells.get((x,y-1,z),{}).get('kind')==c['kind']).lower())
  elif c['kind'] in ['window_corner','window_rounded_corner']:
   dx,dz=FACING[c['facing']];right=(-dz,dx);end=(-dx,-dz)
   c['props'].update(left=str(glass_end(cells.get((x+end[0],y,z+end[1])),(dz,-dx),(dx,dz))).lower(),right=str(glass_end(cells.get((x+right[0],y,z+right[1])),(dx,dz),(-right[0],-right[1]))).lower(),top=str(cells.get((x,y+1,z),{}).get('kind')==c['kind']).lower(),bottom=str(cells.get((x,y-1,z),{}).get('kind')==c['kind']).lower())
 return list(cells.values())
def car(length=16,north=True,south=False,crn=True,anchors=True):
 cells={}
 def add(k,x,y,z,f='north',**props):cells[x,y,z]={'pos':[x,y,z],'kind':k,'facing':f,'props':props}
 ends=[z for z,cab in [(0,north),(length-1,south)] if cab];returns=[1 if z==0 else length-2 for z in ends]
 doors=[5,10] if length>=16 else [3]
 for x in range(5):
  for z in range(length):
   add('cab_skirt' if z in ends else 'floor',x,0,z,'south' if z==length-1 else 'north')
   if z not in ends+returns:add('roof_edge' if x in [0,4] else 'ceiling_double_strip' if x in [1,3] else 'ceiling_panel',x,4,z,'west' if x==0 else 'east')
 bogeys=[3,length-4] if length>=16 else [2,length-3]
 for z in bogeys:add('create:small_bogey',2,0,z,axis='z')
 for x in [0,4]:
  side='west' if x==0 else 'east'
  for z in range(length):
   if z in ends or z in returns:continue
   doorway=any(first<=z<=first+1 for first in doors)
   for y in range(1,4):
    if doorway and y<3:continue
    if doorway and y==3:
     if crn:add('createrailwaysnavigator:advanced_display',x,y,z,side,z_alignment='negative',side='front')
     add('side_display_mount',1 if x==0 else 3,y,z,side)
    else:add('led_horizontal' if y==1 else 'window_panorama',x,y,z,side)
  for z in range(length):
   if z in ends:continue
   add('bogey_skirt' if any(abs(z-b)<=2 for b in bogeys) else 'skirt_end' if z in returns else 'skirt_access' if z%5==0 else 'body_skirt',x,0,z,side)
  for z in returns:
   for y,k in [(1,'cab_side_lower'),(2,'cab_side_windshield_lower'),(3,'cab_side_upper')]:add(k,x,y,z,side)
  for first in doors:
   base=first+1 if x==0 else first
   for col in range(2):
    for row in range(2):add('door',x,row+1,base+(-col if x==0 else col),side,column=str(col),row=str(row),half='lower' if row==0 else 'upper',tall='false',visible='true',open='false',powered='false',hinge='left')
 for z in ends:
  f='north' if z==0 else 'south';inside=1 if z==0 else length-2
  for x in range(5):
   for y,k in [(1,'cab_lower'),(2,'cab_windshield_lower')]:add(k,x,y,z,f)
   add('cab_roof',x,4,z,f)
   if x in [0,4]:add('cab_upper',x,3,z,f)
   else:
    if crn:add('createrailwaysnavigator:advanced_display',x,3,z,f,z_alignment='positive',side='front')
    add('display_mount',x,3,inside,f)
  driver=2 if z==0 else length-3
  add('seat',2,1,driver,f)
  console_f='south' if z==0 else 'north'
  add('cab_console',2,0,inside,console_f);add('create:controls',2,1,inside,console_f)
 for x in [1,3]:
  for z in ([3,4,7,8,12,13] if length>=16 else [1,2,5,6]):add('seat',x,1,z,'east' if x==1 else 'west')
 for z,cab in [(0,north),(length-1,south)]:
  if cab:continue
  f='north' if z==0 else 'south'
  if anchors:add('articulation_anchor',2,0,z,f)
  for x in [0,4]:
   corner='north' if z==0 and x==0 else 'east' if z==0 else 'west' if x==0 else 'south'
   for y in range(1,4):add('corner' if y==1 else 'window_corner',x,y,z,corner)
 if length>=16:
  add('pantograph',2,5,8)
  if north:add('horn',2,5,2)
  if south:add('horn',2,5,length-3,'south')
  for z in [4,length-5]:
   for x in [1,2,3]:add('air_conditioner',x,5,z)
 return connections(cells),bogeys
def layout(crn=True):return car(17,True,True,crn,False)[0]
def quick_build(three=True,crn=True):
 specs=[(16,True,False),(8,False,False),(16,False,True)] if three else [(16,True,False),(16,False,True)]
 cells=[];cars=[];offset=0
 for length,north,south in specs:
  local,bogeys=car(length,north,south,crn)
  for c in local:c['pos'][2]+=offset
  cells.extend(local);cars.append({'offset':offset,'length':length,'bogeys':[offset+z for z in bogeys],'glue':[[0,0,offset],[4,5,offset+length-1]]});offset+=length+2
 return {'name':'LRT-Quick-1-Three-Cars' if three else 'LRT-Quick-2-Two-Cabs','size':[5,6,offset-2],'cars':cars,'cells':cells}
if __name__=='__main__':
 root=Path(__file__).resolve().parents[1];folder=root/'src/gametest/resources';folder.mkdir(parents=True,exist_ok=True)
 (folder/'lrt-single-car.json').write_text(json.dumps(layout(),indent=2)+'\n')
 for three in [True,False]:
  data=quick_build(three);(folder/('lrt-quick-three.json' if three else 'lrt-quick-two.json')).write_text(json.dumps(data,indent=2)+'\n');print(data['name'],len(data['cells']),'blocks',data['size'],[c['length'] for c in data['cars']])

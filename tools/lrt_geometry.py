"""Concept B single-car meshes; shared by baked models, lamps and previews."""
from station_geometry import cube as base_cube, element, cell, transformed, FACES
import math, copy
def cube(a,b,texture='lrt_body',tint=0,faces=None,rotation=None):
 return base_cube(a,b,texture,tint,faces,rotation)
KINDS=['floor','cab_floor','wall','window','corner','window_corner','roof','roof_edge','roof_corner','cab_roof','led_vertical','led_horizontal','cab_lower','cab_windshield_lower','cab_windshield_upper','cab_upper','cab_side_lower','cab_side_windshield_lower','cab_side_windshield_upper','cab_side_upper','pantograph','air_conditioner','display_mount','side_display_mount','underbody_skirt','cab_console','seat','door','articulation_anchor','horn']
CEILING_KINDS={'ceiling_strip','ceiling_double_strip','ceiling_panel','ceiling_bulb','ceiling_ring'}
WINDOW_KINDS={'window_panorama','window_narrow','window_divided','window_tinted','window_frosted','window_rounded_corner'}
SKIRT_KINDS={'body_skirt','bogey_skirt','skirt_end','skirt_access','cab_skirt'}
KINDS+=sorted(CEILING_KINDS)+sorted(WINDOW_KINDS)+sorted(SKIRT_KINDS)
PROFILE_KINDS={'cab_floor','cab_roof','cab_lower','cab_upper','display_mount','side_display_mount','cab_skirt'}
GLASS_KINDS={'window','window_corner','cab_windshield_lower','cab_windshield_upper','cab_side_windshield_lower','cab_side_windshield_upper'}
GLASS_KINDS|=WINDOW_KINDS
GLASS_TEXTURES={'lrt_glass','lrt_glass_tinted','lrt_glass_frosted'}
SIDE_KINDS={'cab_side_lower','cab_side_windshield_lower','cab_side_windshield_upper','cab_side_upper'}
SLOPE=math.tan(math.pi/8); WINDSHIELD_BASE=2.6
def quad(v,texture='lrt_body',tint=0,uv=None,double=False):
 v=[list(p) for p in v]
 # Clipping a curved deck sometimes leaves a triangle. Keep the first three
 # vertices distinct so the baker computes its real upward-facing normal.
 coords=uv or [[0,16],[16,16],[16,0],[0,0]];unique=[];mapped=[]
 for p,t in zip(v,coords):
  if not unique or p!=unique[-1]:unique.append(p);mapped.append(t)
 if len(unique)>1 and unique[0]==unique[-1]:unique.pop();mapped.pop()
 if len(unique)==3:v=unique+[unique[-1]];uv=mapped+[mapped[-1]]
 q={'from':[min(p[i] for p in v) for i in range(3)],'to':[max(p[i] for p in v) for i in range(3)],'texture':texture,'tint':tint,'visible':['north'],'_quad':v,'uv':uv or [[0,16],[16,16],[16,0],[0,0]]}
 if double:q['double']=True
 return q
def faces(geo):
 """Winding-correct faces, including legal cube rotations and pixel-sized glass UVs."""
 out=[]
 for e in geo:
  if '_quad' in e:
   out.append(e)
   if e.get('double'):
    q=copy.deepcopy(e);q['_quad']=list(reversed(q['_quad']));q['uv']=list(reversed(q['uv']));q.pop('double',None);out.append(q)
   continue
  x,y,z=e['from'];X,Y,Z=e['to']
  vs=[[(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],[(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],[(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)]]
  for f,v in zip(FACES,vs):
   if f not in e['visible']:continue
   if 'rotation' in e:
    r=e['rotation'];o=r['origin'];ang=math.radians(r['angle']);c,s=math.cos(ang),math.sin(ang);rv=[]
    for p in v:
     a,b,d=[p[i]-o[i] for i in range(3)];q={'x':[a,c*b-s*d,s*b+c*d],'y':[c*a+s*d,b,-s*a+c*d],'z':[c*a-s*b,s*a+c*b,d]}[r['axis']];rv.append([q[i]+o[i] for i in range(3)])
    v=rv
   uv=[[p[0],16-p[1]] if f in ('north','south') else [p[2],16-p[1]] if f in ('west','east') else [p[0],p[2]] for p in v] if e['texture']=='lrt_glass' else None
   out.append(quad(v,e['texture'],e['tint'],uv))
 return out
def atlas_uv(face):
 """Keep each face within its own sprite; multi-block glass must not sample the atlas."""
 coords=[list(p) for p in face['uv']]
 for axis in range(2):
  lo=min(p[axis] for p in coords);hi=max(p[axis] for p in coords)
  if lo>=0 and hi<=16:continue
  if hi-lo>16:
   for p in coords:p[axis]=(p[axis]-lo)*16/(hi-lo)
  else:
   shift=math.floor(lo/16)*16
   if hi-shift>16:shift=lo
   for p in coords:p[axis]=max(0,min(16,p[axis]-shift))
 return coords
def window(left=False,right=False,top=False,bottom=False):
 x,X=0 if left else .65,16 if right else 15.35;y,Y=0 if bottom else .65,16 if top else 15.35
 vis=[f for f in FACES if not {'west':left,'east':right,'up':top,'down':bottom}.get(f,False)]
 out=[cube([x,y,.8],[X,Y,1.4],'lrt_glass',-1,vis)]
 if not left:out.append(cube([0,0,.25],[.65,16,2.4],'lrt_seal',-1))
 if not right:out.append(cube([15.35,0,.25],[16,16,2.4],'lrt_seal',-1))
 if not bottom:out.append(cube([x,0,.25],[X,.65,2.4],'lrt_seal',-1))
 if not top:out.append(cube([x,15.35,.25],[X,16,2.4],'lrt_seal',-1))
 return out
def window_corner(left=False,right=False,top=False,bottom=False):
 y,Y=0 if bottom else .65,16 if top else 15.35;X,Z=16 if right else 15.35,16 if left else 15.35
 out=[cube([0,0,0],[1.4,16,1.4],'lrt_seal',-1),cube([1.4,y,.8],[X,Y,1.4],'lrt_glass',-1,[f for f in FACES if not {'east':right,'up':top,'down':bottom}.get(f,False)]),cube([.8,y,1.4],[1.4,Y,Z],'lrt_glass',-1,[f for f in FACES if not {'south':left,'up':top,'down':bottom}.get(f,False)])]
 if not right:out.append(cube([15.35,0,.25],[16,16,2.4],'lrt_seal',-1))
 if not left:out.append(cube([.25,0,15.35],[2.4,16,16],'lrt_seal',-1))
 for lo,hi,joined in [(0,.65,bottom),(15.35,16,top)]:
  if not joined:out.extend([cube([1.4,lo,.25],[X,hi,1.4],'lrt_seal',-1),cube([.25,lo,1.4],[1.4,hi,Z],'lrt_seal',-1)])
 return out
def contour(left=True,right=True,inset=0):
 """Six facets around each quarter-circle at the ends of a five-wide cab row."""
 if left and right:return [(0,inset),(16,inset)]
 radius=16 if left!=right else 8
 a=[(radius-(radius-inset)*math.cos(i*math.pi/12),radius-(radius-inset)*math.sin(i*math.pi/12)) for i in range(7)]
 if not left and not right:return a+[(16-x,z) for x,z in reversed(a[:-1])]
 return a if not left else [(16-x,z) for x,z in reversed(a)]
def band(y,Y,z,Z,texture='lrt_body',tint=0,left=True,right=True,inset=0,inside=2.4,close=False):
 out=[];outer=contour(left,right,inset);inner=contour(left,right,inset+inside)
 for i,(a,b) in enumerate(zip(outer,outer[1:])):
  v=[[b[0],y,z+b[1]],[a[0],y,z+a[1]],[a[0],Y,Z+a[1]],[b[0],Y,Z+b[1]]];uv=[[b[0],16-y],[a[0],16-y],[a[0],16-Y],[b[0],16-Y]]
  out.append(quad(v,texture,tint,uv if texture=='lrt_glass' else None));a1,b1=inner[i:i+2]
  out.append(quad([[a1[0],y,z+a1[1]],[b1[0],y,z+b1[1]],[b1[0],Y,Z+b1[1]],[a1[0],Y,Z+a1[1]]],texture,tint,[[a1[0],16-y],[b1[0],16-y],[b1[0],16-Y],[a1[0],16-Y]] if texture=='lrt_glass' else None))
  if close:out.extend([quad([[a[0],y,z+a[1]],[b[0],y,z+b[1]],[b1[0],y,z+b1[1]],[a1[0],y,z+a1[1]]],texture,tint),quad([[a1[0],Y,Z+a1[1]],[b1[0],Y,Z+b1[1]],[b[0],Y,Z+b[1]],[a[0],Y,Z+a[1]]],texture,tint)])
 return out
def nose_z(y):return 6-y*4/4.5 if y<4.5 else 2 if y<13 else 2+(y-13)*.2
def nose(left=True,right=True):
 out=[]
 for a,b,tex,tint in [(0,4.5,'lrt_body',0),(4.5,5,'lrt_body',0),(5,7,'lrt_accent',1),(7,13,'lrt_body',0),(13,16,'lrt_body',0)]:out.extend(band(a,b,nose_z(a),nose_z(b),tex,tint,left,right,close=a==0))
 out.extend(band(13.3,14.8,nose_z(13.3),nose_z(14.8),'lrt_seal',-1,left,right,inset=-.012,inside=.02));return out
def windshield(upper=False,left=False,right=False,top=False,bottom=False):
 lo,hi=(0 if bottom else .65,15.35) if upper else (.65,16 if top else 15.35);base=WINDSHIELD_BASE+(16*SLOPE if upper else 0)
 out=band(lo,hi,base+lo*SLOPE,base+hi*SLOPE,'lrt_glass',-1,left,right,inset=.8,inside=.6)
 y,Y=(15.35,16) if upper else (0,.65)
 out.extend(band(y,Y,base+y*SLOPE,base+Y*SLOPE,'lrt_seal',-1,left,right,inset=.25,inside=2.15,close=True))
 if not upper and not top:out.extend(band(15.35,16,base+15.35*SLOPE,base+16*SLOPE,'lrt_seal',-1,left,right,inset=.25,inside=2.15,close=True))
 if upper and not bottom:out.extend(band(0,.65,base,base+.65*SLOPE,'lrt_seal',-1,left,right,inset=.25,inside=2.15,close=True))
 return out
def side_surface(a,b,start,end,texture,tint,front_right=False,inset=.8,inside=.6):
 v=[[16,b,inset],[16,a,inset],[start,a,inset],[end,b,inset]]
 if front_right:v=[[16-p[0],p[1],p[2]] for p in reversed(v)]
 out=[quad(v,texture,tint,[[p[0],16-p[1]] for p in v] if texture=='lrt_glass' else None)]
 v2=[[p[0],p[1],inset+inside] for p in reversed(v)];out.append(quad(v2,texture,tint,[[p[0],16-p[1]] for p in v2] if texture=='lrt_glass' else None));return out
def side_cab(kind,front_right=False,top=False,bottom=False):
 out=[]
 if kind=='cab_side_lower':
  for a,b,tex,tint in [(0,4.5,'lrt_body',0),(4.5,5,'lrt_body',0),(5,7,'lrt_accent',1),(7,13,'lrt_body',0),(13,16,'lrt_body',0)]:out.extend(side_surface(a,b,nose_z(a),nose_z(b),tex,tint,front_right,0,2.4))
 elif 'windshield' in kind:
  upper=kind.endswith('upper');base=WINDSHIELD_BASE+(16*SLOPE if upper else 0);lo,hi=(0 if bottom else .65,15.35) if upper else (.65,16 if top else 15.35)
  out.extend(side_surface(lo,hi,base+lo*SLOPE,base+hi*SLOPE,'lrt_glass',-1,front_right))
  lo,hi=(15.35,16) if upper else (0,.65);out.extend(side_surface(lo,hi,base+lo*SLOPE,base+hi*SLOPE,'lrt_seal',-1,front_right,.25,2.15))
  if not upper and not top:out.extend(side_surface(15.35,16,base+15.35*SLOPE,base+16*SLOPE,'lrt_seal',-1,front_right,.25,2.15))
 else:out.extend(side_surface(0,16,8,14,'lrt_body',0,front_right,0,2.4))
 return out
def deck(left=True,right=True,roof=False):
 y,Y=(0,3) if roof else (12,16);front,FRONT=(8,12) if roof else (6,6);cut=32 if roof else 16
 # Clip the front corner to the deck's occupied footprint.
 outer=[(x,min(cut-front,z)) for x,z in contour(left,right)];upper=[(x,min(cut-FRONT,z)) for x,z in contour(left,right)]
 out=[]
 for i,(a,b) in enumerate(zip(outer,outer[1:])):
  A,B=upper[i:i+2]
  out.append(quad([[b[0],y,front+b[1]],[a[0],y,front+a[1]],[A[0],Y,FRONT+A[1]],[B[0],Y,FRONT+B[1]]]))
  for high,zz,tex,points in [(False,front,'lrt_body',outer),(True,FRONT,'lrt_body' if roof else 'lrt_floor',upper)]:
   aa,bb=points[i:i+2];yy=Y if high else y;v=[[aa[0],yy,zz+aa[1]],[bb[0],yy,zz+bb[1]],[bb[0],yy,cut],[aa[0],yy,cut]]
   if high:v.reverse()
   if bb[0]>aa[0] and cut-zz-min(aa[1],bb[1])>1e-8:out.append(quad(v,tex,0 if roof or not high else -1,[[p[0],p[2]] for p in v] if high and not roof else None))
 out.append(quad([[0,y,cut],[16,y,cut],[16,Y,cut],[0,Y,cut]],'lrt_body'));return out
def roof_edge(corner=False):
 if corner:
  out=[]
  for i in range(6):
   a,b=i*math.pi/12,(i+1)*math.pi/12;v=[[4-4*math.cos(a),0,4-4*math.sin(a)],[4-4*math.cos(b),0,4-4*math.sin(b)],[4-math.cos(b),3,4-math.sin(b)],[4-math.cos(a),3,4-math.sin(a)]];out.append(quad(list(reversed(v))))
  out.extend([cube([4,0,0],[16,3,16]),cube([0,0,4],[4,3,16])]);return out
 return [quad([[16,0,0],[0,0,0],[0,3,4],[16,3,4]]),cube([0,0,4],[16,3,16]),cube([0,0,0],[16,.1,4],'lrt_ceiling')]
def display_mount(left=True,right=True,side=False):
 # Separate cell BEHIND native CRN boards: POSITIVE cab / NEGATIVE side alignment.
 depth=-16 if side else -8;back=-8 if side else 0
 out=[cube([0,0,back+.15],[16,16,back+2.4],'lrt_body'),cube([0,-.6,depth+.03],[16,0,back+2.4],'lrt_seal',-1),cube([0,16,depth+.03],[16,16.6,back+2.4],'lrt_seal',-1)]
 if not left:out.append(cube([-.6,0,depth+.03],[0,16,back+2.4],'lrt_seal',-1))
 if not right:out.append(cube([16,0,depth+.03],[16.6,16,back+2.4],'lrt_seal',-1))
 return out
def pantograph_base():return [cube([1,-13,2],[15,-11.5,14],'lrt_body'),cube([2,-11.5,1],[5,-8.5,4],'lrt_metal',-1),cube([11,-11.5,1],[14,-8.5,4],'lrt_metal',-1)]
def pantograph_arm():return [cube([0,0,-.4],[.8,14,.4],'lrt_metal',-1)]
def pantograph_head():return [cube([0,0,0],[16,1,5],'lrt_metal',-1)]
def pantograph(raised=True,progress=None):
 p=(1 if raised else 0) if progress is None else progress;angle=math.radians(5+45*p);y,z=14*math.sin(angle),14*math.cos(angle);out=pantograph_base()
 for x in [3,12]:
  out.append(cube([x,-8.5,1.6],[x+.8,5.5,2.4],'lrt_metal',-1,rotation={'origin':[x,-8.5,2],'axis':'x','angle':90-math.degrees(angle)}))
  out.append(cube([x,-8.5+y,2+z-.4],[x+.8,5.5+y,2+z+.4],'lrt_metal',-1,rotation={'origin':[x,-8.5+y,2+z],'axis':'x','angle':math.degrees(angle)-90}))
 out.extend([transformed(e,0,-8.5+2*y,0) for e in pantograph_head()]);return out
def door_fixed(height=32):return [cube([0,0,.06],[1.2,height-1,2.4],'lrt_body'),cube([30.8,0,.06],[32,height-1,2.4],'lrt_body'),cube([0,height-1,.06],[32,height,2.4],'lrt_body')]
def door_leaf(right=False,height=32):
 x,X=(16,30.8) if right else (1.2,16);y=height-1.2;center=height/2
 out=[cube([x,0,.2],[X,5,1.2],'lrt_body'),cube([x,5,.2],[X,7,1.2],'lrt_accent',1),cube([x,7,.2],[X,8,1.2],'lrt_body'),cube([x,8,.2],[x+.5,y,1.2],'lrt_seal',-1),cube([X-.5,8,.2],[X,y,1.2],'lrt_seal',-1),cube([x+.5,8,.2],[X-.5,8.5,1.2],'lrt_seal',-1),cube([x+.5,y-.5,.2],[X-.5,y,1.2],'lrt_seal',-1),cube([x+.5,8.5,.4],[X-.5,y-.5,1],'lrt_glass',-1)]
 a,b=(16.1,17.1) if right else (14.9,15.9);out.extend([cube([a,center-1,.08],[b,center+1,.2],'lrt_seal',-1,['north','west','east','up','down']),cube([a,center-1,1.2],[b,center+1,1.32],'lrt_seal',-1,['south','west','east','up','down'])]);return out
def door_center(right=False,height=32):
 x,X=(16.25,16.95) if right else (15.05,15.75)
 return [cube([x,height/2-.65,z],[X,height/2+.65,Z],'gondola_led',-1,[f]) for z,Z,f in [(.025,.08,'north'),(1.32,1.375,'south')]]
def door_lights(height=32):return [cube([x,7,z],[x+.65,height-1.5,Z],'gondola_led',1,[f]) for x in [.2,31.15] for z,Z,f in [(.01,.05,'north'),(2.4,2.44,'south')]]
def door_status(height=32):return [cube([13,height-.75,z],[19,height-.25,Z],'gondola_led',-1,[f]) for z,Z,f in [(.01,.05,'north'),(2.4,2.44,'south')]]
def door_moving(progress,center=True,height=32):
 from gondola_geometry import wide_motion
 slide,retract=wide_motion(progress)
 return [transformed(e,slide*(1 if right else -1),0,-retract*3.2/3) for right in [False,True] for e in door_leaf(right,height)+(door_center(right,height) if center else [])]
def lights(kind,half='bottom',left=True,right=True):
 if kind=='roof':return [cube([7.25,0,0],[8.75,.001,16],'gondola_led',-1,['down'])]
 if kind=='led_vertical':return [cube([7.6,0,.01],[8.4,16,.05],'gondola_led',1,['north'])]
 if kind=='led_horizontal':return [cube([0,7.6 if half=='bottom' else 8.6,.01],[16,8 if half=='bottom' else 9,.05],'gondola_led',1,['north'])]
 if kind=='cab_lower':return band(13.75,14.35,nose_z(13.75),nose_z(14.35),'gondola_led',-1,left,right,inset=-.05,inside=.005)
 return []

def ceiling_light(kind,left=False,right=False,top=False,bottom=False):
 out=[cube([0,.1,0],[16,3,16]),cube([0,0,0],[16,.1,16],'lrt_ceiling',-1)]
 def led(a,b):out.append(cube(a,b,'lrt_led',1,['down','north','south','east','west']))
 if kind in ['ceiling_strip','ceiling_double_strip']:
  for x in ([7] if kind=='ceiling_strip' else [3,11]):led([x,-.035,0],[x+2,0,16])
 elif kind=='ceiling_panel':
  out.append(cube([2,-.015,3],[14,.02,13],'lrt_metal',-1));led([2.5,-.04,3.5],[13.5,-.015,12.5])
 elif kind=='ceiling_bulb':
  # Low round LED diffuser and a slim metal bezel.
  for i in range(24):
   a,b=i*math.tau/24,(i+1)*math.tau/24
   v=[[8,-.02,8],[8+3.5*math.cos(a),-.02,8+3.5*math.sin(a)],[8+3.5*math.cos(b),-.02,8+3.5*math.sin(b)]]
   out.append(quad(v+[v[-1]],'lrt_led',1))
   out.append(quad([[8+3.5*math.cos(a),-.02,8+3.5*math.sin(a)],[8+4*math.cos(a),-.02,8+4*math.sin(a)],[8+4*math.cos(b),-.02,8+4*math.sin(b)],[8+3.5*math.cos(b),-.02,8+3.5*math.sin(b)]],'lrt_metal',-1))
 elif kind=='ceiling_ring':
  # Internal edges disappear. The exposed perimeter becomes a larger rounded ring.
  a,b=0 if left else 1,16 if right else 15;c,d=0 if top else 1,16 if bottom else 15;r=7
  def strip(x,z,X,Z):led([x,-.035,z],[X,0,Z])
  if not top:strip(a+(r if not left else 0),c,b-(r if not right else 0),c+.65)
  if not bottom:strip(a+(r if not left else 0),d-.65,b-(r if not right else 0),d)
  if not left:strip(a,c+(r if not top else 0),a+.65,d-(r if not bottom else 0))
  if not right:strip(b-.65,c+(r if not top else 0),b,d-(r if not bottom else 0))
  for joined1,joined2,cx,cz,start in [(right,bottom,b-r,d-r,0),(left,bottom,a+r,d-r,90),(left,top,a+r,c+r,180),(right,top,b-r,c+r,270)]:
   if joined1 or joined2:continue
   for i in range(8):
    t,T=math.radians(start+i*11.25),math.radians(start+(i+1)*11.25)
    out.append(quad([[cx+r*math.cos(t),-.035,cz+r*math.sin(t)],[cx+r*math.cos(T),-.035,cz+r*math.sin(T)],[cx+(r-.65)*math.cos(T),-.035,cz+(r-.65)*math.sin(T)],[cx+(r-.65)*math.cos(t),-.035,cz+(r-.65)*math.sin(t)]],'lrt_led',1,double=True))
 return out

def ring_inner(corner):
 out=[];cx,cz,sx,sz={'nw':(0,0,1,1),'ne':(16,0,-1,1),'se':(16,16,-1,-1),'sw':(0,16,1,-1)}[corner]
 for i in range(8):
  a,b=i*math.pi/16,(i+1)*math.pi/16
  out.append(quad([[cx+sx*math.cos(a),-.035,cz+sz*math.sin(a)],[cx+sx*math.cos(b),-.035,cz+sz*math.sin(b)],[cx+sx*.35*math.cos(b),-.035,cz+sz*.35*math.sin(b)],[cx+sx*.35*math.cos(a),-.035,cz+sz*.35*math.sin(a)]],'lrt_led',1,double=True))
 return out

def window_variant(kind,left=False,right=False,top=False,bottom=False):
 if kind=='window_rounded_corner':
  geo=window_corner(left,right,top,bottom)
  geo=[e for e in geo if e['texture']!='lrt_glass' and e['from']!=[0,0,0]]
  y,Y=0 if bottom else .65,16 if top else 15.35
  geo.extend([cube([2.4,y,.8],[16 if right else 15.35,Y,1.4],'lrt_glass',-1),cube([.8,y,2.4],[1.4,Y,16 if left else 15.35],'lrt_glass',-1)])
  for lo,hi,tex in [(y,Y,'lrt_glass')]+([] if bottom else [(0,.65,'lrt_seal')])+([] if top else [(15.35,16,'lrt_seal')]):
   for i in range(8):
    a,b=i*math.pi/16,(i+1)*math.pi/16
    for radius,reverse in [(1.6,False),(1.0,True)]:
     v=[[2.4-radius*math.cos(a),lo,2.4-radius*math.sin(a)],[2.4-radius*math.cos(b),lo,2.4-radius*math.sin(b)],[2.4-radius*math.cos(b),hi,2.4-radius*math.sin(b)],[2.4-radius*math.cos(a),hi,2.4-radius*math.sin(a)]]
     geo.append(quad(list(reversed(v)) if reverse else v,tex,-1))
  return geo
 geo=window(False if kind=='window_narrow' else left,False if kind=='window_narrow' else right,top,bottom)
 if kind=='window_narrow':
  geo[0]['from'][0]=4;geo[0]['to'][0]=12
  geo.extend([cube([0,0,0],[3.35,16,2.4]),cube([3.35,0,.25],[4,16,2.4],'lrt_seal',-1),cube([12,0,.25],[12.65,16,2.4],'lrt_seal',-1),cube([12.65,0,0],[16,16,2.4])])
 if kind=='window_divided':geo.append(cube([7.6,0,.25],[8.4,16,2.4],'lrt_seal',-1))
 if kind in ['window_tinted','window_frosted']:
  for e in geo:
   if e['texture']=='lrt_glass':e['texture']='lrt_glass_'+kind.split('_')[1]
 if kind=='window_panorama':
  for e in geo:
   if e['texture']=='lrt_seal':e['texture']='lrt_body';e['tint']=0
 return geo

def body_skirt(kind):
 # Floor stays walkable. The panel extends beneath it, outside the bogey sweep.
 out=[cube([0,0,0],[16,16,16],'lrt_floor',-1,['south','east','west','up','down'])]
 low=-15 if kind=='bogey_skirt' else -9
 if kind=='skirt_end':
  out.append(quad([[0,16,.06],[16,16,.06],[16,-9,.06],[0,0,.06]],'lrt_body',0,double=True))
 else:out.append(cube([0,low,.06],[16,16,2.4]))
 out.append(cube([0,5,.015],[16,7,.055],'lrt_accent',1,['north']))
 if kind=='skirt_access':
  out.append(cube([2,-5,-.025],[14,11,.01],'lrt_metal',-1,['north']))
  out.append(cube([2.3,-4.7,-.045],[13.7,10.7,-.025],'lrt_body',0,['north']))
  out.append(cube([10,7,-.08],[12,7.6,-.045],'lrt_metal',-1))
 return out
def mesh(kind,raised=True,half='bottom',left=True,right=True,top=False,bottom=False):
 if kind in CEILING_KINDS:return ceiling_light(kind,left,right,top,bottom)
 if kind in WINDOW_KINDS:return window_variant(kind,left,right,top,bottom)
 if kind in SKIRT_KINDS:
  if kind=='cab_skirt':return deck(left,right)+[quad([[x,y,min(16,z)] for x,y,z in q['_quad']],q['texture'],q['tint'],double=True) for q in faces(band(-9,12,6,6,'lrt_body',0,left,right,close=True))]
  return body_skirt(kind)
 if kind=='floor':return [cube([0,12,0],[16,15.9,16],'lrt_body'),cube([0,15.9,0],[16,16,16],'lrt_floor',-1)]
 if kind=='cab_floor':return deck(left,right)
 if kind=='cab_lower':return nose(left,right)
 if kind.startswith('cab_windshield_'):return windshield(kind.endswith('upper'),left,right,top,bottom)
 if kind in SIDE_KINDS:return side_cab(kind,front_right=right,top=top,bottom=bottom)
 if kind=='cab_upper':return band(0,16,8,14,'lrt_body',0,left,right,close=True)
 if kind=='cab_roof':return deck(left,right,True)
 if kind in ['wall','underbody_skirt','led_vertical','led_horizontal']:return [cube([0,0,.06],[16,5,2.4],'lrt_body'),cube([0,5,.06],[16,7,2.4],'lrt_accent',1),cube([0,7,.06],[16,16,2.4],'lrt_body')]
 if kind=='window':return window()
 if kind=='corner':return [cube([0,0,0],[16,16,2.4],'lrt_body'),cube([0,0,2.4],[2.4,16,16],'lrt_body')]
 if kind=='window_corner':return window_corner()
 if kind=='roof':return [cube([0,0,0],[7.25,.1,16],'lrt_ceiling'),cube([8.75,0,0],[16,.1,16],'lrt_ceiling'),cube([7.25,.001,0],[8.75,.1,16],'lrt_ceiling'),cube([0,.1,0],[16,3,16],'lrt_body')]
 if kind in ['roof_edge','roof_corner']:return roof_edge(kind=='roof_corner')
 if kind=='pantograph':return pantograph(raised)
 if kind=='horn':return [cube([2,-13,2],[14,-11.5,14],'lrt_body'),cube([6,-11.5,6],[10,-7,10],'lrt_metal',-1),cube([2,-7,2],[14,-5,14],'lrt_accent',1),cube([4,-5,4],[12,-4,12],'lrt_metal',-1),cube([5,-4,5],[11,-3.5,11],'lrt_seal',-1,['up'])]
 if kind=='air_conditioner':return [cube([0,-13,0],[16,-11.5,16],'lrt_body'),quad([[16,-11.5,1],[0,-11.5,1],[1,-7,3],[15,-7,3]]),quad([[0,-11.5,15],[16,-11.5,15],[15,-7,13],[1,-7,13]]),cube([1,-11.5,3],[15,-7,13],'lrt_body'),cube([2,-6.98,4],[14,-6.95,12],'lrt_vent',-1,['up'])]
 if kind in ['display_mount','side_display_mount']:return display_mount(left,right,kind=='side_display_mount')
 if kind=='cab_console':return [cube([0,12,0],[16,15.9,16],'lrt_body'),cube([0,15.9,0],[16,16,16],'lrt_metal',-1)]
 if kind=='articulation_anchor':return mesh('floor')+[cube([-16,12,-18],[32,15.9,0],'lrt_body'),cube([-16,15.9,-18],[32,16,0],'lrt_floor',-1),cube([1,9,.2],[15,11,3],'lrt_metal',-1)]
 if kind=='seat':return [cube([2,0,3],[4,6,13],'lrt_metal',-1),cube([12,0,3],[14,6,13],'lrt_metal',-1),cube([0,6,2],[16,7,14],'lrt_body'),cube([0,7,2],[16,9,14],'lrt_seat',1),cube([0,9,12],[16,16,14],'lrt_seat',1)]
 if kind=='door':return door_fixed()+door_leaf()+door_leaf(True)+door_lights()+door_status()+[{**e,'texture':'lrt_seal'} for e in door_center()+door_center(True)]
 raise ValueError(kind)

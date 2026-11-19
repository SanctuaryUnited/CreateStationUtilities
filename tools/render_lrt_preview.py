"""Software renders of the shipped meshes, quick builds and animation paths."""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import copy,json,math
import numpy as np
from PIL import Image,ImageDraw,ImageFont
from lrt_geometry import *
from lrt_car_layout import layout,quick_build
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
jar=next(iter((Path.home()/'.gradle/caches/modules-2/files-2.1/maven.modrinth/create/6.0.10+mc1.21.1').glob('*/*.jar')),None)
CREATE=ZipFile(jar) if jar else None;TEXTURES={}
def texture(name):
 if name not in TEXTURES:
  if name.startswith('create:') and CREATE:im=Image.open(BytesIO(CREATE.read('assets/create/textures/'+name.split(':')[1]+'.png')))
  else:im=Image.open(A/f'textures/block/{name}.png')
  TEXTURES[name]=np.array(im.convert('RGBA')).astype(float)
 return TEXTURES[name]
def placed(geo,c):
 turn={'north':0,'east':1,'south':2,'west':3}[c['facing']];x,y,z=[p*16 for p in c['pos']];out=[]
 for q in faces(geo):
  e=copy.deepcopy(q);v=[]
  for X,Y,Z in q['_quad']:
   xx,zz=[(X,Z),(16-Z,X),(16-X,16-Z),(Z,16-X)][turn];v.append([xx+x,Y+y,zz+z])
  e['_quad']=v;e['from']=[min(p[i] for p in v) for i in range(3)];e['to']=[max(p[i] for p in v) for i in range(3)];out.append(e)
 return out
def native_bogey(c):
 if not CREATE:return []
 root='assets/create/models/block/track/bogey/';tex=json.loads(CREATE.read(root+'textures.json'))['textures'];out=[]
 def obj(name,offset):
  verts=[];uv=[];material='bogey';mapping={};text=CREATE.read(root+name+'.obj').decode()
  current=None
  for line in CREATE.read(root+name+'.mtl').decode().splitlines():
   if line.startswith('newmtl '):current=line[7:]
   elif line.startswith('map_Kd '):mapping[current]=line[7:].lstrip('#')
  for line in text.splitlines():
   if line.startswith('v '):verts.append([float(p)*16 for p in line.split()[1:]])
   elif line.startswith('vt '):uv.append([float(p) for p in line.split()[1:3]])
   elif line.startswith('usemtl '):material=mapping[line[7:]]
   elif line.startswith('f '):
    indices=[p.split('/') for p in line.split()[1:]]
    for i in range(1,len(indices)-1):
     ids=[indices[0],indices[i],indices[i+1],indices[i+1]]
     v=[[verts[int(k[0])-1][j]+offset[j] for j in range(3)] for k in ids]
     out.append(quad(v,tex[material],-1,[[uv[int(k[1])-1][0]*16,(1-uv[int(k[1])-1][1])*16] for k in ids]))
 x,y,z=[p*16 for p in c['pos']];obj('bogey_frame',[x+8,y-16.125,z+8])
 for offset in [-16,16]:obj('bogey_wheel',[x+8,y-4.125,z+8+offset])
 # The native casing block is part of the bogey and carries the floor.
 top=json.loads(CREATE.read(root+'top.json'));t=top['textures']
 for e in top['elements']:
  for f,d in e['faces'].items():
   part=cube(e['from'],e['to'],t[d['texture'][1:]],-1,[f]);out.extend(placed([part],c))
 return out
def gangways(cells):
 out=[];anchors=sorted([c for c in cells if c['kind']=='articulation_anchor'],key=lambda c:c['pos'][2])
 for a,b in zip(anchors[::2],anchors[1::2]):
  x,y,z=a['pos'];end=b['pos'][2];start=(z+1)*16;finish=end*16
  count=max(10,min(30,math.ceil((finish-start)/2)));count+=count%2;previous=None
  for n in range(count+1):
   t=n/count;w=40 if n%2==0 else 38.88;inset=0 if n%2==0 else .88;zz=start+(finish-start)*t
   ring=[[x*16+8-w,(y+1)*16+.096,zz],[x*16+8+w,(y+1)*16+.096,zz],[x*16+8+w,(y+4)*16-inset,zz],[x*16+8-w,(y+4)*16-inset,zz]]
   if previous:
    for face in range(4):
     vertices=[previous[face],previous[(face+1)%4],ring[(face+1)%4],ring[face]];tex='lrt_floor' if face==0 else 'lrt_ceiling' if face==2 else 'lrt_bellows_dye';tint=1 if face in [1,3] and n%6==0 else 0 if face in [1,3] else -1
     for v in [vertices,list(reversed(vertices))]:out.append(dict(quad(v,tex,tint),_shade=.63 if n%2==0 else .92))
   previous=ring
 return out
def door_preview(progress=0,closing=False,flash=True):
 color=((255,212,35) if flash else (0,0,0)) if closing else (70,240,105) if progress else (50,85,190)
 center=color if progress else (0,0,0)
 geo=faces(door_fixed())+[dict(q,_color=color) for q in faces(door_lights()+door_status())]+[dict(q,_color=center) if q['texture']=='gondola_led' else q for q in faces(door_moving(progress))]
 return geo,(50,85,190)
def assembly(cells=None,interior=False):
 cells=cells if cells is not None else layout();out=[]
 for c in cells:
  k=c['kind'];x,y,z=c['pos'];p=c['props'];l=p.get('left','true')=='true';r=p.get('right','true')=='true';top=p.get('top','false')=='true';bottom=p.get('bottom','false')=='true'
  if interior and (y>=4 or (x==4 and k not in ['seat','create:small_bogey'])):continue
  if k=='create:small_bogey':out.extend(native_bogey(c));continue
  if k.startswith('createrailwaysnavigator:'):
   geo=[cube([0,0,8 if p['z_alignment']=='positive' else 0],[16,16,16 if p['z_alignment']=='positive' else 8],'lrt_seal',-1)]
  elif k.startswith('create:'):
   geo=[cube([1,0,6],[15,8,14],'lrt_metal',-1),cube([3,7,5],[13,8,10],'lrt_seal',-1)]
  elif k=='door':
   if p['column']!='0' or p['row']!='0':continue
   geo=door_fixed()+door_lights()+[{**e,'tint':1} for e in door_status()]+[{**e,'texture':'lrt_seal'} if e['texture'] in ['gondola_led','lrt_led'] else e for e in door_moving(0)]
  elif k=='window':geo=window(l,r,top,bottom)
  elif k=='window_corner':geo=window_corner(l,r,top,bottom)
  elif k.startswith('cab_windshield'):geo=windshield(k.endswith('upper'),l,r,top,bottom)
  else:
   geo=mesh(k,left=l,right=r,top=top,bottom=bottom)+lights(k,left=l,right=r)
   if k=='ceiling_ring':
    for corner,a,b,flag in [('nw',l,top,p.get('raised','false')=='true'),('ne',r,top,p.get('powered','false')=='true'),('se',r,bottom,p.get('short','false')=='true'),('sw',l,bottom,p.get('half','bottom')=='top')]:
     if a and b and not flag:geo+=ring_inner(corner)
  out.extend(placed(geo,c))
 if not interior:out.extend(gangways(cells))
 return out
def render(geo,size=(1800,640),yaw=52,pitch=12,paint=(249,255,254),accent=(50,85,190),fit=None):
 W,H=size;background=np.array([27,38,53]);out=np.zeros((H,W,3));out[:]=background;depth=np.full((H,W),-1e9)
 yaw,pitch=map(math.radians,[yaw,pitch]);right=-np.array([math.cos(yaw),0,math.sin(yaw)]);forward=np.array([math.sin(yaw)*math.cos(pitch),math.sin(pitch),-math.cos(yaw)*math.cos(pitch)]);up=np.cross(forward,right)
 points=np.array([p for e in (geo if fit is None else fit) for p in e['_quad']]);center=(points.max(0)+points.min(0))/2
 sx=(points-center)@right;sy=(points-center)@up;scale=min((W-80)/max(1,np.ptp(sx)),(H-70)/max(1,np.ptp(sy)));center+=right*(sx.max()+sx.min())/2+up*(sy.max()+sy.min())/2
 def triangle(v,uv,tex,tint,bright,glass):
  v=np.array(v);screen=np.stack(((v-center)@right*scale+W/2,H/2-(v-center)@up*scale,(v-center)@forward),axis=1)
  x0,y0=np.floor(screen[:,:2].min(0)).astype(int);x1,y1=np.ceil(screen[:,:2].max(0)).astype(int);x0=max(0,x0);y0=max(0,y0);x1=min(W-1,x1);y1=min(H-1,y1)
  if x1<x0 or y1<y0:return
  xx,yy=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5);a,b,c=screen;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
  if abs(den)<1e-8:return
  q=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den;r=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;s=1-q-r;dist=q*a[2]+r*b[2]+s*c[2]
  region=out[y0:y1+1,x0:x1+1];mask=(q>=-1e-8)&(r>=-1e-8)&(s>=-1e-8)&(dist>depth[y0:y1+1,x0:x1+1]+1e-7)
  uv=np.array(uv)/16;u=q*uv[0,0]+r*uv[1,0]+s*uv[2,0];v=q*uv[0,1]+r*uv[1,1]+s*uv[2,1]
  tx=(u*tex.shape[1]).astype(int).clip(0,tex.shape[1]-1);ty=(v*tex.shape[0]).astype(int).clip(0,tex.shape[0]-1);col=tex[ty,tx,:3]*np.array(tint)/255*bright
  if glass:alpha=tex[ty,tx,3:4]/255;col=col*alpha+region*(1-alpha)
  region[mask]=col[mask];depth[y0:y1+1,x0:x1+1][mask]=dist[mask]
 solid=[e for e in geo if e['texture'] not in ['lrt_glass','lrt_glass_tinted','lrt_glass_frosted']];glass=sorted([e for e in geo if e['texture'] in ['lrt_glass','lrt_glass_tinted','lrt_glass_frosted']],key=lambda e:np.mean(np.array(e['_quad'])@forward))
 for e in solid+glass:
  v=np.array(e['_quad'],dtype=float);n=np.cross(v[1]-v[0],v[2]-v[0]);length=np.linalg.norm(n)
  if length<1e-8:continue
  n/=length
  if n@forward<=0:continue
  bright=(1 if e['texture'] in ['gondola_led','lrt_led'] else .7+.3*max(0,n@np.array([.35,.75,-.55])))*e.get('_shade',1);tint=e.get('_color',paint if e['tint']==0 else accent if e['tint']==1 else (255,255,255));uv=np.array(atlas_uv(e));tex=texture(e['texture'])
  triangle(v[[0,1,2]],uv[[0,1,2]],tex,tint,bright,e['texture'] in ['lrt_glass','lrt_glass_tinted','lrt_glass_frosted']);triangle(v[[0,2,3]],uv[[0,2,3]],tex,tint,bright,e['texture'] in ['lrt_glass','lrt_glass_tinted','lrt_glass_frosted'])
 return Image.fromarray(np.clip(out,0,255).astype('uint8'))
def main():
 bg=(27,38,53);canvas=Image.new('RGB',(1800,1810),bg);d=ImageDraw.Draw(canvas)
 font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',23);title=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',34);small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',19)
 d.text((40,24),'CREATE TRANSIT  /  LRT B  /  ALPHA 11',font=title,fill='#eff5ff')
 d.text((42,74),'Expanded ceiling lights and windows • deep bogey skirts • native bogeys',font=font,fill='#adc8e3')
 three=quick_build(True);two=quick_build(False)
 canvas.paste(render(assembly(three['cells']),(1800,430),yaw=75,pitch=15),(0,120))
 d.text((42,552),'QUICK BUILD 1  /  16 + 8 + 16 body blocks  /  two gangways  /  5 × 44 total footprint',font=font,fill='#e0edfc')
 d.text((42,589),'Short middle car: centered two-wide doorway on each side. Six native Create small bogeys.',font=small,fill='#adc8e3')
 canvas.paste(render(assembly(two['cells']),(1800,420),yaw=68,pitch=17),(0,644))
 d.text((42,1072),'QUICK BUILD 2  /  16 + 16 body blocks  /  one gangway  /  5 × 34 total footprint',font=font,fill='#e0edfc')
 d.text((42,1110),'Two cab cars joined by one articulated section. Four native Create small bogeys.',font=small,fill='#adc8e3')
 cab=[c for c in three['cells'] if c['pos'][2]<5]
 canvas.paste(render(assembly(cab,True),(600,440),yaw=55,pitch=35),(0,1180))
 door,color=door_preview(1);canvas.paste(render(door,(600,440),yaw=25,pitch=17,accent=color),(600,1180))
 canvas.paste(render(faces(pantograph(1)),(600,440),yaw=45,pitch=15),(1200,1180))
 d.text((40,1625),'Cab floor + driver cutaway',font=font,fill='#e0edfc');d.text((640,1625),'Compact outward plug-slide door',font=font,fill='#e0edfc');d.text((1240,1625),'Smooth hinged pantograph',font=font,fill='#e0edfc')
 d.text((40,1670),'Upward-facing grip floor facets.',font=small,fill='#adc8e3');d.text((640,1670),'Correct glass sprite. Leaves park outside.',font=small,fill='#adc8e3');d.text((1240,1670),'2-second raise / fold; reversible.',font=small,fill='#adc8e3')
 d.text((40,1740),'Software render of the same definitions used by the mod. CRN bays are blank here; route text is supplied by CRN in game.',font=small,fill='#829db8')
 d.text((40,1770),'Complementary shader reflections, bloom and live train rendering still require an in-game visual check.',font=small,fill='#829db8')
 canvas.save(ROOT/'preview/lrt-models.png');print('Rendered both articulated quick builds, the corrected cab floor, outward door and hinged pantograph.')
def animation():
 font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',20);small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',15);frames=[];door_fit=door_preview(0)[0]+door_preview(1)[0];panto_fit=faces(pantograph(False))+faces(pantograph(True))
 # Exact plug/slide and hinged mesh paths, resampled to 10 frames per second.
 for tick in range(0,100,2):
  opening=tick<40;raw=min(1,tick/32) if opening else max(0,1-(tick-40)/40);praw=min(1,tick/40) if opening else max(0,1-(tick-40)/40)
  # Door's two-stage function applies its own easing. Pantograph applies smoothstep.
  geo,color=door_preview(raw,not opening and raw>0,tick%8<4)
  im=Image.new('RGB',(1000,490),(27,38,53));im.paste(render(geo,(560,390),yaw=35,pitch=12,accent=color,fit=door_fit),(0,55));im.paste(render(faces(pantograph(progress=praw*praw*(3-2*praw))),(440,390),yaw=35,pitch=12,fit=panto_fit),(560,55))
  d=ImageDraw.Draw(im);d.text((25,14),'OUTWARD PLUG-SLIDE DOOR',font=font,fill='#eff5ff');d.text((585,14),'ANIMATED PANTOGRAPH',font=font,fill='#eff5ff');d.text((25,455),'Actual mesh paths • fixed frame • cosmetic pantograph • software preview',font=small,fill='#adc8e3');frames.append(im)
 frames[0].save(ROOT/'preview/lrt-animation.gif',save_all=True,append_images=frames[1:],duration=100,loop=0,optimize=True);print('Rendered animated door and pantograph mesh preview.')
if __name__=='__main__':
 import sys
 animation() if '--animation' in sys.argv else main()

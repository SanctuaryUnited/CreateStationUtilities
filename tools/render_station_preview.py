"""Textured orthographic rendering of shipped cuboids; not an in-game screenshot."""
from pathlib import Path
import numpy as np,math
from PIL import Image,ImageDraw,ImageFont
from station_geometry import *
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
textures={}
def rotated_points(points,e):
 points=np.array(points,dtype=float)
 if 'rotation'in e:
  o=np.array(e['rotation']['origin']);ang=math.radians(e['rotation']['angle']);c,s=math.cos(ang),math.sin(ang)
  rot={'x':np.array([[1,0,0],[0,c,-s],[0,s,c]]),'y':np.array([[c,0,s],[0,1,0],[-s,0,c]]),'z':np.array([[c,-s,0],[s,c,0],[0,0,1]])}[e['rotation']['axis']]
  points=(points-o)@rot.T+o
 if '_yaw'in e:
  o,ang=e['_yaw'];o=np.array(o);c,s=math.cos(ang),math.sin(ang);rot=np.array([[c,0,s],[0,1,0],[-s,0,c]]);points=(points-o)@rot.T+o
 return points
def render(mesh,frame=(86,94,106),accent=(52,106,205),rear=False,size=(560,350)):
 W,H=size;out=np.zeros((H,W,3),dtype=float);out[:]=[23,32,45];depth=np.full((H,W),-1e9)
 # Looking from front-right above, or rear-right above.
 yaw=math.radians(28 if not rear else 155);pitch=math.radians(18)
 right=-np.array([math.cos(yaw),0,math.sin(yaw)]);forward=np.array([math.sin(yaw)*math.cos(pitch),math.sin(pitch),-math.cos(yaw)*math.cos(pitch)]);up=np.cross(forward,right)
 allp=np.concatenate([rotated_points([[x,y,z]for x in [e['from'][0],e['to'][0]]for y in [e['from'][1],e['to'][1]]for z in [e['from'][2],e['to'][2]]],e)for e in mesh]);center=(allp.max(0)+allp.min(0))/2
 sx=(allp-center)@right;sy=(allp-center)@up;scale=min((W-70)/max(1,np.ptp(sx)),(H-55)/max(1,np.ptp(sy)))
 center=center+right*(sx.max()+sx.min())/2+up*(sy.max()+sy.min())/2
 def triangle(vertices,uv,texture,tint,brightness,glass):
  verts=np.array(vertices);project=np.stack(((verts-center)@right*scale+W/2,H/2-(verts-center)@up*scale,(verts-center)@forward),axis=1)
  x0,y0=np.floor(project[:,:2].min(0)).astype(int);x1,y1=np.ceil(project[:,:2].max(0)).astype(int);x0=max(0,x0);x1=min(W-1,x1);y0=max(0,y0);y1=min(H-1,y1)
  if x1<x0 or y1<y0:return
  xx,yy=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5);a,b,c=project
  den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1])
  if abs(den)<1e-8:return
  q=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den;r=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;s=1-q-r;dist=q*a[2]+r*b[2]+s*c[2]
  mask=(q>=0)&(r>=0)&(s>=0)&(dist>depth[y0:y1+1,x0:x1+1])
  uv=np.array(uv);u=q*uv[0,0]+r*uv[1,0]+s*uv[2,0];v=q*uv[0,1]+r*uv[1,1]+s*uv[2,1]
  tx=np.minimum((u*texture.shape[1]).astype(int),texture.shape[1]-1).clip(0);ty=np.minimum((v*texture.shape[0]).astype(int),texture.shape[0]-1).clip(0);col=texture[ty,tx,:3]*np.array(tint)/255*brightness
  region=out[y0:y1+1,x0:x1+1];alpha=texture[ty,tx,3:4]/255 if glass else np.ones((*xx.shape,1));col=col*alpha+region*(1-alpha);region[mask]=col[mask]
  depth[y0:y1+1,x0:x1+1][mask]=dist[mask]
 for e in sorted(mesh,key=lambda x:x['texture'].endswith('glass')):
  x,y,z=e['from'];X,Y,Z=e['to'];faces=[[(X,y,z),(x,y,z),(x,Y,z),(X,Y,z)],[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],[(x,y,z),(x,y,Z),(x,Y,Z),(x,Y,z)],[(X,y,Z),(X,y,z),(X,Y,z),(X,Y,Z)],[(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],[(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)]]
  face_names=FACES
  if '_quad'in e:faces=[e['_quad']];face_names=['north']
  n=e['texture'];
  if n not in textures:textures[n]=np.array(Image.open(A/f'textures/block/{n}.png').convert('RGBA')).astype(float)
  tint=frame if e['tint']==0 else accent if e['tint']==1 else(255,255,255)
  for f,vs in zip(face_names,faces):
   if f not in e['visible']:continue
   vs=rotated_points(vs,e)
   if '_swing'in e:
    pivot,angle=e['_swing'];o=np.array([pivot,0,8]);c,s=math.cos(angle),math.sin(angle);rot=np.array([[c,0,s],[0,1,0],[-s,0,c]]);vs=(vs-o)@rot.T+o
   normal=np.cross(vs[1]-vs[0],vs[2]-vs[0]);normal/=max(1e-9,np.linalg.norm(normal))
   if normal@forward<=0:continue
   bright=1 if n=='gondola_led'or n.startswith('led_')or n.startswith('fare_')or n.startswith('kiosk_')else .72+.28*max(0,normal@np.array([.2,.8,-.5]))
   uv=np.array([[0,1],[1,1],[1,0],[0,0]])
   triangle(vs[[0,1,2]],uv[[0,1,2]],textures[n],tint,bright,n.endswith('glass'));triangle(vs[[0,2,3]],uv[[0,2,3]],textures[n],tint,bright,n.endswith('glass'))
 return Image.fromarray(np.clip(out,0,255).astype('uint8'))
def gate(open=False,lanes=3,cap=True):
 mesh=[]
 for lane in range(lanes):
  offset=lane*32;mesh.extend(transformed(e,offset)for e in gate_fixed()+gate_lights());pivot=offset+16
  for e in gate_leaf():
   q=transformed(e,pivot)
   if open:q['_swing']=(pivot,math.radians(-90))
   mesh.append(q)
 if cap:mesh.extend(transformed(e,lanes*32)for e in gate_fixed()+gate_lights())
 return mesh
canvas=Image.new('RGB',(1680,940),(23,32,45));d=ImageDraw.Draw(canvas);font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',19);title=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',30)
d.text((38,24),'CREATE TRANSIT • NORMAL / MIRRORED FARE GATES',font=title,fill='#e1f0ff')
canvas.paste(render(gate(),size=(1620,410)),(30,70))
d.text((38,464),'Normal placement • passage on entrance left • repeat at 2-block intervals',font=font,fill='#d1eaff')
d.text((38,493),'Three independent lanes • 2 blocks per gate module + 1 block for the end reader = 7 blocks wide',font=font,fill='#90badc')
mirrored=[]
for e in gate(True,2,cap=False):
 q={**e,'from':[64-e['to'][0],e['from'][1],e['from'][2]],'to':[64-e['from'][0],e['to'][1],e['to'][2]]}
 if '_swing'in e:q['_swing']=(64-e['_swing'][0],-e['_swing'][1])
 mirrored.append(q)
views=[(mirrored,False,(86,94,106),(52,106,205),'Crouch-place • passage on entrance right'),(gate_fixed()+gate_lights(),False,(37,41,47),(248,134,40),'Dyeable decorative end reader • 1 block'),(gate(False,2),True,(86,94,106),(52,106,205),'Rear • red LEDs and STOP signs')]
for i,(mesh,rear,frame,accent,label)in enumerate(views):
 x=i*560;y=540;canvas.paste(render(mesh,frame,accent,rear,size=(560,320)),(x,y));d.text((x+24,y+322),label,font=font,fill='#b8d4ed')
d.text((38,902),'Shipped geometry and textures • Offline asset preview • Metal reflections and bloom require an in-game shader review',font=font,fill='#90a9bd');canvas.save(ROOT/'preview/station-models.png')
print('Rendered textured station asset preview.')

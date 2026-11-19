from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import json,numpy as np
from PIL import Image,ImageDraw,ImageFont
import render_lrt_preview as raster
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/createtransit';D=R/'build/v14-assets';O=D/'preview';O.mkdir(exist_ok=True)
MC=ZipFile(R/'build/moddev/artifacts/neoforge-21.1.248-client-extra-aka-minecraft-resources.jar')
font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',18);title=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',25)
texture={'FRAME':'frame','DOOR_FRAME':'frame','WALL':'panel','CEILING':'ceiling','COPY_PRIMARY':'panel','HANDRAIL':'trim','METAL':'edge','DOOR_TRIM':'edge','GLASS':'glass','DOOR_GLASS':'glass','DARK':'dark','DOOR_GASKET':'dark','DOOR_DISPLAY':'dark','LIGHT':'led','ACCENT':'led','DOOR_LED':'led','BODY_ACCENT':'white','FOAM':'white','FLOOR':'floor'}
# Native shader glow/reflections are deliberately not faked here.
def geo(doc,field):
 out=[];skin=doc['settings']['skin'].lower()
 for f in doc[field]:
  m=doc['materials'][f[0]];v=np.array(f[1:]).reshape(4,3);key=skin+'_'+texture.get(m,'dark')
  if m in ['WOOD','DOOR_WOOD'] or m=='CEILING' and skin=='wood_trim':key='wood'
  if key not in raster.TEXTURES:
   im=Image.open(BytesIO(MC.read('assets/minecraft/textures/block/spruce_planks.png'))) if key=='wood' else Image.open(A/'textures/block/elevator3/high'/skin/(texture.get(m,'dark')+'.png'))
   raster.TEXTURES[key]=np.array(im.convert('RGBA')).astype(float)
  if m in ['GLASS','DOOR_GLASS']:
   key='lrt_glass';raster.TEXTURES[key]=np.array(Image.open(A/'textures/block/elevator3/high'/skin/'glass.png')).astype(float)
  if m in ['LIGHT','ACCENT','DOOR_LED']:
   key='gondola_led';raster.TEXTURES[key]=np.ones((16,16,4))*255
  color=(255,255,255)
  if m in ['ACCENT','DOOR_LED']:color=(249,150,40) if skin=='wood_trim' else (38,212,229)
  if m in ['BODY_ACCENT','FOAM']:color=(40,110,210)
  out.append({'texture':key,'tint':-1,'_quad':(v*16).tolist(),'from':(v.min(0)*16).tolist(),'to':(v.max(0)*16).tolist(),'visible':['north'],'uv':[[0,16],[0,0],[16,0],[16,16]],'_color':color})
 return out
def interior_render(geo,size=(400,440),n=3):
 W,H=size;out=np.zeros((H,W,3));out[:]=(27,38,53);depth=np.full((H,W),1e10);cam=np.array([n*8,1.28*16,-.7*16]);focal=W*.56
 def tri(v,uv,tex,tint,brightness,glass):
  rel=v-cam;screen=np.column_stack([W/2+focal*rel[:,0]/rel[:,2],H/2-focal*rel[:,1]/rel[:,2],rel[:,2]])
  x0,y0=np.maximum(0,np.floor(screen[:,:2].min(0))).astype(int);x1,y1=np.minimum([W-1,H-1],np.ceil(screen[:,:2].max(0))).astype(int)
  if x1<x0 or y1<y0:return
  xx,yy=np.meshgrid(np.arange(x0,x1+1)+.5,np.arange(y0,y1+1)+.5);a,b,c=screen;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);
  if abs(den)<1e-8:return
  q=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den;r=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den;s=1-q-r;iz=q/a[2]+r/b[2]+s/c[2];zz=1/np.maximum(1e-12,iz);mask=(q>=0)&(r>=0)&(s>=0)&(zz<depth[y0:y1+1,x0:x1+1])
  u=(q*uv[0,0]/a[2]+r*uv[1,0]/b[2]+s*uv[2,0]/c[2])*zz;v=(q*uv[0,1]/a[2]+r*uv[1,1]/b[2]+s*uv[2,1]/c[2])*zz
  tx=(u%1*tex.shape[1]).astype(int).clip(0,tex.shape[1]-1);ty=(v%1*tex.shape[0]).astype(int).clip(0,tex.shape[0]-1);col=tex[ty,tx,:3]*np.array(tint)/255*brightness;region=out[y0:y1+1,x0:x1+1]
  if glass:alpha=tex[ty,tx,3:4]/255;col=col*alpha+region*(1-alpha)
  region[mask]=col[mask];depth[y0:y1+1,x0:x1+1][mask]=zz[mask]
 solids=[e for e in geo if e['texture']!='lrt_glass'];glass=sorted([e for e in geo if e['texture']=='lrt_glass'],key=lambda e:-np.mean(np.array(e['_quad'])[:,2]))
 for e in solids+glass:
  v=np.array(e['_quad']);normal=np.cross(v[1]-v[0],v[2]-v[0]);length=np.linalg.norm(normal)
  if length<1e-8 or normal@(cam-v.mean(0))<=0:continue
  normal/=length;tex=raster.TEXTURES[e['texture']];ww=np.linalg.norm(v[3]-v[0])/16;hh=np.linalg.norm(v[1]-v[0])/16;uv=np.array([[0,hh],[0,0],[ww,0],[ww,hh]])
  brightness=1 if e['texture']=='gondola_led' else .82+.18*max(0,normal@np.array([.1,-.5,-.8]));color=e.get('_color',(255,255,255))
  for ids in [[0,1,2],[0,2,3]]:tri(v[ids],uv[ids],tex,color,brightness,e['texture']=='lrt_glass')
 return Image.fromarray(np.clip(out,0,255).astype('uint8'))

if __name__=='__main__':
 for skin in ['BRUSHED_STEEL','MODERN_DARK','WOOD_TRIM','LIGHT_TRANSIT']:
  im=Image.new('RGB',(1600,570),(27,38,53));dr=ImageDraw.Draw(im);dr.text((25,15),skin.replace('_',' ')+' / PRODUCTION CABIN GEOMETRY',font=title,fill='white')
  for col,variant in enumerate(['SOLID','REAR_PANORAMIC','SCENIC','CYLINDRICAL']):
   doc=json.loads((D/'meshes'/f'3_{variant}_{skin}_SOLID.json').read_text());q=geo(doc,'body')
   # Omit front frame only for this labeled interior cutaway; the shipped mesh is intact.
   q=[e for e in q if not all(v[2]<.51*16 for v in e['_quad'])]
   im.paste(interior_render(q,(400,440)),(col*400,70));dr.text((col*400+22,515),variant.replace('_',' '),font=font,fill='white')
  dr.text((25,548),'Software render from shipped Java meshes • interior cutaway • no shader or in-game testing claim',font=font,fill='#a8c4db');im.save(O/(skin+'.png'))
 for skin in ['BRUSHED_STEEL','MODERN_DARK','WOOD_TRIM','LIGHT_TRANSIT']:
  im=Image.new('RGB',(1200,540),(27,38,53));dr=ImageDraw.Draw(im);dr.text((25,15),skin.replace('_',' ')+' / THREE DOOR MODELS',font=title,fill='white')
  for col,model in enumerate(['GLASS','SOLID','SLIM_WINDOW']):
   doc=json.loads((D/'meshes'/f'3_SOLID_{skin}_{model}.json').read_text());im.paste(raster.render(geo(doc,'landingClosed'),(400,420),yaw=180,pitch=0),(col*400,65));dr.text((col*400+22,498),model.replace('_',' '),font=font,fill='white')
  im.save(O/(skin+'_DOORS.png'))
 print('Rendered eight production-geometry sheets for cabin/door inspection.')

"""Software preview of actual dedicated JSON models and the door renderer geometry."""
from pathlib import Path
import json,inspect,math
from PIL import Image,ImageDraw,ImageFont
import render_lrt_preview as raster
from lrt_geometry import cube,faces
from export_elevator_kit import cabin
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
# Shared rasterization math only; every model and material comes from the elevator kit.
src=inspect.getsource(raster.render).replace("'lrt_glass'","'lrt_glass','elevator_glass'").replace("'lrt_led'","'lrt_led','elevator_led'")
exec(src,raster.__dict__)
def geo_model(name):
 doc=json.loads((A/'models/block'/f'{name}.json').read_text());out=[]
 for e in doc['elements']:
  for face,d in e['faces'].items():out.extend(faces([cube(e['from'],e['to'],doc['textures'][d['texture'][1:]].split('/')[-1],d.get('tintindex',-1),[face])]))
 return out
def build(glass=True,progress=0,cut=False,closing=False,tick=0):
 result=[]
 for p,c in cabin(glass).items():
  x,y,z=p
  if cut and (y==4 or x==2):continue
  name=c['name'].split(':')[1];props=c['props'];state=json.loads((A/'blockstates'/f'{name}.json').read_text());models=[]
  if 'multipart'in state:
   for entry in state['multipart']:
    if all(props.get(k)==v for k,v in entry['when'].items()):models.append(entry['apply']['model'].split(':')[1].split('/')[-1])
  else:
   for key,entry in state['variants'].items():
    if all(props.get(k)==v for k,v in (v.split('=')for v in key.split(','))):models.append(entry['model'].split(':')[1].split('/')[-1])
  geo=[]
  for m in models:geo+=geo_model(m)
  if name.endswith('_door')and props['row']=='0':
   smooth=progress*progress*(3-2*progress);signal=(255,210,42)if closing and tick%12<6 else (54,43,4)if closing else (50,255,105)if progress>=1 or progress>0 and tick%12<6 else (7,51,19)if progress>0 else (17,24,32)
   for zz in [-.05,1.95]:
    for xx in [.25,15.25]:geo+=faces([cube([xx,.5,zz],[xx+.5,29.2,zz+.1],'elevator_led',1)])
    geo +=[dict(q,_color=signal)for q in faces([cube([5,30.3,zz],[11,31.3,zz+.1],'elevator_led',-1)])]
   for side in range(2):
    shift=(-1 if side==0 else 1)*smooth*.42*16;xx=(1.7 if side==0 else 8.1)+shift;XX=(7.9 if side==0 else 14.3)+shift
    if glass:parts=[cube([xx,0,2.15],[XX,9,2.95],'elevator_metal'),cube([xx,26,2.15],[XX,30,2.95],'elevator_metal'),cube([xx,9,2.15],[xx+.65,26,2.95],'elevator_metal'),cube([XX-.65,9,2.15],[XX,26,2.95],'elevator_metal'),cube([xx+.65,9,2.35],[XX-.65,26,2.75],'elevator_glass',-1)]
    else:parts=[cube([xx,0,2.15],[XX,30,2.95],'elevator_metal')]
    parts.append(cube([xx,4,3],[XX,5.2,3.05],'elevator_led',1));geo+=faces(parts)
  if name in ['elevator_solid_wall','elevator_solid_corner','elevator_control_panel','elevator_door_header']:
   corner=name=='elevator_solid_corner'
   for yy,key in [(1,'down'),(14.75,'up')]:
    if props.get(key)=='true':continue
    parts=[cube([-.045 if corner else 0,yy,-.045],[16,yy+.75,-.005],'elevator_led',1),cube([2.005 if corner else 0,yy,2.005],[16,yy+.75,2.045],'elevator_led',1)]
    if corner:parts +=[cube([-.045,yy,-.005],[-.005,yy+.75,16],'elevator_led',1),cube([2.005,yy,2.045],[2.045,yy+.75,16],'elevator_led',1)]
    geo+=faces(parts)
  if name=='elevator_ceiling_light':geo +=[dict(q,_color=(255,239,208))for q in faces([cube([2,-.08,3],[14,.08,13],'elevator_led',-1)])]
  result+=raster.placed(geo,{'pos':p,'facing':props['facing']})
 return result
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
def main():
 out=ROOT/'preview';out.mkdir(exist_ok=True);sheet=Image.new('RGB',(1600,1120),'#1b2635');d=ImageDraw.Draw(sheet);big=ImageFont.truetype(FONT,36);med=ImageFont.truetype(FONT,22);small=ImageFont.truetype(FONT,17)
 d.text((38,28),'CREATE TRANSIT  /  MODULAR ELEVATORS',font=big,fill='#edf5fc');d.text((40,78),'Dedicated kit • dyeable frame + LEDs • independent drive • 3 clear interior blocks',font=med,fill='#91cee9')
 for i,glass in enumerate([True,False]):
  sheet.paste(raster.render(build(glass),(760,580),yaw=143,pitch=17,paint=(255,255,255),accent=(102,190,235)),(20+i*800,120));d.text((42+i*800,128),('GLASS CABIN'if glass else 'SOLID CABIN'),font=med,fill='#e4edf7')
  sheet.paste(raster.render(build(glass,1,True),(760,340),yaw=118,pitch=15,paint=(255,255,255),accent=(102,190,235)),(20+i*800,710));d.text((42+i*800,714),'OPEN DOORS / INTERIOR CUTAWAY',font=small,fill='#9dc5dc')
 d.text((38,1076),'Actual model geometry rendered in software. Final shader appearance requires an in-game client check.',font=small,fill='#abc3d8');sheet.save(out/'elevator-kit-preview.png')
 frames=[];fit=build(True)
 for t in list(range(0,41,4))+list(range(41,62,4))+list(range(62,103,4))+[103,107]:
  p=min(1,t/40)if t<62 else max(0,1-(t-62)/40);im=raster.render(build(True,p,False,t>=62,t),(800,720),yaw=143,pitch=17,accent=(102,190,235),fit=fit);draw=ImageDraw.Draw(im);draw.text((20,15),'OPENING'if t<40 else'OPEN'if t<62 else'CLOSING'if t<102 else'CLOSED',font=med,fill='#edf5fc');frames.append(im)
 frames[0].save(out/'elevator-door-animation.gif',save_all=True,append_images=frames[1:],duration=200,loop=0)
 print('Rendered actual JSON model preview and eased door animation.')
if __name__=='__main__':main()

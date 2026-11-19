"""Software previews from JSON exported by the production CabinGeometry class."""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import json,hashlib,math,os
import numpy as np
from PIL import Image,ImageDraw,ImageFont
import render_lrt_preview as raster
ROOT=Path(__file__).resolve().parents[1];DATA=ROOT/'build/elevator3-assets';OUT=DATA/'preview';OUT.mkdir(exist_ok=True)
MC_PATH=os.environ.get('MINECRAFT_CLIENT_JAR')
if not MC_PATH:
 candidates=sorted((Path.home()/'.gradle/caches').glob('**/minecraft_1.21.1_client.jar'))
 if not candidates:raise SystemExit('Set MINECRAFT_CLIENT_JAR to your Minecraft 1.21.1 client JAR before rendering.')
 MC_PATH=str(candidates[0])
MC=ZipFile(MC_PATH)
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
TITLE=ImageFont.truetype(BOLD,32);LABEL=ImageFont.truetype(FONT,18);SMALL=ImageFont.truetype(FONT,14)
NAMES={'REAR_PANORAMIC':'Rear panoramic','SOLID':'Solid enclosed','CYLINDRICAL':'Cylindrical rear','WOOD':'Wood trim','SCENIC':'Scenic glass box'}
def mc_texture(name):return np.array(Image.open(BytesIO(MC.read('assets/minecraft/textures/block/'+name+'.png'))).convert('RGBA')).astype(float)
raster.TEXTURES['gondola_led']=np.ones((16,16,4))*255
raster.TEXTURES['lrt_glass']=mc_texture('glass');raster.TEXTURES['lrt_glass'][:,:,3]*=.42
raster.TEXTURES['model_dark']=mc_texture('gray_concrete')
def geo(doc,cut=False):
 s=doc['settings'];out=[]
 for face in doc['body']+doc['doors']:
  m=doc['materials'][face[0]];v=np.array(face[1:]).reshape((4,3))
  if cut and (np.min(v[:,1])>=3.1 or np.min(v[:,0])>=float(s['size'])-.07):continue
  if m in ('FRAME','METAL'):tex='elevator_metal'
  elif m=='GLASS':tex='lrt_glass'
  elif m in ('LIGHT','ACCENT'):tex='gondola_led'
  elif m=='DARK':tex='model_dark'
  else:
   name=s['wood' if m=='WOOD' else 'floor'].split(':')[1];tex='model_'+name
   if tex not in raster.TEXTURES:raster.TEXTURES[tex]=mc_texture(name)
  tint=0 if m=='FRAME' else 1 if m=='ACCENT' else -1
  entry={'texture':tex,'tint':tint,'_quad':(v*16).tolist(),'from':(v.min(0)*16).tolist(),'to':(v.max(0)*16).tolist(),'visible':['north'],'uv':[[0,16],[0,0],[16,0],[16,16]]}
  if m=='LIGHT':entry['_color']=(255,239,211)
  out.append(entry)
 return out
def render(doc,size=(400,350),yaw=43,pitch=12,cut=False):return raster.render(geo(doc,cut),size,yaw=yaw,pitch=pitch,paint=(249,249,249),accent=(94,190,236))
def write_primary():
 sheet=Image.new('RGB',(1760,2200),'#1b2635');d=ImageDraw.Draw(sheet);d.text((32,23),'CREATE: TRANSIT  /  ELEVATORS 3.0',font=TITLE,fill='#edf5ff');d.text((34,72),'20 implemented cabin models • external footprints • open entrances',font=LABEL,fill='#9cc8de')
 for row,(style,label) in enumerate(NAMES.items()):
  for col,n in enumerate(range(2,6)):
   doc=json.loads((DATA/'models'/f'{n}x{n}_{style.lower()}.json').read_text());im=render(doc,(420,355),yaw=137 if style=='REAR_PANORAMIC' else 43);sheet.paste(im,(col*440+10,row*410+120));d.text((col*440+30,row*410+474),f'{n}×{n}  |  {label}',font=LABEL,fill='#edf5ff');im.save(OUT/f'{n}x{n}_{style.lower()}.png')
 d.text((34,2170),'Actual production geometry • software render • shader reflections and in-game lighting are not shown.',font=SMALL,fill='#9bb3c9');sheet.save(OUT/'Elevators3-All-20-Cabins.png')
def write_catalog():
 docs=[json.loads(p.read_text()) for p in sorted((DATA/'catalog').glob('*.json'))]
 for style,label in NAMES.items():
  family=[x for x in docs if x['settings']['variant']==style];cols=6;rows=math.ceil(len(family)/cols);sheet=Image.new('RGB',(1800,rows*255+135),'#1b2635');d=ImageDraw.Draw(sheet);d.text((26,20),label.upper()+' / ALL DOOR & GLAZING COMBINATIONS',font=LABEL,fill='#edf5ff');d.text((26,52),'Two views per configuration: front and rear • closed panels • sizes 2×2 through 5×5',font=SMALL,fill='#9cc8de')
  for index,doc in enumerate(family):
   x=(index%cols)*300;y=(index//cols)*255+87;s=doc['settings'];left=render(doc,(150,195),yaw=43,pitch=8);right=render(doc,(150,195),yaw=137,pitch=8);sheet.paste(left,(x,y));sheet.paste(right,(x+150,y));
   text=f"{s['size']}×{s['size']}  F:{s['front'][0]}  R:{s['rear'][0] if s['rearEnabled']=='true' else '—'}"
   d.text((x+12,y+195),text,font=SMALL,fill='#edf5ff');detail='Sides: '+('L' if s['leftGlass']=='true' else '—')+('R' if s['rightGlass']=='true' else '—') if style=='REAR_PANORAMIC' else ('Curved glazing: '+s['curvedGlass'] if style=='CYLINDRICAL' else 'S = solid   G = glass');d.text((x+12,y+216),detail,font=SMALL,fill='#9cc8de')
  sheet.save(OUT/f'Elevators3-Catalog-{style}.png')
def write_finishes():
 paths=sorted((DATA/'finishes').glob('*.json'));sheet=Image.new('RGB',(1600,1750),'#1b2635');d=ImageDraw.Draw(sheet);d.text((24,20),'INTERIOR FINISHES / LIGHTING / HANDRAILS',font=TITLE,fill='#edf5ff')
 for i,p in enumerate(paths):
  doc=json.loads(p.read_text());x=i%4*400;y=i//4*330+70
  im=raster.render([e for e in geo(doc) if min(v[1] for v in e['_quad'])>=3.075*16],(390,270),yaw=40,pitch=-65) if p.name.startswith('LIGHT') else render(doc,(390,270),pitch=10,cut=True);sheet.paste(im,(x,y));d.text((x+22,y+272),p.stem.replace('_',' ').title(),font=LABEL,fill='#edf5ff')
 d.text((24,1722),'Finish views use roof cutaways; lighting views isolate the ceiling underside. All geometry is shipped.',font=SMALL,fill='#9bb3c9');sheet.save(OUT/'Elevators3-Interior-Finishes.png')
if __name__=='__main__':
 import sys
 write_primary();print('Primary 20-cabin overview rendered',flush=True)
 if '--primary-only' not in sys.argv:write_catalog();print('184 structural/door/glazing cases rendered',flush=True);write_finishes()

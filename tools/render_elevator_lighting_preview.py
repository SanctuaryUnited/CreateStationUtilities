"""Render the dedicated ceiling models from below, with connected ring examples."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json
from render_elevator_preview import raster,geo_model
from lrt_geometry import cube,faces
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit';FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
def ring(points):
 geo=[];points=set(points)
 for x,z in points:
  n=(x,z-1)in points;e=(x+1,z)in points;s=(x,z+1)in points;w=(x-1,z)in points
  props={'facing':'north','north':str(n).lower(),'east':str(e).lower(),'south':str(s).lower(),'west':str(w).lower(),**{k:str((x+dx,z+dz)in points).lower()for k,(dx,dz)in {'ne':(1,-1),'se':(1,1),'sw':(-1,1),'nw':(-1,-1)}.items()}}
  model=json.loads((A/'blockstates/elevator_ceiling_ring.json').read_text());piece=[]
  for p in model['multipart']:
   if all(props.get(k)==v for k,v in p.get('when',{}).items()):piece+=geo_model(p['apply']['model'].split('/')[-1])
  X0=0 if w else 2;X1=16 if e else 14;Z0=0 if n else 3;Z1=16 if s else 13;lamps=[]
  if not n:lamps.append((X0,2,X1,3))
  if not s:lamps.append((X0,13,X1,14))
  if not w:lamps.append((2,Z0,3,Z1))
  if not e:lamps.append((13,Z0,14,Z1))
  for adjacent,diag,boxes in [(n and e,'ne',[(13,0,14,3),(14,2,16,3)]),(e and s,'se',[(13,13,16,14),(13,14,14,16)]),(s and w,'sw',[(2,13,3,16),(0,13,2,14)]),(w and n,'nw',[(0,2,3,3),(2,0,3,2)])]:
   if adjacent and props[diag]=='false':lamps+=boxes
  for a,b,c,d in lamps:piece+=faces([cube([a,-.24,b],[c,-.19,d],'elevator_led',1)])
  geo+=raster.placed(piece,{'pos':[x,0,z],'facing':'north'})
 return geo

def main():
 W,H=1650,1050;im=Image.new('RGB',(W,H),'#1b2635');d=ImageDraw.Draw(im);title=ImageFont.truetype(FONT,34);font=ImageFont.truetype(FONT,22);small=ImageFont.truetype(FONT,17)
 d.text((35,24),'CREATE TRANSIT / ELEVATOR CEILING LIGHTS',font=title,fill='#edf5fc');d.text((35,72),'Five dedicated roof fixtures • dyeable lamps + housings • underside view',font=font,fill='#91cee9')
 names=['ceiling_strip','ceiling_double_strip','ceiling_panel','ceiling_bulb','ceiling_ring']
 for i,name in enumerate(names):
  geo=geo_model('elevator_'+name+'_item');tile=raster.render(geo,(320,310),yaw=52,pitch=-28,accent=(102,190,235));im.paste(tile,(i*330,125));d.text((i*330+22,445),name.replace('ceiling_','').replace('_',' ').upper(),font=font,fill='#e5f2fd')
 d.text((35,510),'CONNECTED RINGS / ONE CONTINUOUS OUTLINE',font=font,fill='#edf5fc')
 examples=[([(0,0)],'SINGLE'),([(0,0),(1,0)],'TWO BLOCKS'),([(0,0),(1,0),(0,1),(1,1)],'2 × 2'),([(0,0),(1,0),(0,1)],'L SHAPE')]
 for i,(points,label)in enumerate(examples):
  tile=raster.render(ring(points),(400,330),yaw=52,pitch=-28,accent=(102,190,235));im.paste(tile,(i*410+5,570));d.text((i*410+25,925),label,font=font,fill='#e5f2fd')
 d.text((35,995),'Actual model geometry rendered in software. Place these blocks in the roof; crouch + dye changes the light color.',font=small,fill='#abc3d8');out=ROOT/'preview/elevator-ceiling-lights.png';im.save(out);print(out)
if __name__=='__main__':main()

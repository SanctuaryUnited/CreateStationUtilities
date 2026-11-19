"""Original native 16px tiles and shader companions, never crops concept art."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,hashlib
R=Path(__file__).resolve().parents[1]; A=R/'src/main/resources/assets'; O=A/'createtransit/textures/block/elevator3'
colors={'brushed_steel':(179,188,199),'modern_dark':(43,51,62),'wood_trim':(40,43,48),'light_transit':(221,228,232)}
manifest={'resolution':[16,16],'source':'Original deterministic pixel artwork. Reference sheets used for design only.','skins':list(colors),'door_models':['GLASS','SOLID','SLIM_WINDOW'],'textures':[]}
for quality in ['off','low','medium','high']:
 for skin,base in colors.items():
  for tile in ['frame','panel','ceiling','trim','glass','dark','white','floor','edge','led']:
   im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
   rgb=(160,170,184) if tile=='trim' else (9,15,21) if tile=='dark' else (255,255,255) if tile in ('white','led') else base
   if tile=='edge':rgb=(57,66,78) if skin in ('modern_dark','wood_trim') else (151,166,181)
   if tile=='ceiling' and skin in ('modern_dark','wood_trim'):rgb=(26,32,41)
   for y in range(16):
    for x in range(16):
     grain=((x//4+y*3)%5)-2 if tile not in ('glass','white','led','dark') else 0
     im.putpixel((x,y),tuple(max(0,min(255,c+grain)) for c in rgb)+(255,))
   if tile in ('frame','panel','ceiling'):
    d.line((0,0,15,0),fill=tuple(max(0,c-28) for c in rgb)+(255,));d.line((0,0,0,15),fill=tuple(max(0,c-32) for c in rgb)+(255,));d.line((1,1,15,1),fill=tuple(min(255,c+16) for c in rgb)+(255,))
   if tile=='glass':
    im=Image.new('RGBA',(16,16),(110,172,201,14));d=ImageDraw.Draw(im);d.line((1,11,7,5),fill=(211,240,250,96));d.line((9,12,12,9),fill=(211,240,250,64))
   if tile=='floor':
    for y in range(16):
     for x in range(16):
      v=(48,54,62) if (x//2+y//2)%2 else (35,40,48);im.putpixel((x,y),v+(255,))
   path=O/quality/skin;path.mkdir(parents=True,exist_ok=True);im.save(path/(tile+'.png'))
   normal=Image.new('RGB',(16,16),(128,128,255))
   if quality in ('medium','high') and tile in ('panel','frame','trim','ceiling'):
    nd=ImageDraw.Draw(normal);nd.line((0,0,15,0),fill=(128,148 if quality=='high' else 137,253));nd.line((0,0,0,15),fill=(148 if quality=='high' else 137,128,253))
   normal.save(path/(tile+'_n.png'))
   smooth=0 if quality=='off' else 120 if quality=='low' else 185 if quality=='medium' else 224
   if tile in ('white','dark','floor'):smooth=45 if quality!='off' else 0
   metal=tile in ('frame','trim','edge') or tile=='panel' and skin!='wood_trim'
   Image.new('RGBA',(16,16),(smooth,230 if metal and quality in ('medium','high') else 10,0,254 if tile=='led' and quality!='off' else 0)).save(path/(tile+'_s.png'))
   manifest['textures'].append(str((path/(tile+'.png')).relative_to(A/'createtransit/textures')))
# Touchscreen panels: diffuse templates and directional call states, single-height and double-height.
T=A/'createtransit/textures/block/elevator3/touch';T.mkdir(parents=True,exist_ok=True)
for layout in ['both','up','down']:
 for state in ['idle','hover','pressed','up_called','down_called','both_called','disabled','error']:
  im=Image.new('RGBA',(16,32 if layout=='both' else 16),(10,17,22,255));d=ImageDraw.Draw(im);h=im.height
  edge=(250,64,70,255) if state=='error' else (60,225,255,255) if state in ['hover','pressed','up_called','down_called','both_called'] else (108,125,139,255)
  d.rounded_rectangle((0,0,15,h-1),radius=2,outline=edge,width=1)
  for up,cy in ([(True,9),(False,23)] if layout=='both' else [(layout=='up',8)]):
   lit=state in ['pressed','both_called','error'] or state==('up_called' if up else 'down_called')
   color=(250,64,70,255) if state=='error' else (185,250,255,255) if lit else (102,112,120,255)
   points=[(4,cy+2),(8,cy-2),(12,cy+2)] if up else [(4,cy-2),(8,cy+2),(12,cy-2)]
   d.line(points,fill=color,width=2)
  im.save(T/(layout+'_'+state+'.png'))
manifest['touch_layouts']=['up','both','down'];manifest['states']=['idle','hover','pressed','up_called','down_called','both_called','disabled','error']
(O/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
atlas=A/'minecraft/atlases/blocks.json';doc=json.loads(atlas.read_text());doc['sources']=[s for s in doc['sources'] if not s.get('resource','').startswith(('createtransit:block/elevator3/','createtransit:block/elevator_door/'))]
for name in manifest['textures']+[str(p.relative_to(A/'createtransit/textures')) for p in T.glob('*.png')]:
 key='createtransit:'+name[:-4];doc['sources'].append({'type':'minecraft:single','resource':key,'sprite':key})
atlas.write_text(json.dumps(doc,indent=2)+'\n')
print('Generated',len(manifest['textures']),'native skin tiles with two companions each, 24 touchscreen tiles, and explicit atlas entries.')

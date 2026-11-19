"""Render production DoorGeometry exports, not concept art. Needs NumPy/Pillow and MINECRAFT_CLIENT_JAR."""
from pathlib import Path
from zipfile import ZipFile
from io import BytesIO
import os,json,numpy as np
from PIL import Image,ImageDraw,ImageFont
import render_lrt_preview as raster
ROOT=Path(__file__).resolve().parents[1];DATA=ROOT/'build/door-previews';OUT=ROOT/'build/door-style-preview.png';mc=ZipFile(os.environ['MINECRAFT_CLIENT_JAR'])
styles=['SOLID','GLASS','WOOD_TRIM','SLIM_WINDOW','DIVIDED_GLASS','INDUSTRIAL','RETRO','DARK_MODERN','ART_DECO','CLINICAL','TRANSIT','EXECUTIVE','HERITAGE','HIGH_TECH','COASTAL','MINIMAL','COPYCAT']
materials={'SOLID':('iron_block','polished_andesite'),'WOOD_TRIM':('iron_block','oak_planks'),'EXECUTIVE':('black_concrete','dark_oak_planks'),'ART_DECO':('green_concrete','gold_block'),'RETRO':('smooth_sandstone','gold_block'),'HERITAGE':('cut_copper','spruce_planks'),'INDUSTRIAL':('gray_concrete','polished_deepslate'),'DARK_MODERN':('gray_concrete','polished_deepslate'),'CLINICAL':('white_concrete','light_blue_concrete'),'TRANSIT':('iron_block','blue_concrete'),'COASTAL':('smooth_sandstone','cyan_concrete'),'HIGH_TECH':('white_concrete','light_gray_concrete'),'COPYCAT':('oak_planks','gold_block')}
def texture(name):
 name={'smooth_sandstone':'sandstone_top'}.get(name,name)
 key='door_'+name
 if key not in raster.TEXTURES:raster.TEXTURES[key]=np.array(Image.open(BytesIO(mc.read('assets/minecraft/textures/block/'+name+'.png'))).convert('RGBA')).astype(float)
 return key
raster.TEXTURES['gondola_led']=np.ones((16,16,4))*255
sheet=Image.new('RGB',(1600,1930),'#172536');draw=ImageDraw.Draw(sheet);font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',17);title=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',28);draw.text((25,18),'CREATE: TRANSIT / 17 PRODUCTION DOOR STYLES',font=title,fill='white');draw.text((25,55),'Hallway-facing landing views • shared cabin panel designs • actual mesh, software render',font=font,fill='#9fbad0')
for index,style in enumerate(styles):
 doc=json.loads((DATA/(style+'.json')).read_text());a,b=materials.get(style,('iron_block','polished_andesite'));images=[]
 for pose in ['closed']:
  geo=[]
  for face in doc[pose]:
   m=doc['materials'][face[0]];v=np.array(face[1:]).reshape((4,3));name={'COPY_PRIMARY':a,'COPY_SECONDARY':b,'FRAME':'iron_block','METAL':'iron_block','DARK':'gray_concrete','GLASS':'glass'}.get(m,'white_concrete');tex='gondola_led' if m in ['LIGHT','ACCENT'] else texture(name)
   if m=='GLASS':
    raster.TEXTURES['lrt_glass']=raster.TEXTURES[tex].copy();raster.TEXTURES['lrt_glass'][:,:,3]=100;tex='lrt_glass'
   geo.append({'texture':tex,'tint':1 if m=='ACCENT' else -1,'_quad':(v*16).tolist(),'from':(v.min(0)*16).tolist(),'to':(v.max(0)*16).tolist(),'visible':['north'],'uv':[[0,16],[0,0],[16,0],[16,16]]})
  images.append(raster.render(geo,(780,560),yaw=43,pitch=8,paint=(249,249,249),accent=(94,190,236)).resize((390,280)))
 x=index%4*400;y=index//4*360+90
 for n,im in enumerate(images):sheet.paste(im,(x+n*195,y))
 draw.text((x+15,y+285),str(index+1)+'. '+style.replace('_',' ').title(),font=font,fill='white')
draw.text((25,1900),'2-block landing height • 1/2-block frame depth • panel movement stays on one depth plane',font=font,fill='#9fbad0');sheet.save(OUT);print(OUT)

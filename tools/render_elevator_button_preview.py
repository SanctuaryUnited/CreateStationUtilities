"""Actual call-button model flush against supporting wall, in two dye colors."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
from render_elevator_preview import geo_model,raster
from lrt_geometry import cube,faces
ROOT=Path(__file__).resolve().parents[1];FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
im=Image.new('RGB',(1400,790),'#1b2635');d=ImageDraw.Draw(im);title=ImageFont.truetype(FONT,32);font=ImageFont.truetype(FONT,20)
d.text((35,25),'CREATE TRANSIT / WALL-MOUNTED ELEVATOR CALLS',font=title,fill='#edf5fc')
d.text((35,77),'Mounting plate rests on the clicked block face • white default • dyeable housing',font=font,fill='#91cee9')
wall=faces([cube([0,0,16],[16,16,32],'elevator_floor',-1)])
for i,paint in enumerate([(255,255,255),(110,176,214)]):
 geo=wall+geo_model('elevator_floor_call');tile=raster.render(geo,(670,580),yaw=32,pitch=15,paint=paint);im.paste(tile,(15+i*700,125));d.text((50+i*700,715),'WHITE'if i==0 else'DYED HOUSING',font=font,fill='#e4edf7')
d.text((35,755),'Actual model geometry rendered in software; collision and placement are tested in Minecraft.',font=ImageFont.truetype(FONT,16),fill='#abc3d8')
p=ROOT/'preview/elevator-buttons.png';im.save(p);print(p)

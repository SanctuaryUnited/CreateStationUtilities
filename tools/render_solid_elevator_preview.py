"""Actual solid cabin geometry with floor/roof edge LEDs; software preview."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
from render_elevator_preview import build,raster
ROOT=Path(__file__).resolve().parents[1]
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
im=Image.new('RGB',(1500,870),'#1b2635');d=ImageDraw.Draw(im);title=ImageFont.truetype(FONT,32);font=ImageFont.truetype(FONT,21);small=ImageFont.truetype(FONT,17)
d.text((35,25),'CREATE TRANSIT / CONNECTED SOLID ELEVATOR',font=title,fill='#edf5fc')
d.text((35,76),'Continuous metal walls • floor LED edge • roof LED edge • dyeable accents',font=font,fill='#91cee9')
for i,(cut,label) in enumerate([(False,'COMPLETE CABIN'),(True,'INTERIOR CUTAWAY')]):
 geo=build(False,1,cut)
 im.paste(raster.render(geo,(740,630),yaw=118 if cut else 143,pitch=15,paint=(255,255,255),accent=(102,190,235)),(i*750,135))
 d.text((35+i*750,135),label,font=font,fill='#edf5fc')
d.text((35,811),'Actual models and dynamic lamp geometry rendered in software. Final shader appearance needs an in-game client check.',font=small,fill='#abc3d8')
out=ROOT/'preview/elevator-solid-connected.png';im.save(out);print(out)

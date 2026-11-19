"""Render the shipped inset cabin kit and an exact footprint plan."""
from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageFont
from gondola_geometry import *
from inset_cabin_layout import geometry,oriented
from render_station_preview import render
ROOT=Path(__file__).resolve().parents[1]
canvas=Image.new('RGB',(1680,1900),(23,32,45));d=ImageDraw.Draw(canvas)
title=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',30)
font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',20)
small=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',17)
d.text((32,23),'CREATE TRANSIT • CABLE GONDOLA CABIN • ALPHA 8',font=title,fill='#e1f0ff')
def cable_mesh():
    source=ROOT/'src/main/resources/assets/createtransit/models/block'
    grip=[]
    for e in json.loads((source/'cable_grip_partial.json').read_text())['elements']:
        a,b=e['from'],e['to'];grip.append(cube([40-b[0],111.875-b[1],40+a[2]],[40-a[0],111.875-a[1],40+b[2]],next(iter(e['faces'].values()))['texture'][1:],-1))
    for width,low,high in [(3,-.625,.625),(1.8,-1.5,-.625),(1.8,.625,1.5)]:grip.append(cube([40-width/2,96+low,-16],[40+width/2,96+high,96],'cable_steel',-1))
    return grip
canvas.paste(render(geometry()+cable_mesh(),frame=(203,210,219),accent=(52,106,205),size=(840,490)),(0,65))
canvas.paste(render(geometry(True,True),frame=(203,210,219),accent=(52,106,205),size=(840,490)),(840,65))
d.text((35,535),'Hanging cable grip • 5 × 5 grid • ~3 × 3 cabin',font=font,fill='#bbd7ef')
d.text((878,535),'Interior • two-face corner lights • bench seats',font=font,fill='#bbd7ef')
# Floor reaches 13..67 pixels within the 0..80 block grid.
ox,oy,s=100,598,3.5
d.rectangle((ox,oy,ox+80*s,oy+80*s),outline='#557088',width=2)
d.rectangle((ox+13*s,oy+13*s,ox+67*s,oy+67*s),fill='#28456c',outline='#88c0f0',width=2)
for n in range(0,81,16):
    d.line((ox+n*s,oy,ox+n*s,oy+80*s),fill='#45586b');d.line((ox,oy+n*s,ox+80*s,oy+n*s),fill='#45586b')
d.line((ox+16*s,oy+15*s,ox+64*s,oy+15*s),fill='#80d9ff',width=7)
d.text((116,690),'~3 × 3 shell',font=font,fill='#d5e9fa')
d.text((80,889),'Inset floor rims keep the base narrow too',font=small,fill='#bbd7ef')
junction=[]
for y in [-16,0]:junction.extend(oriented(e,0,0,y,0)for e in mesh('led_vertical_outer_corner')+led_lights('led_vertical_outer_corner'))
junction.extend(oriented(e,0,0,16,0)for e in mesh('led_horizontal_outer_corner')+led_lights('led_horizontal_outer_corner'))
junction.extend(oriented(e,2,16,16,0)for e in mesh('led_horizontal')+led_lights('led_horizontal'))
junction.extend(oriented(e,1,0,16,16)for e in mesh('led_horizontal')+led_lights('led_horizontal'))
canvas.paste(render(junction,frame=(203,210,219),accent=(52,106,205),rear=True,size=(560,285)),(560,595))
d.text((593,889),'Vertical → horizontal • continuous corner glow',font=small,fill='#bbd7ef')
door=wide_fixed()+wide_lights()+wide_status()+wide_moving(0,center=True)
door=[{**e,'texture':'gondola_seal'}if e in wide_center_lights()+wide_center_lights(True)else e for e in door]
canvas.paste(render(door,frame=(203,210,219),accent=(52,106,205),size=(560,285)),(1120,595))
d.text((1140,889),'3 × 3 door • flush panels • center signal lamps',font=small,fill='#bbd7ef')
labels={'led_vertical_outer_corner':'Outer corner vertical LEDs','led_horizontal_outer_corner':'Outer corner horizontal LEDs','floor_outer_corner':'Inset floor corner','floor_edge':'Inset floor rim','outer_corner':'Inset outer corner post'}
for i,kind in enumerate(KINDS):
    row,col=divmod(i,6);x=col*280;y=942+row*184
    geom=mesh(kind)+(led_lights(kind)if kind in LED_KINDS else door_status()if kind=='door'else[])
    canvas.paste(render(geom,frame=(249,255,254),accent=(249,255,254),rear=kind.endswith('outer_corner'),size=(280,145)),(x,y))
    d.text((x+12,y+150),labels.get(kind,kind.replace('_',' ').title()),font=small,fill='#bbd7ef')
canvas.paste(render([cube([6.5,-1.5,0],[9.5,1.5,16],'cable_steel',-1)],size=(280,145)),(1400,1494))
d.text((1412,1644),'Cable Track (Steam n Rails)',font=small,fill='#bbd7ef')
d.text((35,1687),'23 white dyeable cabin parts + Cable Track • Create stations, controls and schedules power the line',font=font,fill='#d5e9fa')
d.text((35,1718),'Actual shipped meshes • Offline preview • Graphical client and Complementary shader appearance remain unverified',font=small,fill='#8fa9bf')
for i,(shape,half)in enumerate([('straight','bottom'),('outer_left','bottom'),('inner_left','bottom'),('outer_right','top'),('inner_right','top')]):
    x=336*i;canvas.paste(render(roof_edge(shape,half),frame=(203,210,219),accent=(52,106,205),size=(336,105)),(x,1760));d.text((x+25,1868),shape.replace('_',' ')+' / '+half,font=small,fill='#bbd7ef')
canvas.save(ROOT/'preview/gondola-models.png');print('Rendered inset cabin, 5x5 footprint plan, LED junction and 3x3 doorway from shipped meshes.')

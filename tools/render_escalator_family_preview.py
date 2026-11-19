"""Preview the actual shared-family blockstate selections, including power-off trim."""
from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageFont
import render_escalator_preview as kit

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'preview/escalator-family'

def piece(kind,pos,glass,left,right,powered=False,stopped=False,through=False,front=False):
    name='escalator_'+('glass' if glass else 'solid')+('_redstone' if powered else '')
    props={'facing':'north','shape':kind,'left':str(left).lower(),'right':str(right).lower(),
           'slope_join':str(front if kind=='flat' else kind in ('lower_landing','upper_landing')).lower(),
           'flat_join':str(through).lower(),'reversed':'false','stopped':str(stopped).lower()}
    doc=json.loads((kit.A/'blockstates'/(name+'.json')).read_text());geo=[]
    for rule in doc['multipart']:
        if all(props[k]==v for k,v in rule['when'].items()):geo+=kit.load_model(rule['apply']['model'])
    geo+=kit.dynamic(kind,props,.04)
    return kit.raster.placed(geo,{'pos':pos,'facing':'north'})

def run(flat=False,glass=True,powered=False,stopped=False,lone=False):
    parts=[('flat',(0,0,5),False),('flat',(0,0,4),False),('lower_landing',(0,0,3),True),
           ('incline',(0,0,2),False),('incline',(0,1,1),False),('upper_landing',(0,2,0),True),('flat',(0,2,-1),False)]
    if flat:parts=[('flat',(0,0,z),False) for z in range(5)]
    if lone:parts=[('flat',(0,0,0),False)]
    neighbors={(y,z):kind for kind,(_,y,z),_ in parts}
    geo=[]
    for x in range(2):
        for kind,(_,y,z),through in parts:
            front=False
            if kind=='flat':
                through=neighbors.get((y,z+1)) in ('flat','lower_landing','upper_landing')
                front=neighbors.get((y,z-1)) in ('flat','lower_landing','upper_landing')
            geo+=piece(kind,(x,y,z),glass,x==1,x==0,powered,stopped,through,front)
    return geo

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    im=Image.new('RGB',(1800,1130),(27,38,53));draw=ImageDraw.Draw(im)
    bold='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf';font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    draw.text((35,25),'CREATE TRANSIT / CLEAN DECKS + FLAT INVENTORY ICONS',font=ImageFont.truetype(bold,31),fill='#eff6ff')
    draw.text((37,73),'Alpha 0.1.0 patch 2 • actual model geometry and item textures • software render',font=ImageFont.truetype(font,20),fill='#9fdcea')
    on=run(glass=False,powered=True);off=run(glass=False,powered=True,stopped=True)
    panels=[('TRAVELATOR: ROUNDED ENDS','Automatic entrance and exit returns on a flat run.',run(flat=True),None),
            ('TRAVELATOR TO ESCALATOR','Continuous handrails and glass; no internal end housings.',run(),None),
            ('FLAT DECK CLOSE-UP','Moving belt meets the fixed end plates without overlaps.',run(flat=True),None)]
    for i,(title,caption,geo,fit) in enumerate(panels):
        x=30+(i%2)*900;y=128+(i//2)*477
        draw.text((x+8,y),title,font=ImageFont.truetype(bold,24),fill='#eff6ff')
        im.paste(kit.raster.render(geo,(870,366),yaw=130,pitch=65 if i==2 else 24,accent=(76,196,216),fit=fit),(x,y+39))
        draw.text((x+8,y+416),caption,font=ImageFont.truetype(font,17),fill='#b4cfe5')
    draw.text((938,605),'2D INVENTORY ICONS',font=ImageFont.truetype(bold,24),fill='#eff6ff')
    for i,(name,label) in enumerate([('glass','Glass'),('solid','Solid'),('glass_redstone','Redstone glass'),('solid_redstone','Redstone solid')]):
        doc=json.loads((kit.A/'models/item'/f'escalator_{name}.json').read_text())
        sprite=Image.new('RGBA',(64,64))
        for identifier in doc['textures'].values():
            sprite=Image.alpha_composite(sprite,Image.open(kit.A/'textures'/(identifier.split(':')[1]+'.png')).convert('RGBA'))
        sprite=sprite.resize((170,170),Image.Resampling.NEAREST)
        x=937+i*208;im.paste(sprite,(x,675),sprite)
        draw.text((x+4,875),label,font=ImageFont.truetype(font,18),fill='#b4cfe5')
    catalog=json.loads((ROOT/'build/inventory-validation/inventory-layout.json').read_text())
    assert catalog['transit_tabs_adjacent']
    draw.text((939,932),'Verified NeoForge tab order',font=ImageFont.truetype(font,18),fill='#9fdcea')
    for x,X,label in [(938,1249,'VEHICLE KITS'),(1261,1756,'STATION UTILITIES')]:
        draw.rounded_rectangle((x,968,X,1014),radius=6,fill='#334b63',outline='#52c6df',width=2)
        draw.text((x+18,979),label,font=ImageFont.truetype(bold,22),fill='#eff6ff')
    draw.text((38,1082),'Software preview of shipped geometry and icons. Complementary appearance and hardware FPS still need an in-game check.',font=ImageFont.truetype(font,18),fill='#89a9c2')
    path=OUT/'CREATE-Transit-Alpha-0.1.0-Terminal-Preview.png';im.save(path);print(path)

if __name__=='__main__':main()

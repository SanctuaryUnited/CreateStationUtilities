"""Close-up views of released Alpha15.3 model JSON and native moving geometry."""
from pathlib import Path
import sys,json
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
PROJECT=ROOT
sys.path.insert(0,str(PROJECT/'tools'))
import render_escalator_preview as kit
OUT=ROOT/'preview/escalator-inner-led';OUT.mkdir(parents=True,exist_ok=True)
BG=(27,38,53)
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'

def piece(kind,material,pos=(0,0,0),joined_left=False,joined_right=False,slope_join=False):
    props={'facing':'north','left':str(joined_left).lower(),'right':str(joined_right).lower(),'reversed':'false','stopped':'false','slope_join':str(slope_join).lower(),'flat_join':'false'}
    state=json.loads((kit.A/'blockstates'/f'escalator_{kind}_{material}.json').read_text())
    geo=[]
    for entry in state['multipart']:
        if all(props.get(k)==v for k,v in entry['when'].items()):geo+=kit.load_model(entry['apply']['model'])
    geo+=kit.dynamic(kind,props,.04)
    return kit.raster.placed(geo,{'pos':pos,'facing':'north'})

def card(filename,title,subtitle,geo,yaw=132,pitch=21):
    im=Image.new('RGB',(1400,1120),BG);d=ImageDraw.Draw(im)
    d.text((42,28),title,font=ImageFont.truetype(BOLD,34),fill='#f0f6ff')
    d.text((44,82),subtitle,font=ImageFont.truetype(FONT,23),fill='#9fdcea')
    im.paste(kit.raster.render(geo,(1340,900),yaw=yaw,pitch=pitch,accent=(76,196,216)),(30,135))
    d=ImageDraw.Draw(im)
    d.text((44,1055),'Actual release models • cyan LED dye shown • software render',font=ImageFont.truetype(FONT,22),fill='#b4cfe5')
    im.save(OUT/filename)

glass=[]
for x in (0,1):
    glass+=piece('lower_landing','glass',(x,0,1),joined_left=x==1,joined_right=x==0,slope_join=True)
    glass+=piece('incline','glass',(x,0,0),joined_left=x==1,joined_right=x==0)
card('Escalator-Glass-Inner-LED.png','GLASS LANDING + INCLINE','Inside + outside LED trim, with a raised passage-side channel.',glass,yaw=132,pitch=21)
card('Escalator-Solid-Inner-LED.png','SOLID INCLINE PIECE','Inside LED above the tread edges; outside LED below the casing.',piece('incline','solid'),yaw=144,pitch=29)
print('Rendered two close-ups from shipped model JSON and moving geometry.')

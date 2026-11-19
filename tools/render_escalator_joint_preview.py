"""Software preview of the shipped joint meshes; no shader/FPS claim."""
from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageFont
import render_escalator_preview as kit
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'preview/escalators-smart';OUT.mkdir(parents=True,exist_ok=True)

def piece(kind,pos,glass=True,left=False,right=False):
    name='escalator_'+('glass' if glass else 'solid')
    props={'facing':'north','shape':kind,'left':str(left).lower(),'right':str(right).lower(),'slope_join':'true','flat_join':'false','reversed':'false','stopped':'false'}
    doc=json.loads((kit.A/'blockstates'/(name+'.json')).read_text());geo=[]
    for rule in doc['multipart']:
        if all(props[k]==v for k,v in rule['when'].items()):geo+=kit.load_model(rule['apply']['model'])
    geo+=kit.dynamic(kind,props,.04)
    return kit.raster.placed(geo,{'pos':pos,'facing':'north'})

def main():
    im=Image.new('RGB',(1600,1230),(27,38,53));d=ImageDraw.Draw(im)
    bold='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf';font='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
    d.text((35,25),'CREATE TRANSIT / ESCALATOR JOINT FIX',font=ImageFont.truetype(bold,34),fill='#f0f6ff')
    d.text((36,77),'Actual released model geometry • cyan LED dye • software render',font=ImageFont.truetype(font,23),fill='#9fdcea')
    for row,(label,kinds) in enumerate([('LOWER LANDING + INCLINE',[('lower_landing',(0,0,1)),('incline',(0,0,0))]),('INCLINE + UPPER LANDING',[('incline',(0,0,1)),('upper_landing',(0,1,0))])]):
        geo=[]
        for x in (0,1):
            for kind,pos in kinds:geo+=piece(kind,(x,pos[1],pos[2]),left=x==1,right=x==0)
        y=130+row*525;d.text((36,y),label,font=ImageFont.truetype(bold,24),fill='#f0f6ff')
        image=kit.raster.render(geo,(1520,455),yaw=132,pitch=24,accent=(76,196,216));im.paste(image,(40,y+42))
    d.text((36,1180),'Matching mitered edges close the handrail gaps at both ends.',font=ImageFont.truetype(font,22),fill='#b4cfe5')
    p=OUT/'Escalator-Joints-Alpha16.5.png';im.save(p);print(p)
if __name__=='__main__':main()

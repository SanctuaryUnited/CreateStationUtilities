"""Render the shipped expanded LRT meshes and a connected 2x2 ceiling ring."""
from pathlib import Path
from zipfile import ZipFile
from PIL import Image,ImageDraw,ImageFont
import render_lrt_preview as r
from lrt_geometry import *
from lrt_car_layout import quick_build
ROOT=Path(__file__).resolve().parents[1]
cache=ROOT.parents[1]/'build-cache/caches/modules-2/files-2.1/maven.modrinth/create/6.0.10+mc1.21.1';jar=next(cache.glob('*/*.jar'),None)
if jar:r.CREATE=ZipFile(jar)
font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',21)
bold=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',30)
def main():
 im=Image.new('RGB',(1800,1850),(27,38,53));d=ImageDraw.Draw(im)
 d.text((35,20),'CREATE TRANSIT / LRT EXPANSION / ALPHA 11',font=bold,fill='#eef5ff')
 d.text((35,66),'5 ceiling styles / 6 window styles / 5 skirt modules / dyeable housing and accents',font=font,fill='#a8c7e1')
 data=quick_build(False);im.paste(r.render(r.assembly(data['cells']),(1800,410),yaw=73,pitch=15),(0,105))
 d.text((35,506),'Updated two-cab QuickBuild: native bogeys covered from the sides; articulated joint preserved.',font=font,fill='#eef5ff')
 for i,k in enumerate(['ceiling_strip','ceiling_double_strip','ceiling_panel','ceiling_bulb','ceiling_ring']):
  im.paste(r.render(faces(mesh(k,left=False,right=False)),(350,240),yaw=30,pitch=-25),(i*355+5,560));d.text((i*355+25,800),k.replace('ceiling_','').replace('_',' ').upper(),font=font,fill='#eef5ff')
 ring=[]
 for x in range(2):
  for z in range(2):ring+=r.placed(ceiling_light('ceiling_ring',x>0,x<1,z>0,z<1),{'pos':[x,0,z],'facing':'north'})
 im.paste(r.render(ring,(700,300),yaw=10,pitch=-60),(0,855))
 d.text((40,1140),'Connected 2x2 ring: shared edges disappear.',font=font,fill='#a8c7e1')
 # Three blocks show continuous tinted/frosted glazing and dedicated rounded corners.
 w=[]
 for z,k in enumerate(['window_panorama','window_tinted','window_frosted']):w+=r.placed(mesh(k,left=z>0,right=z<2),{'pos':[0,0,z],'facing':'west'})
 im.paste(r.render(w,(1100,300),yaw=25,pitch=12),(700,855))
 d.text((750,1140),'Panoramic / tinted / frosted. Models use separate glass layers.',font=font,fill='#a8c7e1')
 for i,k in enumerate(sorted(WINDOW_KINDS)):
  im.paste(r.render(faces(mesh(k,left=False,right=False)),(300,270),yaw=25,pitch=12),(i*300,1200));d.text((i*300+18,1465),k.replace('window_','').replace('_',' ').upper(),font=font,fill='#eef5ff')
 for i,k in enumerate(['body_skirt','bogey_skirt','skirt_end','skirt_access','cab_skirt']):
  im.paste(r.render(faces(mesh(k,left=False,right=False)),(355,245),yaw=25,pitch=12),(i*355,1525));d.text((i*355+18,1770),k.replace('_',' ').upper(),font=font,fill='#eef5ff')
 d.text((35,1815),'Actual mesh render. Live client, CRN content and Complementary shader appearance still need a visual check.',font=font,fill='#809bb5')
 p=ROOT/'preview/lrt-expansion-alpha11.png';im.save(p);print(p)
if __name__=='__main__':main()

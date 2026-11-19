"""Render the released structure NBT, baked JSON meshes and moving tread geometry."""
from pathlib import Path
import sys,json,math,inspect,copy
import numpy as np
from PIL import Image,ImageDraw,ImageFont
PROJECT=Path(__file__).resolve().parents[1];sys.path.insert(0,str(PROJECT/'tools'))
import render_lrt_preview as raster
from lrt_geometry import quad,cube,faces
from nbt_read import read
A=PROJECT/'src/main/resources/assets/createtransit';OUT=PROJECT/'preview/escalators-refined';OUT.mkdir(parents=True,exist_ok=True)
# Reuse only the existing software rasterizer, with these dedicated materials.
src=inspect.getsource(raster.render).replace("'lrt_glass','lrt_glass_tinted','lrt_glass_frosted'","'escalator_glass'").replace("'gondola_led','lrt_led'","'escalator_led','escalator_arrow','escalator_stop'")
exec(src,raster.__dict__)
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf';BOLD='/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf'
def box(a,b,tex,tint=-1,color=None):
 result=faces([cube(a,b,'escalator_'+tex,tint)])
 if color is not None:
  for q in result:q['_color']=color
 return result
def load_model(path):
 doc=json.loads((A/'models'/ (path.split(':')[1]+'.json')).read_text())
 return [quad(q['vertices'],doc['textures'][q['texture'][1:]].split('/')[-1],q['tint'],q['uv'])for q in doc['quads']]
def dynamic(kind,props,phase):
 from escalator_geometry import moving
 return [quad(q['vertices'],'escalator_'+q['texture'][1:],q['tint'],q['uv']) for q in moving(kind,props['left']=='false',props['right']=='false',phase,props['reversed']=='true',props['stopped']=='true',props.get('flat_join','false')=='true',props.get('slope_join','false')=='true')]
def build(name,phase=0):
 data=read(PROJECT/'schematics'/ (name+'.nbt'));result=[]
 blocks={tuple(b['pos']):data['palette'][b['state']] for b in data['blocks']}
 for b in data['blocks']:
  state=data['palette'][b['state']];name=state['Name'];pos=b['pos']
  if not name.startswith('createtransit:escalator_'):continue
  props=dict(state['Properties']);kind=props.get('shape',name.split('escalator_')[1].rsplit('_',1)[0])
  dx,dz={'north':(0,-1),'east':(1,0),'south':(0,1),'west':(-1,0)}[props['facing']]
  target=(pos[0]+dx,pos[1],pos[2]+dz) if kind=='lower_landing' else (pos[0]-dx,pos[1]-1,pos[2]-dz)
  neighbor=blocks.get(target,{});neighbor_props=neighbor.get('Properties',{})
  neighbor_kind=neighbor_props.get('shape',neighbor.get('Name','').split('escalator_')[-1].rsplit('_',1)[0])
  props['slope_join']=str(kind in ('lower_landing','upper_landing') and neighbor_kind=='incline').lower()
  flat_target=(pos[0]-dx,pos[1],pos[2]-dz) if kind=='lower_landing' else (pos[0]+dx,pos[1],pos[2]+dz)
  flat_neighbor=blocks.get(flat_target,{});flat_props=flat_neighbor.get('Properties',{})
  flat_kind=flat_props.get('shape',flat_neighbor.get('Name','').split('escalator_')[-1].rsplit('_',1)[0])
  props['flat_join']=str(kind in ('lower_landing','upper_landing') and flat_neighbor.get('Name','').startswith('createtransit:escalator_') and flat_kind!='incline').lower()
  if kind=='flat':
   for key,sign in (('flat_join',-1),('slope_join',1)):
    n=blocks.get((pos[0]+dx*sign,pos[1],pos[2]+dz*sign),{})
    props[key]=str(n.get('Name','').startswith('createtransit:escalator_')).lower()
  doc=json.loads((A/'blockstates'/ (name.split(':')[1]+'.json')).read_text());geo=[]
  for entry in doc['multipart']:
   if all(props.get(k)==v for k,v in entry['when'].items()):geo+=load_model(entry['apply']['model'])
  geo+=dynamic(kind,props,phase)
  result+=raster.placed(geo,{'pos':pos,'facing':props['facing']})
 return result
GLASS='Escalator-QuickBuild-2-Wide-Glass';SOLID='Escalator-QuickBuild-2-Wide-Solid';FLAT='Travelator-QuickBuild-3-Wide'
def main():
 W,H=1800,1390;bg=(27,38,53);sheet=Image.new('RGB',(W,H),bg);d=ImageDraw.Draw(sheet)
 big=ImageFont.truetype(BOLD,37);med=ImageFont.truetype(BOLD,25);small=ImageFont.truetype(FONT,21)
 d.text((42,30),'CREATE TRANSIT  /  REFINED ESCALATOR KIT',font=big,fill='#edf5fc')
 d.text((44,88),'Concept-matched casing, terminal rails, landing plates and direction indicators',font=small,fill='#91cee9')
 for i,(name,label)in enumerate([(GLASS,'GLASS ESCALATOR'),(SOLID,'SOLID ESCALATOR')]):
  x=i*900;sheet.paste(raster.render(build(name,.04),(880,610),yaw=142,pitch=24,accent=(76,196,216)),(x+10,172))
  d.text((x+44,146),label,font=med,fill='#edf5fc');d.text((x+44,793),'2 blocks wide  /  4-block rise  /  no middle rail',font=small,fill='#b4cfe5')
 d.line((42,854,W-42,854),fill='#496579',width=2)
 d.text((44,889),'WIDENED TRAVELATOR',font=med,fill='#edf5fc')
 sheet.paste(raster.render(build(FLAT,.04),(1712,300),yaw=88,pitch=18,accent=(76,196,216)),(44,935))
 d.text((44,1255),'3 blocks wide  /  flat belt  /  matching sections connect side by side',font=small,fill='#b4cfe5')
 d.text((44,1328),'Software preview of actual kit geometry. Support blocks hidden; Minecraft shaders may change the appearance.',font=ImageFont.truetype(FONT,18),fill='#89a9c2')
 sheet.save(OUT/'Escalator-Refined-Preview.png')
 if "--still" in sys.argv:return
 # One full phase of the exact renderer, including the handrail seams.
 frames=[];fit=build(GLASS,0)
 for t in range(36):
  phase=t/36;im=Image.new('RGB',(860,760),bg)
  im.paste(raster.render(build(GLASS,phase),(840,650),yaw=142,pitch=27,accent=(76,196,216),fit=fit),(10,66))
  dr=ImageDraw.Draw(im);dr.text((24,20),'MOVING STEPS + HANDRAILS',font=ImageFont.truetype(BOLD,23),fill='#edf5fc');dr.text((24,724),'Actual mesh paths • software animation preview',font=ImageFont.truetype(FONT,17),fill='#a8c6de');frames.append(im)
 # A full phase repeat takes 1 / 0.7 seconds; 36 frames at 40 ms closely match it.
 frames[0].save(OUT/'Escalator-Refined-Motion.gif',save_all=True,append_images=frames[1:],duration=40,loop=0,optimize=True)
 print('Rendered actual glass/solid widened QuickBuilds and a flat travelator, plus tread animation.')
if __name__=='__main__':main()

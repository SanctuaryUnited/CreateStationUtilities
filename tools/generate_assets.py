"""Reproducible Minecraft models, original pixel textures, labPBR maps, and chime."""
from pathlib import Path
from PIL import Image,ImageDraw
import json, math, random, wave, struct, subprocess
R=Path(__file__).resolve().parents[1]/'src/main/resources'
A=R/'assets/createtransit';D=R/'data/createtransit'
# Platform regeneration must preserve the later vehicle/station registries.
preserved={p:json.loads(p.read_text()) for p in [A/'lang/en_us.json',A/'sounds.json',R/'assets/minecraft/atlases/blocks.json'] if p.exists()}
def js(path,obj):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,indent=2)+'\n')
tex=A/'textures/block';tex.mkdir(parents=True,exist_ok=True)
random.seed(17)
for name,smooth,f0,emission in [('frame',168,232,0),('accent',192,12,0),('glass',238,10,0),('led_accent',180,10,254),('led_white',180,10,254),('led_green',180,10,254),('led_amber',180,10,254),('led_off',140,10,0)]:
 im=Image.new('RGBA',(32,32));normal=Image.new('RGBA',(32,32),(128,128,255,255));spec=Image.new('RGBA',(32,32),(smooth,f0,0,emission))
 colors={'led_accent':(255,255,255),'led_white':(244,250,255),'led_green':(80,255,118),'led_amber':(255,183,35),'led_off':(32,39,44)}
 for y in range(32):
  brush=random.randrange(-8,9)
  for x in range(32):
   if name=='glass':
    stripe=((x-y)%32 in [5,6,18,19]);c=(175,213,226,68 if stripe else 24)
   elif name in colors:
    base=colors[name];v=0.93 if (x%4==0 or y%4==0) else 1;c=tuple(int(a*v)for a in base)+(255,)
   else:
    v=232+brush+random.randrange(-3,4) if name=='frame' else 240+random.randrange(-3,4);c=(v,v,v,255)
   im.putpixel((x,y),c)
 im.save(tex/(name+'.png'));normal.save(tex/(name+'_n.png'));spec.save(tex/(name+'_s.png'))
 # Full bright fallback for renderers honoring OptiFine-style emissive companions.
 if emission:im.save(tex/(name+'_e.png'))
js(R/'assets/minecraft/atlases/blocks.json',{'sources':[{'type':'minecraft:single','resource':'createtransit:block/'+name,'sprite':'createtransit:block/'+name}for name in ['led_accent','led_white','led_green','led_amber','led_off']]})
js(R/'pack.mcmeta',{'pack':{'pack_format':34,'description':'Create Transit platform assets — labPBR 1.3'}})
textures={name:'createtransit:block/'+name for name in ['frame','accent','glass','led_accent','led_white','led_green','led_amber','led_off']};textures['particle']=textures['frame']
from panel_geometry import FACES as faces,stationary,leaf,element,box,lights,JAMB,SLIDE,size,transition_trim,corner_return
# Remove previous generated variants so obsolete surfaces cannot enter the new package.
for pattern in ['platform_screen_door_*','platform_gate_*','platform_wall_*','platform_barrier_*','compact_screen_door_*','compact_wall_*']:
 for p in (A/'models/block').glob(pattern+'.json'):p.unlink()
def cube(a,b,t,tint=-1):return element(box(a,b,t,tint))
def model(name,elements,layer='solid'):
 js(A/'models/block'/f'{name}.json',{'parent':'minecraft:block/block','render_type':'minecraft:'+layer,'ambientocclusion':False,'textures':textures,'elements':elements})
for name,full,door in [('platform_screen_door',True,True),('platform_gate',False,True),('platform_wall',True,False),('platform_barrier',False,False),('compact_screen_door',32,True),('compact_wall',32,False)]:
 parts=[]
 for col in range(4):
  for row in range(3):
   for left in [False,True]:
    for right in [False,True]:
     for panel in range(3):
      mesh=stationary(full,door,col,row,left,right,panel=panel)
      for layer in ['frame','glass']:
       m=f'{name}_{col}_{row}_{int(left)}{int(right)}_{layer}'+(f'_p{panel}' if panel else '')
       model(m,[element(e)for e in mesh if (e['texture']=='glass')==(layer=='glass')],'translucent' if layer=='glass' else 'solid')
       for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]:
        parts.append({'when':{'facing':facing,'column':str(col),'row':str(row),'left':str(left).lower(),'right':str(right).lower(),'panel':str(panel)},'apply':{'model':'createtransit:block/'+m,'y':rotation,'uvlock':True}})
 if not door:
  for row in range(3):
   for side in [1,2]:
    for panel in range(3):
     mesh=corner_return(full,row,side,panel)
     for layer in ['frame','glass']:
      m=f'{name}_return_{row}_{side}_{panel}_{layer}'
      model(m,[element(e) for e in mesh if (e['texture']=='glass')==(layer=='glass')],'translucent' if layer=='glass' else 'solid')
      for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]:
       parts.append({'when':{'facing':facing,'row':str(row),'corner':str(side),'panel':str(panel)},'apply':{'model':'createtransit:block/'+m,'y':rotation,'uvlock':True}})
 js(A/'blockstates'/f'{name}.json',{'multipart':parts})
 elements=[];width=2 if door else 1;h,_=size(full);scale=min(16/(width*16),16/h)
 def itempart(e,x=0,y=0):
  e=element(e)
  e['from']=[(e['from'][0]+x)*scale,(e['from'][1]+y)*scale,(e['from'][2]-8)*scale+8]
  e['to']=[(e['to'][0]+x)*scale,(e['to'][1]+y)*scale,(e['to'][2]-8)*scale+8];elements.append(e)
 for col in range(width):
  for row in range((size(full)[0]+15)//16):
   for e in stationary(full,door,col,row,leds=True):itempart(e,col*16,row*16)
 if door:
  for side in [0,1]:
   for e in leaf(full):itempart(e,JAMB if side==0 else 16)
 js(A/'models/item'/f'{name}.json',{'parent':'minecraft:block/block','render_type':'minecraft:translucent','textures':textures,'elements':elements})
 js(D/'loot_table/blocks'/f'{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:block_state_property','block':'createtransit:'+name,'properties':{'column':'0','row':'0'}},{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'createtransit:'+name,'functions':[{'function':'minecraft:copy_custom_data','source':'block_entity','ops':[{'source':'paint','target':'paint','op':'replace'},{'source':'accent','target':'accent','op':'replace'},{'source':'panel','target':'panel','op':'replace'}]}]}]}]})
 js(D/'recipe'/f'{name}.json',{'type':'minecraft:crafting_shaped','pattern':['IGI','IGI','IRI'] if door else ['IGI','IGI','III'],'key':{'I':{'item':'minecraft:iron_ingot'},'G':{'item':'minecraft:glass'},**({'R':{'item':'minecraft:redstone'}}if door else {})},'result':{'id':'createtransit:'+name,'count':1}})
# The moving renderer consumes the exact geometry used by the item/Blockbench models.
j=Path(__file__).resolve().parents[1]/'src/main/java/com/sanctuarygames/createtransit/client/PanelMesh.java'
lines=['package com.sanctuarygames.createtransit.client;','/** Generated by tools/generate_assets.py from panel_geometry.py. */','final class PanelMesh {','record Cuboid(double x1,double y1,double z1,double x2,double y2,double z2,String texture,int tint,int faces) {}']
for full,label in [(True,'FULL'),(False,'HALF'),(32,'COMPACT')]:
 for suffix,mesh in [('',leaf(full)),('_DIVIDED',leaf(full,1)),('_SOLID',leaf(full,2)),('_DOOR_LIGHTS',lights(full,True)),('_WALL_LIGHTS',lights(full,False))]:
  lines.append('static final Cuboid[] '+label+suffix+' = {')
  for e in mesh:
   mask=sum(1<<faces.index(f)for f in e['visible'])
   lines.append('new Cuboid('+','.join(str(v)for v in e['from']+e['to'])+',"'+e['texture']+'",'+str(e['tint'])+','+str(mask)+'),')
  lines.append('};')
for side in ['left','right']:
 lines.append('static final Cuboid[] TALL_TRIM_'+side.upper()+' = {')
 for e in transition_trim(side):lines.append('new Cuboid('+','.join(str(v)for v in e['from']+e['to'])+',"frame",0,63),')
 lines.append('};')
lines.append('static final double JAMB = '+str(JAMB)+', SLIDE = '+str(SLIDE)+';')
lines.append('static Cuboid[] leaf(int height,int panel){return height==32?(panel==1?COMPACT_DIVIDED:panel==2?COMPACT_SOLID:COMPACT):height==48?(panel==1?FULL_DIVIDED:panel==2?FULL_SOLID:FULL):(panel==1?HALF_DIVIDED:panel==2?HALF_SOLID:HALF);}')
lines.append('}');j.write_text('\n'.join(lines)+'\n')

for name,elems in [('display_mount',[cube([0,12,5],[16,16,11],'frame',0),cube([0,12,4.9],[16,13,5],'accent',1)]),('display_post',[cube([6,0,5],[10,16,11],'frame',0),cube([6,2,4.9],[10,3,5],'accent',1)])]:
 model(name,elems)
 js(A/'blockstates'/f'{name}.json',{'multipart':[{'when':{'facing':f},'apply':{'model':'createtransit:block/'+name,'y':rot}}for f,rot in [('north',0),('east',90),('south',180),('west',270)]]})
 js(A/'models/item'/f'{name}.json',{'parent':'createtransit:block/'+name})
 js(D/'loot_table/blocks'/f'{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'createtransit:'+name,'functions':[{'function':'minecraft:copy_custom_data','source':'block_entity','ops':[{'source':'paint','target':'paint','op':'replace'},{'source':'accent','target':'accent','op':'replace'},{'source':'panel','target':'panel','op':'replace'}]}]}]}]})
 js(D/'recipe'/f'{name}.json',{'type':'minecraft:crafting_shaped','pattern':['III'] if name=='display_mount' else ['I','I','I'],'key':{'I':{'item':'minecraft:iron_ingot'}},'result':{'id':'createtransit:'+name,'count':3}})
lang={'itemGroup.createtransit.main':'Create Transit — Platforms','subtitles.createtransit.close_chime':'Platform door closing warning'}
for name,label in [('platform_screen_door','Full-Height Platform Screen Doors'),('platform_gate','Half-Height Sliding Platform Gates'),('compact_screen_door','Two-Block-High Platform Screen Doors'),('compact_wall','Two-Block-High Platform Glass Wall'),('platform_wall','Full-Height Platform Glass Wall'),('platform_barrier','Half-Height Platform Glass Barrier'),('display_mount','Display Mount Beam'),('display_post','Display Support Post'),('transit_glass','Transit Glass'),('led_strip','LED Strip'),('transit_seat','Transit Seat')]:lang['block.createtransit.'+name]=label
js(A/'lang/en_us.json',lang)
js(A/'sounds.json',{'door_close_chime':{'subtitle':'subtitles.createtransit.close_chime','sounds':[{'name':'createtransit:door_close_chime','volume':.8}]}})
sounds=A/'sounds';sounds.mkdir(exist_ok=True)
# Existing audio assets are retained; platform doors now use the LRT cues.
preview=Path(__file__).resolve().parents[1]/'preview';preview.mkdir(exist_ok=True)
import uuid
names=['frame','accent','glass','led_accent','led_white','led_green','led_amber','led_off']
for name,full in [('full_height',True),('half_height',False),('two_block_height',32)]:
 elems=[];outliner=[];groups=[]
 def bb(e,x,y,label):
  a=[e['from'][0]+x,e['from'][1]+y,e['from'][2]];b=[e['to'][0]+x,e['to'][1]+y,e['to'][2]];uid=str(uuid.uuid5(uuid.NAMESPACE_URL,name+label))
  faceset={f:{'uv':[v*2 for v in uv['uv']],'texture':names.index(e['texture'])}for f,uv in element(e)['faces'].items()}
  elems.append({'name':label,'from':a,'to':b,'faces':faceset,'uuid':uid,'type':'cube','visibility':True});return uid
 for col in range(2):
  for row in range((size(full)[0]+15)//16):
   for i,e in enumerate(stationary(full,True,col,row,leds=True)):outliner.append(bb(e,col*16,row*16,f'static_{col}_{row}_{i}'))
 for side in range(2):
  gid=str(uuid.uuid5(uuid.NAMESPACE_URL,name+str(side)));x=JAMB if side==0 else 16
  children=[bb(e,x,0,f'leaf_{side}_{i}')for i,e in enumerate(leaf(full))]
  outliner.append({'name':'left_leaf' if side==0 else 'right_leaf','origin':[x,0,1.5],'uuid':gid,'children':children});groups.append(gid)
 animators={gid:{'name':'left_leaf' if side==0 else 'right_leaf','type':'bone','keyframes':[{'channel':'position','data_points':[{'x':0,'y':0,'z':0}],'time':0,'interpolation':'linear'},{'channel':'position','data_points':[{'x':-SLIDE if side==0 else SLIDE,'y':0,'z':0}],'time':1,'interpolation':'linear'}]}for side,gid in enumerate(groups)}
 js(preview/f'{name}.bbmodel',{'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':name,'resolution':{'width':32,'height':32},'elements':elems,'textures':[{'path':f'../src/main/resources/assets/createtransit/textures/block/{n}.png','name':n+'.png','id':str(i),'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,n))}for i,n in enumerate(names)],'outliner':outliner,'animations':[{'name':'open','loop':'once','length':1,'animators':animators}]})
print('Generated two-block edge-aligned doors, fixed accent LED jambs, top-only status lights, walls and labPBR assets.')

# Preserve unrelated kits' translations, sound cues and atlas registrations.
for path,old in preserved.items():
 current=json.loads(path.read_text())
 if 'sources' in current:
  merged=old['sources'][:]
  for entry in current['sources']:
   if entry not in merged:merged.append(entry)
  current['sources']=merged
 else:current={**old,**current}
 js(path,current)

subprocess.run(['python3',str(Path(__file__).with_name('optimize_platform_states.py'))],check=True)

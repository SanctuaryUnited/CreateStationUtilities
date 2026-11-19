"""Dedicated modular elevator models, materials, recipes and loot. No vehicle assets reused."""
from pathlib import Path
from PIL import Image,ImageDraw
import json
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit';D=ROOT/'src/main/resources/data/createtransit'
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n')
materials={'metal':(229,234,239,255),'floor':(92,102,115,255),'rubber':(32,40,51,255),'glass':(169,213,232,70),'led':(255,255,255,255),'panel':(24,41,58,255),'rail':(162,172,184,255)}
for name,color in materials.items():
 im=Image.new('RGBA',(32,32),color);draw=ImageDraw.Draw(im)
 if name=='metal':
  for y in range(0,32,4):draw.line((0,y,31,y),fill=(225,230,236,255))
  draw.line((0,31,31,31),fill=(207,215,225,255))
 elif name=='floor':
  for x in range(2,32,4):
   for y in range(2,32,4):draw.point((x,y),fill=(105,115,127,255))
 elif name=='glass':pass
 elif name=='panel':
  draw.rounded_rectangle((3,3,28,10),radius=1,fill=(45,164,224,255));draw.rectangle((6,5,23,7),fill=(190,236,255,255))
  for y in [15,23]:
   for x in [8,20]:draw.ellipse((x,y,x+4,y+4),fill=(184,216,229,255))
 path=A/'textures/block'/f'elevator_{name}.png';path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
 Image.new('RGB',(32,32),(128,128,255)).save(path.with_name(path.stem+'_n.png'))
 Image.new('RGBA',(32,32),(235 if name in ['metal','glass'] else 40,230 if name=='metal' else 0,0,255 if name=='led' else 0)).save(path.with_name(path.stem+'_s.png'))
textures={k:'createtransit:block/elevator_'+k for k in materials};textures['particle']=textures['metal']
def box(a,b,t='metal',tint=0):
 faces={f:{'uv':[0,0,16,16],'texture':'#'+t,**({'tintindex':tint} if tint>=0 else {})}for f in ['north','south','west','east','up','down']}
 if t=='glass':
  thin=min(range(3),key=lambda i:b[i]-a[i]);caps=['up','down']+(['north','south'] if thin==0 else ['west','east'])
  for f in caps:faces[f]['cullface']=f
 return {'from':a,'to':b,'faces':faces}
def model(name,els,glass=False):
 v={'ambientocclusion':not glass,'textures':textures,'elements':els}
 if glass:v['render_type']='minecraft:translucent'
 dump(A/'models/block'/f'elevator_{name}.json',v)
def accents(corner=False):
 e=[box([0,2,-.03],[16,3.25,.03],'led',1),box([0,2,2.01],[16,3.25,2.05],'led',1)]
 if corner:e+=[box([-.03,2,2],[.03,3.25,16],'led',1),box([2.01,2,2],[2.05,3.25,16],'led',1)]
 return e
models={
 'floor':[box([0,0,0],[16,14,16],'metal',0),box([0,14,0],[16,15,16],'metal',1),box([0,15,0],[16,16,16],'floor',0)],
 'solid_wall':[box([0,0,0],[16,16,2])],
 'solid_corner':[box([0,0,0],[16,16,2]),box([0,0,2],[2,16,16])],
 'roof':[box([0,0,0],[16,4,16]),box([1,4,1],[15,4.5,15],'metal',0)],
 'ceiling_light':[box([0,0,0],[16,4,16]),box([1,-.12,2],[15,.12,14],'rubber',-1)],
 'led_strip':[box([5.5,0,0],[10.5,16,2]),box([7,0,-.03],[9,16,.03],'led',1)],
 'door_header':[box([0,0,0],[16,16,2])],
 'control_panel':[box([0,0,0],[16,16,2]),box([4,4,2],[12,14,3],'rubber',-1),box([4.8,4.8,3.01],[11.2,13.2,3.06],'panel',-1)],
 'guide_rail':[box([5,0,5],[11,16,11],'rail',0),box([5,0,4.9],[11,16,5.1])],
 'drive':[box([0,0,0],[16,16,16]),box([3,3,-.05],[13,12,.05],'panel',-1),box([0,13,-.05],[16,14.5,.05],'led',1)],
 'floor_call':[box([4,4,14],[12,13,16]),box([6,6,13.94],[10,10,13.99],'panel',-1)],
 'glass_pane':[box([0,0,.8],[16,16,1.2],'glass',-1)],
 'glass_corner_pane':[box([0,0,.8],[16,16,1.2],'glass',-1),box([.8,0,1.2],[1.2,16,16],'glass',-1)],
 'glass_post':[box([0,0,0],[2,16,2])],
 'glass_left':[box([0,0,0],[1.5,16,2])], 'glass_right':[box([14.5,0,0],[16,16,2])],
 'glass_up':[box([0,14.5,0],[16,16,2])],'glass_down':[box([0,0,0],[16,1.5,2])]+accents(),
 'glass_corner_up':[box([0,14.5,0],[16,16,2]),box([0,14.5,2],[2,16,16])],
 'glass_corner_down':[box([0,0,0],[16,1.5,2]),box([0,0,2],[2,1.5,16])]+accents(True),
 'door_frame_0':[box([0,0,0],[1.5,16,2]),box([14.5,0,0],[16,16,2])],
 'door_frame_1':[box([0,0,0],[1.5,16,2]),box([14.5,0,0],[16,16,2]),box([0,14,0],[16,16,2])],
}
solid_parts=['solid_wall','solid_corner','door_header','control_panel']
for name in solid_parts:
 for element in models[name]:
  for face in element['faces'].values():
   if face['texture']=='#metal':face['texture']='#wall_metal'
textures['wall_metal']='createtransit:block/elevator_wall_metal'
im=Image.new('RGBA',(32,32),(229,234,239,255));draw=ImageDraw.Draw(im)
for y in range(0,32,4):draw.line((0,y,31,y),fill=(226,231,237,255))
path=A/'textures/block/elevator_wall_metal.png';im.save(path)
Image.new('RGB',(32,32),(128,128,255)).save(path.with_name('elevator_wall_metal_n.png'))
Image.new('RGBA',(32,32),(235,230,0,0)).save(path.with_name('elevator_wall_metal_s.png'))
for k,v in models.items():model(k,v,k in ['glass_pane','glass_corner_pane'])
parts=['floor','solid_wall','solid_corner','roof','ceiling_light','led_strip','door_header','control_panel','guide_rail','glass_wall','glass_corner','glass_door','solid_door','glass_landing_door','solid_landing_door','drive','floor_call']
for name in parts:
 states={}
 if 'door' in name and name!='door_header':
  for i,f in enumerate(['north','east','south','west']):
   for r in range(2):states[f'facing={f},row={r}']={'model':f'createtransit:block/elevator_door_frame_{r}','y':i*90,'uvlock':True}
  dump(A/'blockstates'/f'elevator_{name}.json',{'variants':states})
 elif name in solid_parts:
  for i,f in enumerate(['north','east','south','west']):states['facing='+f]={'model':'createtransit:block/elevator_'+name,'y':i*90,'uvlock':True}
  dump(A/'blockstates'/f'elevator_{name}.json',{'variants':states})
 elif name in ['glass_wall','glass_corner']:
  multi=[]
  for i,f in enumerate(['north','east','south','west']):
   def add(mod,extra={}):multi.append({'when':{'facing':f,**extra},'apply':{'model':'createtransit:block/elevator_'+mod,'y':i*90,'uvlock':True}})
   add('glass_corner_pane' if name=='glass_corner' else 'glass_pane')
   if name=='glass_corner':add('glass_post')
   else:
    add('glass_left',{'left':'false'});add('glass_right',{'right':'false'})
   for side in ['up','down']:add(('glass_corner_' if name=='glass_corner' else 'glass_')+side,{side:'false'})
  dump(A/'blockstates'/f'elevator_{name}.json',{'multipart':multi})
 else:
  for i,f in enumerate(['north','east','south','west']):states['facing='+f]={'model':'createtransit:block/elevator_'+name,'y':i*90,'uvlock':True}
  dump(A/'blockstates'/f'elevator_{name}.json',{'variants':states})
 if name in ['glass_wall','glass_corner']:
  els=models['glass_corner_pane' if name=='glass_corner' else 'glass_pane']+models['glass_corner_up' if name=='glass_corner' else 'glass_up']+models['glass_corner_down' if name=='glass_corner' else 'glass_down']+(models['glass_post'] if name=='glass_corner' else models['glass_left']+models['glass_right']);model(name+'_item',els,True);parent=name+'_item'
 elif 'door' in name and name!='door_header':
  els=[box([0,0,0],[1.5,32,2]),box([14.5,0,0],[16,32,2]),box([0,30,0],[16,32,2])]
  for x,xx in [(1.7,7.9),(8.1,14.3)]:
   if 'solid' in name:els.append(box([x,0,2.15],[xx,30,2.95]))
   else:els +=[box([x,0,2.15],[xx,9,2.95]),box([x,26,2.15],[xx,30,2.95]),box([x,9,2.15],[x+.65,26,2.95]),box([xx-.65,9,2.15],[xx,26,2.95]),box([x+.65,9,2.35],[xx-.65,26,2.75],'glass',-1)]
   els.append(box([x,4,3],[xx,5.2,3.05],'led',1))
  for x in [.25,15.25]:els.append(box([x,.5,-.05],[x+.5,29.2,.05],'led',1))
  els.append(box([5,30.3,-.05],[11,31.3,.05],'rubber',-1));model(name+'_item',els,'glass' in name);parent=name+'_item'
 elif name=='ceiling_light':
  model(name+'_item',models[name]+[box([2,-.08,3],[14,.08,13],'led',1)]);parent=name+'_item'
 elif name in solid_parts:
  els=list(models[name])
  for y in [1,14.75]:
   els += [box([-.045 if name=='solid_corner' else 0,y,-.045],[16,y+.75,-.005],'led',1),box([2.005 if name=='solid_corner' else 0,y,2.005],[16,y+.75,2.045],'led',1)]
   if name=='solid_corner':els +=[box([-.045,y,-.005],[-.005,y+.75,16],'led',1),box([2.005,y,2.045],[2.045,y+.75,16],'led',1)]
  model(name+'_item',els);parent=name+'_item'
 else:parent=name
 item={'parent':'createtransit:block/elevator_'+parent}
 if 'door' in name and name!='door_header':item['display']={'gui':{'rotation':[30,135,0],'translation':[0,-3,0],'scale':[.45,.45,.45]},'ground':{'scale':[.35,.35,.35]}}
 dump(A/'models/item'/f'elevator_{name}.json',item)
 conditions=[{'condition':'minecraft:survives_explosion'}]
 if 'door' in name and name!='door_header':conditions.append({'condition':'minecraft:block_state_property','block':'createtransit:elevator_'+name,'properties':{'row':'0'}})
 loot={'type':'minecraft:block','pools':[{'rolls':1,'conditions':conditions,'entries':[{'type':'minecraft:item','name':'createtransit:elevator_'+name,'functions':[{'function':'minecraft:copy_custom_data','source':'block_entity','ops':[{'source':v,'target':v,'op':'replace'}for v in ['paint','accent']]}]}]}]}
 dump(D/'loot_table/blocks'/f'elevator_{name}.json',loot)
recipes={
 'floor':(['III','IAI','III'],{'I':'iron_ingot','A':'andesite'},4),'solid_wall':(['III','III'],{'I':'iron_ingot'},4),'solid_corner':(['I ','II'],{'I':'iron_ingot'},2),'roof':(['III'],{'I':'iron_ingot'},4),
 'ceiling_light':([' L ','IRI','III'],{'I':'iron_ingot','R':'redstone','L':'glowstone'},2),'led_strip':(['IRI','RGR','IRI'],{'I':'iron_nugget','R':'redstone','G':'glowstone_dust'},4),'door_header':(['III',' R '],{'I':'iron_ingot','R':'redstone'},2),'control_panel':(['IRI','IBI','III'],{'I':'iron_ingot','R':'redstone','B':'stone_button'},1),'guide_rail':(['I','I','I'],{'I':'iron_ingot'},8),
 'glass_wall':(['IGI','IGI','IGI'],{'I':'iron_nugget','G':'glass'},4),'glass_corner':(['IG','GG'],{'I':'iron_nugget','G':'glass'},2),'glass_door':(['IGI','IGI','IRI'],{'I':'iron_ingot','G':'glass','R':'redstone'},1),'solid_door':(['IPI','IPI','IRI'],{'I':'iron_ingot','P':'iron_trapdoor','R':'redstone'},1),
 'glass_landing_door':(['DR'],{'D':'createtransit:elevator_glass_door','R':'createtransit:elevator_guide_rail'},1),'solid_landing_door':(['DR'],{'D':'createtransit:elevator_solid_door','R':'createtransit:elevator_guide_rail'},1),'drive':(['IRI','PCP','III'],{'I':'iron_ingot','R':'redstone','P':'piston','C':'copper_ingot'},1),'floor_call':(['IRI',' B '],{'I':'iron_ingot','R':'redstone','B':'stone_button'},1)}
for name,(pattern,keys,n)in recipes.items():dump(D/'recipe'/f'elevator_{name}.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':pattern,'key':{k:{'item':v if ':'in v else 'minecraft:'+v}for k,v in keys.items()},'result':{'id':'createtransit:elevator_'+name,'count':n}})
p=A/'lang/en_us.json';lang=json.loads(p.read_text());lang.update({'block.createtransit.elevator_'+k:'Elevator '+k.replace('_',' ').title() for k in parts});lang['tooltip.createtransit.elevator']='Independent modular elevator. Dye frame; crouch + dye accents.';dump(p,lang)
print(f'Generated {len(parts)} dedicated elevator parts; {len(models)+6} block models; six LabPBR materials.')

"""Dedicated concept-based escalator assets, with no other kit materials."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,random
from escalator_geometry import base,sides,flat_sides,moving,comb
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit';D=ROOT/'src/main/resources/data/createtransit'
def dump(p,v):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n')
colors={'metal':(232,237,241,255),'rail':(166,178,188,255),'underbody':(51,61,70,255),'panel':(15,21,25,255),'glass':(174,218,237,46),'steps':(108,120,129,255),'comb':(128,140,149,255),'rubber':(21,24,27,255),'grip':(58,64,69,255),'edge':(242,190,34,255),'led':(255,255,255,255),'trim_off':(26,31,35,255),'arrow':(255,255,255,255),'stop':(255,255,255,255)}
for name,c in colors.items():
 im=Image.new('RGBA',(64,64),c);dr=ImageDraw.Draw(im)
 if name in ('steps','comb'):
  for x in range(0,64,4):
   dr.line((x,0,x,63),fill=(44,53,59,255));dr.line((x+1,0,x+1,63),fill=(162,172,178,255))
 if name in ('metal','rail'):
  rng=random.Random(23)
  for y in range(64):
   delta=rng.randint(-2,2);dr.line((0,y,63,y),fill=tuple(max(0,min(255,v+delta)) for v in c[:3])+(255,))
 if name=='glass':
  dr.rectangle((9,0,11,63),fill=(215,242,255,60));dr.rectangle((12,0,16,63),fill=(215,242,255,25))
 if name in ('arrow','stop'):
  im=Image.new('RGBA',(64,64),(9,16,20,255));dr=ImageDraw.Draw(im)
  if name=='arrow':dr.polygon([(32,7),(9,31),(22,31),(22,56),(42,56),(42,31),(55,31)],fill=(56,255,111,255))
  else:
   dr.ellipse((7,7,56,56),fill=(242,39,39,255));dr.rectangle((15,27,48,36),fill=(255,255,255,255))
 p=A/'textures/block'/f'escalator_{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
 Image.new('RGB',(64,64),(128,128,255)).save(p.with_name(p.stem+'_n.png'))
 Image.new('RGBA',(64,64),(225 if name in ('metal','rail') else 65,230 if name in ('metal','rail') else 0,0,255 if name in ('led','arrow','stop') else 0)).save(p.with_name(p.stem+'_s.png'))
textures={k:'createtransit:block/escalator_'+k for k in colors};textures['particle']=textures['metal']
for p in (A/'models/block').glob('escalator_*.json'):p.unlink()
def model(name,qs,glass=False):
 dump(A/'models/block'/f'escalator_{name}.json',{'loader':'createtransit:escalator_mesh','ambientocclusion':not glass,'render_type':'minecraft:translucent' if glass else 'minecraft:solid','textures':textures,'quads':qs})
def side_model(name,quads):
 model(name,quads)
 # A separate ordinary material removes both native fullbright and shader
 # emission. Geometry and the LED dye channel remain identical when unlit.
 model(name+'_off',[{**q,'texture':'#trim_off' if q['texture']=='#led' else q['texture']} for q in quads])
langp=A/'lang/en_us.json';lang=json.loads(langp.read_text());kinds=['lower_landing','incline','flat','upper_landing']
for kind in kinds:
 b=base(kind);model(kind+'_base',b)
 for joined_left in (False,True):
  for joined_right in (False,True):
   c=comb(kind,not joined_left,not joined_right)
   if c:model(kind+f'_comb_{int(joined_left)}{int(joined_right)}',c)
 for glass in (True,False):
  material='glass' if glass else 'solid';name=f'{kind}_{material}';item=b[:];multi=[]
  for left in (True,False):
   side='left' if left else 'right';opaque,pane=sides(kind,glass,left)
   side_model(name+'_'+side,opaque);item+=opaque+pane
   if pane:model(name+'_'+side+'_pane',pane,True)
   if kind=='flat':
    for back,front,suffix in ((True,True,''),(False,True,'_start'),(True,False,'_end'),(False,False,'_both')):
     flat_opaque,flat_pane=flat_sides(glass,left,back,front)
     side_model(name+'_'+side+suffix,flat_opaque)
     if glass:model(name+'_'+side+suffix+'_pane',flat_pane,True)
   if kind in ('lower_landing','upper_landing'):
    joined,_=sides(kind,glass,left,True);side_model(name+'_'+side+'_slope_join',joined)
    for slope_join in (False,True):
     through,through_pane=sides(kind,glass,left,slope_join,True)
     side_model(name+'_'+side+'_through'+('_slope_join' if slope_join else ''),through)
    if glass:model(name+'_'+side+'_through_pane',through_pane,True)
  for i,facing in enumerate(['north','east','south','west']):
   def entry(model_name,when):
    return {'when':{'facing':facing,**when},'apply':{'model':f'createtransit:block/escalator_{model_name}','y':i*90}}
   multi.append(entry(kind+'_base',{}))
   for side in ('left','right'):
    if kind in ('lower_landing','upper_landing'):
     for flat_join in (False,True):
      for joined in (False,True):multi.append(entry(name+'_'+side+('_through' if flat_join else '')+('_slope_join' if joined else ''),{side:'false','slope_join':str(joined).lower(),'flat_join':str(flat_join).lower()}))
    elif kind=='flat':
     for back,front,suffix in ((True,True,''),(False,True,'_start'),(True,False,'_end'),(False,False,'_both')):
      multi.append(entry(name+'_'+side+suffix,{side:'false','flat_join':str(back).lower(),'slope_join':str(front).lower()}))
    else:multi.append(entry(name+'_'+side,{side:'false'}))
    if glass:
     if kind in ('lower_landing','upper_landing'):
      for flat_join in (False,True):multi.append(entry(name+'_'+side+('_through' if flat_join else '')+'_pane',{side:'false','flat_join':str(flat_join).lower()}))
     elif kind=='flat':
      for back,front,suffix in ((True,True,''),(False,True,'_start'),(True,False,'_end'),(False,False,'_both')):
       multi.append(entry(name+'_'+side+suffix+'_pane',{side:'false','flat_join':str(back).lower(),'slope_join':str(front).lower()}))
     else:multi.append(entry(name+'_'+side+'_pane',{side:'false'}))
   if kind in ('lower_landing','upper_landing'):
    for l in (False,True):
     for r in (False,True):multi.append(entry(kind+f'_comb_{int(l)}{int(r)}',{'left':str(l).lower(),'right':str(r).lower(),'flat_join':'false'}))
   elif kind=='flat':
    for l in (False,True):
     for r in (False,True):
      for end,flag in (('lower_landing','flat_join'),('upper_landing','slope_join')):
       multi.append(entry(end+f'_comb_{int(l)}{int(r)}',{'left':str(l).lower(),'right':str(r).lower(),flag:'false'}))
  item+=moving(kind,True,True)+comb(kind,True,True)
  model(name+'_item',item,glass)
  dump(A/'blockstates'/f'escalator_{name}.json',{'multipart':multi})
  dump(A/'models/item'/f'escalator_{name}.json',{'parent':f'createtransit:block/escalator_{name}_item','display':{'gui':{'rotation':[30,225,0],'translation':[0,-3,0],'scale':[.42,.42,.42]},'fixed':{'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[75,45,0],'scale':[.3,.3,.3]}}})
  full='escalator_'+name
  lang['block.createtransit.'+full]=('Travelator' if kind=='flat' else 'Escalator '+kind.replace('_',' ').title())+' ('+material.title()+')'
  dump(D/'loot_table/blocks'/f'{full}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'createtransit:escalator_'+material}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
  patterns={'lower_landing':[' M ','IRI','GGG'],'incline':['  M',' IR','GGG'],'flat':['MRM','III','GGG'],'upper_landing':['M M','IRI','GGG']}
  dump(D/'recipe'/f'{full}.json',{'type':'minecraft:crafting_shaped','category':'building','pattern':patterns[kind],'key':{'M':{'item':'minecraft:glass' if glass else 'minecraft:iron_block'},'R':{'item':'minecraft:redstone'},'I':{'item':'minecraft:iron_ingot'},'G':{'item':'minecraft:gold_nugget'}},'result':{'id':'createtransit:'+full,'count':2}})
# All four fixed shape IDs remain registered for existing worlds. Crafting,
# pick block and drops lead to the shared escalator / travelator family item.
for material in ('glass','solid'):
 for kind in kinds:
  (D/'recipe'/f'escalator_{kind}_{material}.json').unlink(missing_ok=True)
  dump(D/'recipe'/f'escalator_{kind}_{material}_to_family.json',{'type':'minecraft:crafting_shapeless','category':'building','ingredients':[{'item':f'createtransit:escalator_{kind}_{material}'}],'result':{'id':'createtransit:escalator_'+material,'count':1}})
 for powered in (False,True):
  full='escalator_'+material+('_redstone' if powered else '')
  multi=[]
  for kind in kinds:
   doc=json.loads((A/'blockstates'/f'escalator_{kind}_{material}.json').read_text())
   for rule in doc['multipart']:
    casing=any(rule['apply']['model'].startswith(f'createtransit:block/escalator_{kind}_{material}_{side}') for side in ('left','right')) and not rule['apply']['model'].endswith('_pane')
    for stopped in ((False,True) if powered and casing else (None,)):
     when={**rule['when'],'shape':kind};apply=rule['apply']
     if stopped is not None:
      when['stopped']=str(stopped).lower()
      if stopped:apply={**apply,'model':apply['model']+'_off'}
     multi.append({'when':when,'apply':apply})
  dump(A/'blockstates'/f'{full}.json',{'multipart':multi})
  dump(A/'models/item'/f'{full}.json',{'parent':f'createtransit:item/escalator_incline_{material}'})
  dump(D/'loot_table/blocks'/f'{full}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'createtransit:'+full}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
  lang['block.createtransit.'+full]=('Redstone Escalator / Travelator' if powered else 'Escalator / Travelator')+' ('+material.title()+')'
  if powered:
   recipe={'type':'minecraft:crafting_shapeless','category':'building','ingredients':[{'item':'createtransit:escalator_'+material},{'item':'minecraft:redstone'}],'result':{'id':'createtransit:'+full,'count':1}}
  else:
   recipe={'type':'minecraft:crafting_shaped','category':'building','pattern':[' M ','IRI','GGG'],'key':{'M':{'item':'minecraft:glass' if material=='glass' else 'minecraft:iron_block'},'R':{'item':'minecraft:redstone'},'I':{'item':'minecraft:iron_ingot'},'G':{'item':'minecraft:gold_nugget'}},'result':{'id':'createtransit:'+full,'count':2}}
  dump(D/'recipe'/f'{full}.json',recipe)
lang.update({'tooltip.createtransit.escalator':'Wrench: reverse connected belt • Redstone: stop • Matching adjacent lanes widen',
 'tooltip.createtransit.escalator_powered':'Redstone: run and light connected belt • No signal: stop and LED trim off • Wrench: reverse',
 'tooltip.createtransit.escalator_smart':'Level row: travelator • Build uphill for steps and landings • Matching adjacent lanes widen',
 'message.createtransit.escalator.forward':'Connected belt: forward / uphill',
 'message.createtransit.escalator.reverse':'Connected belt: reverse / downhill',
 'message.createtransit.escalator.limit':'Belt exceeds %s connected sections; divide it into smaller runs.'})
dump(langp,lang)
from generate_escalator_icons import generate as generate_icons
generate_icons(A)
# Flywheel unit surfaces use the same existing materials and atlas UVs.
for material in ('steps','rail','edge','grip','arrow','stop'):
 texture='createtransit:block/escalator_'+material
 dump(A/'models/block'/f'escalator_surface_{material}.json',{'loader':'createtransit:escalator_mesh','ambientocclusion':False,'shade':False,'render_type':'minecraft:solid','textures':{'particle':texture,'surface':texture},'quads':[{'vertices':[[0,0,0],[0,16,0],[16,16,0],[16,0,0]],'uv':[[0,16],[0,0],[16,0],[16,16]],'texture':'#surface','tint':-1}]})
frames=[]
for kind in kinds:
 for left in (False,True):
  for right in (False,True):
   for phase in (0,.04,.11,.37,.91):
    for flat_join in (False,True):
     for slope_join in (False,True):frames.append({'kind':kind,'left':left,'right':right,'phase':phase,'flat_join':flat_join,'slope_join':slope_join,'quads':moving(kind,left,right,phase,flat_join=flat_join,slope_join=slope_join)})
dump(ROOT/'src/clienttest/resources/escalator-visual-contract.json',frames)
print('Generated rounded flat terminals, continuous landing joints, seam-free pane ends and 320 moving reference frames.')

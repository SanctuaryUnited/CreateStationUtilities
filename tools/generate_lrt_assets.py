"""Generate the redesigned single-car kit, including faceted baked geometry."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,itertools,uuid,math,random
from lrt_geometry import *
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/createtransit';T=A/'textures/block';D=R/'data/createtransit'
def js(p,o):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(o,indent=2)+'\n')
# Purge the earlier kit's generated geometry, unused wheels and fake displays.
for folder,pattern in [(A/'models/block','lrt_*.json'),(A/'models/item','lrt_*.json'),(A/'blockstates','lrt_*.json'),(D/'recipe','lrt_*.json'),(D/'loot_table/blocks','lrt_*.json'),(ROOT/'preview','lrt_*.bbmodel'),(T,'lrt_*.png'),(A/'textures/gui','lrt_bogey.png')]:
 for p in folder.glob(pattern):p.unlink()
materials={'lrt_body':(245,247,248,255),'lrt_accent':(247,247,247,255),'lrt_glass':(55,81,100,90),'lrt_floor':(42,49,57,255),'lrt_seat':(237,237,242,255),'lrt_metal':(63,73,85,255),'lrt_seal':(18,23,29,255),'lrt_ceiling':(239,242,245,255),'lrt_vent':(43,51,59,255),'lrt_bellows':(48,53,60,255),'lrt_bellows_dye':(224,226,230,255),'lrt_led':(253,254,255,255),'lrt_glass_tinted':(23,36,49,160),'lrt_glass_frosted':(191,208,215,190)}
for name,color in materials.items():
 im=Image.new('RGBA',(32,32),color);d=ImageDraw.Draw(im);rng=random.Random(name)
 if name in ['lrt_body','lrt_metal','lrt_accent']:
  for y in range(32):
   for x in range(32):
    n=rng.choice([-1,0,0,0,1]);d.point((x,y),fill=tuple(max(0,min(255,c+n)) for c in color[:3])+(255,))
 elif name in ['lrt_floor','lrt_seat']:
  for y in range(0,32,4):
   for x in range(0,32,4):d.point((x,y),fill=tuple(max(0,c-13) for c in color[:3])+(255,))
 elif name=='lrt_vent':
  for x in range(0,32,4):d.line((x,0,x,31),fill=(17,23,29,255),width=2)
 elif name.startswith('lrt_glass'):pass # Clear continuous glazing; shader reflections supply the highlights.
 T.mkdir(parents=True,exist_ok=True);im.save(T/(name+'.png'))
 if name=='lrt_bellows_dye':
  for x in range(0,32,4):d.line((x,0,x,31),fill=(209,211,215,255),width=1)
  for y in range(0,32,4):d.line((0,y,31,y),fill=(215,217,221,255),width=1)
  im.save(T/(name+'.png'))
 normal=Image.new('RGBA',(32,32),(128,128,255,255))
 if name=='lrt_bellows_dye':
  nd=ImageDraw.Draw(normal)
  for x in range(0,32,4):nd.line((x,0,x,31),fill=(112,128,253,255),width=1)
 normal.save(T/(name+'_n.png'))
 # Painted enamel is a dielectric; only exposed structural metal is metallic.
 Image.new('RGBA',(32,32),(185 if name in ['lrt_body','lrt_accent','lrt_glass'] else 130,230 if name=='lrt_metal' else 40 if name.startswith('lrt_glass') else 12,0,254 if name=='lrt_led' else 0)).save(T/(name+'_s.png'))
texnames=list(materials)+['gondola_led'];textures={n:'createtransit:block/'+n for n in texnames};textures['particle']='createtransit:block/lrt_body'
def model(name,geo,item=False,glass=False):
 if item and any(e['texture'] in GLASS_TEXTURES for e in geo):glass=True
 quads=[{'vertices':e['_quad'],'uv':atlas_uv(e), 'texture':'#'+e['texture'],'tint':e['tint']} for e in faces(geo)]
 # Use baked quads so both axes of the curved/sloped cab remain exact.
 o={'parent':'minecraft:block/block','loader':'createtransit:lrt_mesh','render_type':'minecraft:translucent' if glass else 'minecraft:cutout_mipped','ambientocclusion':not glass,'textures':textures,'quads':quads}
 if item:o['display']={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.65,.65,.65]},'ground':{'translation':[0,3,0],'scale':[.25,.25,.25]}}
 js(A/f'models/{"item" if item else "block"}/{name}.json',o)
def layers(name,geo):
 out=[]
 for glass in [False,True]:
  part=[e for e in geo if (e['texture'] in GLASS_TEXTURES)==glass]
  if part:model(name+('_glass' if glass else ''),part,glass=glass);out.append({'model':'createtransit:block/'+name+('_glass' if glass else '')})
 return out
turns={'north':0,'east':90,'south':180,'west':270}
def append(parts,when,applies,rot):
 for a in applies:parts.append({'when':when,'apply':{**a,**({'y':rot} if rot else {})}})
ingredients=['minecraft:iron_ingot','minecraft:iron_nugget','minecraft:copper_ingot','minecraft:redstone','minecraft:string','minecraft:glass','minecraft:quartz','minecraft:andesite','minecraft:stone','minecraft:iron_bars','minecraft:slime_ball','minecraft:black_dye','minecraft:white_dye','minecraft:smooth_stone','minecraft:leather','minecraft:stick','minecraft:tripwire_hook','minecraft:gray_dye','minecraft:light_gray_dye','minecraft:blue_dye','minecraft:gold_nugget','minecraft:amethyst_shard','minecraft:calcite','minecraft:flint','minecraft:clay_ball','minecraft:coal','minecraft:paper','minecraft:lapis_lazuli','minecraft:obsidian','minecraft:copper_block','minecraft:polished_andesite','minecraft:polished_diorite','minecraft:polished_granite','minecraft:deepslate','minecraft:tuff','minecraft:prismarine','minecraft:sea_lantern','minecraft:glowstone','minecraft:yellow_dye','minecraft:red_dye','minecraft:green_dye','minecraft:cyan_dye','minecraft:magenta_dye','minecraft:orange_dye','minecraft:purple_dye','minecraft:brown_dye']
for kind in KINDS:
 name='lrt_'+kind;parts=[]
 if kind=='door':
  for tall in [False,True]:
   for col in range(2):
    for row in range(3 if tall else 2):
     a=layers(f'{name}_{"tall" if tall else "compact"}_{col}_{row}',cell(door_fixed(48 if tall else 32),col,row))
     for f,rot in turns.items():append(parts,{'facing':f,'column':col,'row':row,'tall':tall},a,rot)
 elif kind in GLASS_KINDS or kind in PROFILE_KINDS or kind in SIDE_KINDS or kind=='ceiling_ring':
  flags4=kind in GLASS_KINDS or kind=='ceiling_ring'
  for flags in itertools.product([False,True],repeat=4 if flags4 else 2):
   l,r=flags[:2];top,bottom=flags[2:] if flags4 else (False,False)
   geo=window_variant(kind,*flags) if kind in WINDOW_KINDS else ceiling_light(kind,*flags) if kind=='ceiling_ring' else window(*flags) if kind=='window' else window_corner(*flags) if kind=='window_corner' else windshield(kind.endswith('upper'),*flags) if kind.startswith('cab_windshield') else mesh(kind,left=l,right=r,top=top,bottom=bottom)
   a=layers(name+'_'+'_'.join(str(int(v)) for v in flags),geo)
   for f,rot in turns.items():append(parts,{'facing':f,**dict(zip(['left','right','top','bottom'],flags))},a,rot)
 else:
  options=itertools.product([False,True],['bottom']) if kind=='pantograph' else itertools.product([True],['bottom','top']) if kind=='led_horizontal' else [(True,'bottom')]
  for raised,half in options:
   suffix=('_raised' if raised else '_folded') if kind=='pantograph' else '_'+half if kind=='led_horizontal' else '';a=layers(name+suffix,pantograph_base() if kind=='pantograph' else mesh(kind,raised,half))
   for f,rot in turns.items():append(parts,{'facing':f,**({'raised':raised} if kind=='pantograph' else {'half':half} if kind=='led_horizontal' else {})},a,rot)
 if kind=='ceiling_ring':
  # Four spare decor flags store diagonal occupancy without adding world states.
  for corner,a,b,flag,value in [('nw','left','top','raised',False),('ne','right','top','powered',False),('se','right','bottom','short',False),('sw','left','bottom','half','bottom')]:
   extra=layers(name+'_inner_'+corner,ring_inner(corner))
   for f,rot in turns.items():append(parts,{'facing':f,a:True,b:True,flag:value},extra,rot)
 js(A/f'blockstates/{name}.json',{'multipart':parts})
 geo=mesh(kind)+lights(kind)
 if kind=='door':
  geo=[{**e,'from':[v/3 for v in e['from']],'to':[v/3 for v in e['to']]} for e in geo]
 model(name,geo,True)
 # Free-format Blockbench meshes preserve the actual faceted surfaces.
 elems=[]
 for i,e in enumerate(faces(mesh(kind)+lights(kind))):
  uid=str(uuid.uuid5(uuid.NAMESPACE_URL,name+str(i)));verts={str(k):v for k,v in enumerate(e['_quad'])}
  elems.append({'name':e['texture']+'_'+str(i),'uuid':uid,'type':'mesh','origin':[0,0,0],'vertices':verts,'faces':{'face':{'vertices':list(verts),'uv':{str(k):[v*2 for v in uv] for k,uv in enumerate(atlas_uv(e))},'texture':texnames.index(e['texture'])}}})
 js(ROOT/f'preview/{name}.bbmodel',{'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':name,'resolution':{'width':32,'height':32},'elements':elems,'outliner':[e['uuid'] for e in elems],'textures':[{'path':'../src/main/resources/assets/createtransit/textures/block/'+n+'.png','name':n+'.png','id':str(i)} for i,n in enumerate(texnames)]})
 js(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'createtransit:'+name,'functions':[{'function':'minecraft:copy_custom_data','source':'block_entity','ops':[{'source':'paint','target':'paint','op':'replace'},{'source':'accent','target':'accent','op':'replace'}]}]}]}]})
 glass=kind in GLASS_KINDS or kind=='door';key='G' if glass else 'S'
 js(D/f'recipe/{name}.json',{'type':'minecraft:crafting_shaped','pattern':['I'+key+'I',' C '],'key':{'I':{'item':'minecraft:iron_ingot'},key:{'item':'minecraft:glass' if glass else 'minecraft:smooth_stone'},'C':{'item':ingredients[KINDS.index(kind)]}},'result':{'id':'createtransit:'+name,'count':1 if kind in ['door','seat','pantograph'] else 2}})
for tag,values in [('data/minecraft/tags/block/mineable/pickaxe.json',['createtransit:lrt_'+k for k in KINDS]),('data/create/tags/block/seats.json',['createtransit:lrt_seat']),('data/create/tags/block/safe_nbt.json',['createtransit:lrt_'+k for k in KINDS])]:
 p=R/tag;o=json.loads(p.read_text()) if p.exists() else {'replace':False,'values':[]};o['values']=[v for v in o['values'] if not isinstance(v,str) or not v.startswith('createtransit:lrt_')]+values;js(p,o)
atlas=R/'assets/minecraft/atlases/blocks.json';o=json.loads(atlas.read_text());o['sources']=[s for s in o['sources'] if not s.get('sprite','').startswith('createtransit:block/lrt_')]
for n in materials:
 sprite='createtransit:block/'+n;o['sources'].append({'type':'minecraft:single','resource':sprite,'sprite':sprite})
js(atlas,o)
lang=json.loads((A/'lang/en_us.json').read_text());lang={k:v for k,v in lang.items() if not (k.startswith('block.createtransit.lrt_') or k.startswith('bogey.createtransit.lrt') or k.startswith('bogeymenu.createtransit.lrt') or k=='tooltip.createtransit.lrt_articulation')}
labels={'cab_console':'Cab Controls Base','cab_floor':'Shaped Cab Floor','cab_roof':'Swept Cab Roof','cab_lower':'Sculpted Nose','cab_upper':'Cab Display Bay Corner','cab_side_lower':'Cab Side Body','cab_side_upper':'Cab Side Header','cab_windshield_lower':'Wraparound Windshield — Lower','cab_windshield_upper':'Wraparound Windshield — Upper','cab_side_windshield_lower':'Cab Side Glazing — Lower','cab_side_windshield_upper':'Cab Side Glazing — Upper','display_mount':'CRN Cab Board Housing','side_display_mount':'CRN Side Board Housing','window':'Connected Slim Window','door':'Two-Wide Sliding Door','roof_corner':'Rounded Roof Corner','led_horizontal':'Horizontal LED Body Panel','horn':'Roof Horn'}
for k in KINDS:lang['block.createtransit.lrt_'+k]='LRT '+labels.get(k,k.replace('_',' ').title())
lang.update({'itemGroup.createtransit.lrt':'Create Transit — LRT Vehicles','tooltip.createtransit.lrt_part':'Modular LRT. Five blocks wide; three clear interior rows. Two quick-build schematics included.','tooltip.createtransit.lrt_display':'Place behind a CRN Advanced Display Board. Cab: positive alignment. Side: negative alignment. CRN provides the display.','tooltip.createtransit.lrt_pantograph':'Right-click or redstone smoothly folds/raises. Works on assembled trains too.','tooltip.createtransit.lrt_articulation':'Dye the anchor for colored bellows. Shift-click with dye for the accent. Glue each car separately.','tooltip.createtransit.lrt_horn':'Place on a carriage roof. Hold left Shift while driving to sound the train whistle.'});js(A/'lang/en_us.json',lang)
lines=['package com.sanctuarygames.createtransit.lrt.client;','public final class LrtMesh {','public record Face(double[] vertices,float[] uv,String texture,int tint){}']
groups=[('LEFT',door_leaf()),('RIGHT',door_leaf(True)),('CENTER_LEFT',door_center()),('CENTER_RIGHT',door_center(True)),('LIGHTS',door_lights()),('STATUS',door_status()),('CEILING_LIGHTS',lights('roof')),('LED_VERTICAL',lights('led_vertical')),('LED_HORIZONTAL_BOTTOM',lights('led_horizontal')),('LED_HORIZONTAL_TOP',lights('led_horizontal','top'))]
groups.extend([('PANTO_ARM',pantograph_arm()),('PANTO_HEAD',pantograph_head())])
groups.extend([('TALL_LEFT',door_leaf(height=48)),('TALL_RIGHT',door_leaf(True,48)),('TALL_CENTER_LEFT',door_center(height=48)),('TALL_CENTER_RIGHT',door_center(True,48)),('TALL_LIGHTS',door_lights(48)),('TALL_STATUS',door_status(48))])
for l,r in itertools.product([False,True],repeat=2):groups.append(('CAB_LIGHTS_'+str(int(l))+str(int(r)),lights('cab_lower',left=l,right=r)))
for name,geo in groups:
 lines.append('public static final Face[] '+name+'={')
 for e in faces(geo):lines.append('new Face(new double[]{'+','.join(str(v) for p in e['_quad'] for v in p)+'},new float[]{'+','.join(str(v)+'f' for p in atlas_uv(e) for v in p)+'},"'+e['texture']+'",'+str(e['tint'])+'),')
 lines.append('};')
lines.extend(['public static Face[] cabLights(boolean left,boolean right){return left?(right?CAB_LIGHTS_11:CAB_LIGHTS_10):(right?CAB_LIGHTS_01:CAB_LIGHTS_00);}','}']);(ROOT/'src/main/java/com/sanctuarygames/createtransit/lrt/client/LrtMesh.java').write_text('\n'.join(lines)+'\n')
def bounds(e):
 v=faces([e])[0]['_quad'];a=[min(p[i] for p in v) for i in range(3)];b=[max(p[i] for p in v) for i in range(3)]
 # Flat shell faces get a thin collision surface on their inside.
 u=[v[1][i]-v[0][i] for i in range(3)];w=[v[2][i]-v[0][i] for i in range(3)];n=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]]
 for i in range(3):
  if b[i]-a[i]<1e-7:
   if n[i]>0:a[i]-=.04
   else:b[i]+=.04
 return [round(v,6) for v in a+b]
def expr(geo):
 # Cuboid bounds must include every face, not only their first outward face.
 boxes=[]
 for e in geo:
  box=bounds(e) if '_quad' in e else [min(p[i] for q in faces([e]) for p in q['_quad']) for i in range(3)]+[max(p[i] for q in faces([e]) for p in q['_quad']) for i in range(3)]
  box=[round(v,6) for v in box]
  if all(box[i+3]>box[i] for i in range(3)):boxes.append('TransitBarrierBlock.rotated(f,'+','.join(str(v) for v in box)+')')
 return 'Shapes.or('+','.join(boxes)+')' if boxes else 'Shapes.empty()'
lines=['package com.sanctuarygames.createtransit.lrt;','import com.sanctuarygames.createtransit.block.TransitBarrierBlock;','import net.minecraft.core.Direction;','import net.minecraft.world.level.block.state.properties.Half;','import net.minecraft.world.phys.shapes.*;','final class LrtShapes {',
 'private record Key(LrtBlock.Kind kind,Direction facing,boolean raised,boolean left,boolean right){}',
 'private static final java.util.Map<Key,VoxelShape> CACHE=new java.util.concurrent.ConcurrentHashMap<>();',
 'static VoxelShape shape(LrtBlock.Kind kind,Direction f,Half half,boolean raised,boolean left,boolean right){',
 'boolean profile=switch(kind){case CAB_FLOOR,CAB_SKIRT,CAB_ROOF,CAB_LOWER,CAB_UPPER,CAB_WINDSHIELD_LOWER,CAB_WINDSHIELD_UPPER,DISPLAY_MOUNT,SIDE_DISPLAY_MOUNT->true;default->false;};',
 'boolean side=switch(kind){case CAB_SIDE_LOWER,CAB_SIDE_WINDSHIELD_LOWER,CAB_SIDE_WINDSHIELD_UPPER,CAB_SIDE_UPPER->true;default->false;};',
 'var key=new Key(kind,f,kind==LrtBlock.Kind.PANTOGRAPH&&raised,profile&&left,(profile||side)&&right);',
 'return CACHE.computeIfAbsent(key,k->build(k.kind(),k.facing(),k.raised(),k.left(),k.right()));}',
 'private static VoxelShape build(LrtBlock.Kind kind,Direction f,boolean raised,boolean left,boolean right){return switch(kind){']
for k in KINDS:
 if k in ['seat','door']:continue
 if k in CEILING_KINDS:shape=expr([cube([0,0,0],[16,3,16])])
 elif k in PROFILE_KINDS or k in SIDE_KINDS or k.startswith('cab_windshield'):
  choices={(l,r):expr(mesh(k,left=l,right=r)) for l,r in itertools.product([False,True],repeat=2)};shape='left?(right?'+choices[True,True]+':'+choices[True,False]+'):(right?'+choices[False,True]+':'+choices[False,False]+')'
 elif k=='pantograph':shape='raised?'+expr(mesh(k,True))+':'+expr(mesh(k,False))
 elif k in WINDOW_KINDS:shape=expr(mesh(k))
 else:shape=expr(mesh(k))
 lines.append('case '+k.upper()+' -> '+shape+';')
lines.extend(['};}','static VoxelShape seat(Direction f){return '+expr(mesh('seat'))+';}','}']);(ROOT/'src/main/java/com/sanctuarygames/createtransit/lrt/LrtShapes.java').write_text('\n'.join(lines)+'\n')
print('Generated',len(KINDS),'white dyeable LRT modules, two-high outward doors, articulated anchors and animated pantograph meshes.')

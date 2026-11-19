"""Generate the original modular cabin kit, collision meshes and editor files."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,itertools,uuid
from gondola_geometry import *
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/createtransit';D=R/'data/createtransit';T=A/'textures/block'
def js(p,obj):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,indent=2)+'\n')
def texture(name,im,metal=False,emissive=False):
    im.save(T/(name+'.png'))
    Image.new('RGBA',im.size,(128,128,255,255)).save(T/(name+'_n.png'))
    Image.new('RGBA',im.size,(185 if metal else 60,230 if metal else 0,0,254 if emissive else 0)).save(T/(name+'_s.png'))
    if emissive:im.save(T/(name+'_e.png'))
metal=Image.new('RGBA',(32,32),(224,226,230,255));d=ImageDraw.Draw(metal)
for y in range(0,32,4):d.line((0,y,31,y),fill=(209,212,217,255))
for x,y in [(2,2),(29,2),(2,29),(29,29)]:d.rectangle((x,y,x+1,y+1),fill=(157,162,172,255))
texture('gondola_metal',metal,True)
accent=Image.new('RGBA',(32,32),(232,232,232,255));d=ImageDraw.Draw(accent)
for y in [0,31]:d.line((0,y,31,y),fill=(184,184,184,255))
texture('gondola_accent',accent,True)
glass=Image.new('RGBA',(32,32),(147,213,238,52));d=ImageDraw.Draw(glass);d.line((5,0,31,26),fill=(222,245,255,55),width=2);texture('gondola_glass',glass)
floor=Image.new('RGBA',(32,32),(54,62,71,255));d=ImageDraw.Draw(floor)
for y in range(2,32,4):
 for x in range(2,32,4):d.rectangle((x,y,x+1,y+1),fill=(95,104,112,255))
texture('gondola_floor',floor)
ceiling=Image.new('RGBA',(32,32),(206,211,216,255));d=ImageDraw.Draw(ceiling);d.rectangle((2,2,29,29),outline=(170,181,191,255));texture('gondola_ceiling',ceiling)
fabric=Image.new('RGBA',(32,32),(215,215,215,255));d=ImageDraw.Draw(fabric)
for y in range(0,32,2):
 for x in range(y%4,32,4):d.point((x,y),fill=(166,166,166,255))
texture('gondola_fabric',fabric)
texture('gondola_seal',Image.new('RGBA',(32,32),(36,43,52,255)))
texture('gondola_led',Image.new('RGBA',(32,32),(255,255,255,255)),emissive=True)
names=['gondola_metal','gondola_accent','gondola_glass','gondola_floor','gondola_ceiling','gondola_fabric','gondola_led','led_white','led_green','gondola_seal']
textures={n:'createtransit:block/'+n for n in names};textures['particle']=textures['gondola_metal']
atlas=json.loads((R/'assets/minecraft/atlases/blocks.json').read_text());existing={x['sprite']for x in atlas['sources']}
for n in names:
 if textures[n]not in existing:atlas['sources'].append({'type':'minecraft:single','resource':textures[n],'sprite':textures[n]})
js(R/'assets/minecraft/atlases/blocks.json',atlas)
js(R/'data/create/tags/block/seats.json',{'replace':False,'values':['createtransit:gondola_seat']})
js(R/'data/minecraft/tags/block/doors.json',{'replace':False,'values':['createtransit:gondola_door','createtransit:gondola_double_door']})
pickaxe=R/'data/minecraft/tags/block/mineable/pickaxe.json'
tag=json.loads(pickaxe.read_text())if pickaxe.exists()else{'replace':False,'values':[]}
for value in ['createtransit:gondola_'+k for k in KINDS]:
 if value not in tag['values']:tag['values'].append(value)
js(pickaxe,tag)
def model(name,mesh,glass=False):
 js(A/f'models/block/{name}.json',{'parent':'minecraft:block/block','render_type':'minecraft:translucent'if glass else'minecraft:solid','ambientocclusion':not glass,'textures':textures,'elements':[element(e)for e in mesh]})
facings=[('north',0),('east',90),('south',180),('west',270)]
for kind in KINDS:
 name='gondola_'+kind;parts=[]
 if kind in ['window','window_corner']:
  keys=['left','right','top','bottom']if kind=='window'else['top','bottom']
  for values in itertools.product([False,True],repeat=len(keys)):
   flags=dict(zip(keys,values));suffix=''.join(str(int(v))for v in values)
   shape=window(*values)if kind=='window'else window_corner(*values)
   for glass in [False,True]:
    m=name+'_'+suffix+('_glass'if glass else'');model(m,[e for e in shape if(e['texture']=='gondola_glass')==glass],glass)
    for f,rot in facings:parts.append({'when':{'facing':f,**{k:str(v).lower()for k,v in flags.items()}},'apply':{'model':'createtransit:block/'+m,'y':rot}})
 elif kind=='roof_edge':
  for shape,half in itertools.product(ROOF_SHAPES,['bottom','top']):
   m=name+'_'+shape+'_'+half;model(m,roof_edge(shape,half))
   for f,rot in facings:parts.append({'when':{'facing':f,'half':half,'shape':shape},'apply':{'model':'createtransit:block/'+m,'y':rot}})
 elif kind in LED_KINDS:
  for half in ['bottom','top']:
   m=name+'_'+half;model(m,led_body(kind,half))
   for f,rot in facings:parts.append({'when':{'facing':f,'half':half},'apply':{'model':'createtransit:block/'+m,'y':rot}})
 elif kind=='double_door':
  for height in [32,48]:
   for col,row in itertools.product(range(3),range(height//16)):
    m=name+f'_{height}_{col}_{row}';model(m,cell(wide_fixed(height),col,row))
    for f,rot in facings:parts.append({'when':{'facing':f,'column':str(col),'tall':str(height==48).lower(),**({'row':str(row)}if height==48 else{'half':'lower'if row==0 else'upper'})},'apply':{'model':'createtransit:block/'+m,'y':rot}})
 elif kind=='door':
  for row,half in enumerate(['lower','upper']):
   for side in ['single','left','right']:
    fixed=door_fixed()if side=='single'else[e for e in door_fixed()if e['from'][1]>=31 or (side=='left'and e['from'][0]<8)or(side=='right'and e['from'][0]>8)]
    m=name+'_'+half+'_'+side;model(m,cell(fixed,0,row))
    for f,rot in facings:parts.append({'when':{'facing':f,'half':half,'paired':'false'if side=='single'else'true',**({}if side=='single'else{'hinge':side})},'apply':{'model':'createtransit:block/'+m,'y':rot}})
 else:
  for glass in [False,True]:
   elems=[e for e in mesh(kind)if(e['texture']=='gondola_glass')==glass]
   if not elems:continue
   m=name+('_glass'if glass else'');model(m,elems,glass)
   for f,rot in facings:parts.append({'when':{'facing':f},'apply':{'model':'createtransit:block/'+m,'y':rot}})
 js(A/f'blockstates/{name}.json',{'multipart':parts})
 itemmesh=mesh(kind)+(led_lights(kind)if kind in LED_KINDS else door_status()if kind=='door'else[])
 elements=[element(e)for e in itemmesh]
 if kind=='double_door':
  for e in elements:
   e['from']=[e['from'][0]/3,e['from'][1]/3,e['from'][2]/3+6];e['to']=[e['to'][0]/3,e['to'][1]/3,e['to'][2]/3+6]
 if kind=='door':
  for e in elements:
   e['from']=[e['from'][0]*.5+4,e['from'][1]*.5,e['from'][2]*.5+4];e['to']=[e['to'][0]*.5+4,e['to'][1]*.5,e['to'][2]*.5+4]
 js(A/f'models/item/{name}.json',{'parent':'minecraft:block/block','render_type':'minecraft:translucent'if any(e['texture']=='gondola_glass'for e in itemmesh)else'minecraft:solid','textures':textures,'elements':elements,'display':{'gui':{'rotation':[25,225,0],'translation':[0,0,0],'scale':[.9,.9,.9]}}})
 conditions=[{'condition':'minecraft:survives_explosion'}]
 if kind in ['door','double_door']:conditions.insert(0,{'condition':'minecraft:block_state_property','block':'createtransit:'+name,'properties':{'half':'lower',**({'column':'0'}if kind=='double_door'else{})}})
 js(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':conditions,'entries':[{'type':'minecraft:item','name':'createtransit:'+name,'functions':[{'function':'minecraft:copy_custom_data','source':'block_entity','ops':[{'source':'paint','target':'paint','op':'replace'},{'source':'accent','target':'accent','op':'replace'}]}]}]}]})
 recipes={
 'floor':(['III','SSS'],{'S':'smooth_stone'}), 'wall':(['I','C','I'],{'C':'white_concrete'}),
 'window':(['III','GGG','III'],{'G':'glass'}), 'corner':(['I ','II'],{}),
 'window_corner':(['IG','GI'],{'G':'glass'}), 'roof':(['III','CCC'],{'C':'white_concrete'}),
 'roof_edge':(['I  ','II ','III'],{}), 'led_pillar':(['III',' R ','III'],{'R':'redstone'}),
 'hanger':(['III',' I ','III'],{}), 'seat':(['III',' W ','III'],{'W':'white_wool'}),
 'door':(['IGI','GGG','IGI'],{'G':'glass'}),
 'led_vertical':(['IRI','IRI','IRI'],{'R':'redstone'}),
 'led_vertical_double':(['RIR','RIR','RIR'],{'R':'redstone'}),
 'led_vertical_corner':(['RI ','RI ','RII'],{'R':'redstone'}),
 'led_horizontal':(['III','RRR'],{'R':'redstone'}),
 'led_horizontal_double':(['RRR','III','RRR'],{'R':'redstone'}),
 'led_horizontal_corner':(['RRR','I R','III'],{'R':'redstone'}),
 'double_door':(['DGD','III'],{'D':'@gondola_door','G':'glass'}),
 'outer_corner':(['I I','III'],{}),
 'floor_edge':(['ISS'],{'S':'smooth_stone'}),
 'floor_outer_corner':(['IS',' S'],{'S':'smooth_stone'}),
 'led_vertical_outer_corner':(['IRI','I R','IRI'],{'R':'redstone'}),
 'led_horizontal_outer_corner':(['IRR','R I','III'],{'R':'redstone'})}
 pattern,keys=recipes[kind];keys={'I':'iron_ingot',**keys}
 recipe={'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':('createtransit:'+v[1:])if v.startswith('@')else('minecraft:'+v)}for k,v in keys.items()},'result':{'id':'createtransit:'+name,'count':1 if kind in ['door','double_door','seat','hanger']else 2}}
 js(D/f'recipe/{name}.json',recipe)
 # Blockbench reference: one-block pieces, door leaf pivot/sweep is documented separately.
 bb=[]
 for i,e in enumerate(itemmesh):
  bb.append({'name':e['texture']+'_'+str(i),'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,name+str(i))),'type':'cube','from':e['from'],'to':e['to'],'faces':{f:{'uv':[0,0,16,16],'texture':names.index(e['texture'])}for f in e['visible']}})
 js(ROOT/f'preview/{name}.bbmodel',{'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':name,'resolution':{'width':32,'height':32},'elements':bb,'outliner':[e['uuid']for e in bb],'textures':[{'path':'../src/main/resources/assets/createtransit/textures/block/'+n+'.png','name':n+'.png','id':str(i)}for i,n in enumerate(names)]})
# Java collision shapes use the exact generated geometry.
lines=['package com.sanctuarygames.createtransit.block;','import net.minecraft.core.Direction;','import net.minecraft.world.level.block.state.properties.*;','import net.minecraft.world.phys.shapes.*;','/** Generated from tools/gondola_geometry.py. */','final class GondolaShapes {','private static VoxelShape box(Direction f,double x,double y,double z,double X,double Y,double Z){return TransitBarrierBlock.rotated(f,x,y,z,X,Y,Z);}','static VoxelShape shape(GondolaBlock.Kind kind,Direction f,Half half){return switch(kind){']
def java_shape(boxes):return 'Shapes.or('+','.join('box(f,'+','.join(str(v)for v in e['from']+e['to'])+')'for e in boxes)+')'
for kind in [k for k in KINDS if k not in ['seat','door','double_door']]:
 boxes=mesh(kind)
 if kind=='window':boxes=[cube([0,0,0],[16,16,3])]
 if kind.startswith('led_horizontal'):expr='half==Half.TOP?'+java_shape(led_body(kind,'top'))+':'+java_shape(boxes)
 else:expr=java_shape(boxes)
 lines.append('case '+kind.upper()+' -> '+expr+';')
lines+=['};}','static VoxelShape seat(Direction f){return '+java_shape(mesh('seat'))+';}','static VoxelShape roof(Direction f,Half half,StairsShape shape){return switch(shape){']
for shape in ROOF_SHAPES:lines.append('case '+shape.upper()+' -> half==Half.TOP?'+java_shape(roof_edge(shape,'top'))+':'+java_shape(roof_edge(shape))+';')
lines+=['};}','}'];(ROOT/'src/main/java/com/sanctuarygames/createtransit/block/GondolaShapes.java').write_text('\n'.join(lines)+'\n')
lines=['package com.sanctuarygames.createtransit.client;','/** Generated from tools/gondola_geometry.py. */','final class GondolaMesh {','record Cube(double x1,double y1,double z1,double x2,double y2,double z2,String texture,int tint,int faces) {}']
for name,elems in [('LEAF',door_leaf()),('DOOR_LIGHTS',door_lights()),('STATUS',door_status()),('PILLAR_LIGHTS',pillar_lights())]+[(('WIDE_'if height==48 else'LEGACY_WIDE_')+name,elems)for height in [32,48]for name,elems in [('LEFT',wide_leaf(False,height)),('RIGHT',wide_leaf(True,height)),('LIGHTS',wide_lights(height)),('STATUS',wide_status(height)),('CENTER_LEFT',wide_center_lights(False,height)),('CENTER_RIGHT',wide_center_lights(True,height))]]:
 lines.append('static final Cube[] '+name+'={')
 for e in elems:lines.append('new Cube('+','.join(str(v)for v in e['from']+e['to'])+',"'+e['texture']+'",'+str(e['tint'])+','+str(sum(1<<FACES.index(f)for f in e['visible']))+'),')
 lines.append('};')
for kind in LED_KINDS:
 for half in ['bottom','top']:
  lines.append('static final Cube[] '+kind.upper()+'_'+half.upper()+'={')
  for e in led_lights(kind,half):lines.append('new Cube('+','.join(str(v)for v in e['from']+e['to'])+',\"'+e['texture']+'\",'+str(e['tint'])+','+str(sum(1<<FACES.index(f)for f in e['visible']))+'),')
  lines.append('};')
lines.append('}');(ROOT/'src/main/java/com/sanctuarygames/createtransit/client/GondolaMesh.java').write_text('\n'.join(lines)+'\n')
lang=json.loads((A/'lang/en_us.json').read_text());labels={'floor':'Floor Tile','wall':'Body Panel','window':'Connected Window','corner':'Body Corner','window_corner':'Glass Corner','roof':'Roof Tile','roof_edge':'Roof Stairs','led_pillar':'Accent LED Pillar','hanger':'Roof Hanger Mount','seat':'Bench Seat','door':'Sliding Door','double_door':'Wide Double Sliding Door','led_vertical':'Vertical LED Strip','led_vertical_double':'Twin Vertical LED Strips','led_vertical_corner':'Corner Vertical LED Strip','led_horizontal':'Horizontal LED Strip','led_horizontal_double':'Twin Horizontal LED Strips','led_horizontal_corner':'Corner Horizontal LED Strip'}
for kind,label in labels.items():lang['block.createtransit.gondola_'+kind]='Gondola '+label
lang.update({'itemGroup.createtransit.gondolas':'Create Transit — Gondolas','tooltip.createtransit.gondola_part':'One-block cabin component. Superglue parts into your own Create vehicle.','tooltip.createtransit.gondola_hanger':'Place in the roof row. Requires a separate working bogey; this is not a cable drive.','tooltip.createtransit.gondola_seat':'Right-click to sit. Works as a seat on assembled Create vehicles.','tooltip.createtransit.gondola_door':'1 wide, 2 high. Pair adjacent leaves. Create station door controls supported.','tooltip.createtransit.fare_gate':'2 wide: post + gate. Crouch-place mirrors the passage to your right at the green entrance.'})
for kind,label in {'outer_corner':'Inset Outer Corner Post','led_vertical_outer_corner':'Outer Corner Vertical LED Junction','led_horizontal_outer_corner':'Outer Corner Horizontal LED Junction','floor_edge':'Inset Floor Rim','floor_outer_corner':'Inset Floor Corner'}.items():lang['block.createtransit.gondola_'+kind]='Gondola '+label
lang.update({'tooltip.createtransit.gondola_roof':'Stair placement: inner/outer corners and upside-down edges. Wrench side to flip height.', 'tooltip.createtransit.gondola_led':'Accent dye controls glow. Horizontal strips mount at top/bottom. Wrench side to flip height.', 'tooltip.createtransit.gondola_double_door':'Place at center. 3 wide, 3 high: two 1-block leaves + 0.5-block side panels. Create station controls.', 'tooltip.createtransit.gondola_outer_corner':'Click near the chosen footprint corner. Stack to continue that corner; wrench the top to rotate.'})
js(A/'lang/en_us.json',lang)
from generate_gondola_audio import generate as generate_audio
generate_audio()
import runpy
runpy.run_path(str(Path(__file__).with_name('generate_cable_assets.py')))
print('Generated 23 modular cabin components, inset floor rims/corner posts, continuous two-face LED junctions and a centered 3x3 double door with legacy 3x2 support.')

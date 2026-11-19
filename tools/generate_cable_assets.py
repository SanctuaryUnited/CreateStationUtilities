"""Original cable/grip models; no Steam 'n' Rails meshes or textures are bundled."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,math
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/createtransit'
def js(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
def box(a,b,texture='cable_steel',rotation=None):
    e={'from':a,'to':b,'faces':{f:{'texture':'#'+texture,'uv':[0,0,16,16]}for f in ['north','south','east','west','up','down']}}
    if rotation:e['rotation']=rotation
    return e
def rod(x=8,y=0,z=0,length=16,rotation=None):
    return [box([x-width/2,y+low,z],[x+width/2,y+high,z+length],rotation=rotation)for width,low,high in [(3,-.625,.625),(1.8,-1.5,-.625),(1.8,.625,1.5)]]
def model(name,elems):js(A/f'models/block/{name}.json',{'parent':'minecraft:block/block','render_type':'minecraft:cutout_mipped','ambientocclusion':False,'textures':{'particle':'createtransit:block/cable_steel','cable_steel':'createtransit:block/cable_steel','cable_grip_metal':'createtransit:block/cable_grip_metal','cable_grip_rubber':'createtransit:block/cable_grip_rubber'},'elements':elems})
# XO rotates through blockstate; individual JSON elements allow only up to 45 degrees.
meshes={'z_ortho':rod(),'x_ortho':rod()}
for name,angle in [('diag',45),('diag_2',-45)]:meshes[name]=rod(z=8-8*math.sqrt(2),length=16*math.sqrt(2),rotation={'origin':[8,0,8],'axis':'y','angle':angle})
meshes['ascending']=rod(length=16*math.sqrt(2),rotation={'origin':[8,0,0],'axis':'x','angle':-45})
meshes['teleport']=rod()
def xrod():return [box([0,lo,8-w/2],[16,hi,8+w/2])for w,lo,hi in [(3,-.625,.625),(1.8,-1.5,-.625),(1.8,.625,1.5)]]
meshes['cross_ortho']=rod()+xrod();meshes['cross_diag']=meshes['diag']+meshes['diag_2']
for prefix,diag in [('d1','diag'),('d2','diag_2')]:
    meshes['cross_'+prefix+'_xo']=meshes[diag]+xrod();meshes['cross_'+prefix+'_zo']=meshes[diag]+rod()
for name,elems in meshes.items():model('cable_'+name,elems)
model('cable_segment',rod(x=0));model('cable_empty',[])
mapping={'zo':('z_ortho',0),'xo':('x_ortho',90),'pd':('diag',0),'nd':('diag_2',0),'cr_o':('cross_ortho',0),'cr_d':('cross_diag',0),'cr_pdx':('cross_d1_xo',0),'cr_pdz':('cross_d1_zo',0),'cr_ndx':('cross_d2_xo',0),'cr_ndz':('cross_d2_zo',0),'as':('ascending',0),'aw':('ascending',90),'an':('ascending',180),'ae':('ascending',270),'ts':('teleport',0),'tw':('teleport',90),'tn':('teleport',180),'te':('teleport',270),'none':('empty',0)}
js(A/'blockstates/cable_track.json',{'variants':{'shape='+shape:{'model':'createtransit:block/cable_'+name,**({'y':angle}if angle else{})}for shape,(name,angle)in mapping.items()}})
js(A/'models/item/cable_track.json',{'parent':'createtransit:block/cable_z_ortho','display':{'gui':{'rotation':[30,135,0],'translation':[0,3,0],'scale':[1.2,1.2,1.2]}}})
# Create's bogey renderer origin includes a 1.5-block offset. A 16px clamp
# center aligns with the cable in suspended use; the stem reaches the roof mount.
grip=[box([-4,14,-9],[4,18,9],'cable_grip_metal'),box([-2,18,-8],[2,21,8],'cable_grip_metal'),box([-1.5,21,-2],[1.5,28,2],'cable_grip_metal'),box([-4,28,-4],[4,32,4],'cable_grip_metal')]
for z in [-6,6]:
    grip += [box([-5.5,13.5,z-2],[-4,18.5,z+2],'cable_grip_rubber'),box([4,13.5,z-2],[5.5,18.5,z+2],'cable_grip_rubber')]
model('cable_grip_partial',grip)
# All visible grip geometry is supplied by the bogey renderer.
js(A/'blockstates/cable_grip.json',{'variants':{'':{'model':'createtransit:block/cable_empty'}}})
for name,color in [('cable_steel',(117,125,137,255)),('cable_grip_metal',(215,223,234,255)),('cable_grip_rubber',(33,38,43,255))]:
    im=Image.new('RGBA',(32,32),color);draw=ImageDraw.Draw(im)
    for i in range(-32,64,6):draw.line((i,0,i+32,32),fill=tuple(max(0,c-22)for c in color[:3])+(255,),width=1)
    p=A/f'textures/block/{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
    Image.new('RGBA',(32,32),(128,128,255,255)).save(p.with_stem(name+'_n'))
    Image.new('RGBA',(32,32),(195,230,0,0)if name!='cable_grip_rubber'else(65,0,0,0)).save(p.with_stem(name+'_s'))
atlas=R/'assets/minecraft/atlases/blocks.json';data=json.loads(atlas.read_text());known={v.get('sprite')for v in data['sources']}
for name in ['cable_steel','cable_grip_metal','cable_grip_rubber']:
    sprite='createtransit:block/'+name
    if sprite not in known:data['sources'].append({'type':'minecraft:single','resource':sprite,'sprite':sprite})
js(atlas,data)
for file,value in [('data/create/tags/block/tracks.json','createtransit:cable_track'),('data/create/tags/block/safe_nbt.json','createtransit:cable_grip'),('data/minecraft/tags/block/mineable/pickaxe.json','createtransit:cable_track')]:
    p=R/file;data=json.loads(p.read_text())if p.exists()else{'replace':False,'values':[]}
    data['values']=[v for v in data['values']if(v.get('id')if isinstance(v,dict)else v)!=value]
    data['values'].append({'id':value,'required':False})
    js(p,data)
js(R/'data/createtransit/loot_table/blocks/cable_track.json',{'type':'minecraft:block','neoforge:conditions':[{'type':'neoforge:mod_loaded','modid':'railways'}],'pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'createtransit:cable_track'}]}]})
js(R/'data/createtransit/loot_table/blocks/cable_grip.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'create:railway_casing'}]}]})
js(R/'data/createtransit/recipe/cable_track.json',{'type':'minecraft:crafting_shaped','neoforge:conditions':[{'type':'neoforge:mod_loaded','modid':'railways'}],'pattern':['III',' S '],'key':{'I':{'item':'minecraft:iron_ingot'},'S':{'item':'minecraft:string'}},'result':{'id':'createtransit:cable_track','count':8}})
lang=json.loads((A/'lang/en_us.json').read_text());lang.update({'block.createtransit.cable_track':'Gondola Cable Track','block.createtransit.cable_grip':'Gondola Cable Grip','bogey.createtransit.cable_grip':'Gondola Cable Grip','tooltip.createtransit.cable_track':'Steam n Rails: link a station, enter assembly, then look up and use Railway Casing to create a hanging grip.','tooltip.createtransit.gondola_hanger':'With Steam n Rails: build below a Cable Grip and superglue the roof mount and cabin to it.'});js(A/'lang/en_us.json',lang)
print('Generated original steel cable models for all 19 track shapes, curve partial, hanging grip, labPBR materials and conditional recipe.')

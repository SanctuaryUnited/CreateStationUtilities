"""Add a dedicated, dyeable floor marker plate without regenerating existing kits."""
from pathlib import Path
import json
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/createtransit'
D=ROOT/'src/main/resources/data/createtransit'
def write(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n')
textures={k:'createtransit:block/elevator_'+k for k in ['metal','led','marker_icon']}
def box(a,b,tex,tint=-1):
 return {'from':a,'to':b,'faces':{face:{'uv':[0,0,16,16],'texture':'#'+tex,'tintindex':tint}for face in ['north','south','east','west','up','down']}}
els=[box([3,0,3],[13,2,13],'metal',0),box([5,2.01,5],[11,2.04,11],'marker_icon')]
# LED channels stop at each corner, avoiding overlapping coplanar surfaces.
els += [box([3.5,2.01,3.5],[12.5,2.04,4.25],'led',1),box([3.5,2.01,11.75],[12.5,2.04,12.5],'led',1),box([3.5,2.01,4.25],[4.25,2.04,11.75],'led',1),box([11.75,2.01,4.25],[12.5,2.04,11.75],'led',1)]
write(A/'models/block/elevator_floor_marker.json',{'ambientocclusion':True,'textures':textures,'elements':els})
write(A/'models/item/elevator_floor_marker.json',{'parent':'createtransit:block/elevator_floor_marker'})
write(A/'blockstates/elevator_floor_marker.json',{'variants':{'facing='+f:{'model':'createtransit:block/elevator_floor_marker','y':i*90,'uvlock':True}for i,f in enumerate(['north','east','south','west'])}})
im=Image.new('RGBA',(32,32),(22,30,38,255));draw=ImageDraw.Draw(im)
draw.rectangle((7,22,25,25),fill=(220,229,237,255))
draw.rectangle((14,8,18,19),fill=(220,229,237,255))
draw.polygon([(10,12),(16,5),(22,12)],fill=(220,229,237,255))
im.save(A/'textures/block/elevator_marker_icon.png')
Image.new('RGB',(32,32),(128,128,255)).save(A/'textures/block/elevator_marker_icon_n.png')
Image.new('RGBA',(32,32),(20,0,0,0)).save(A/'textures/block/elevator_marker_icon_s.png')
write(D/'recipe/elevator_floor_marker.json',{'type':'minecraft:crafting_shaped','category':'redstone','pattern':['IRI',' P '],'key':{'I':{'item':'minecraft:iron_nugget'},'R':{'item':'minecraft:redstone'},'P':{'item':'minecraft:light_weighted_pressure_plate'}},'result':{'id':'createtransit:elevator_floor_marker','count':2}})
write(D/'loot_table/blocks/elevator_floor_marker.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'createtransit:elevator_floor_marker','functions':[{'function':'minecraft:copy_custom_data','source':'block_entity','ops':[{'source':v,'target':v,'op':'replace'}for v in ['paint','accent']]}]}]}]})
p=A/'lang/en_us.json';d=json.loads(p.read_text());d.update({
 'block.createtransit.elevator_floor_marker':'Elevator Floor Marker',
 'tooltip.createtransit.elevator_marker':'Place on the landing floor, outside the shaft. Wrench: select. Crouch + wrench: bind selected drive.',
 'tooltip.createtransit.elevator_call_links':'Crouch + wrench: toggle selected marker link. Linked buttons call every linked elevator. Crouch + empty hand: clear links.',
 'tooltip.createtransit.elevator_drive_link':'Wrench: select this drive for floor marker binding.',
 'message.createtransit.elevator.drive_selected':'Drive selected (%s, %s, %s). Crouch + wrench a floor marker to bind it.',
 'message.createtransit.elevator.select_drive':'Wrench a loaded drive first. Marker must be within 8 blocks horizontally and 256 blocks above its base.',
 'message.createtransit.elevator.marker_bound':'Floor Y=%s is bound to drive (%s, %s, %s).',
 'message.createtransit.elevator.marker_selected':'Floor marker Y=%s selected. Crouch + wrench a call button to add/remove its link.',
 'message.createtransit.elevator.unbound_marker':'No loaded drive bound. Wrench a drive, then crouch + wrench this marker.',
 'message.createtransit.elevator.link_count':'Call button: %s floor marker links. Crouch + empty hand to clear all links.',
 'message.createtransit.elevator.select_marker':'Wrench a floor marker first, then crouch + wrench this button.',
 'message.createtransit.elevator.link_failed':'Cannot link: check loaded marker/drive, 256-block range, 32-link limit, or conflicting stops for the same elevator.',
 'message.createtransit.elevator.links_cleared':'Marker links cleared. Button now uses the nearest elevator.',
 'message.createtransit.elevator.called':'Called %s elevator(s).',
});write(p,d)
print('Generated dedicated floor marker model, icon, recipe, loot and linking messages.')

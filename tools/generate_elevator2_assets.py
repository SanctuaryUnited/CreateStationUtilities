"""Add dedicated marker rails and connected rail endings without regenerating other kits."""
from pathlib import Path
import json,copy
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit';D=ROOT/'src/main/resources/data/createtransit'
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n')
base=json.loads((A/'models/block/elevator_guide_rail.json').read_text())
def box(a,b,texture,tint=-1):
 return {'from':a,'to':b,'faces':{f:{'uv':[0,0,16,16],'texture':'#'+texture,**({'tintindex':tint}if tint>=0 else {})}for f in ['north','south','east','west','up','down']}}
marker=copy.deepcopy(base)
marker['elements'] += [box([4,5,4.5],[12,11,5],'rubber'),box([4.5,5.3,4.45],[11.5,6.2,4.49],'led',1),box([4.5,9.8,4.45],[11.5,10.7,4.49],'led',1),box([6,7,4.45],[10,9,4.49],'panel')]
dump(A/'models/block/elevator_floor_marker_rail.json',marker)
for end in ['top','bottom']:
 model=copy.deepcopy(base);model['elements']=[box([5,14.5 if end=='top'else 0,4.8],[11,16 if end=='top'else 1.5,11.2],'metal',0)]
 dump(A/'models/block'/f'elevator_rail_{end}.json',model)
for name in ['guide_rail','floor_marker_rail']:
 multi=[]
 for i,f in enumerate(['north','east','south','west']):
  def add(model,extra=None):multi.append({'when':{'facing':f,**(extra or {})},'apply':{'model':'createtransit:block/'+model,'y':i*90,'uvlock':True}})
  add('elevator_'+name);add('elevator_rail_top',{'up':'false'});add('elevator_rail_bottom',{'down':'false'})
 dump(A/'blockstates'/f'elevator_{name}.json',{'multipart':multi})
dump(A/'models/item/elevator_floor_marker_rail.json',{'parent':'createtransit:block/elevator_floor_marker_rail'})
loot=json.loads((D/'loot_table/blocks/elevator_guide_rail.json').read_text())
def replace(value):
 if isinstance(value,dict):return {k:replace(v)for k,v in value.items()}
 if isinstance(value,list):return [replace(v)for v in value]
 return value.replace('elevator_guide_rail','elevator_floor_marker_rail')if isinstance(value,str)else value
dump(D/'loot_table/blocks/elevator_floor_marker_rail.json',replace(loot))
dump(D/'recipe/elevator_floor_marker_rail.json',{'type':'minecraft:crafting_shapeless','category':'redstone','ingredients':[{'item':'createtransit:elevator_guide_rail'},{'item':'minecraft:redstone'},{'item':'minecraft:paper'}],'result':{'id':'createtransit:elevator_floor_marker_rail','count':1}})
langPath=A/'lang/en_us.json';lang=json.loads(langPath.read_text());lang['block.createtransit.elevator_floor_marker_rail']='Elevator Floor Marker Guide Rail';dump(langPath,lang)
tagPath=ROOT/'src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json'
if tagPath.exists():
 tag=json.loads(tagPath.read_text());v='createtransit:elevator_floor_marker_rail'
 if v not in tag['values']:tag['values'].append(v)
 dump(tagPath,tag)
# Keep the previous reachability manifest unchanged apart from the new roots.
p=ROOT/'tools/release-models.txt';models=set(p.read_text().splitlines());models.update(['block/elevator_floor_marker_rail','block/elevator_rail_top','block/elevator_rail_bottom','item/elevator_floor_marker_rail']);p.write_text('\n'.join(sorted(models))+'\n')
print('Added marker rail, connected rail ends, recipe, dyed drops and English name')

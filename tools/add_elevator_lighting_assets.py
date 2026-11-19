"""Dedicated ceiling housings and item previews. Run after generate_elevator_assets.py."""
from generate_elevator_assets import ROOT,A,D,dump,box,model,models
import json,itertools
names=['ceiling_strip','ceiling_double_strip','ceiling_panel','ceiling_bulb','ceiling_ring']
roof=models['roof'];housing={
'ceiling_strip':[box([6,-.23,0],[10,-.03,16])],
'ceiling_double_strip':[box([3,-.23,0],[6,-.03,16]),box([10,-.23,0],[13,-.03,16])],
'ceiling_panel':[box([2,-.21,2],[14,-.03,14],'rubber',-1)],
'ceiling_bulb':[box([5,-.23,5],[11,-.01,11],'rubber',-1)],
}
lamps={
'ceiling_strip':[box([7,-.27,0],[9,-.22,16],'led',1)],
'ceiling_double_strip':[box([4,-.27,0],[5,-.22,16],'led',1),box([11,-.27,0],[12,-.22,16],'led',1)],
'ceiling_panel':[box([3,-.25,3],[13,-.20,13],'led',1)],
'ceiling_bulb':[box([5,-.5,6],[11,-.22,10],'led',1),box([6,-.5,5],[10,-.22,6],'led',1),box([6,-.5,10],[10,-.22,11],'led',1)],
'ceiling_ring':[box([2,-.24,2],[14,-.19,3],'led',1),box([2,-.24,13],[14,-.19,14],'led',1),box([2,-.24,3],[3,-.19,13],'led',1),box([13,-.24,3],[14,-.19,13],'led',1)]}
for name in names[:-1]:
 model(name,roof+housing[name]);model(name+'_item',roof+housing[name]+lamps[name]);dump(A/'blockstates'/f'elevator_{name}.json',{'variants':{'facing='+f:{'model':'createtransit:block/elevator_'+name,'y':i*90,'uvlock':True}for i,f in enumerate(['north','east','south','west'])}})
# Ring housings follow the complete connected perimeter; no internal dividers.
for mask in range(16):
 n,e,s,w=[bool(mask&(1<<i))for i in range(4)];x=0 if w else 1.5;X=16 if e else 14.5;z=0 if n else 3.5;Z=16 if s else 12.5;els=[]
 if not n:els.append(box([x,-.18,1.5],[X,-.02,3.5]))
 if not s:els.append(box([x,-.18,12.5],[X,-.02,14.5]))
 if not w:els.append(box([1.5,-.18,z],[3.5,-.02,Z]))
 if not e:els.append(box([12.5,-.18,z],[14.5,-.02,Z]))
 model('ring_housing_'+str(mask),els)
for i,corner in enumerate(['ne','se','sw','nw']):
 def rotate(p):
  x,y,z=p
  for _ in range(i):x,z=16-z,x
  return [x,y,z]
 els=[]
 for a,b in [([12.5,-.18,0],[14.5,-.02,3.5]),([14.5,-.18,1.5],[16,-.02,3.5])]:
  points=[rotate(a),rotate(b)];els.append(box([min(q[j]for q in points)for j in range(3)],[max(q[j]for q in points)for j in range(3)]))
 model('ring_inner_'+corner,els)
model('ceiling_ring',roof);model('ceiling_ring_item',roof+json.loads((A/'models/block/elevator_ring_housing_0.json').read_text())['elements']+lamps['ceiling_ring'])
multi=[{'apply':{'model':'createtransit:block/elevator_ceiling_ring'}}];dirs=['north','east','south','west'];diag=['ne','se','sw','nw']
for i,f in enumerate(dirs):
 for mask in range(16):multi.append({'when':{'facing':f,**{dirs[(j+i)%4]:str(bool(mask&(1<<j))).lower()for j in range(4)}},'apply':{'model':'createtransit:block/elevator_ring_housing_'+str(mask),'y':i*90,'uvlock':True}})
 for j,c in enumerate(diag):multi.append({'when':{'facing':f,dirs[(j+i)%4]:'true',dirs[(j+1+i)%4]:'true',diag[(j+i)%4]:'false'},'apply':{'model':'createtransit:block/elevator_ring_inner_'+c,'y':i*90,'uvlock':True}})
dump(A/'blockstates/elevator_ceiling_ring.json',{'multipart':multi})
patterns=[[' I ','ILI',' R '],['I I','LLL','IRI'],['III','LLL','IRI'],[' G ','ILI',' R '],['ILI','L L','IRI']]
for name,pattern in zip(names,patterns):
 dump(A/'models/item'/f'elevator_{name}.json',{'parent':'createtransit:block/elevator_'+name+'_item','display':{'gui':{'rotation':[210,135,0],'scale':[.65,.65,.65]}}})
 dump(D/'recipe'/f'elevator_{name}.json',{'type':'minecraft:crafting_shaped','category':'building','pattern':pattern,'key':{k:{'item':'minecraft:'+v}for k,v in {'I':'iron_ingot','L':'glowstone_dust','R':'redstone','G':'glass'}.items()if any(k in row for row in pattern)},'result':{'id':'createtransit:elevator_'+name,'count':2}})
 dump(D/'loot_table/blocks'/f'elevator_{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'createtransit:elevator_'+name,'functions':[{'function':'minecraft:copy_custom_data','source':'block_entity','ops':[{'source':v,'target':v,'op':'replace'}for v in ['paint','accent']]}]}]}]})
p=A/'lang/en_us.json';lang=json.loads(p.read_text());lang.update({'block.createtransit.elevator_'+k:'Elevator '+k.replace('_',' ').title()for k in names});dump(p,lang)
print('Generated five dedicated elevator ceiling variants and connected ring perimeter housings.')

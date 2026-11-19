"""Compare baked inputs for every reachable state against the original Alpha 12 resources."""
from pathlib import Path
from zipfile import ZipFile
import json,itertools,sys
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
prior=Path(sys.argv[1])
with ZipFile(prior) as original:
 cache={}
 def signature(part):
  model=part['apply']['model']
  if model not in cache:
   mesh=json.loads((A/'models'/Path(model.split(':')[1]+'.json')).read_text())['elements']
   cache[model]=None if not mesh else json.dumps(mesh,sort_keys=True,separators=(',',':'))
  return (cache[model],part['apply'].get('y',0),part['apply'].get('uvlock',False))
 total=0;before=0;after=0
 for name,height,door in [('platform_screen_door',48,True),('platform_gate',24,True),('compact_screen_door',32,True),('platform_wall',48,False),('platform_barrier',24,False),('compact_wall',32,False)]:
  old=json.loads(original.read('assets/createtransit/blockstates/'+name+'.json'))['multipart'];new=json.loads((A/'blockstates'/f'{name}.json').read_text())['multipart'];before+=len(old);after+=len(new)
  for facing,col,row,left,right,panel,corner in itertools.product(['north','east','south','west'],range(2 if door else 1),range((height+15)//16),['false','true'],['false','true'],range(3),range(3)):
   state={'facing':facing,'column':str(col),'row':str(row),'left':left,'right':right,'panel':str(panel),'corner':str(corner)}
   def select(parts):return sorted([signature(p) for p in parts if all(state[k]==v for k,v in p['when'].items()) and signature(p)[0] is not None])
   assert select(old)==select(new),(name,state)
   total+=1
 print('PASS:',total,'reachable geometry combinations unchanged;',before,'->',after,'multipart rules;',round(100*(1-after/before),1),'percent reduction')

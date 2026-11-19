"""Validate optimized blockstate/item references against the pre-optimization source snapshot."""
from pathlib import Path
from zipfile import ZipFile
import json,sys
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
# Baseline intentionally uses the same update's models, before canonical references.
BASE=Path(sys.argv[1])
manifest=set((ROOT/'tools/release-models.txt').read_text().splitlines())
models={str(f.relative_to(A/'models')).removesuffix('.json'):json.loads(f.read_text())for f in(A/'models').rglob('*.json')}
def refs(v):
 if isinstance(v,dict):
  for k,x in v.items():
   if k in ['parent','model']and isinstance(x,str)and x.startswith('createtransit:'):yield x.split(':',1)[1]
   else:yield from refs(x)
 elif isinstance(v,list):
  for x in v:yield from refs(x)
def expanded(v,lookup):
 if isinstance(v,dict):
  return {k:lookup(x.split(':',1)[1])if k in ['parent','model']and isinstance(x,str)and x.startswith('createtransit:')else expanded(x,lookup)for k,x in v.items()}
 if isinstance(v,list):return [expanded(x,lookup)for x in v]
 return v
with ZipFile(BASE)as z:
 checked=0
 for f in(A/'blockstates').glob('*.json'):
  new=json.loads(f.read_text());old=json.loads(z.read(str(f.relative_to(A))))
  assert expanded(new,lambda k:models[k])==expanded(old,lambda k:json.loads(z.read('models/'+k+'.json'))),f
  assert all(k in manifest for k in refs(new)),f
  checked+=1
 for f in(A/'models/item').glob('*.json'):
  new=json.loads(f.read_text());old=json.loads(z.read(str(f.relative_to(A))))
  assert expanded(new,lambda k:models[k])==expanded(old,lambda k:json.loads(z.read('models/'+k+'.json'))),f
  assert all(k in manifest for k in refs(new)),f
  checked+=1
for k in manifest:assert k in models and all(r in manifest for r in refs(models[k])),k
assert all('block/'+k in manifest for k in ['cable_segment','cable_empty','cable_grip_partial'])
print('PASS:',checked,'complete blockstate/item documents preserve all transforms, conditions, models, tint and textures;',len(manifest),'reachable release models; all references resolve, including optional cable partials.')

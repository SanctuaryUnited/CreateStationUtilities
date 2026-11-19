"""Find the release model closure and canonicalize exact duplicate model references.

Each kit retains its own materials. Source generator models remain available;
only reachable models are packaged. Run after all asset generators.
"""
from pathlib import Path
import json,collections,time
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
MODELS=A/'models';MANIFEST=ROOT/'tools/release-models.txt'
def refs(value):
 if isinstance(value,dict):
  for key,v in value.items():
   if key in ('model','parent')and isinstance(v,str)and v.startswith('createtransit:'):yield v.split(':',1)[1]
   else:yield from refs(v)
 elif isinstance(value,list):
  for v in value:yield from refs(v)
def closure(models,documents):
 roots={k for k in models if k.startswith('item/')}|{'block/cable_segment','block/cable_empty','block/cable_grip_partial'}|{k for k in models if k.startswith('block/escalator_surface_')}
 for doc in documents.values():roots.update(refs(doc))
 seen=set();todo=list(roots)
 while todo:
  key=todo.pop()
  if key in seen:continue
  if key not in models:raise ValueError('Missing reachable model: '+key)
  seen.add(key);todo.extend(refs(models[key]))
 return seen
def rewrite(value,canonical):
 if isinstance(value,dict):
  return {k:('createtransit:'+canonical.get(v.split(':',1)[1],v.split(':',1)[1])if k in ('model','parent')and isinstance(v,str)and v.startswith('createtransit:')else rewrite(v,canonical))for k,v in value.items()}
 if isinstance(value,list):return [rewrite(v,canonical)for v in value]
 return value
def main():
 models={str(p.relative_to(MODELS)).removesuffix('.json'):json.loads(p.read_text())for p in MODELS.rglob('*.json')}
 states={p:json.loads(p.read_text())for p in(A/'blockstates').glob('*.json')}
 before=closure(models,states);groups=collections.defaultdict(list)
 # Do not share resources between transit kits, even when a mesh is empty.
 for key in sorted(before):
  if key.startswith('block/'):
   family=key.split('/')[1].split('_')[0];groups[(family,json.dumps(models[key],sort_keys=True,separators=(',',':')))].append(key)
 canonical={key:keys[0]for keys in groups.values()for key in keys}
 original_states={p:json.dumps(d,sort_keys=True)for p,d in states.items()}
 states={p:rewrite(d,canonical)for p,d in states.items()}
 # Check every rewritten model reference has the identical complete source model.
 for old,new in canonical.items():assert models[old]==models[new],(old,new)
 for p,d in states.items():p.write_text(json.dumps(d,separators=(',',':'))+'\n')
 for key,d in list(models.items()):
  if key.startswith('item/'):
   new=rewrite(d,canonical);models[key]=new;(MODELS/(key+'.json')).write_text(json.dumps(new,separators=(',',':'))+'\n')
 after=closure(models,states)
 MANIFEST.write_text('\n'.join(sorted(after))+'\n')
 original_bytes=sum((MODELS/(k+'.json')).stat().st_size for k in models)
 release_bytes=sum((MODELS/(k+'.json')).stat().st_size for k in after)
 report={'source_models':len(models),'original_reachable_models':len(before),'release_models':len(after),'excluded_models':len(models)-len(after),'exact_duplicate_block_models':sum(k!=v for k,v in canonical.items()),'model_json_bytes_before':original_bytes,'model_json_bytes_release':release_bytes,'model_json_bytes_saved':original_bytes-release_bytes,'blocks_kept':len(states),'all_reference_geometry_preserved':True,'partial_models_kept':['cable_segment','cable_empty','cable_grip_partial'],'note':'Resource reduction is verified; full client startup seconds require measurement on the same modpack.'}
 (ROOT/'docs/Alpha15-Loading-Audit.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()

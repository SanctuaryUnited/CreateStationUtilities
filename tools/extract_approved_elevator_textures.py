#!/usr/bin/env python3
"""Crop user-supplied sheets; nearest-neighbor sample to 16px, no generated artwork.
Usage: python3 tools/extract_approved_elevator_textures.py REFERENCE_DIRECTORY
"""
from PIL import Image
from pathlib import Path
import json,hashlib,shutil,sys
root=Path(__file__).resolve().parents[1]
source=Path(sys.argv[1]); out=root/'src/main/resources/assets/createtransit/textures/block/elevator3'
refs=root/'reference/elevators3'; refs.mkdir(parents=True,exist_ok=True)
for p in source.iterdir():
 if p.suffix.lower() in ('.png','.jpeg','.jpg'): shutil.copy2(p,refs/p.name)
steel='75EA056E-75C5-4AB1-AA27-CDDF2DD10B7B.jpeg'
gun='09AFEE78-1E04-4FB2-A84A-10518CB0BF98.jpeg'
common='76894C2E-3731-4C67-9AAE-85E0F9F95AD3.jpeg'
specs={
 'brushed_steel':{'panel':(steel,(17,108,269,364)),'frame':(steel,(17,108,269,364)),'ceiling':(steel,(279,414,522,663)),'floor':(steel,(17,414,269,663)),'edge':(steel,(279,414,522,663)),'base_trim':(steel,(532,600,773,607))},
 'modern_dark':{'panel':(gun,(24,111,144,239)),'frame':(gun,(24,111,144,239)),'ceiling':(gun,(308,305,426,431)),'floor':(gun,(168,305,286,431)),'edge':(gun,(24,420,143,430)),'base_trim':(gun,(24,420,143,430))}}
shared={'silver':(steel,(279,414,522,663)),'trim':(steel,(279,414,522,663)),'dark':(gun,(24,400,143,414)),'led':(gun,(451,309,559,428)),'white':(gun,(451,309,559,428)),'glass':(common,(290,860,357,934))}
provenance=[]
for quality in ('off','low','medium','high'):
 for old in ('wood_trim','light_transit','modern_dark','brushed_steel'):
  p=out/quality/old
  if p.exists():shutil.rmtree(p)
 for skin,tiles in specs.items():
  d=out/quality/skin;d.mkdir(parents=True,exist_ok=True)
  for name,(sheet,rect) in {**shared,**tiles}.items():
   im=Image.open(source/sheet).convert('RGB').crop(rect)
   exact=refs/'extracted'/skin/(name+'.png');exact.parent.mkdir(parents=True,exist_ok=True)
   if quality=='high': im.save(exact)
   tile=im.resize((16,16),Image.Resampling.NEAREST);target=d/(name+'.png');tile.save(target)
   provenance.append({'path':str(target.relative_to(root)),'source':sheet,'crop':list(rect),'operation':'crop; nearest-neighbor sample to 16x16; no recoloring','sha256':hashlib.sha256(target.read_bytes()).hexdigest()})
for p in (root/'src/main/resources/assets/createtransit/models/block/elevator3').glob('*.json'):
 if p.name.startswith(('wood_trim_','light_transit_')):p.unlink()
manifest=root/'tools/release-models.txt'
manifest.write_text('\n'.join(l for l in manifest.read_text().splitlines() if not l.startswith(('block/elevator3/wood_trim_','block/elevator3/light_transit_')))+'\n')
atlas=root/'src/main/resources/assets/minecraft/atlases/blocks.json';data=json.loads(atlas.read_text())
data['sources']=[s for s in data['sources'] if not ('elevator3/' in s.get('resource','') and '/touch/' not in s.get('resource',''))]
for p in sorted(out.glob('*/*/*.png')):
 if p.parent.name not in specs:continue
 resource='createtransit:block/elevator3/'+p.relative_to(out).with_suffix('').as_posix()
 data['sources'].append({'type':'minecraft:single','resource':resource,'sprite':resource})
atlas.write_text(json.dumps(data,indent=2)+'\n')
(refs/'texture-provenance.json').write_text(json.dumps({'source_hashes':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in refs.iterdir() if p.is_file() and p.suffix in ('.jpeg','.png')},'note':'Supplied JPEG sheets are flattened reference boards, not lossless source 16px tiles. Original boards and lossless full-resolution crops retained. Runtime 16px tiles sample supplied pixels without new artwork.','tiles':provenance},indent=2)+'\n')
print(f'Extracted {len(provenance)} runtime tiles from supplied sheets; original references retained.')

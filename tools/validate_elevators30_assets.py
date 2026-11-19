from pathlib import Path
import json,hashlib
from PIL import Image
root=Path(__file__).resolve().parents[1];ref=root/'reference/elevators3';manifest=json.loads((ref/'texture-provenance.json').read_text());count=0
for item in manifest['tiles']:
 p=root/item['path'];im=Image.open(p);assert im.size==(16,16),p
 expected=Image.open(ref/item['source']).convert('RGB').crop(item['crop']).resize((16,16),Image.Resampling.NEAREST)
 assert im.convert('RGB').tobytes()==expected.tobytes(),p
 assert hashlib.sha256(p.read_bytes()).hexdigest()==item['sha256'],p
 count+=1
textures=root/'src/main/resources/assets/createtransit/textures/block/elevator3'
assert not list(textures.glob('*/wood_trim/*'));assert not list(textures.glob('*/light_transit/*'))
atlas=json.loads((root/'src/main/resources/assets/minecraft/atlases/blocks.json').read_text())
for s in atlas['sources']:
 resource=s.get('resource','')
 if resource.startswith('createtransit:block/elevator3/'):
  assert (root/'src/main/resources/assets/createtransit/textures'/Path(resource.split(':',1)[1]+'.png')).exists(),resource
for p in (root/'src/main/resources').rglob('*.json'):json.loads(p.read_text())
print(f'PASS: {count} exact crop/sampling matches, native 16x16 dimensions, hashes, skin removal, atlas references and resource JSON syntax.')

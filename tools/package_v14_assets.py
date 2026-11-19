from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import hashlib,json,shutil
R=Path(__file__).resolve().parents[1];D=R/'build/v14-assets';O=Path('/workspace/scratch/52ce15fa3a0d/Station-Utilities-V14');A=R/'src/main/resources/assets/createtransit'
readme='''# Create: Station Utilities — V14 assets

These are implemented asset files, not concept art.

- `assets/createtransit/textures/block/elevator3/`: 504 original PNGs — native skin tiles, LabPBR companions, and terminal/intermediate touchscreen states.
- `assets/createtransit/models/block/elevator3/`: 36 Minecraft block-model reference poses for all 12 door skin/model pairs.
- `meshes/`: 192 exports from the production CabinGeometry class, each with closed/opening/open cabin and landing doors. Coordinates are Minecraft blocks; face records contain material index and four XYZ vertices.
- `preview/`: eight software-rendered cabin/door sheets from those meshes. The cabin views remove the front frame to show the interior. Material projection is approximate; images do not show real shader reflections, world lighting, or gameplay.
- `animation/animations.json`: metadata describing the actual Java 40-tick interpolation. The runtime does not load a third-party animation format.
- `BUILDING_GUIDE.md`, `IMPLEMENTATION.md`: configuration, tests and limitations.

Models/animations are procedural Java in the mod. The separate complete source ZIP contains the generators, exporters, production geometry and renderer. The 36 native JSON models are static reference snapshots, not new individually registered door blocks. Select the combinations through the elevator menus.

Custom PNG artwork is original deterministic pixel code. References guided the designs; no showcase images were used as texture atlases. Minecraft plank textures are referenced by resource ID, not included as loose files. Existing project licensing applies; original generated model motor/door recordings retain their separate CC0 notice in the source project.
'''
manifest={'version':'0.2.0-dev14','png_count':504,'native_door_models':36,'production_meshes':192,'preview_sheets':8,'files':[]}
with ZipFile(O/'Station-Utilities-V14-Assets.zip','w',ZIP_DEFLATED,compresslevel=9) as z:
 z.writestr('README.md',readme)
 pairs=[]
 for p in (A/'textures/block/elevator3').rglob('*'):
  if p.is_file():pairs.append((p,'assets/createtransit/'+p.relative_to(A).as_posix()))
 for p in (A/'models/block/elevator3').glob('*.json'):pairs.append((p,'assets/createtransit/'+p.relative_to(A).as_posix()))
 for folder in ['meshes','preview']:
  for p in (D/folder).glob('*'):pairs.append((p,folder+'/'+p.name))
 pairs.extend([(A/'elevator3/animations.json','animation/animations.json'),(R/'docs/V14_ELEVATORS3_GUIDE.md','BUILDING_GUIDE.md'),(R/'docs/V14_IMPLEMENTATION.md','IMPLEMENTATION.md')])
 for p,name in sorted(pairs,key=lambda e:e[1]):
  z.write(p,name);manifest['files'].append({'path':name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
 z.writestr('manifest.json',json.dumps(manifest,indent=2)+'\n')
with ZipFile(O/'Station-Utilities-V14-Assets.zip') as z:assert z.testzip() is None;assert len(z.namelist())==len(set(z.namelist()))
source=O/'CREATE-Transit-Alpha-0.1.0-Source.zip';renamed=O/'Create-Station-Utilities-V14-Source.zip'
if source.exists():source.rename(renamed)
shutil.copy2(R/'docs/ELEVATORS3_SIMPLE_WORKFLOW.md',O/'Create-Transit-Simple-Elevator-Guide.md')
paths=[O/'create-station-utilities-0.2.0-dev14.jar',renamed,O/'Station-Utilities-V14-Assets.zip',O/'Create-Transit-Simple-Elevator-Guide.md',O/'Elevators3-Implementation-Status.md']
(O/'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in paths))
print(json.dumps([{'path':str(p),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in paths],indent=2))

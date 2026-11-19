"""Package the current source, compiled JAR and actual-geometry asset previews.

Run after build, native validation and ExportCabinAssets/render_elevator3_assets.
Usage: python tools/package_elevator3_release.py /absolute/output/directory [--code-only]
"""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from collections import Counter
import hashlib, json, shutil, sys, re

ROOT = Path(__file__).resolve().parents[1]
OUT = Path(sys.argv[1]).resolve()
OUT.mkdir(parents=True, exist_ok=True)
DATA = ROOT / 'build/elevator3-assets'
VERSION = re.search(r"^version = '([^']+)'", (ROOT/'build.gradle').read_text(), re.M).group(1)

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def audit_zip(path):
    with ZipFile(path) as z:
        assert z.testzip() is None, path
        assert len(z.namelist()) == len(set(z.namelist())), path
        assert all(not p.startswith('/') and '..' not in Path(p).parts for p in z.namelist())

jar = ROOT / 'build/libs' / f'create-station-utilities-{VERSION}.jar'
with ZipFile(jar) as z:
    metadata = z.read('META-INF/neoforge.mods.toml').decode()
    assert f'version="{VERSION}"' in metadata and 'modId="createtransit"' in metadata
    classes = list((ROOT / 'build/classes/java/main').rglob('*.class'))
    for p in classes:
        key = p.relative_to(ROOT / 'build/classes/java/main').as_posix()
        assert z.read(key) == p.read_bytes(), f'Stale class: {key}'
    allowed_models = set((ROOT/'tools/release-models.txt').read_text().splitlines())
    intentionally_excluded_models = 0
    for p in (ROOT / 'src/main/resources').rglob('*'):
        if p.is_file():
            key = p.relative_to(ROOT / 'src/main/resources').as_posix()
            if key.startswith('assets/createtransit/models/') and key.endswith('.json') and key[len('assets/createtransit/models/'):-5] not in allowed_models:
                assert key not in z.namelist(), f'Unreachable source-only model packaged: {key}'
                intentionally_excluded_models += 1
                continue
            if key != 'META-INF/neoforge.mods.toml':
                assert z.read(key) == p.read_bytes(), f'Stale resource: {key}'
    assert not any(p.startswith('data/createtransit/recipe/lrt_') for p in z.namelist())
    sounds = json.loads(z.read('assets/createtransit/sounds.json'))
    for entry in sounds.values():
        for sound in entry.get('sounds', []):
            name = sound if isinstance(sound, str) else sound['name']
            if name.startswith('createtransit:'):
                assert 'assets/createtransit/sounds/' + name.split(':', 1)[1] + '.ogg' in z.namelist(), name
shutil.copy2(jar, OUT / jar.name)
audit_zip(OUT / jar.name)

# Audio/code patches can reuse the unchanged visual catalog from dev2.
if '--code-only' in sys.argv[2:]:
    report=OUT/'Elevators3-Implementation-Status.md'
    shutil.copy2(ROOT/'docs/ELEVATORS3_IMPLEMENTATION.md',report)
    excluded={'build','run','.gradle','.git','__pycache__','.idea'}
    sources=[p for p in ROOT.rglob('*') if p.is_file() and not any(x in excluded for x in p.relative_to(ROOT).parts) and p.suffix!='.pyc']
    source_zip=OUT/'CREATE-Transit-Alpha-0.1.0-Source.zip'
    with ZipFile(source_zip,'w',ZIP_DEFLATED,compresslevel=9) as z:
        for p in sorted(sources):z.write(p,'create-transit/'+p.relative_to(ROOT).as_posix())
    audit_zip(source_zip)
    with ZipFile(source_zip) as z:
        for p in sources:assert z.read('create-transit/'+p.relative_to(ROOT).as_posix())==p.read_bytes()
    artifacts=[OUT/jar.name,source_zip,report]
    (OUT/'SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in artifacts))
    print(json.dumps({'version':VERSION,'compiled_classes_verified':len(classes),'source_files_verified':len(sources),'source_only_models_intentionally_excluded':intentionally_excluded_models,'artifacts':[{'path':str(p),'bytes':p.stat().st_size,'sha256':sha(p)} for p in artifacts]},indent=2))
    sys.exit(0)

counts = {name: len(list((DATA/name).glob('*.json'))) for name in ['models', 'catalog', 'finishes']}
assert counts == {'models': 20, 'catalog': 184, 'finishes': 19}, counts
previews = sorted((DATA/'preview').glob('*.png'))
assert len(previews) == 27, len(previews)
records = []
for group in counts:
    for p in sorted((DATA/group).glob('*.json')):
        doc = json.loads(p.read_text())
        records.append({'path': p.relative_to(DATA).as_posix(), 'key': doc['key'], 'settings': doc['settings'], 'sha256': sha(p)})
assert len({json.dumps(r['settings'], sort_keys=True) for r in records if r['path'].startswith('catalog/')}) == 184
provenance = {
    'mod_version': VERSION,
    'description': 'Software renders of the same CabinGeometry mesh used by the native model-cabin renderer. No shader screenshots or hardware FPS claims.',
    'coordinate_units': 'Minecraft blocks; local X/Z are the footprint, local Y is vertical. Face entries are material index followed by four XYZ vertices.',
    'primary_door_pose': 'open', 'catalog_door_pose': 'closed',
    'counts': counts,
    'family_counts': dict(Counter(r['settings']['variant'] for r in records if r['path'].startswith('catalog/'))),
    'source_sha256': {str(p.relative_to(ROOT)): sha(p) for p in [
        ROOT/'src/main/java/com/sanctuarygames/createtransit/elevator/model/CabinConfiguration.java',
        ROOT/'src/main/java/com/sanctuarygames/createtransit/elevator/model/CabinGeometry.java',
        ROOT/'tools/elevator3/ExportCabinAssets.java', ROOT/'tools/render_elevator3_assets.py']},
    'rendered_png_sha256': {p.name: sha(p) for p in previews},
    'configurations': records,
}
(DATA/'catalog-index.json').write_text(json.dumps(provenance, indent=2)+'\n')
(DATA/'README.md').write_text('''# Create: Transit — Elevators 3.0 asset preview catalog

Open `preview/Elevators3-All-20-Cabins.png` for the 20 base size/style models.
The five `Elevators3-Catalog-*.png` sheets show all 184 distinct structural,
door and glazing combinations, with front/rear views of each configuration.
F = front door, R = rear door, S = solid, G = glass, dash = disabled.
For panoramic cabins L/R means enabled left/right side glazing.
The interior finish sheet shows all four ceiling lights and four handrail
choices plus eleven vanilla woods. These 19 examples do not exhaust every
possible dye, material, light and door combination.

Meshes in models/, catalog/ and finishes/ were exported from the shipped
CabinGeometry Java class. Each face has one material index and twelve XYZ
values for four vertices. Settings and hashes are in catalog-index.json.
The source archive contains the full production renderer, collision code,
exporter and software preview renderer; no separately designed mockup is used.

Software previews approximate glass and omit actual shader reflections,
world lighting, Minecraft GUI and gameplay. Vanilla textures appear only
inside rendered images; no loose Minecraft texture files are redistributed.
Mod source/assets retain their existing rights. The two original generated
door/motor recordings have the separate CC0 notice included here.

See BUILDING_GUIDE.md and IMPLEMENTATION.md for installation and test limits.
''')
asset_zip = OUT/'Create-Transit-Elevators3-Assets-and-Previews.zip'
with ZipFile(asset_zip, 'w', ZIP_DEFLATED, compresslevel=9) as z:
    for p in sorted(DATA.rglob('*')):
        if p.is_file(): z.write(p, p.relative_to(DATA).as_posix())
    for source, target in [
        ('docs/ELEVATORS3_BUILDING_GUIDE.md', 'BUILDING_GUIDE.md'),
        ('docs/ELEVATORS3_IMPLEMENTATION.md', 'IMPLEMENTATION.md'),
        ('docs/ELEVATOR3_AUDIO_LICENSE.md', 'AUDIO_LICENSE.md'),
        ('src/main/resources/assets/createtransit/sounds/elevator_model_door.ogg','original-audio/elevator_model_door.ogg'),
        ('src/main/resources/assets/createtransit/sounds/elevator_model_motor.ogg','original-audio/elevator_model_motor.ogg')]:
        z.write(ROOT/source, target)
audit_zip(asset_zip)
shutil.copy2(DATA/'preview/Elevators3-All-20-Cabins.png', OUT/'Elevators3-All-20-Cabins.png')
shutil.copy2(ROOT/'docs/ELEVATORS3_IMPLEMENTATION.md', OUT/'Elevators3-Implementation-Status.md')

excluded = {'build', 'run', '.gradle', '.git', '__pycache__', '.idea'}
sources = [p for p in ROOT.rglob('*') if p.is_file() and not any(x in excluded for x in p.relative_to(ROOT).parts) and p.suffix != '.pyc']
source_zip = OUT/'CREATE-Transit-Alpha-0.1.0-Source.zip'
with ZipFile(source_zip, 'w', ZIP_DEFLATED, compresslevel=9) as z:
    for p in sorted(sources): z.write(p, 'create-transit/'+p.relative_to(ROOT).as_posix())
audit_zip(source_zip)
with ZipFile(source_zip) as z:
    for p in sources:
        assert z.read('create-transit/'+p.relative_to(ROOT).as_posix()) == p.read_bytes()

artifacts = [OUT/jar.name, source_zip, OUT/'Elevators3-Implementation-Status.md', OUT/'Elevators3-All-20-Cabins.png', asset_zip]
(OUT/'SHA256SUMS.txt').write_text(''.join(sha(p)+'  '+p.name+'\n' for p in artifacts))
print(json.dumps({'version': VERSION, 'compiled_classes_verified': len(classes), 'source_files_verified': len(sources), 'source_only_models_intentionally_excluded': intentionally_excluded_models, 'mesh_counts': counts, 'preview_pngs': len(previews), 'artifacts': [{'path': str(p), 'bytes': p.stat().st_size, 'sha256': sha(p)} for p in artifacts]}, indent=2))

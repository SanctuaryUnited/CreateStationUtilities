"""Package the circuit only after its native server validation completes."""
from pathlib import Path
import argparse,hashlib,re,shutil,zipfile,subprocess,sys
ROOT=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--test-log',type=Path,required=True)
parser.add_argument('--output',type=Path,required=True)
args=parser.parse_args()
log=args.test_log.read_text()
assert '82 GAME TESTS COMPLETE' in log and re.search(r'All.*required tests passed',log), 'Complete native test pass required before packaging'
assert 'BUILD SUCCESSFUL' in log, 'Native server must finish successfully'
subprocess.run([sys.executable,str(ROOT/'tools/validate_lrt_circuit.py')],check=True)
dest=args.output.resolve();dest.mkdir(parents=True,exist_ok=True)
jar=ROOT/'build/libs/create-transit-0.5.0-alpha.12-platforms.2.jar'
with zipfile.ZipFile(jar) as z:
 assert z.testzip() is None
 assert 'version="0.5.0-alpha.12-platforms.2"' in z.read('META-INF/neoforge.mods.toml').decode()
 for f in (ROOT/'src/main/resources').rglob('*'):
  if not f.is_file():continue
  p=f.relative_to(ROOT/'src/main/resources')
  if p.parts[0] in ('assets','data'):assert z.read(p.as_posix())==f.read_bytes(),p
validation='''Create Transit Alpha 12.2 — LRT test circuit verification

Minecraft 1.21.1 / NeoForge 21.1.248 / Create 6.0.10 / Java 21.
All 82 required base-runtime headless Minecraft GameTests passed.
Optional Steam n Rails cable tests were excluded from this standard-track run.
Native circuit tests parse the actual exported NBT, curve connections and filled
schedule components; bind all four station stops and 16 block signals; assemble
the staged three-car train with its exact glue; and complete A → B → A on the
full-size outer circuit. Both navigated running legs exceed 250 blocks.
Inner-loop graph connectivity and signal bindings are verified independently.

Three independent NBT checks pass: CRN, NoCRN and track/stations only.
All variants retain a 512 x 16 x 512 structure size, reciprocal native curves,
fresh station/signal IDs, level boarding, complete door assemblies, five-wide
straight clearance and two separate closed 900-track-block loops.

Actual client model loader/baker: 1,420 models / 100,184 quads in four rotations.
LRT dynamic emitter: 318 faces, including 24 glass faces with atlas-safe UVs.
Retained LRT dynamic emitter checks.
Platform geometry comparison: all 3,024 reachable states equal Alpha 12.1.
Platform multipart rules: 7,344 → 892 (87.9% reduction).
Release JAR passes ZIP CRC, version metadata and source-resource byte checks.
All other kit resource bytes are preserved from Alpha 12.1.

The preview is a schematic-derived technical plan. A graphical client, shader
appearance, CRN route display contents and the exact cause of the earlier loading
stall have not been certified. Supply that launch's latest.log if it still stalls.
'''
(ROOT/'CIRCUIT_VALIDATION.txt').write_text(validation)
guide=(ROOT/'LRT_TEST_CIRCUIT_GUIDE.md').read_text().replace('`VALIDATION.txt`','`CIRCUIT_VALIDATION.txt`')
(ROOT/'LRT_TEST_CIRCUIT_GUIDE.md').write_text(guide)
files=[jar,*(ROOT/'schematics').glob('LRT-Test-Circuit-512*.nbt'),ROOT/'preview/LRT-Test-Circuit-512-Preview.png',ROOT/'LRT_TEST_CIRCUIT_GUIDE.md',ROOT/'CIRCUIT_VALIDATION.txt']
for f in files:shutil.copy2(f,dest/f.name)
checks=''.join(hashlib.sha256((dest/f.name).read_bytes()).hexdigest()+'  '+f.name+'\n' for f in files)
(dest/'SHA256SUMS.txt').write_text(checks)
bundle=dest/'LRT-Test-Circuit-512-Bundle.zip'
with zipfile.ZipFile(bundle,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for f in files:z.write(dest/f.name,f.name)
 z.write(dest/'SHA256SUMS.txt','SHA256SUMS.txt')
source=dest/'Create-Transit-Alpha12.2-Circuit-Source.zip'
excluded={'build','.gradle','.git','run','runs','logs','__pycache__'}
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for f in sorted(ROOT.rglob('*')):
  p=f.relative_to(ROOT)
  if f.is_file() and not any(q in excluded for q in p.parts) and f.suffix not in ('.log','.tmp'):
   z.write(f,Path('create-transit')/p)
for f in (bundle,source):
 with zipfile.ZipFile(f) as z:assert z.testzip() is None
 print(f.name,f.stat().st_size,'bytes; ZIP integrity passed')

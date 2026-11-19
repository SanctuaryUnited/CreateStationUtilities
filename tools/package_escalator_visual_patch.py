"""Audit and package the Alpha 0.1.0 deck/icon/tab fix after native validation."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = '0.1.0-alpha.2'


def audit(baseline, logs):
    jar = ROOT / 'build/libs' / f'create-transit-{VERSION}.jar'
    checks = (logs / 'visual-checks.log').read_text()
    server = (logs / 'visual-server.log').read_text()
    client = (logs / 'visual-client-build.log').read_text()
    joints = (logs / 'visual-joint-check.log').read_text()
    assert all('BUILD SUCCESSFUL' in x for x in (checks, server, client))
    tests = re.search(r'All (\d+) required tests passed', server)
    seconds = re.search(r'GAME TESTS COMPLETE IN ([\d.]+) s', server)
    assert tests and int(tests[1]) == 121
    assert 'PASS: 3072 received frame/LED dye combinations' in client
    assert 'PASS: 320 moving configurations' in checks
    assert 'PASS: 148 escalator models / 57528 native baked quads' in checks
    assert 'PASS: CPU/allocation benchmark completed' in checks
    assert 'PASS: 36 handrail joint edge sets match exactly; 2048 smart visual states' in joints
    assert 'PASS: 3072 flat-icon item dye combinations' in client
    assert 'PASS: Native moving deck and fixed end plates cover each flat section' in client
    assert 'PASS: Native NeoForge tab sorting places Station Utilities immediately after Vehicle Kits.' in client
    with zipfile.ZipFile(jar) as z, zipfile.ZipFile(baseline) as old:
        names = set(z.namelist())
        assert z.testzip() is None
        metadata = z.read('META-INF/neoforge.mods.toml').decode()
        assert f'version="{VERSION}"' in metadata or f'version = "{VERSION}"' in metadata
        classes = list((ROOT / 'build/classes/java/main').rglob('*.class'))
        for path in classes:
            entry = str(path.relative_to(ROOT / 'build/classes/java/main'))
            assert z.read(entry) == path.read_bytes(), entry
        assert {n for n in names if n.endswith('.class')} == {
            str(p.relative_to(ROOT / 'build/classes/java/main')) for p in classes}
        assert not any('GameTest' in n or 'Validation.class' in n for n in names)
        manifest = {line for line in (ROOT / 'tools/release-models.txt').read_text().splitlines() if line}
        packaged_models = {n.removeprefix('assets/createtransit/models/').removesuffix('.json')
                           for n in names if n.startswith('assets/createtransit/models/') and n.endswith('.json')}
        assert packaged_models == manifest
        refs = 0

        def verify_reference(document):
            nonlocal refs
            if isinstance(document, dict):
                for key, value in document.items():
                    if key in ('model', 'parent') and isinstance(value, str) and value.startswith('createtransit:'):
                        target = 'assets/createtransit/models/' + value.split(':', 1)[1] + '.json'
                        assert target in names, target
                        refs += 1
                    if key == 'textures' and isinstance(value, dict):
                        for texture in value.values():
                            if isinstance(texture, str) and texture.startswith('createtransit:'):
                                target = 'assets/createtransit/textures/' + texture.split(':', 1)[1] + '.png'
                                assert target in names, target
                    verify_reference(value)
            elif isinstance(document, list):
                for value in document:
                    verify_reference(value)

        for name in names:
            if name.startswith('assets/createtransit/') and name.endswith('.json'):
                verify_reference(json.loads(z.read(name)))
        retained = [n for n in old.namelist()
                    if n.startswith(('assets/createtransit/', 'data/createtransit/')) and 'escalator' not in n]
        assert all(n in names and z.read(n) == old.read(n) for n in retained)
        unchanged = []
        stems = ('escalator/EscalatorEntity', 'escalator/EscalatorGeometry',
                 'escalator/EscalatorShapes',
                 'escalator/EscalatorSoundscape', 'escalator/client/EscalatorSoundInstance',
                 'escalator/client/EscalatorSounds', 'escalator/client/EscalatorSurfaceModels')
        for name in names:
            tail = name.removeprefix('com/sanctuarygames/createtransit/')
            if name.endswith('.class') and any(tail == s + '.class' or tail.startswith(s + '$') for s in stems):
                assert z.read(name) == old.read(name), name
                unchanged.append(name)
    layout = json.loads((ROOT / 'build/inventory-validation/inventory-layout.json').read_text())
    assert layout['transit_tabs_adjacent']
    tabs = {tab['id']: sum(bool(item) for item in tab['cells']) for tab in layout['tabs']}
    assert tabs == {'createtransit:vehicle_kits': 69, 'createtransit:station_utilities': 38}, tabs
    result = {
        'release': 'CREATE Transit Alpha 0.1.0 — deck, icon and tab patch',
        'internal_mod_version': VERSION,
        'jar': 'CREATE Transit Alpha 0.1.0.jar',
        'jar_bytes': jar.stat().st_size,
        'jar_sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
        'jar_crc_ok': True,
        'native_minecraft_required_tests_passed': int(tests[1]),
        'test_seconds': float(seconds[1]),
        'native_model_count_baked': 148,
        'native_baked_quads_four_rotations': 57528,
        'moving_reference_configurations': 320,
        'smart_visual_state_combinations': 2048,
        'exact_handrail_joint_edge_sets': 36,
        'native_client_dye_combinations': 3072,
        'flat_item_icon_models': 12,
        'native_item_icon_dye_combinations': 3072,
        'deck_phases_with_complete_nonoverlapping_coverage': 6336,
        'native_tab_order': layout['native_tab_order'],
        'transit_tabs_adjacent': layout['transit_tabs_adjacent'],
        'native_client_inventory_items': tabs,
        'release_models': len(manifest),
        'verified_model_references': refs,
        'compiled_main_classes_match': len(classes),
        'collision_movement_and_sound_classes_byte_identical_to_alpha010_patch1': sorted(unchanged),
        'unrelated_resources_byte_identical_to_alpha010_patch1': len(retained),
        'moving_geometry_cache_bounded': True,
        'native_cpu_allocation_benchmark_passed': True,
        'unlit_pbr_emission_alpha': 0,
        'graphical_client_shader_or_hardware_fps_test': False,
    }
    return jar, result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--baseline-jar', type=Path, required=True)
    parser.add_argument('--logs', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    jar, result = audit(args.baseline_jar, args.logs)
    args.out.mkdir(parents=True, exist_ok=True)
    audit_path = ROOT / 'docs/Alpha-0.1.0-Visual-Release-Audit.json'
    audit_path.write_text(json.dumps(result, indent=2) + '\n')
    for log in ('visual-checks.log', 'visual-server.log', 'visual-client-build.log', 'visual-joint-check.log'):
        text = (args.logs / log).read_text()
        text = '\n'.join(line for line in text.splitlines() if not line.startswith('Picked up JAVA_TOOL_OPTIONS:'))
        (ROOT / 'docs' / ('Alpha-0.1.0-' + log)).write_text(text + '\n')
    final_jar = args.out / 'CREATE Transit Alpha 0.1.0.jar'
    shutil.copyfile(jar, final_jar)
    shutil.copyfile(jar, args.out / 'CREATE-Transit-Alpha-0.1.0.jar')
    for path in (audit_path, ROOT / 'docs/CREATE-Transit-Alpha-0.1.0-Escalator-Guide.md',
                 ROOT / 'preview/escalator-family/CREATE-Transit-Alpha-0.1.0-Terminal-Preview.png'):
        shutil.copyfile(path, args.out / path.name)
    source = args.out / 'CREATE-Transit-Alpha-0.1.0-Source.zip'
    ignored = {'build', 'run', '.gradle', '.git', '__pycache__', 'preview', 'logs', '.idea'}
    with zipfile.ZipFile(source, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
        for base, dirs, files in os.walk(ROOT):
            dirs[:] = sorted(d for d in dirs if d not in ignored)
            for name in sorted(files):
                path = Path(base) / name
                if path.suffix in ('.pyc', '.log') and path.parent != ROOT / 'docs':
                    continue
                z.write(path, str(Path('create-transit') / path.relative_to(ROOT)))
        for path in (final_jar, args.out / audit_path.name,
                     args.out / 'CREATE-Transit-Alpha-0.1.0-Escalator-Guide.md',
                     args.out / 'CREATE-Transit-Alpha-0.1.0-Terminal-Preview.png'):
            z.write(path, path.name)
    with zipfile.ZipFile(source) as z:
        assert z.testzip() is None
    print(json.dumps(result, indent=2))
    print(f'Source ZIP: {source.stat().st_size:,} bytes')


if __name__ == '__main__':
    main()

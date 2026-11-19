"""Validate packaged chime provenance, resources and full mono Vorbis decoding.
Requires ffprobe and ffmpeg. Optional argument: previous release JAR.
"""
from pathlib import Path
from zipfile import ZipFile
import hashlib, json, subprocess, sys

ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
ASSETS=RES/'assets/createtransit'
credits=json.loads((RES/'META-INF/createtransit-elevator-chimes-cc0.json').read_text())
sounds=json.loads((ASSETS/'sounds.json').read_text())
language=json.loads((ASSETS/'lang/en_us.json').read_text())
assert len(credits)==6
assert len({r['packaged_sound'] for r in credits})==6
assert len({r['source_url'] for r in credits})==6
for r in credits:
    assert r['license']=='CC0-1.0'
    path=RES/r['packaged_file']
    assert hashlib.sha256(path.read_bytes()).hexdigest()==r['mono_sha256'], path
    event=r['packaged_sound'].split(':',1)[1]
    definition=sounds[event]
    assert definition['sounds']==[{'name':r['packaged_sound'],'attenuation_distance':12}]
    assert definition['subtitle'] in language
    data=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_entries','format=duration:stream=codec_name,sample_rate,channels','-of','json',str(path)]))
    stream=data['streams'][0]
    assert stream['codec_name']=='vorbis' and stream['sample_rate']=='48000' and stream['channels']==1
    assert .1<float(data['format']['duration'])<10
    subprocess.run(['ffmpeg','-v','error','-i',str(path),'-f','null','-'],check=True)
for name in ['otis','schindler_ht','arrival_ding','elevator_bell','japan_arrival','classic_bell_doors','fm_bell','synth_ding','none']:
    assert 'gui.createtransit.elevator.chime.'+name in language
if len(sys.argv)>1:
    with ZipFile(sys.argv[1]) as old:
        for name in ['elevator_solid_open','elevator_glass_open']:
            path='assets/createtransit/sounds/'+name+'.ogg'
            assert old.read(path)==(RES/path).read_bytes(), 'Original cue changed: '+name
print('PASS: six distinct CC0 source records match shipped audio hashes; all six mono 48 kHz Vorbis files decode; nine selector labels; original Otis/Schindler bytes preserved when a baseline JAR is supplied.')

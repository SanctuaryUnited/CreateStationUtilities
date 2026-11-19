"""Inspect the encoded wide-door cues, including pitches and the air-noise tail."""
from pathlib import Path
import json,re,subprocess
import numpy as np
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
sounds=json.loads((A/'sounds.json').read_text());lang=json.loads((A/'lang/en_us.json').read_text())
timing=(ROOT/'src/main/java/com/sanctuarygames/createtransit/block/WideDoorTiming.java').read_text()
rate=44100
def peak(data,start,end):
    clip=data[round(start*rate):round(end*rate)];freq=np.fft.rfftfreq(len(clip),1/rate)
    spectrum=abs(np.fft.rfft(clip*np.hanning(len(clip))));return freq[np.argmax(spectrum)]
for name,ticks,first_pitch in [('gondola_door_open','OPEN_TICKS',523.25),('gondola_door_close','CLOSE_TICKS',659.25)]:
    event=sounds[name];assert event['subtitle']in lang and event['sounds']==[{'name':'createtransit:'+name}]
    p=A/'sounds'/(name+'.ogg');assert p.stat().st_size>5000
    info=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(p)],text=True))
    stream=info['streams'][0];assert stream['channels']==1 and int(stream['sample_rate'])==rate
    travel=int(re.search(ticks+r'=(\d+)',timing)[1])/20
    assert abs(float(info['format']['duration'])-(travel+.05))<.01
    data=np.frombuffer(subprocess.check_output(['ffmpeg','-v','error','-i',str(p),'-f','f32le','-ac','1','-ar',str(rate),'-']),dtype='<f4')
    assert .2<max(abs(data))<.9,'Cue must be audible without clipping'
    assert abs(peak(data,.03,.19)-first_pitch)<10,(name,'first piano pitch')
    # The closing second strike waits until 0.45 seconds; pitch is unchanged.
    assert abs(peak(data,.30,.42)-first_pitch)<10,(name,'closing note spacing')
    assert abs(peak(data,.50,.67)-523.25)<10,(name,'single opening / second closing note')
    hiss=data[round(1.2*rate):round(1.4*rate)];assert .008<np.sqrt(np.mean(hiss**2))<.05,'Air hiss continues after piano tail'
    spectrum=abs(np.fft.rfft(hiss*np.hanning(len(hiss))));assert np.count_nonzero(spectrum>max(spectrum)*.2)>500,'Air tail is broadband'
    assert max(abs(data[round((travel+.02)*rate):]))<.004,'Hiss release finishes with travel'
print('PASS: one C5 opening note, two E5/C5 closing notes; broadband hiss, silent release, mono Ogg, no clipping, matching travel durations and registered subtitles.')

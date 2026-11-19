"""Reproducible piano/air cues for three-wide gondola doors."""
from pathlib import Path
import json,subprocess,tempfile,wave
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/createtransit'
RATE=44100

def piano(file,semitones,duration=.65):
    pcm=subprocess.check_output(['ffmpeg','-v','error','-i',str(ROOT/'tools/audio'/file),'-f','f32le','-ac','1','-ar',str(RATE),'-'])
    data=np.frombuffer(pcm,dtype='<f4');n=int(RATE*duration)
    note=np.interp(np.arange(n)*2**(semitones/12),np.arange(len(data)),data,left=0,right=0)
    note*=.30/max(.001,np.max(np.abs(note)))
    fade=int(RATE*.16);note[-fade:]*=np.linspace(1,0,fade)
    return note

def cue(open):
    travel=1.6 if open else 2.0
    mix=np.zeros(round(RATE*(travel+.05)))
    # Band-limited original noise with a short valve attack and smooth release.
    rng=np.random.default_rng(610 if open else 611)
    noise=rng.normal(0,1,len(mix));freq=np.fft.rfftfreq(len(noise),1/RATE)
    band=(1-np.exp(-(freq/650)**4))*np.exp(-(freq/6200)**4)
    hiss=np.fft.irfft(np.fft.rfft(noise)*band,n=len(noise))
    hiss*=.10/max(.001,np.max(np.abs(hiss)))
    t=np.arange(len(mix))/RATE
    envelope=np.minimum(1,t/.06)*np.minimum(1,np.maximum(0,(travel-t)/.18))
    mix+=hiss*envelope
    strikes=[('piano_cs5.wav',-1,0)]if open else[('piano_f5.wav',-1,0),('piano_cs5.wav',-1,.45)]
    for file,semitones,start in strikes:
        note=piano(file,semitones);offset=round(start*RATE);mix[offset:offset+len(note)]+=note
    return np.clip(mix,-1,1)

def generate():
    sounds=json.loads((A/'sounds.json').read_text());lang=json.loads((A/'lang/en_us.json').read_text())
    with tempfile.TemporaryDirectory(prefix='transit-gondola-audio-')as tmp:
        for open,name,label in [(True,'gondola_door_open','Gondola door opening chime and air hiss'),(False,'gondola_door_close','Gondola door closing chime and air hiss')]:
            data=cue(open);wav=Path(tmp)/(name+'.wav')
            with wave.open(str(wav),'wb')as w:
                w.setnchannels(1);w.setsampwidth(2);w.setframerate(RATE);w.writeframes((data*32767).astype('<i2').tobytes())
            subprocess.run(['ffmpeg','-y','-v','error','-i',str(wav),'-fflags','+bitexact','-c:a','libvorbis','-q:a','5',str(A/'sounds'/(name+'.ogg'))],check=True)
            sounds[name]={'subtitle':'subtitles.createtransit.'+name,'sounds':[{'name':'createtransit:'+name}]}
            lang['subtitles.createtransit.'+name]=label
    for path,data in [(A/'sounds.json',sounds),(A/'lang/en_us.json',lang)]:path.write_text(json.dumps(data,indent=2)+'\n')
    (A/'sounds/NOTICE.txt').write_text((ROOT/'THIRD_PARTY_AUDIO.md').read_text())
    print('Generated wide-door C5 opening and E5/C5 closing piano cues with original air hiss; mono 1.65s / 2.05s.')

if __name__=='__main__':generate()

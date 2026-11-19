"""Original two-strike bell and descending HT-inspired cues (no recordings)."""
from pathlib import Path
import math,wave,struct,json,subprocess
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit';RATE=44100
OUT=A/'sounds';OUT.mkdir(exist_ok=True)
def make(name,seconds,components):
 values=[]
 for i in range(round(seconds*RATE)):
  t=i/RATE;v=0
  for start,freq,amp,decay,ratio in components:
   u=t-start
   if u>=0:v+=amp*(1-math.exp(-u*160))*math.exp(-u/decay)*math.sin(2*math.pi*freq*u+ratio*math.sin(2*math.pi*freq*2.02*u)*math.exp(-u/.18))
  # End smoothly so the longer bell tail has no cut-off click.
  v*=min(1.,max(0.,(seconds-t)/.12))
  values.append(v)
 peak=max(abs(v)for v in values);scale=.68/max(peak,.68)
 path=OUT/(name+'.wav')
 with wave.open(str(path),'wb')as w:w.setparams((1,2,RATE,0,'NONE','not compressed'));w.writeframes(b''.join(struct.pack('<h',round(max(-1,min(1,v*scale))*32767))for v in values))
 subprocess.run(['ffmpeg','-v','error','-y','-i',str(path),'-c:a','libvorbis','-q:a','5',str(OUT/(name+'.ogg'))],check=True);path.unlink()
# Same bell twice, with a delayed second strike and a longer second decay.
make('elevator_solid_open',2.65,[(0,880,.45,.30,.12),(0,1765,.17,.16,.06),(0,2390,.07,.10,.02),(.65,880,.45,.62,.12),(.65,1765,.17,.30,.06),(.65,2390,.07,.20,.02)])
# A distinct, deeper high-to-low pair with tighter spacing than the solid bell.
make('elevator_glass_open',1.65,[(0,659.25,.34,.20,.12),(0,1318.5,.08,.12,.04),(.32,523.25,.34,.34,.12),(.32,1046.5,.08,.18,.04)])
p=A/'sounds.json';d=json.loads(p.read_text())
for name in ['elevator_solid_open','elevator_glass_open']:d[name]={'subtitle':'subtitles.createtransit.'+name,'sounds':[{'name':'createtransit:'+name}]}
p.write_text(json.dumps(d,indent=2)+'\n')
p=A/'lang/en_us.json';d=json.loads(p.read_text());d.update({'subtitles.createtransit.elevator_solid_open':'Solid elevator opening chime','subtitles.createtransit.elevator_glass_open':'Glass elevator opening chime'});p.write_text(json.dumps(d,indent=2)+'\n')
print('Generated spaced double-strike solid bell and descending glass chime.')
if __name__=='__main__':pass

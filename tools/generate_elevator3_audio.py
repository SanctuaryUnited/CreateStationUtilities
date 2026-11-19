"""Original CC0 procedural model elevator sounds; requires numpy and ffmpeg."""
from pathlib import Path
import numpy as np,json,wave,subprocess,tempfile
root=Path(__file__).resolve().parents[1]/'src/main/resources/assets/createtransit';sr=22050;rng=np.random.default_rng(301)
def write(name,data):
 with tempfile.TemporaryDirectory() as tmp:
  p=Path(tmp)/'sound.wav'
  with wave.open(str(p),'wb') as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(sr);w.writeframes((np.clip(data,-1,1)*32767).astype('<i2').tobytes())
  subprocess.run(['ffmpeg','-loglevel','error','-y','-i',str(p),'-c:a','libvorbis','-q:a','4',str(root/'sounds'/f'{name}.ogg')],check=True)
t=np.arange(int(sr*.85))/sr;noise=np.convolve(rng.normal(0,1,len(t)),np.ones(9)/9,'same');env=np.sin(np.pi*t/t[-1])**1.5
write('elevator_model_door',(noise*.11+np.sin(t*2*np.pi*145)*.018)*env)
t=np.arange(sr*2)/sr;env=1+.025*np.sin(t*2*np.pi*.5)
write('elevator_model_motor',(np.sin(t*2*np.pi*90)+.28*np.sin(t*2*np.pi*180)+.08*np.sin(t*2*np.pi*360))*.10*env)
p=root/'sounds.json';s=json.loads(p.read_text())
for suffix in ['door','motor']:s['elevator_model_'+suffix]={'subtitle':'subtitles.createtransit.elevator_model_'+suffix,'sounds':['createtransit:elevator_model_'+suffix]}
p.write_text(json.dumps(s,indent=2)+'\n')
p=root/'lang/en_us.json';s=json.loads(p.read_text());s['subtitles.createtransit.elevator_model_door']='Elevator doors slide';s['subtitles.createtransit.elevator_model_motor']='Elevator motor hums';p.write_text(json.dumps(s,indent=2,ensure_ascii=False)+'\n')

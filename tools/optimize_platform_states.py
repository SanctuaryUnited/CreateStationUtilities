"""Drop duplicate multipart rules while preserving every valid platform assembly mesh."""
from pathlib import Path
import json
from panel_geometry import size
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
def rules(name,height,door):
 old=json.loads((A/'blockstates'/f'{name}.json').read_text())['multipart'];new=[]
 for part in old:
  when=part['when'];model=part['apply']['model'];row=int(when['row'])
  if row>=(height+15)//16:continue
  # Empty glass layers must not enter model matching or baking.
  elements=json.loads((A/'models'/Path(model.split(':')[1]+'.json')).read_text())['elements']
  if not elements:continue
  if 'corner' in when:
   new.append(part);continue
  if int(when['column'])>= (2 if door else 1):continue
  if door:
   if when['left']!='false' or when['right']!='false' or when['panel']!='0':continue
   part={'when':{k:v for k,v in when.items() if k not in ['left','right','panel']},'apply':part['apply']}
  new.append(part)
 (A/'blockstates'/f'{name}.json').write_text(json.dumps({'multipart':new},indent=2)+'\n')
 print(name,len(old),'->',len(new))
if __name__=='__main__':
 for name,height,door in [('platform_screen_door',48,True),('platform_gate',24,True),('compact_screen_door',32,True),('platform_wall',48,False),('platform_barrier',24,False),('compact_wall',32,False)]:rules(name,height,door)

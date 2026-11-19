"""Native Minecraft block-model snapshots of production two-block landing meshes.
Runtime doors use DoorGeometry Java for continuous interpolation, not a sprite animation.
"""
from pathlib import Path
import json, numpy as np
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/createtransit';O=A/'models/block/elevator3';O.mkdir(parents=True,exist_ok=True)
materials={'FRAME':'frame','DOOR_FRAME':'frame','WALL':'panel','CEILING':'ceiling','COPY_PRIMARY':'panel','HANDRAIL':'trim','METAL':'edge','DOOR_TRIM':'edge','GLASS':'glass','DOOR_GLASS':'glass','DARK':'dark','DOOR_GASKET':'dark','DOOR_DISPLAY':'dark','LIGHT':'led','ACCENT':'led','DOOR_LED':'led','BODY_ACCENT':'white','FOAM':'white','FLOOR':'floor'}
keys=[]
for skin in ['BRUSHED_STEEL','MODERN_DARK','WOOD_TRIM','LIGHT_TRANSIT']:
 for model in ['GLASS','SOLID','SLIM_WINDOW']:
  d=json.loads((R/'build/v14-assets/meshes'/f'2_SOLID_{skin}_{model}.json').read_text())
  for state,field in [('closed','landingClosed'),('opening','landingOpening'),('open','landingOpen')]:
   elements=[];tex={}
   for f in d[field]:
    material=d['materials'][f[0]];v=np.array(f[1:]).reshape(4,3)*16;n=np.cross(v[1]-v[0],v[2]-v[0])
    if np.linalg.norm(n)<1e-8:continue
    axis=int(np.argmax(abs(n)));direction=['east','up','south'][axis] if n[axis]>0 else ['west','down','north'][axis]
    key='m'+str(f[0]);tile=materials.get(material,'dark');tex[key]='minecraft:block/spruce_planks' if material=='DOOR_WOOD' else 'createtransit:block/elevator3/high/'+skin.lower()+'/'+tile
    elements.append({'from':v.min(0).round(6).tolist(),'to':v.max(0).round(6).tolist(),'shade':material not in ['DOOR_LED','LIGHT','ACCENT'],'faces':{direction:{'texture':'#'+key,'uv':[0,0,16,16]}}})
   key=skin.lower()+'_'+model.lower()+'_'+state;out={'credit':'Exported from Create Station Utilities production DoorGeometry; static reference pose. Runtime uses continuous geometry.','textures':tex,'elements':elements,'render_type':'minecraft:translucent' if model!='SOLID' else 'minecraft:cutout'}
   (O/(key+'.json')).write_text(json.dumps(out,separators=(',',':'))+'\n');keys.append('block/elevator3/'+key)
p=R/'tools/release-models.txt';old=p.read_text().splitlines();p.write_text('\n'.join(sorted(set(old+keys)))+'\n')
P=A/'elevator3';P.mkdir(exist_ok=True);(P/'animations.json').write_text(json.dumps({'runtime':'com.sanctuarygames.createtransit.elevator.model.DoorGeometry','duration_ticks':40,'interpolation':'p*p*(3-2*p)','default':'standard_center_slide','optional':['left_slide','right_slide','two_speed'],'snapshots':[0,.5,1],'note':'Metadata documents Java animation. No unsupported animation library required.'},indent=2)+'\n')
print('Exported',len(keys),'native Minecraft static door models and runtime animation metadata.')

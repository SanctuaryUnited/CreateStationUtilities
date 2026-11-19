"""Exact generated-mesh preview; no in-game or shader rendering implied."""
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.colors import to_rgba
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
from panel_geometry import stationary,leaf,corner_return,size,JAMB
ROOT=Path(__file__).resolve().parents[1]
colors={'frame':'#ccd4dd','accent':'#298ebc','glass':'#789eaf','led_accent':'#49cfff','led_white':'#eaffff'}
def draw(ax,height,panel,corner=False):
 mesh=[]
 def add(e,x=0,y=0):
  mesh.append(({**e,'from':[e['from'][0]+x,e['from'][1]+y,e['from'][2]],'to':[e['to'][0]+x,e['to'][1]+y,e['to'][2]]}))
 if corner:
  for row in range((height+15)//16):
   for e in stationary(height,False,0,row,leds=True,panel=panel)+corner_return(height,row,2,panel):add(e,0,row*16)
 else:
  for col in [-1,0,1,2]:
   for row in range((height+15)//16):
    for e in stationary(height,col in [0,1],col if col in [0,1] else 0,row,left=col==2,right=col==-1,leds=True,panel=panel):add(e,col*16,row*16)
  for side in [0,1]:
   for e in leaf(height,panel):add(e,JAMB if side==0 else 16)
 polys=[];cs=[]
 for e in mesh:
  x,z,y=e['from'];X,Z,Y=e['to'];t=e['texture']
  fs={'north':[(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)],'south':[(X,Y,z),(x,Y,z),(x,Y,Z),(X,Y,Z)],'west':[(x,Y,z),(x,y,z),(x,y,Z),(x,Y,Z)],'east':[(X,y,z),(X,Y,z),(X,Y,Z),(X,y,Z)],'up':[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],'down':[(x,Y,z),(X,Y,z),(X,y,z),(x,y,z)]}
  for f in e['visible']:
   polys.append(fs[f]);rgba=list(to_rgba(colors[t],.28 if t=='glass' else 1));shade=1 if t.startswith('led') else {'north':.92,'south':.85,'east':.72,'west':.8,'up':1,'down':.65}[f];rgba[:3]=[c*shade for c in rgba[:3]];cs.append(rgba)
 ax.add_collection3d(Poly3DCollection(polys,facecolors=cs,edgecolors='none',zsort='average'))
 ax.set_facecolor('#172330');ax.set_proj_type('ortho');ax.view_init(elev=16 if corner else 6,azim=-55 if corner else -83)
 ax.set_xlim(-4 if corner else -18,22 if corner else 50);ax.set_ylim(-2,20 if corner else 7);ax.set_zlim(0,50);ax.set_box_aspect((26 if corner else 68,22 if corner else 9,50));ax.set_axis_off()
fig=plt.figure(figsize=(15,13),facecolor='#172330')
fig.suptitle('CREATE TRANSIT  /  PLATFORM CUSTOMIZATION',x=.06,y=.975,ha='left',color='white',fontsize=22,weight='bold')
for row,height in enumerate([24,32,48]):
 for col in range(4):
  ax=fig.add_subplot(3,4,row*4+col+1,projection='3d');draw(ax,height,col if col<3 else 1,col==3)
  if row==0:ax.set_title(['CLEAR GLASS','DIVIDED GLASS','SOLID PANEL','AUTO CORNER'][col],color='#64d5ff',fontsize=12,pad=0)
 fig.text(.025,.77-row*.285,['HALF HEIGHT','2 BLOCKS','3 BLOCKS'][row],color='#b8c9d7',rotation=90,fontsize=11,va='center')
fig.text(.07,.045,'Crouch + empty-hand click: clear → divided → solid     •     Dyeable frames and accent LEDs',color='white',fontsize=12)
fig.text(.07,.022,'Actual generated geometry • Offline preview, not an in-game screenshot • Corners shown as individual return modules',color='#9eafbc',fontsize=10)
fig.subplots_adjust(left=.055,right=.98,top=.9,bottom=.065,wspace=.01,hspace=.02)
fig.savefig(ROOT/'preview/platform-customization-alpha12.png',dpi=140);plt.close(fig)

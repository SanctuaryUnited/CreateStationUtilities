"""Offline geometry preview. This is not an in-game or shader screenshot."""
from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.colors import to_rgba
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/createtransit'
colors={'frame':('#555d65',1),'accent':('#326db5',1),'glass':('#abd1e1',.18),'led_accent':('#318cff',1),'led_green':('#42ee77',1),'led_amber':('#ffb723',1),'led_white':('#ebf8ff',1)}
from panel_geometry import stationary,leaf,lights,size,transition_trim,JAMB,SLIDE
def cubes(full,slide,mixed=False):
 out=[]
 def add(e,x=0,y=0):
  a=e['from'];b=e['to'];t=e['texture'];t='led_green' if t=='led_white' and slide==1 else t
  out.append(([a[0]+x,a[2],a[1]+y],[b[0]+x,b[2],b[1]+y],t,e['visible']))
 # Independent walls flank the two-column door; LEDs stay on its jambs.
 for col in ([-1,0,1,2,3]if mixed else[-1,0,1,2]):
  door=col in [0,1];style=True if mixed and col in [-1,3]else full
  for row in range((size(style)[0]+15)//16):
   left=(col==2 or mixed and col==3 and row<2);right=(col==-1 and(not mixed or row<2))or(mixed and col==2)
   for e in stationary(style,door,col if door else 0,row,left=left,right=right,leds=not(mixed and col in [-1,3])):add(e,col*16,row*16)
  if mixed and col in [-1,3]:
   for e in lights(True,False):
    if(col==-1 and e['from'][0]>14)or(col==3 and e['from'][0]<2):e={**e,'from':[e['from'][0],28,e['from'][2]]}
    add(e,col*16)
   for e in transition_trim('right'if col==-1 else'left'):add(e,col*16)
 for side in range(2):
  for e in leaf(full):add(e,(JAMB if side==0 else 16)+(-SLIDE*slide if side==0 else SLIDE*slide))
 return out

def draw(ax,full,slide,mixed=False):
 ax.set_facecolor('#eff3f6');ax.set_proj_type('ortho');ax.view_init(elev=0,azim=-90)
 polygons=[];facecolors=[];edgecolors=[]
 for a,b,t,visible in cubes(full,slide,mixed):
  x,y,z=a;X,Y,Z=b
  faces=[[(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)],[(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)],[(x,y,z),(x,Y,z),(x,Y,Z),(x,y,Z)],[(X,y,z),(X,Y,z),(X,Y,Z),(X,y,Z)],[(x,y,Z),(X,y,Z),(X,Y,Z),(x,Y,Z)],[(x,y,z),(X,y,z),(X,Y,z),(x,Y,z)]]
  faces=[f for name,f in zip(['north','south','west','east','up','down'],faces)if name in visible]
  col,alpha=colors[t]
  polygons.extend(faces);facecolors.extend([to_rgba(col,alpha)]*len(faces));edgecolors.extend([(0,0,0,0)if t=='glass' or t.startswith('led_')else(.1,.15,.2,.12)]*len(faces))
 ax.add_collection3d(Poly3DCollection(polygons,facecolors=facecolors,edgecolors=edgecolors,linewidths=.15,zsort='average'))

 ax.set_xlim(-18,66 if mixed else 50);ax.set_ylim(-2,8);ax.set_zlim(0,52);ax.set_box_aspect((84 if mixed else 68,10,52));ax.set_axis_off()
 if full==32:
  # Exact cell-space guide, not a rendered or integrated CRN display.
  ax.plot([0,32,32,0,0],[-.2]*5,[32,32,48,48,32],color='#397d9b',linestyle='--',linewidth=1)
  ax.text(16,-.3,40,'CRN panel space',ha='center',va='center',color='#397d9b',fontsize=9)
 label='Mixed 2 / 3-block heights'if mixed else'Two-block-high full glass'if full==32 else'Three-block-high full glass'if full else'Half-height gate'
 ax.set_title(label+' · '+('open'if slide==1 else'closed'),fontsize=14,color='#182636',pad=-8)
fig=plt.figure(figsize=(14,14),facecolor='#eff3f6');fig.suptitle('CREATE TRANSIT · PLATFORM DOOR ASSETS',fontsize=22,fontweight='bold',color='#182636')
for i,(full,slide)in enumerate([(True,0),(True,1),(32,0),(32,1),(False,0),(False,1)],1):draw(fig.add_subplot(4,2,i,projection='3d'),full,slide)
draw(fig.add_subplot(4,2,(7,8),projection='3d'),32,0,mixed=True)
fig.text(.5,.025,'All doors 2 blocks wide • Clear display row above compact doors • Mixed-height glass trim • Offline geometry preview',ha='center',fontsize=11,color='#536273')
fig.subplots_adjust(top=.88,bottom=.04,wspace=.02,hspace=.05);fig.savefig(ROOT/'preview/platform-models.png',dpi=140);plt.close(fig)
frames=[]
for i in range(16):
 t=i/15;t=t*t*(3-2*t)
 fig=plt.figure(figsize=(14,5),facecolor='#eff3f6')
 draw(fig.add_subplot(1,3,1,projection='3d'),True,t);draw(fig.add_subplot(1,3,2,projection='3d'),32,t);draw(fig.add_subplot(1,3,3,projection='3d'),False,t)
 fig.suptitle('Sliding door motion · generated geometry preview',fontsize=16)
 fig.canvas.draw();frames.append(Image.frombuffer('RGBA',fig.canvas.get_width_height(),fig.canvas.buffer_rgba(),'raw','RGBA',0,1).convert('RGB'));plt.close(fig)
seq=[frames[0]]*5+frames+[frames[-1]]*12+list(reversed(frames))+[frames[0]]*5
seq[0].save(ROOT/'preview/platform-motion.gif',save_all=True,append_images=seq[1:],duration=65,loop=0,optimize=True)
print('Rendered model geometry preview and motion GIF.')

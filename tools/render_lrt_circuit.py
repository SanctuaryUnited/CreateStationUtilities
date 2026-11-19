"""Technical plan of the exported schematic; not a game screenshot."""
from pathlib import Path
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle,Polygon
from export_lrt_circuit import curve_points
from nbt_read import read
R=Path(__file__).resolve().parents[1];meta=json.loads((R/'schematics/LRT-Test-Circuit-512.json').read_text());nbt=read(R/'schematics/LRT-Test-Circuit-512.nbt');cells={tuple(b['pos']):nbt['palette'][b['state']]['Name'] for b in nbt['blocks']}
BG='#102030';PANEL='#172c3b';WHITE='#e7f1f7';GRAY='#aac1d0';CYAN='#53d9ff';GOLD='#ffc66d'
fig=plt.figure(figsize=(17,12),facecolor=BG);gs=fig.add_gridspec(3,2,width_ratios=[1.65,1],height_ratios=[1,1,.82],left=.045,right=.96,top=.9,bottom=.085,wspace=.10,hspace=.29)
fig.text(.045,.965,'SANCTUARY TRANSIT  /  LRT TEST CIRCUIT',color=WHITE,fontsize=24,weight='bold');fig.text(.045,.932,'512 × 512 blocks  •  Two station layouts  •  Standard Create gauge  •  Decorative overhead power',color=GRAY,fontsize=12)
ax=fig.add_subplot(gs[:,0]);ax.set_facecolor(PANEL)
for v in range(0,513,64):ax.axhline(v,color='#29404f',lw=.5);ax.axvline(v,color='#29404f',lw=.5)
ax.add_patch(Rectangle((0,0),512,512,fill=False,edgecolor=CYAN,lw=1.5))
# Ground platforms and walkways directly from the exported cells.
for p,name in cells.items():
 if p[1]==0 and name in ['minecraft:stone_bricks','minecraft:light_gray_concrete']:
  ax.add_patch(Rectangle((p[0],p[2]),1,1,color='#365566',lw=0))
for loop,cs in meta['track_centers'].items():
 color=CYAN if loop=='outer' else GOLD
 for side in ['west','east']:ax.plot([cs[side]+.5]*2,[144,369],color=color,lw=2)
 for side in ['north','south']:ax.plot([144,369],[cs[side]+.5]*2,color=color,lw=2)
 for c in meta['curves']:
  if c['loop']==loop:
   pts=curve_points(c);ax.plot([p[0] for p in pts],[p[2] for p in pts],color=color,lw=2)
for x1,x2,label in [(75,81,'A'),(420,425,'B'),(443,450,'B')]:ax.add_patch(Rectangle((x1,198),x2-x1+1,116,facecolor='#dde5ea',edgecolor='#354654',lw=.5))
for signal in meta['signals']:x,_,z=signal['pos'];ax.scatter([x+.5],[z+.5],s=18,c='#6affac',zorder=10,edgecolor=BG)
for x,z,dx,dz,color in [(72,178,0,-23,CYAN),(300,72,23,0,CYAN),(440,334,0,23,CYAN),(214,440,-23,0,CYAN),(84,334,0,23,GOLD),(300,428,-23,0,GOLD)]:ax.annotate('',(x+dx,z+dz),(x,z),arrowprops={'arrowstyle':'->','color':color,'lw':1.5})
ax.annotate('A  /  CENTER PLATFORM',(78,253),(165,248),color=WHITE,fontsize=11,arrowprops={'arrowstyle':'-','color':WHITE});ax.annotate('B  /  SIDE PLATFORMS',(438,253),(257,285),color=WHITE,fontsize=11,arrowprops={'arrowstyle':'-','color':WHITE})
ax.text(256,192,'Broad return turns at both ends',ha='center',color=GRAY,fontsize=11);ax.text(256,345,'Open center for scenery,\nextra test rigs and expansions',ha='center',color=GRAY,fontsize=12,linespacing=1.5)
ax.text(25,495,'N ↑',color=WHITE,fontsize=13);ax.text(255,484,'512-block site boundary',ha='center',color=GRAY,fontsize=10)
ax.set_xlim(-8,520);ax.set_ylim(520,-8);ax.set_aspect('equal');ax.tick_params(colors=GRAY,labelsize=9);ax.set_xlabel('Blocks east from schematic origin',color=GRAY);ax.set_ylabel('Blocks south from schematic origin',color=GRAY)
def station(ax,xlo,xhi,label):
 ax.set_facecolor(PANEL)
 for p,name in cells.items():
  x,y,z=p
  if not(xlo<=x<=xhi and 190<=z<=320):continue
  if y==2 and name in ['minecraft:smooth_stone','minecraft:yellow_concrete']:
   ax.add_patch(Rectangle((x,z),1,1,color='#d9e4e8' if name=='minecraft:smooth_stone' else GOLD,lw=0))
  if y==3 and name=='createtransit:compact_wall':ax.add_patch(Rectangle((x,z),1,1,color='#70c5da',lw=0))
  if y==3 and name=='createtransit:compact_screen_door':ax.add_patch(Rectangle((x,z),1,1,color='#79ffa9',lw=0))
 for cs in meta['track_centers'].values():
  for side in ['west','east']:
   x=cs[side]
   if xlo<=x<=xhi:ax.plot([x+.5]*2,[190,320],color=CYAN if cs==meta['track_centers']['outer'] else GOLD,lw=1.5)
 ax.add_patch(Rectangle((xlo,195),xhi-xlo,4,facecolor='#8093a0',alpha=.75))
 if xlo<100:
  for start,length in [(215,16),(233,8),(243,16)]:ax.add_patch(Rectangle((70,start),5,length,facecolor='#50a2c2',edgecolor=WHITE,lw=.6))
 for station in meta['stations']:
  x,_,z=station['pos']
  if xlo<=x<=xhi:ax.scatter([x+.5],[z+.5],c='#ffffff',s=23,zorder=10)
 ax.set_xlim(xlo,xhi);ax.set_ylim(320,190);ax.set_aspect(.37);ax.set_axis_off();ax.set_title(label,loc='left',color=WHITE,fontsize=13,pad=8)
station(fig.add_subplot(gs[0,1]),62,94,'A  /  Shared center platform · staged 3-car LRT')
station(fig.add_subplot(gs[1,1]),413,458,'B  /  Separate side platforms · overhead footbridge')
ax=fig.add_subplot(gs[2,1]);ax.set_facecolor(PANEL)
ax.add_patch(Rectangle((67,0),23,1,color='#61727c'))
for x in [72,84]:
 ax.add_patch(Rectangle((x-.5,1),2,.13,color='#313e48'))
 ax.add_patch(Rectangle((x-2,2),5,1,color='#eff4f5'));ax.add_patch(Rectangle((x-2,3),5,5,fill=False,edgecolor='#b6dae9',lw=1.7))
 ax.plot([x+.5]*2,[2,8],lw=.5,ls=':',color=GRAY)
ax.add_patch(Rectangle((75,1),7,2,color='#c2d0d7'));ax.add_patch(Rectangle((75,2.82),7,.18,color=GOLD))
for x in [75,81.8125]:ax.add_patch(Rectangle((x,3),.1875,2,color='#64cee9'))
ax.annotate('',(72.5,8.75),(84.5,8.75),arrowprops={'arrowstyle':'<->','color':CYAN});ax.text(78.5,9.1,'12 blocks between track centers',color=CYAN,ha='center',fontsize=10)
ax.annotate('',(88,1),(88,3),arrowprops={'arrowstyle':'<->','color':GOLD});ax.text(88.4,2,'2 blocks',color=GOLD,fontsize=10,va='center')
ax.text(78.5,3.5,'LEVEL BOARDING',ha='center',color=WHITE,fontsize=9);ax.text(72.5,4.7,'5-wide\nLRV',ha='center',color=GRAY,fontsize=10);ax.text(84.5,4.7,'5-wide\nLRV',ha='center',color=GRAY,fontsize=10)
ax.set_xlim(68,90);ax.set_ylim(0,10);ax.set_aspect('equal');ax.set_axis_off();ax.set_title('Platform and carriage floor alignment',loc='left',color=WHITE,fontsize=13)
fig.text(.045,.046,'Cyan: outer loop   •   Gold: inner loop   •   Green dots: 16 native block signals   •   Green bays: screen doors',color=GRAY,fontsize=11)
fig.text(.045,.024,'Layout preview from exported structure data. Curves and stops use native Create NBT; overhead wires are visual only.',color=GRAY,fontsize=10)
(R/'preview').mkdir(exist_ok=True);fig.savefig(R/'preview/LRT-Test-Circuit-512-Preview.png',dpi=140);plt.close(fig)
print('Rendered exact schematic layout preview')

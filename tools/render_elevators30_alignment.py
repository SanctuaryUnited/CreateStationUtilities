"""Exact coordinate drawing. This is an offline geometry check, not Minecraft capture."""
from pathlib import Path
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle
out=Path('verification/dev16');out.mkdir(parents=True,exist_ok=True)
fig,axes=plt.subplots(1,3,figsize=(14,5.6));fig.patch.set_facecolor('#f5f7fb')
navy='#15354f';cyan='#1a8eac';gold='#d28b24'
def rect(ax,x,y,w,h,color,fill=False): ax.add_patch(Rectangle((x,y),w,h,fill=fill,facecolor=color,edgecolor=color,linewidth=1.8,alpha=.22 if fill else 1))
for ax in axes:
 ax.set_aspect('equal');ax.grid(alpha=.15);ax.tick_params(labelsize=8);ax.set_facecolor('white')
a=axes[0];a.set_title('Front — common passage center',fontsize=11)
rect(a,0,2,48,48,navy);rect(a,1,0,46,40,cyan);rect(a,8,2,32,32,gold)
a.axvline(24,ls='--',color='#a6afbb',lw=.9);a.set(xlim=(-3,51),ylim=(-4,55),xlabel='Minecraft pixels',ylabel='Height from cabin base (px)')
a.text(24,52,'Landing: 48 × 48 px',ha='center',fontsize=9,color=navy);a.text(24,-3,'Cabin: 46 × 40 px',ha='center',fontsize=9,color=cyan)
a.text(24,17,'32 px\nclear passage',ha='center',va='center',fontsize=10,color=gold)
a.text(4,38,'7 px',ha='center',fontsize=8,color=cyan);a.text(44,38,'7 px',ha='center',fontsize=8,color=cyan)
a=axes[1];a.set_title('Side — frames stay in their own space',fontsize=11)
rect(a,-16,2,16,48,navy);rect(a,-8,2,8,48,navy,True);rect(a,1,0,46,40,cyan);rect(a,1,2,8,38,cyan,True)
a.axvline(0,ls='--',color='#a6afbb');a.text(-5,53,'Landing\nblock',ha='center',fontsize=9,color=navy);a.text(25,43,'Cabin',ha='center',fontsize=9,color=cyan)
a.annotate('8 px frame depth',xy=(5,18),xytext=(17,25),fontsize=9,arrowprops={'arrowstyle':'->','color':cyan},color=cyan)
a.annotate('1 px moving clearance',xy=(.5,4),xytext=(11,-8),fontsize=8,arrowprops={'arrowstyle':'->','color':'#666'})
a.set(xlim=(-19,50),ylim=(-10,60),xlabel='Depth (px)')
a=axes[2];a.set_title('Top — 1 px inset on every side',fontsize=11)
rect(a,0,0,48,48,navy);rect(a,1,1,46,46,cyan);rect(a,1,1,7,8,cyan,True);rect(a,40,1,7,8,cyan,True);rect(a,0,-8,8,8,navy,True);rect(a,40,-8,8,8,navy,True)
a.plot([8,8],[-8,12],color=gold,ls='--');a.plot([40,40],[-8,12],color=gold,ls='--');a.text(24,5,'32 px',ha='center',fontsize=9,color=gold);a.text(24,24,'46 × 46 px\nexterior body',ha='center',fontsize=10,color=cyan)
a.set(xlim=(-3,51),ylim=(-12,51),xlabel='Width (px)')
fig.suptitle('Create: Transit • Elevators 3.0 • 3×3 geometry',fontsize=17,color=navy,y=.98)
fig.text(.5,.02,'Exact coordinates from the implementation • landing base aligns with cabin floor top • no shader or gameplay claim',ha='center',fontsize=9,color='#53687c')
fig.tight_layout(rect=(0,.06,1,.92));fig.savefig(out/'Elevators30-Alignment.png',dpi=160);fig.savefig(out/'Elevators30-Alignment.svg');print(out/'Elevators30-Alignment.png')

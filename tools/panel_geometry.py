"""Shared two-block, edge-aligned geometry for models, renderer and previews.

North-facing platform modules occupy z=0..3 pixels. Fixed frame shells leave
an internal sliding channel; neighboring walls receive the retracting leaves.
"""
FACES=['north','south','west','east','up','down']
DEPTH=3
JAMB=1.5
LEAF_WIDTH=16-JAMB
SLIDE=LEAF_WIDTH

def box(a,b,t,tint=-1,faces=None):
    return {'from':a,'to':b,'texture':t,'tint':tint,'visible':faces or FACES.copy()}

def size(style):
    height=48 if style is True else 24 if style is False else int(style)
    return height,(44 if height==48 else 30 if height==32 else 24)

def transition_trim(side):
    x1,x2=(0,JAMB) if side=='left' else (16-JAMB,16)
    return [box([x1,28,z1],[x2,32,z2],'frame',0)for z1,z2 in [(.12,.9),(2.65,2.88)]]

def lights(full,door,left=False,right=False):
    height,body=size(full);out=[];width=32 if door else 16
    for x,enabled in [(.45,door or not left),(width-1.05,door or not right)]:
        if enabled:
            for z in [0,2.9]:out.append(box([x,5,z],[x+.6,body-2,z+.1],'led_accent',1))
    if door:
        locations=[(10,22,45,47)] if height==48 else [(10,22,30.3,31.7)] if height==32 else [(0,1.5,22.5,23.5),(30.5,32,22.5,23.5)]
        for x1,x2,y1,y2 in locations:
            for z in [0,2.9]:out.append(box([x1,y1,z],[x2,y2,z+.1],'led_white'))
    return out

def stationary(full,door,col,row,left=False,right=False,leds=False,panel=0):
    h,body=size(full);lo=row*16;hi=min(lo+16,h);out=[]
    if door and col not in [0,1]:return out # legacy columns are migrated on load
    def add(a,b,t,tint=-1,faces=None):
        a=a.copy();b=b.copy();a[1]=max(a[1],lo);b[1]=min(b[1],hi)
        if b[1]>a[1]:
            a[1]-=lo;b[1]-=lo;out.append(box(a,b,t,tint,faces))
    def frame(x1,y1,x2,y2,t='frame',tint=0):
        for z1,z2 in [(.12,.9),(2.65,2.88)]:add([x1,y1,z1],[x2,y2,z2],t,tint)
    if door:
        x1,x2=(0,JAMB) if col==0 else (16-JAMB,16)
        frame(x1,0,x2,3);frame(x1,3,x2,5,'accent',1);frame(x1,5,x2,body)
    else:
        l=0 if left else JAMB;r=16 if right else 16-JAMB
        if l:frame(0,0,l,3);frame(0,3,l,5,'accent',1);frame(0,5,l,body)
        if r<16:frame(r,0,16,3);frame(r,3,16,5,'accent',1);frame(r,5,16,body)
        frame(l,0,r,3);frame(l,3,r,5,'accent',1);frame(l,body-2,r,body)
        add([l,5,1],[r,body-2,1.6],'glass',faces=['north','south'])
    if h>=32:
        add([0,body,.12],[16,h,2.88],'frame',0)
        add([0,body,0],[16,body+(.2 if h==32 else 1),.1],'accent',1)
        add([0,body,2.9],[16,body+(.2 if h==32 else 1),3],'accent',1)
    if leds:
        for e in lights(full,door,left,right):
            # Clip module-space LEDs to this cell; item and static preview only.
            a=e['from'].copy();b=e['to'].copy();start=col*16 if door else 0
            a[0]=max(a[0],start);b[0]=min(b[0],start+16)
            if b[0]>a[0]:a[0]-=start;b[0]-=start;add(a,b,e['texture'],e['tint'],e['visible'])
    return panel_style(out,panel,body)

def leaf(full,panel=0):
    _,h=size(full);w=LEAF_WIDTH
    out=[box([0,0,1.7],[.75,h,2.5],'frame',0),box([w-.75,0,1.7],[w,h,2.5],'frame',0),box([.75,0,1.7],[w-.75,3,2.5],'frame',0),box([.75,3,1.7],[w-.75,5,2.5],'accent',1),box([.75,h-2,1.7],[w-.75,h,2.5],'frame',0)]
    for y in range(0,h,16):
        low=max(5,y);high=min(h-2,y+16)
        if high>low:out.append(box([.75,low,1.8],[w-.75,high,2.4],'glass',faces=['north','south']))
    return panel_style(out,panel,h) # All accent LEDs are fixed to the jambs; none travel with the leaves.

def element(e):
    a=e['from'];b=e['to'];f={}
    for face in e['visible']:
        uv=[0,0,16,16]
        if e['texture']=='glass':
            ybase=(a[1]//16)*16
            uv=[16-b[0],16-(b[1]-ybase),16-a[0],16-(a[1]-ybase)] if face=='north' else [a[0],16-(b[1]-ybase),b[0],16-(a[1]-ybase)]
        f[face]={'texture':'#'+e['texture'],'uv':uv}
        if e['tint']>=0:f[face]['tintindex']=e['tint']
    return {'from':a.copy(),'to':b.copy(),'faces':f}


def panel_style(mesh,panel,body):
    """Replace glass, or split panes around a physical center mullion (no coplanar overlay)."""
    if not panel:return mesh
    result=[]
    for e in mesh:
        if e['texture']!='glass':result.append(e);continue
        a=e['from'];b=e['to']
        if panel==2:
            result.append(box(a.copy(),b.copy(),'frame',0));continue
        mid=(a[0]+b[0])/2;half=.4
        result.append(box(a.copy(),[mid-half,b[1],b[2]],'glass',faces=e['visible']))
        result.append(box([mid+half,a[1],a[2]],b.copy(),'glass',faces=e['visible']))
        result.append(box([mid-half,a[1],a[2]],[mid+half,b[1],b[2]],'frame',0))
    return result

def corner_return(full,row,side,panel=0):
    # Clip the perpendicular return at z=3 so it meets, rather than overlaps, the front screen.
    mesh=stationary(full,False,0,row,panel=panel)
    result=[]
    for e in mesh:
        a=e['from'];b=e['to']
        z1=max(3,a[0]);z2=min(16,b[0])
        if z2<=z1:continue
        x1,x2=(a[2],b[2]) if side==1 else (16-b[2],16-a[2])
        # Turn north/south faces into west/east faces.
        turn={'north':'west' if side==1 else 'east','south':'east' if side==1 else 'west','west':'north','east':'south','up':'up','down':'down'}
        result.append(box([x1,a[1],z1],[x2,b[1],z2],e['texture'],e['tint'],[turn[f] for f in e['visible']]))
    return result

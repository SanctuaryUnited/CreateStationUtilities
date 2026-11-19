"""Buildable 5x5 occupied grid with a roughly 3x3 visible shell."""
from gondola_geometry import *
def oriented(e,turn=0,x=0,y=0,z=0):
    a,b=e['from'],e['to'];points=[(a[0],a[2]),(a[0],b[2]),(b[0],a[2]),(b[0],b[2])]
    def rotate(X,Z):return [(X,Z),(16-Z,X),(16-X,16-Z),(Z,16-X)][turn]
    points=[rotate(*p)for p in points];order=['north','east','south','west']
    faces=[order[(order.index(f)+turn)%4]if f in order else f for f in e['visible']]
    return {**e,'from':[min(p[0]for p in points)+x,a[1]+y,min(p[1]for p in points)+z],'to':[max(p[0]for p in points)+x,b[1]+y,max(p[1]for p in points)+z],'visible':faces}
def layout():
    cells=[]
    def add(kind,x,y,z,turn=0,**flags):cells.append(dict(kind=kind,x=x,y=y,z=z,turn=turn,**flags))
    corners=[(0,0,0),(4,0,1),(4,4,2),(0,4,3)]
    edges=[(x,0,2)for x in range(1,4)]+[(x,4,0)for x in range(1,4)]+[(0,z,1)for z in range(1,4)]+[(4,z,3)for z in range(1,4)]
    for x in range(1,4):
        for z in range(1,4):
            add('floor',x,0,z)
            turn=2 if z==1 else 0 if z==3 else 1 if x==1 else 3
            if (x,z)==(2,2):add('hanger',x,4,z)
            else:
                shape='outer_left'if x in [1,3]and z in [1,3]else'straight'
                if shape=='outer_left':turn={(1,1):2,(3,1):3,(3,3):0,(1,3):1}[(x,z)]
                add('roof_edge',x,4,z,turn,shape=shape)
    for x,z,turn in edges:add('floor_edge',x,0,z,turn);add('led_horizontal',x,4,z,turn)
    for x,z,turn in corners:
        add('floor_outer_corner',x,0,z,turn);add('led_horizontal_outer_corner',x,4,z,turn)
        for y in range(1,4):add('led_vertical_outer_corner',x,y,z,turn)
    for y in range(1,4):
        for x,z,turn in edges:
            if z==0:continue
            v=x if z==4 else z;left,right=(v>1,v<3)if turn in [0,1]else(v<3,v>1)
            add('window',x,y,z,turn,left=left,right=right,top=y<3,bottom=y>1)
    add('double_door',3,1,0,2)
    for z in range(1,4):add('seat',1,1,z,1);add('seat',3,1,z,3)
    return cells
def geometry(open=False,cutaway=False):
    out=[]
    for c in layout():
        k=c['kind'];x,y,z=c['x'],c['y'],c['z']
        if cutaway and(y==4 or(z==0 and y>0)):continue
        if k=='window':elems=window(*(c[v]for v in ['left','right','top','bottom']))
        elif k=='roof_edge':elems=roof_edge(c['shape'])
        elif k=='double_door':
            elems=wide_fixed()+wide_lights()+[{**e,'texture':'gondola_led','tint':1}for e in wide_status()]
            elems += wide_moving(1 if open else 0)
            for side in [False,True]:
                slide,retract=wide_motion(1 if open else 0)
                elems += [transformed({**e,'texture':'led_green'if open else'gondola_seal'},(1 if side else-1)*slide,0,retract)for e in wide_center_lights(side)]
        else:elems=mesh(k)+(led_lights(k)if k in LED_KINDS else[])
        out.extend(oriented(e,c['turn'],x*16,y*16,z*16)for e in elems)
    return out

"""Original block-scale cabin geometry shared by models, collision and previews."""
from station_geometry import cube,element,transformed,cell,FACES
KINDS=['floor','wall','window','corner','window_corner','roof','roof_edge','led_pillar','hanger','seat','door','led_vertical','led_vertical_double','led_vertical_corner','led_horizontal','led_horizontal_double','led_horizontal_corner','double_door','outer_corner','led_vertical_outer_corner','led_horizontal_outer_corner','floor_edge','floor_outer_corner']
LED_KINDS=[k for k in KINDS if k.startswith('led_')]
ROOF_SHAPES=['straight','inner_left','inner_right','outer_left','outer_right']
def flipped_y(e):
    faces=[{'up':'down','down':'up'}.get(f,f)for f in e['visible']]
    return {**e,'from':[e['from'][0],16-e['to'][1],e['from'][2]],'to':[e['to'][0],16-e['from'][1],e['to'][2]],'visible':faces}
def roof_edge(shape='straight',half='bottom'):
    # Canonical north points to the HIGH edge, matching vanilla stair rules.
    out=[];cuts=[0,3,6,10,13,16]
    def n(z):return 6 if z<10 else 4 if z<13 else 2
    def side(x,left):return (6 if x<10 else 4 if x<13 else 2)if left else(6 if x>6 else 4 if x>3 else 2)
    for x,X in zip(cuts,cuts[1:]):
        for z,Z in zip(cuts,cuts[1:]):
            h=n((z+Z)/2)
            if shape!='straight':h=(max if shape.startswith('inner')else min)(h,side((x+X)/2,shape.endswith('left')))
            out.append(cube([x,0,z],[X,h,Z],'gondola_metal'))
    return [flipped_y(e)for e in out]if half=='top'else out
def led_body(kind,half='bottom'):
    if kind=='led_vertical_outer_corner':return [cube([13,0,13],[16,16,16],'gondola_metal')]
    if kind=='led_horizontal_outer_corner':
        out=[cube([13,0,13],[16,2,16],'gondola_metal')]
        return [flipped_y(e)for e in out]if half=='top'else out
    if kind=='led_pillar':return [cube([0,0,0],[3,16,3],'gondola_metal')]
    if kind=='led_vertical':return [cube([6,0,0],[10,16,1.5],'gondola_metal')]
    if kind=='led_vertical_double':return [cube([1,0,0],[4,16,1.5],'gondola_metal'),cube([12,0,0],[15,16,1.5],'gondola_metal')]
    if kind=='led_vertical_corner':return led_body('led_pillar')
    h=4 if kind=='led_horizontal_double'else 2
    out=[cube([0,0,0],[16,h,3],'gondola_metal')]
    if kind=='led_horizontal_corner':out.append(cube([0,0,3],[3,h,16],'gondola_metal'))
    return [flipped_y(e)for e in out]if half=='top'else out
def led_front_lights(kind,half='bottom'):
    def strip(x,y,X,Y):return cube([x,y,-.045],[X,Y,0],'gondola_led',1,['north'])
    if kind in ['led_pillar','led_vertical_corner']:
        out=[strip(.8,0,2.2,16)]
        if kind=='led_vertical_corner':out.append(cube([-.045,0,.8],[0,16,2.2],'gondola_led',1,['west']))
        return out
    if kind=='led_vertical':return [strip(7,0,9,16)]
    if kind=='led_vertical_double':return [strip(1.8,0,3.2,16),strip(12.8,0,14.2,16)]
    out=[strip(0,.55,16,1.45)]
    if kind=='led_horizontal_double':out.append(strip(0,2.55,16,3.45))
    if kind=='led_horizontal_corner':out.append(cube([-.045,.55,0],[0,1.45,16],'gondola_led',1,['west']))
    return [flipped_y(e)for e in out]if half=='top'else out
def led_lights(kind,half='bottom'):
    if kind in ['led_vertical_outer_corner','led_horizontal_outer_corner']:
        # Canonical NW footprint corner: a SE post lights the cabin-facing S/E faces.
        # Partition the T junctions so coplanar LED quads never overlap.
        spans=[(14.55,0,16.045,1.45),(13,.55,14.55,1.45)]
        if kind=='led_vertical_outer_corner':
            spans=[(14.55,0,16.045,16)]
        out=[]
        for x,y,X,Y in spans:
            out += [cube([x,y,16],[X,Y,16.045],'gondola_led',1,['south']),cube([16,y,x],[16.045,Y,X],'gondola_led',1,['east'])]
        return [flipped_y(e)for e in out]if half=='top'else out
    # Back faces keep trims visible when mounted flush against a support face.
    lights=led_front_lights(kind);out=list(lights)
    for e in lights:
        a,b=e['from'],e['to']
        if e['visible']==['north']:
            depth=3 if kind in ['led_pillar','led_vertical_corner']or kind.startswith('led_horizontal')else 1.5
            x=max(a[0],3)if kind=='led_horizontal_corner'else a[0]
            out.append(cube([x,a[1],depth],[b[0],b[1],depth+.045],'gondola_led',1,['south']))
        elif e['visible']==['west']:
            depth=3
            z=max(a[2],3)if kind=='led_horizontal_corner'else a[2]
            out.append(cube([depth,a[1],z],[depth+.045,b[1],b[2]],'gondola_led',1,['east']))
    return [flipped_y(e)for e in out]if half=='top'else out
def window(left=False,right=False,top=False,bottom=False):
    x,X=(0 if left else 1.5),(16 if right else 14.5)
    y,Y=(0 if bottom else 5),(16 if top else 14.5)
    mesh=[cube([x,y,1.35],[X,Y,1.65],'gondola_glass',-1,['north','south'])]
    if not left:mesh.append(cube([0,0,0],[1.5,16,3],'gondola_metal'))
    if not right:mesh.append(cube([14.5,0,0],[16,16,3],'gondola_metal'))
    if not bottom:mesh += [cube([x,0,0],[X,1,3],'gondola_metal'),cube([x,1,0],[X,4,3],'gondola_accent',1),cube([x,4,0],[X,5,3],'gondola_metal')]
    if not top:mesh.append(cube([x,14.5,0],[X,16,3],'gondola_metal'))
    return mesh
def window_corner(top=False,bottom=False):
    y,Y=(0 if bottom else 5),(16 if top else 14.5)
    out=[cube([0,0,0],[3,16,3],'gondola_metal'),cube([3,y,1.35],[16,Y,1.65],'gondola_glass',-1,['north','south']),cube([1.35,y,3],[1.65,Y,16],'gondola_glass',-1,['east','west'])]
    if not bottom:
        for y,Y,texture,tint in [(0,1,'gondola_metal',0),(1,4,'gondola_accent',1),(4,5,'gondola_metal',0)]:out += [cube([3,y,0],[16,Y,3],texture,tint),cube([0,y,3],[3,Y,16],texture,tint)]
    if not top:out += [cube([3,14.5,0],[16,16,3],'gondola_metal'),cube([0,14.5,3],[3,16,16],'gondola_metal')]
    return out
def mesh(kind):
    if kind=='floor':return [cube([0,12,0],[16,15,16],'gondola_metal'),cube([0,15,0],[16,16,16],'gondola_floor',-1)]
    if kind=='floor_edge':return [cube([0,12,0],[16,15,3],'gondola_metal'),cube([0,15,0],[16,16,3],'gondola_floor',-1)]
    if kind=='floor_outer_corner':return [cube([13,12,13],[16,15,16],'gondola_metal'),cube([13,15,13],[16,16,16],'gondola_floor',-1)]
    if kind=='wall':return [cube([0,0,0],[16,1,3],'gondola_metal'),cube([0,1,0],[16,4,3],'gondola_accent',1),cube([0,4,0],[16,16,3],'gondola_metal')]
    if kind=='outer_corner':return [cube([13,0,13],[16,1,16],'gondola_metal'),cube([13,1,13],[16,4,16],'gondola_accent',1),cube([13,4,13],[16,16,16],'gondola_metal')]
    if kind=='window':return window()
    if kind=='corner':return mesh('wall')+[cube([0,0,3],[3,1,16],'gondola_metal'),cube([0,1,3],[3,4,16],'gondola_accent',1),cube([0,4,3],[3,16,16],'gondola_metal')]
    if kind=='window_corner':return window_corner()
    if kind=='roof':return [cube([0,0,0],[16,1,16],'gondola_ceiling',-1),cube([0,1,0],[16,6,16],'gondola_metal')]
    if kind=='roof_edge':return roof_edge()
    if kind in LED_KINDS:return led_body(kind)
    if kind=='hanger':return mesh('roof')+[cube([5,6,5],[11,14,11],'gondola_metal'),cube([3,14,3],[13,16,13],'gondola_metal'),cube([2.98,14.5,5],[3,15.5,11],'gondola_accent',1,['west']),cube([13,14.5,5],[13.02,15.5,11],'gondola_accent',1,['east'])]
    if kind=='seat':return [cube([2,0,3],[4,6,13],'gondola_metal'),cube([12,0,3],[14,6,13],'gondola_metal'),cube([0,6,2],[16,7,14],'gondola_metal'),cube([0,7,2],[16,9,14],'gondola_fabric',1),cube([0,9,12],[16,16,14],'gondola_fabric',1),cube([0,9,14],[16,16,15],'gondola_metal')]
    if kind=='door':return door_fixed()+door_leaf()+door_lights()
    if kind=='double_door':return wide_fixed()+wide_leaf(False)+wide_leaf(True)+wide_lights()+wide_status()
    raise ValueError(kind)
def pillar_lights():return led_lights('led_pillar')
def door_fixed():return [cube([0,0,0],[1,31,1.1],'gondola_metal'),cube([15,0,0],[16,31,1.1],'gondola_metal'),cube([0,31,0],[16,32,3],'gondola_metal')]
def framed_leaf(x,X,height):
    return [cube([x,0,1.5],[X,1,2.5],'gondola_metal'),cube([x,1,1.5],[X,4,2.5],'gondola_accent',1),cube([x,4,1.5],[X,4.8,2.5],'gondola_metal'),cube([x,4.8,1.5],[x+.7,height-1,2.5],'gondola_metal'),cube([X-.7,4.8,1.5],[X,height-1,2.5],'gondola_metal'),cube([x+.7,height-2.5,1.5],[X-.7,height-1,2.5],'gondola_metal'),cube([x+.7,4.8,1.7],[x+1,height-2.5,2.3],'gondola_seal',-1),cube([X-1,4.8,1.7],[X-.7,height-2.5,2.3],'gondola_seal',-1),cube([x+1,4.8,1.9],[X-1,height-2.5,2.1],'gondola_glass',-1,['north','south'])]
def door_leaf():return framed_leaf(1,15,32)
def door_lights():return [cube([x,4,z],[x+.6,30.4,Z],'gondola_led',1,[face])for x in [.2,15.2]for z,Z,face in [(-.045,0,'north'),(1.1,1.145,'south')]]
def door_status():return [cube([5,31.2,z],[11,31.8,Z],'led_white',-1,[face])for z,Z,face in [(-.045,0,'north'),(3,3.045,'south')]]
def wide_fixed(height=48):
    out=[]
    for x in [0,40]:
        out += [cube([x,0,0],[x+8,1,1.1],'gondola_metal'),cube([x,1,0],[x+8,4,1.1],'gondola_accent',1),cube([x,4,0],[x+8,height,1.1],'gondola_metal')]
    return out+[cube([8,height-1,0],[40,height,3],'gondola_metal')]
def wide_leaf(right=False,height=48):
    x=24 if right else 8
    # Alpha 8: two more pixels cabinward, nearly flush at z=.2..1.2.
    # Only leaves move; the existing frame and accent LEDs are unchanged.
    a,b=(24,24.95)if right else(23.05,24)
    middle=height/2
    bezels=[cube([a,middle-1,.08],[b,middle+1,.2],'gondola_seal',-1,['north','west','east','up','down']),cube([a,middle-1,1.2],[b,middle+1,1.32],'gondola_seal',-1,['south','west','east','up','down'])]
    return [transformed(e,0,0,-1.3)for e in framed_leaf(x,x+16,height)]+bezels
def wide_center_lights(right=False,height=48):
    a,b=(24.1,24.8)if right else(23.2,23.9)
    middle=height/2
    return [cube([a,middle-.65,z],[b,middle+.65,Z],'led_white',-1,[face])for z,Z,face in [(.025,.08,'north'),(1.32,1.375,'south')]]
def smooth(progress):
    t=max(0,min(1,progress));return t*t*(3-2*t)
def wide_motion(progress):
    eased=smooth(progress)
    return smooth((eased-.2)/.8)*16,smooth(eased/.2)*3
def wide_moving(progress,height=48,center=False):
    slide,retract=wide_motion(progress)
    return [transformed(e,(-1 if side==0 else 1)*slide,0,retract)for side in [0,1]for e in wide_leaf(side==1,height)+(wide_center_lights(side==1,height)if center else[])]
def wide_lights(height=48):return [cube([x,4,z],[x+.8,height-1.6,Z],'gondola_led',1,[face])for x in [6.6,40.6]for z,Z,face in [(-.045,0,'north'),(1.1,1.145,'south')]]
def wide_status(height=48):return [cube([19,height-.8,z],[29,height-.2,Z],'led_white',-1,[face])for z,Z,face in [(-.045,0,'north'),(3,3.045,'south')]]

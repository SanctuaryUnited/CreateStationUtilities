"""Escalator mesh geometry in model pixels; NORTH rises toward z=0."""
import math

def face(vertices, texture='metal', tint=0):
    return {'vertices': vertices, 'uv': [[0,16],[0,0],[16,0],[16,16]], 'texture': '#'+texture, 'tint': tint}

def box(a,b,texture='metal',tint=0,slope=False):
    x,y,z=a; X,Y,Z=b
    vs=[[(x,y,z),(x,Y,z),(X,Y,z),(X,y,z)], [(X,y,Z),(X,Y,Z),(x,Y,Z),(x,y,Z)],
        [(x,y,Z),(x,Y,Z),(x,Y,z),(x,y,z)], [(X,y,z),(X,Y,z),(X,Y,Z),(X,y,Z)],
        [(x,Y,z),(x,Y,Z),(X,Y,Z),(X,Y,z)], [(x,y,Z),(x,y,z),(X,y,z),(X,y,Z)]]
    if slope: vs=[[(a,b+16-c,c) for a,b,c in v] for v in vs]
    return [face(v,texture,tint) for v in vs]

def prism(x,X,points,texture='metal',tint=0):
    points=list(points)
    if sum(z*Y-Z*y for (z,y),(Z,Y) in zip(points,points[1:]+points[:1]))<0:points.reverse()
    qs=[]
    for i in range(1,len(points)-1):
        tri=[points[0],points[i],points[i+1]];back=list(reversed(tri))
        qs.append(face([(x,y,z) for z,y in tri+[tri[-1]]],texture,tint))
        qs.append(face([(X,y,z) for z,y in back+[back[-1]]],texture,tint))
    for (z,y),(Z,Y) in zip(points,points[1:]+points[:1]):
        qs.append(face([(x,y,z),(X,y,z),(X,Y,Z),(x,Y,Z)],texture,tint))
    return qs

def rail_path(kind,flat_join=False,both_ends=False):
    if both_ends:
        lower=rail_path('lower_landing')[:-1]
        return lower+[(16-z,y) for z,y in reversed(lower)]
    if flat_join:return [(16,18),(0,18)]
    if kind=='incline': return [(16,18),(0,34)]
    if kind=='flat': return [(16,18),(0,18)]
    points=[(14.8,3),(14.8,14.8)]
    points += [(11.6+3.2*math.cos(i*math.pi/12),14.8+3.2*math.sin(i*math.pi/12)) for i in range(1,7)]
    points += [(0,18)]
    return [(16-z,y) for z,y in points] if kind=='upper_landing' else points

def rail_offsets(kind,slope_join=False,flat_join=False,both_ends=False):
    path=rail_path(kind,flat_join,both_ends); normals=[]
    for i,(z,y) in enumerate(path):
        adjacent=[]
        for a,b in [(path[max(0,i-1)],path[i]),(path[i],path[min(len(path)-1,i+1)])]:
            dz,dy=b[0]-a[0],b[1]-a[1];length=math.hypot(dz,dy)
            if length:adjacent.append((-dy/length,dz/length))
        if len(adjacent)==2:
            a,b=adjacent;d=1+a[0]*b[0]+a[1]*b[1]
            normals.append(((a[0]+b[0])/d,(a[1]+b[1])/d))
        else:normals.append(adjacent[0])
    # Shared miter plane at each 45-degree/flat transition. Both pieces use
    # exactly the same pair of edge vertices instead of perpendicular caps.
    m=math.tan(math.pi/8)
    if kind=='incline':normals[0]=normals[-1]=(-m,-1)
    elif slope_join:
        if flat_join:normals[0 if kind=='upper_landing' else -1]=(-m,-1)
        else:normals[-1]=(m,1) if kind=='upper_landing' else (-m,-1)
    return normals

def rail(x,X,kind,slope_join=False,flat_join=False,both_ends=False):
    path=rail_path(kind,flat_join,both_ends);normals=rail_offsets(kind,slope_join,flat_join,both_ends)
    qs=[]
    for i,((z,y),(Z,Y)) in enumerate(zip(path,path[1:])):
        nz,ny=normals[i];NZ,NY=normals[i+1]
        points=[(z+nz,y+ny),(Z+NZ,Y+NY),(Z-NZ,Y-NY),(z-nz,y-ny)]
        if sum(a*d-c*b for (a,b),(c,d) in zip(points,points[1:]+points[:1]))<0:points.reverse()
        # Emit the two side quads and the inner/outer strips once. Interior
        # segment caps used to overlap at every rounded terminal segment.
        qs.append(face([(x,y,z) for z,y in points],'rubber',-1))
        qs.append(face([(X,y,z) for z,y in reversed(points)],'rubber',-1))
        for (z,y),(Z,Y) in zip(points,points[1:]+points[:1]):
            if (z,y) in [(path[i][0]+normals[i][0],path[i][1]+normals[i][1]),(path[i][0]-normals[i][0],path[i][1]-normals[i][1])] and (Z,Y) in [(path[i][0]+normals[i][0],path[i][1]+normals[i][1]),(path[i][0]-normals[i][0],path[i][1]-normals[i][1])]:
                if i>0 or flat_join and slope_join and kind=='upper_landing':continue
            if (z,y) in [(path[i+1][0]+normals[i+1][0],path[i+1][1]+normals[i+1][1]),(path[i+1][0]-normals[i+1][0],path[i+1][1]-normals[i+1][1])] and (Z,Y) in [(path[i+1][0]+normals[i+1][0],path[i+1][1]+normals[i+1][1]),(path[i+1][0]-normals[i+1][0],path[i+1][1]-normals[i+1][1])]:
                if i<len(path)-2 or slope_join and (not flat_join or kind=='lower_landing'):continue
            qs.append(face([(x,y,z),(X,y,z),(X,Y,Z),(x,Y,Z)],'rubber',-1))
    return qs

def base(kind):return box([0,-5,0],[16,1.85,16],'underbody',-1,kind=='incline')

def sides(kind,glass,left,slope_join=False,flat_join=False,both_ends=False):
    x,X=(0,3) if left else (13,16);sloped=kind=='incline'
    opaque=box([x,-5,0],[X,0,16],'underbody',-1,sloped)
    opaque+=box([x,0,0],[X,6,16],'metal',0,sloped)
    opaque+=box([x+.25,5.65,0],[X-.25,6.25,16],'rail',0,sloped)
    opaque+=box([x,-4.75,0],[X,-3.9,16],'rubber',-1,sloped)
    if both_ends:
        poly=[(2.2,6),(13.8,6),(13.8,14.5),(13.65,15.65),(12.8,16.5),(11.6,16.8),
              (4.4,16.8),(3.2,16.5),(2.35,15.65),(2.2,14.5)]
    elif kind in ('lower_landing','upper_landing') and not flat_join:
        top=[(0,16.8),(11.6,16.8),(12.8,16.5),(13.65,15.65),(13.8,14.5)]
        poly=[(0,6),(13.8,6)]+list(reversed(top))
        if kind=='upper_landing':poly=[(16-z,y) for z,y in poly]
    else:
        rise=16 if sloped else 0;poly=[(0,6+rise),(16,6),(16,16.8),(0,16.8+rise)]
    pane=prism((x+X)/2-.14,(x+X)/2+.14,poly,'glass',-1) if glass else []
    if not glass:opaque+=prism(x+.2,X-.2,poly,'metal',0)
    path=rail_path(kind,flat_join,both_ends)
    for (z,y),(Z,Y) in zip(path,path[1:]):
        if max(y,Y)>=15:opaque+=prism(x+.35,X-.35,[(z,y-1.4),(Z,Y-1.4),(Z,Y-1.8),(z,y-1.8)],'rail',-1)
    opaque+=rail(x+.1,X-.1,kind,slope_join,flat_join,both_ends)
    outer=-.012 if left else 16.012
    opaque+=box([outer-.015,1.15,0],[outer+.015,2.25,16],'led',1,sloped)
    # Raise the passage-side strip above the highest animated tread edge.
    # Its metal housing joins the existing casing and stays inside the side rail.
    a,b=(2.6,3) if left else (13,13.4)
    opaque+=box([a,5.9,0],[b,8,16],'rail',0,sloped)
    inner=3.012 if left else 12.988
    opaque+=box([inner-.015,6.45,0],[inner+.015,7.55,16],'led',1,sloped)
    caps=('lower_landing','upper_landing') if both_ends else (kind,) if kind in ('lower_landing','upper_landing') and not flat_join else ()
    for cap in caps:
        z,Z=(13.65,15.82) if cap=='lower_landing' else (.18,2.35)
        opaque+=box([x+.12,2.5,z],[X-.12,11.6,Z],'rubber',-1)
        opaque+=box([x+.4,3.2,z-.025],[X-.4,10.8,Z+.025],'panel',-1)
        opaque+=box([x+.1,-.1,z-.15],[X-.1,2.4,Z+.15],'rail',0)
    # Internal glass end faces overlay one another and make a dark seam under
    # shaders. Connected pane faces stay open while the side surfaces meet.
    if kind=='incline':pane=without_end_faces(pane,0,16)
    elif kind=='lower_landing':pane=without_end_faces(pane,0,*([16] if flat_join else []))
    elif kind=='upper_landing':pane=without_end_faces(pane,16,*([0] if flat_join else []))
    if flat_join and kind in ('lower_landing','upper_landing'):
        opaque=without_end_faces(opaque,16 if kind=='lower_landing' else 0)
    return opaque,pane

def without_end_faces(quads,*ends):
    return [q for q in quads if not any(all(abs(p[2]-z)<1e-8 for p in q['vertices']) for z in ends)]

def flat_sides(glass,left,back_join,front_join):
    kind='flat' if back_join==front_join else 'upper_landing' if back_join else 'lower_landing'
    opaque,pane=sides(kind,glass,left,both_ends=not back_join and not front_join)
    ends=([16] if back_join else [])+([0] if front_join else [])
    return without_end_faces(opaque,*ends),without_end_faces(pane,*ends)

def treads(slope,phase):
    phase=phase%1;cuts=sorted({0.,1.,*[phase+i/4 for i in range(-4,5) if 0<phase+i/4<1]})
    for a,b in zip(cuts,cuts[1:]):
        h=.125+(min(1,max(0,math.ceil(((a+b)/2-phase)*4-1e-8)/4+phase)) if slope else 0)
        yield a,b,h

def moving(kind,left,right,phase=0,reverse=False,stopped=False,flat_join=None,slope_join=None):
    # Match the runtime's visible surfaces. Hidden box sides/undersides are omitted.
    flat_tile=kind=='flat'
    if flat_join is None:flat_join=flat_tile
    if slope_join is None:slope_join=flat_tile
    both_ends=flat_tile and not flat_join and not slope_join
    if flat_tile and not flat_join and slope_join:kind='lower_landing'
    elif flat_tile and flat_join and not slope_join:kind='upper_landing'
    through=not flat_tile and kind in ('lower_landing','upper_landing') and flat_join
    back_cap=both_ends or kind=='lower_landing' and not through
    front_cap=both_ends or kind=='upper_landing' and not through
    deck_min=4 if front_cap else 0
    deck_max=12 if back_cap else 16
    slope=kind=='incline';x=3.02 if left else 0;X=12.98 if right else 16;qs=[]
    for a,b,h in treads(slope,phase):
        z,Z,y=max(deck_min,(1-b)*16),min(deck_max,(1-a)*16),h*16
        if Z<=z:continue
        q=box([x,y-4.1,z],[X,y,Z],'steps',-1);qs += [q[4],q[1]]
        qs.append(box([x,y+.008,z],[X,y+.025,min(Z,z+.14)],'rail',-1)[4])
        for outer,xx in [(left,x),(right,X-.55)]:
            if outer:qs.append(box([xx,y+.03,z],[xx+.55,y+.055,Z],'edge',-1)[4])
    for outer,xx in [(left,.4),(right,13.4)]:
        if not outer:continue
        for i in range(4):
            u=(phase+i/4)%1;z=(1-u)*16;Z=min(16,z+.13)
            if Z<=z:continue
            if not through and kind=='lower_landing' and z>11.5:continue
            if not through and kind=='upper_landing' and z<4.5:continue
            if both_ends and (z<4.5 or z>11.5):continue
            height=19.44 if slope else 19.04
            qs.append(box([xx,height,z],[xx+2.2,height+.04,Z],'grip',-1,slope)[4])
    caps=('lower_landing','upper_landing') if both_ends else (kind,) if kind in ('lower_landing','upper_landing') and not through else ()
    for cap in caps:
        lower=cap=='lower_landing';entry=lower!=reverse;tex='arrow' if entry and not stopped else 'stop'
        z=15.86 if lower else .14
        for outer,xx in [(left,.45),(right,13.45)]:
            if outer:qs.append(box([xx,5.2,z-.012],[xx+2.1,9.5,z+.012],tex,-1)[1 if lower else 0])
    return qs

def comb(kind,left,right):
    if kind not in ('lower_landing','upper_landing'):return []
    x=3.02 if left else 0;X=12.98 if right else 16;z,Z=(12,16) if kind=='lower_landing' else (0,4)
    a,b=(12,12.75) if kind=='lower_landing' else (3.25,4)
    return box([x,1.85,z],[X,2.06,Z],'comb',-1)+box([x,2.065,a],[X,2.1,b],'edge',-1)

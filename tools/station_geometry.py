"""Exact shared cuboids for station models, renderer, Blockbench and preview."""
FACES=['north','south','west','east','up','down']
def cube(a,b,texture='frame',tint=0,faces=None,rotation=None):
 e={'from':a,'to':b,'texture':texture,'tint':tint,'visible':faces or FACES}
 if rotation:e['rotation']=rotation
 return e
def element(e):
 d={'from':e['from'],'to':e['to'],'faces':{f:{'texture':'#'+e['texture'],'uv':[0,0,16,16],**({'tintindex':e['tint']}if e['tint']>=0 else{})}for f in e['visible']}}
 if 'rotation'in e:d['rotation']=e['rotation']
 return d
def gate_fixed():
 out=[]
 for x in [0]:
  lo=x+.6;hi=x+15.4
  out += [cube([x,0,0],[x+16,2,16]),cube([x,2,0],[x+16,3,16],'accent',1),cube([lo,3,1],[hi,19,15]),cube([x,19,0],[x+16,20,16],'accent',1),cube([x,20,0],[x+16,22,16])]
  for z in [0,15]:
   out += [cube([lo,3,z],[lo+1,19,z+1]),cube([hi-1,3,z],[hi,19,z+1]),cube([lo+1,3,z],[hi-1,19,z+1],'station_dark',-1)]
 return out
def gate_lights():
 out=[]
 for x in [0]:
  for start in [2,10]:out += [cube([x+start,12,-.04],[x+start+4,16,0],'fare_arrow',-1,['north']),cube([x+start,12,16],[x+start+4,16,16.04],'fare_stop',-1,['south'])]
  out += [cube([x+7.5,5,-.04],[x+8.5,10.5,0],'led_green',-1,['north']),cube([x+7.5,5,16],[x+8.5,10.5,16.04],'led_red',-1,['south']),cube([x+6,17,-.04],[x+10,19,0],'kiosk_reader',-1,['north'])]
  for z,f in [(-.035,'north'),(16,'south')]:out += [cube([x+.8,3,z],[x+1.4,19,z+.035],'led_accent',1,[f]),cube([x+14.6,3,z],[x+15.2,19,z+.035],'led_accent',1,[f])]
 return out
def gate_leaf():
 return [cube([0,3,7.5],[.6,18.7,8.5]),cube([15.3,3,7.5],[15.9,18.7,8.5]),cube([.6,3,7.5],[15.3,3.7,8.5]),cube([.6,18,7.5],[15.3,18.7,8.5]),cube([.6,3.7,7.9],[15.3,18,8.1],'glass',-1,['north','south'])]
def kiosk_fixed():
 return [cube([1,0,3],[15,2,15]),cube([1,2,5],[15,17,15]),cube([1,17,8],[15,27,15]),cube([1,27,5],[15,29,15]),cube([1,17,5],[3,27,8]),cube([13,17,5],[15,27,8]),cube([1,2,3],[2,29,5]),cube([14,2,3],[15,29,5]),cube([2,2,3],[14,12,5]),cube([2,14,3],[14,17,5]),cube([2,27,3],[14,29,5]),cube([2,17,3],[3,27,5]),cube([13,17,3],[14,27,5]),cube([3,17,3],[13,27,3.04],'kiosk_screen',-1,['north'],{'origin':[8,17,3],'axis':'x','angle':22.5,'rescale':False}),cube([1,29,3],[15,32,15]),cube([2.5,29.3,2.96],[13.5,31.7,3],'kiosk_sign',-1,['north']),cube([3,3,2.9],[13,10,3],'station_dark',-1,['north']),cube([3,12,4.85],[13,14,5],'station_dark',-1,['north']),cube([9.5,14.4,2.8],[12.5,16.6,3],'kiosk_reader',-1,['north'])]
def kiosk_lights():
 return [cube([1.2,2,2.96],[1.8,29,3],'led_accent',1,['north']),cube([14.2,2,2.96],[14.8,29,3],'led_accent',1,['north']),cube([2,2,2.96],[14,2.6,3],'led_accent',1,['north'])]
def transformed(e,x=0,y=0,z=0,mirror=False):
 d={**e,'from':[e['from'][0]+x,e['from'][1]+y,e['from'][2]+z],'to':[e['to'][0]+x,e['to'][1]+y,e['to'][2]+z]}
 if mirror:d['from'][0],d['to'][0]=x-e['to'][0],x-e['from'][0]
 return d
def cell(mesh,col,row):
 out=[]
 for e in mesh:
  a=[max(e['from'][0],col*16),max(e['from'][1],row*16),e['from'][2]];b=[min(e['to'][0],(col+1)*16),min(e['to'][1],(row+1)*16),e['to'][2]]
  if a[0]>=b[0]or a[1]>=b[1]:continue
  d={**e,'from':[a[0]-col*16,a[1]-row*16,a[2]],'to':[b[0]-col*16,b[1]-row*16,b[2]]}
  if 'rotation'in e:d['rotation']={**e['rotation'],'origin':[e['rotation']['origin'][0]-col*16,e['rotation']['origin'][1]-row*16,e['rotation']['origin'][2]]}
  out.append(d)
 return out

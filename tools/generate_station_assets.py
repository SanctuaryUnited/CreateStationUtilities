"""Original fare-gate/kiosk pixels, PBR companions, models, audio and editor assets."""
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,subprocess,wave,uuid
import numpy as np
from station_geometry import *
ROOT=Path(__file__).resolve().parents[1];R=ROOT/'src/main/resources';A=R/'assets/createtransit';D=R/'data/createtransit';T=A/'textures/block'
def js(p,obj):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,indent=2)+'\n')
def texture(name,im,emission=False):
 im.save(T/(name+'.png'));Image.new('RGBA',im.size,(128,128,255,255)).save(T/(name+'_n.png'))
 Image.new('RGBA',im.size,(170,10,0,254 if emission else 0)).save(T/(name+'_s.png'))
 if emission:im.save(T/(name+'_e.png'))
dark=Image.new('RGBA',(32,32),(18,26,34,255));texture('station_dark',dark)
red=Image.new('RGBA',(32,32),(255,50,38,255));texture('led_red',red,True)
arrow=dark.copy();d=ImageDraw.Draw(arrow);d.polygon([(16,3),(28,15),(22,15),(22,28),(10,28),(10,15),(4,15)],fill=(84,255,105));texture('fare_arrow',arrow,True)
stop=dark.copy();d=ImageDraw.Draw(stop);d.regular_polygon((16,16,15),8,rotation=22.5,fill=(244,49,42),outline=(255,230,225));d.text((4,11),'STOP',font=ImageFont.load_default(size=9),fill='white');texture('fare_stop',stop,True)
screen=Image.new('RGBA',(64,64),(9,24,37,255));d=ImageDraw.Draw(screen)
for x,label in [(4,'TICKET'),(34,'PASS')]:
 d.rectangle((x,8,x+26,55),outline=(70,210,255),width=2);d.text((x+2,41),label,font=ImageFont.load_default(size=7),fill=(175,234,255))
 d.rectangle((x+6,17,x+20,30),fill=(168,238,255));d.line((x+7,23,x+19,23),fill=(14,58,79),width=2)
texture('kiosk_screen',screen,True)
sign=Image.new('RGBA',(128,32),(14,26,38,255));d=ImageDraw.Draw(sign);font=ImageFont.load_default(size=12);d.text((5,9),'TICKETS & PASSES',font=font,fill=(175,233,255));texture('kiosk_sign',sign,True)
reader=dark.copy();d=ImageDraw.Draw(reader)
for radius in [5,10,15]:d.arc((16-radius,20-radius,16+radius,20+radius),210,330,fill=(71,208,255),width=2)
d.ellipse((14,21,17,24),fill=(100,227,255));texture('kiosk_reader',reader,True)
ticket=Image.new('RGBA',(32,32),(236,234,211,255));d=ImageDraw.Draw(ticket);d.rectangle((0,2,31,6),fill=(40,130,200));d.text((2,10),'TRANSIT',font=ImageFont.load_default(size=6),fill=(30,45,57));
for x in range(3,29,3):d.line((x,22,x,29),fill=(45,54,60),width=1)
texture('ticket_paper',ticket)
names=['frame','accent','glass','led_accent','led_white','led_green','led_red','station_dark','fare_arrow','fare_stop','kiosk_screen','kiosk_sign','kiosk_reader','ticket_paper']
textures={n:'createtransit:block/'+n for n in names};textures['particle']=textures['frame']
atlas=json.loads((R/'assets/minecraft/atlases/blocks.json').read_text());existing={x['sprite']for x in atlas['sources']}
for n in names:
 if textures[n]not in existing:atlas['sources'].append({'type':'minecraft:single','resource':textures[n],'sprite':textures[n]})
js(R/'assets/minecraft/atlases/blocks.json',atlas)
for name,width,mesh,lamps in [('fare_gate',2,gate_fixed(),gate_lights()),('fare_reader',1,gate_fixed(),gate_lights()),('ticket_kiosk',1,kiosk_fixed(),kiosk_lights())]:
 parts=[]
 for col in range(width):
  for row in range(2):
   m=f'{name}_{col}_{row}'
   js(A/f'models/block/{m}.json',{'parent':'minecraft:block/block','render_type':'minecraft:solid','ambientocclusion':False,'textures':textures,'elements':[element(e)for e in cell(mesh,col,row)]})
   for f,rot in [('north',0),('east',90),('south',180),('west',270)]:parts.append({'when':{'facing':f,'column':str(col),'row':str(row)},'apply':{'model':'createtransit:block/'+m,'y':rot}})
 js(A/f'blockstates/{name}.json',{'multipart':parts})
 if name=='fare_gate':
  # Read-only third-column fallback keeps pre-conversion saved blocks renderable.
  for row in range(2):
   m=f'fare_gate_2_{row}'
   js(A/f'models/block/{m}.json',{'parent':'minecraft:block/block','render_type':'minecraft:solid','ambientocclusion':False,'textures':textures,'elements':[element(e)for e in cell(gate_fixed(),0,row)]})
   for f,rot in [('north',0),('east',90),('south',180),('west',270)]:parts.append({'when':{'facing':f,'column':'2','row':str(row)},'apply':{'model':'createtransit:block/'+m,'y':rot}})
  js(A/f'blockstates/{name}.json',{'multipart':parts})
 itemmesh=mesh+lamps
 if name=='fare_gate':itemmesh=itemmesh+[transformed(e,16)for e in gate_leaf()]
 scale=16/max(width*16,32);elems=[]
 for e in itemmesh:
  el=element(e);el['from']=[e['from'][0]*scale,e['from'][1]*scale,(e['from'][2]-8)*scale+8];el['to']=[e['to'][0]*scale,e['to'][1]*scale,(e['to'][2]-8)*scale+8]
  if 'rotation'in el:
   o=el['rotation']['origin'];el['rotation']={**el['rotation'],'origin':[o[0]*scale,o[1]*scale,(o[2]-8)*scale+8]}
  elems.append(el)
 js(A/f'models/item/{name}.json',{'parent':'minecraft:block/block','render_type':'minecraft:translucent','ambientocclusion':False,'textures':textures,'elements':elems,'display':{'gui':{'rotation':[25,225,0],'translation':[0,0,0],'scale':[.9,.9,.9]}}})
 js(D/f'loot_table/blocks/{name}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:block_state_property','block':'createtransit:'+name,'properties':{'column':'0','row':'0'}},{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'createtransit:'+name,'functions':[{'function':'minecraft:copy_custom_data','source':'block_entity','ops':[{'source':'paint','target':'paint','op':'replace'},{'source':'accent','target':'accent','op':'replace'}]}]}]}]})
 pattern=['IGI','IRI','III']if name=='fare_gate'else[' I ','IRI','III']if name=='fare_reader'else['IGI','IPI','IRI']
 js(D/f'recipe/{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{'I':{'item':'minecraft:iron_ingot'},'R':{'item':'minecraft:redstone'},**({'G':{'item':'minecraft:glass'}}if name!='fare_reader'else{}),**({'P':{'item':'minecraft:paper'}}if name=='ticket_kiosk'else{})},'result':{'id':'createtransit:'+name,'count':1}})
 # Editable models retain real game coordinates and named swing pivots.
 elements=[];outline=[]
 def bb(e,label):
  uid=str(uuid.uuid5(uuid.NAMESPACE_URL,name+label));el=element(e);elements.append({'name':label,'uuid':uid,'type':'cube','from':e['from'],'to':e['to'],'faces':{f:{'uv':v['uv'],'texture':names.index(e['texture'])}for f,v in el['faces'].items()},**({'rotation':[e['rotation']['angle'],0,0],'origin':e['rotation']['origin']}if 'rotation'in e else{})});return uid
 for i,e in enumerate(mesh+lamps):outline.append(bb(e,'body_'+str(i)))
 animators={}
 if name=='fare_gate':
  x=16;gid=str(uuid.uuid5(uuid.NAMESPACE_URL,name+'swing'));label='gate_leaf'
  outline.append({'name':label,'uuid':gid,'origin':[x,0,8],'children':[bb(transformed(e,x),label+str(i))for i,e in enumerate(gate_leaf())]})
  animators[gid]={'name':label,'type':'bone','keyframes':[{'channel':'rotation','time':t,'data_points':[{'x':0,'y':0 if t==0 else -90,'z':0}],'interpolation':'linear'}for t in [0,.5]]}
 js(ROOT/f'preview/{name}.bbmodel',{'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':name,'resolution':{'width':32,'height':32},'elements':elements,'outliner':outline,'textures':[{'path':'../src/main/resources/assets/createtransit/textures/block/'+n+'.png','name':n+'.png','id':str(i)}for i,n in enumerate(names)],'animations':[{'name':'open','length':.5,'loop':'once','animators':animators}]if animators else[]})
lines=['package com.sanctuarygames.createtransit.client;','/** Generated from tools/station_geometry.py. */','final class StationMesh {','record Cube(double x1,double y1,double z1,double x2,double y2,double z2,String texture,int tint,int faces) {}']
for name,mesh in [('GATE_LIGHTS',gate_lights()),('KIOSK_LIGHTS',kiosk_lights()),('LEAF',gate_leaf())]:
 lines.append('static final Cube[] '+name+' = {')
 for e in mesh:lines.append('new Cube('+','.join(str(v)for v in e['from']+e['to'])+',"'+e['texture']+'",'+str(e['tint'])+','+str(sum(1<<FACES.index(f)for f in e['visible']))+'),')
 lines.append('};')
lines.append('}');(ROOT/'src/main/java/com/sanctuarygames/createtransit/client/StationMesh.java').write_text('\n'.join(lines)+'\n')
items=A/'textures/item';items.mkdir(exist_ok=True)
for name,ispass in [('rp_ticket',False),('rp_pass',True)]:
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im);d.rectangle((1,4,14,12),fill=(52,110,183)if ispass else(235,231,209),outline=(170,220,250)if ispass else(85,92,96));d.rectangle((2,5,13,6),fill=(180,230,250)if ispass else(48,120,193));
 if ispass:d.rectangle((3,8,6,10),fill=(216,188,90));d.line((8,9,12,9),fill=(210,239,255))
 else:
  for x in range(3,13,2):d.line((x,8,x,11),fill=(49,62,74))
 im.save(items/(name+'.png'));js(A/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'createtransit:item/'+name}})
lang=json.loads((A/'lang/en_us.json').read_text());lang.update({'itemGroup.createtransit.fares':'Create Transit — Fares','block.createtransit.fare_gate':'Automatic One-Way Fare Gate','block.createtransit.fare_reader':'Decorative Fare Reader','tooltip.createtransit.fare_reader':'1-wide end post for a fare-gate row. Independently dyeable.','block.createtransit.ticket_kiosk':'Ticket / Pass Kiosk','item.createtransit.rp_ticket':'RP Transit Ticket','item.createtransit.rp_pass':'RP Transit Pass','subtitles.createtransit.fare_chime':'Fare gate welcome chime','tooltip.createtransit.fare_gate':'2 wide: post + gate. Crouch-place mirrors the passage to your right at the green entrance.','tooltip.createtransit.ticket_kiosk':'1 wide, 2 high. Right-click for free RP tickets and passes.','tooltip.createtransit.rp_fare':'Roleplay keepsake. Automatic gates do not require or consume this.','gui.createtransit.title':'CREATE TRANSIT','gui.createtransit.ticket':'Print Ticket','gui.createtransit.pass':'Issue Pass','gui.createtransit.choose':'Choose your ticket or pass','gui.createtransit.free':'Free tickets and passes','gui.createtransit.rp':'Roleplay only • No payment required','gui.createtransit.dispensing':'Dispensing...','gui.createtransit.received':'Added to your inventory'});js(A/'lang/en_us.json',lang)
sounds=json.loads((A/'sounds.json').read_text());sounds['fare_gate_chime']={'subtitle':'subtitles.createtransit.fare_chime','sounds':[{'name':'createtransit:fare_gate_chime','volume':.8}]};js(A/'sounds.json',sounds)
# Short two-note sampled piano chime: C5 / E5, below the platform warning.
rate=44100;mix=np.zeros(int(rate*.65))
for file,semitones,start in [('piano_cs5.wav',-1,0),('piano_f5.wav',-1,.16)]:
 pcm=subprocess.check_output(['ffmpeg','-v','error','-i',str(ROOT/'tools/audio'/file),'-f','f32le','-ac','1','-ar',str(rate),'-']);data=np.frombuffer(pcm,dtype='<f4');n=int(rate*.48)
 note=np.interp(np.arange(n)*2**(semitones/12),np.arange(len(data)),data,left=0,right=0);note*=.3/max(.001,np.max(np.abs(note)));fade=int(rate*.12);note[-fade:]*=np.linspace(1,0,fade);offset=int(start*rate);length=min(n,len(mix)-offset);mix[offset:offset+length]+=note[:length]
wav=ROOT/'preview/fare-chime.wav'
with wave.open(str(wav),'wb')as w:w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes((np.clip(mix,-1,1)*32767).astype('<i2').tobytes())
subprocess.run(['ffmpeg','-y','-loglevel','error','-i',str(wav),'-c:a','libvorbis','-q:a','5',str(A/'sounds/fare_gate_chime.ogg')],check=True)
print('Generated fare gates, kiosks, fixed direction signals, two dye channels, labPBR, RP items, Blockbench models and medium-pitch piano chime.')

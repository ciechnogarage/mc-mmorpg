"""Build the editable race_nieumarly v4 art asset (no live server writes)."""
from pathlib import Path
from PIL import Image, ImageDraw
import math, json, uuid, base64, io, random
OUT=Path('/opt/docker/mc/data/world1')  # serwer (narzędzie przeniesione z world1/tools do repo MC 22.10)/'output/undead-player-v4'
OUT.mkdir(parents=True,exist_ok=True)
def uid(s): return str(uuid.uuid5(uuid.NAMESPACE_URL,'mcmmorpg/undead/v1/'+s))
groups=[]; nodes={}; roots=[]; cubes=[]; spec=[]
def group(name,origin,parent=None):
 g={'name':name,'uuid':uid(name),'origin':origin,'rotation':[0,0,0],'export':True,'visibility':True,'isOpen':True,'autouv':0,'shade':True,'children':[]};groups.append(g)
 n={'uuid':g['uuid'],'isOpen':True,'children':[]};nodes[name]=n
 (nodes[parent]['children'] if parent else roots).append(n)
 return name
root=group('root',[0,0,0]); torso=group('torso',[0,12,0],root); head=group('hi_head',[0,24,-.8],torso)
next(g for g in groups if g['name']=='torso')['rotation']=[-7,0,-2]
next(g for g in groups if g['name']=='hi_head')['rotation']=[5,0,3]
for side,x in [('right',5.5),('left',-5.5)]:
 group(side+'_arm',[x,22.5,0],torso);group(side+'_forearm',[x,18.5,0],side+'_arm')
 group(side+'_leg',[1.8 if x>0 else -1.8,12,0],root);group(side+'_shin',[1.8 if x>0 else -1.8,6.5,0],side+'_leg')
 group(side+'_foot',[1.8 if x>0 else -1.8,1.5,0],side+'_shin')
group('ir_mainhand',[5.5,12.5,-.6],'right_forearm');group('il_offhand',[-5.5,12.5,-.6],'left_forearm')
def cube(name,a,b,mat,bone,rot=None,origin=None,paint=None):
 c={'name':name,'uuid':uid('cube/'+name),'type':'cube','from':a,'to':b,'origin':origin or [0,0,0],'rotation':rot or [0,0,0],'box_uv':False,'autouv':0,'shade':True,'visibility':True,'export':True,'faces':{}}
 cubes.append(c);nodes[bone]['children'].append(c['uuid']);spec.append((c,mat,paint))
# Skull, jaw and silhouette. Forward is -Z.
cube('cervical_spine',[-1.05,22.7,-.9],[1.05,25.1,1.1],'bone',head)
cube('skull',[-3.5,25.9,-3.2],[3.5,31.8,3.2],'skull',head,paint='skull')
cube('jaw',[-2.25,24.3,-3.0],[2.35,25.45,1.4],'bone',head,paint='jaw')
for side,x in [('r',2.8),('l',-2.8)]:
 cube('cheek_'+side,[x-.55,25.5,-3.5],[x+.55,27.1 if x>0 else 26.8,-2.3],'bone',head)
# Recessed sockets are drawn on skull; little inset-looking eyes stand just above surface.
for name,x in [('r',1.8),('l',-1.8)]:
 cube('soul_eye_'+name,[x-.45,28.0,-3.24],[x+.45,28.6 if x>0 else 28.4,-3.20],'soul',head)
 cube('brow_'+name,[x-1.15,29.05 if x>0 else 29.3,-3.4],[x+1.15,29.45 if x>0 else 29.65,-2.8],'bone',head)
for i,x in enumerate([-1.7,-.7,1.5]):cube('tooth_'+str(i),[x-.30,25.1+(i%2)*.25,-3.28],[x+.30,25.95,-2.8],'bone',head)
cube('temple_remnant',[-3.58,29.9,-1.6],[-3.48,31.85,2.6],'skin',head)
# Hollow-looking chest with actual raised ribs; cloth covers back and one side.
cube('chest_shadow',[-3.1,16.0,-1.5],[3.1,23.3,1.7],'dark',torso)
cube('back_tunic',[-3.6,13.1,1.05],[3.6,23.2,2.25],'cloth',torso,paint='back')
cube('tunic_left_panel',[-3.9,14,-2.25],[-1.6,23.5,2],'cloth',torso,paint='chest_cloth')
cube('collar_left',[-4.2,22.0,-2.2],[-1.9,24.8,2.2],'cloth',torso,rot=[0,0,22.5],origin=[-3,23,0])
cube('sternum',[-.4,18.3,-2.15],[.4,22.3,-1.4],'bone',torso)
# Ribs descend from the sternum, then wrap into the side of the chest.
for row,(y,length) in enumerate([(19.15,1.65),(21.1,2.5)]):
 cube('rib_r_'+str(row),[.25,y,-2.4],[length,y+.55,-1.7],'bone',torso,rot=[0,0,-22.5],origin=[.25,y,-2])
 cube('rib_side_'+str(row),[length-.35,y-.7,-1.8],[length+.25,y-.1,.25],'bone',torso)
 cube('rib_l_'+str(row),[-1.8,y-.35,-2.4],[-.25,y+.2,-1.7],'bone',torso,rot=[0,0,22.5],origin=[-.25,y,-2])
cube('rib_lower_stump',[.05,17.8,-2.05],[1.05,18.35,-1.55],'bone',torso,rot=[0,0,-22.5],origin=[.05,18,-1.8])
cube('waist_wrap',[-3.55,12,-2.1],[3.55,16.5,2.1],'cloth',torso)
cube('belt',[-3.85,13.2,-2.4],[3.85,14.7,2.4],'leather',torso)
cube('buckle',[-.9,13.0,-2.65],[.9,14.85,-2.35],'bronze',torso,paint='buckle')
cube('sash',[-2.9,15.1,-2.48],[3.4,16.25,-2.13],'red',torso,rot=[0,0,-22.5],origin=[0,15.5,-2.2])
# One broad, stepped shoulder shroud gives the upper body a distinct silhouette.
cube('mantle_back',[-5.0,19.6,1.9],[1.0,24.0,3.25],'cloth',torso,paint='mantle')
cube('mantle_torn_tab',[-4.7,18.2,2.0],[-2.8,19.7,3.05],'cloth',torso)
cube('mantle_fold',[-5.3,21.2,-1.8],[-3.6,24.0,2.65],'cloth',torso,rot=[0,0,22.5],origin=[-4,23,0])
cube('mantle_clasp',[-3.1,21.5,-2.55],[-1.9,22.7,-2.2],'bronze',torso,paint='buckle')
# Two broad tails and a missing corner: clear negative space, no strip skirt.
cube('front_long_tail',[-3.75,7.1,-2.25],[-.55,13.2,-1.65],'cloth','left_leg',paint='tail')
cube('front_long_tip',[-3.75,6.1,-2.25],[-2.05,7.15,-1.65],'cloth','left_leg')
cube('front_short_tail',[.7,10.1,-2.25],[3.65,13.2,-1.65],'cloth','right_leg',paint='tail')
cube('back_long_tail',[-3.7,6.6,1.7],[-.35,13.2,2.35],'cloth','left_leg',paint='tail')
cube('back_long_tip',[-2.7,5.8,1.7],[-.35,6.7,2.35],'cloth','left_leg')
cube('back_short_tail',[.45,9.3,1.7],[3.65,13.2,2.35],'cloth','right_leg',paint='tail')
cube('sash_tail',[-1.65,8.9,-2.65],[-.55,13.2,-2.3],'red','left_leg')
# Asymmetry: one clothed arm, one exposed skeletal arm.
cube('right_humerus',[4.7,18.4,-.7],[6.3,23.3,.8],'bone','right_arm')
cube('right_shoulder_rag',[3.95,22.6,-1.35],[6.9,23.5,1.35],'cloth','right_arm')
cube('right_elbow',[4.55,17.7,-.9],[6.45,19.1,.95],'bone','right_forearm')
for i,x in enumerate([4.55,5.7]):cube('right_forearm_bone_'+str(i),[x,13.6,-.6],[x+.65,18.3,.65],'bone','right_forearm')
cube('right_hand',[4.5,12.4,-.95],[6.35,14.2,.95],'bone','right_forearm',paint='fingers')
cube('right_thumb',[4.05,13.2,-1.05],[4.65,14.0,.15],'bone','right_forearm')
cube('left_sleeve',[-7.1,18.5,-1.65],[-4.0,23.8,1.65],'cloth','left_arm')
cube('left_forearm_skin',[-6.55,13.4,-1.1],[-4.55,18.5,1.1],'skin','left_forearm')
cube('left_hand',[-6.55,12.2,-1.0],[-4.55,14.2,1.0],'skin','left_forearm',paint='fingers')
cube('left_wrap',[-6.68,15,-1.25],[-4.42,17.8,1.25],'wrap','left_forearm',paint='bandage')
cube('left_cuff',[-7.2,18.6,-1.75],[-3.9,19.4,1.75],'wrap','left_arm')
# Legs stay close to standard player proportions; boots can plant on y=0.
for side,x in [('right',1.85),('left',-1.85)]:
 cube(side+'_trousers',[x-1.45,8.0,-1.5],[x+1.45,12.2,1.5],'trousers',side+'_leg')
 cube(side+'_lower_femur',[x-.7,6.2,-.7],[x+.7,8.2,.8],'bone',side+'_leg')
 cube(side+'_knee',[x-.78,5.9,-.95],[x+.78,7.0,.95],'bone',side+'_shin')
 cube(side+'_tibia',[x-.65,1.7,-.6],[x+.65,6.3,.7],'bone',side+'_shin')
 cube(side+'_boot',[x-1.55,0,-2.2],[x+1.55,2.3,1.55],'leather',side+'_foot')
 cube(side+'_sole',[x-1.6,0,-2.25],[x+1.6,.55,1.6],'dark',side+'_foot')
 if side=='left':cube('left_shin_gaiter',[x-1.0,2,-1.1],[x+1.0,5.2,1.1],'leather',side+'_shin',paint='gaiter')
 else: cube('right_ankle_wrap',[x-.82,2.25,-.82],[x+.82,3.6,.85],'wrap',side+'_shin',paint='bandage')
# Forward-set skull and jaw make the side silhouette hunched.
for c,mat,decor in spec:
 if c['uuid'] in nodes[head]['children']:
  for field in ['from','to','origin']:c[field][2]-=.9
# Texture atlas: deliberately authored pixel clusters, directional edges, stitches and wear.
COLORS={'bone':(174,171,139),'skull':(153,162,137),'skin':(97,119,107),'cloth':(49,64,76),'trousers':(43,49,56),'dark':(30,37,41),'wrap':(142,133,108),'leather':(66,48,41),'bronze':(153,112,55),'red':(108,48,52),'soul':(71,222,194)}
atlas=Image.new('RGBA',(256,256),(0,0,0,0)); pen=ImageDraw.Draw(atlas); px=1;py=1;rowh=0

def tile(w,h,mat,key,face,decor):
 global px,py,rowh
 if px+w+1>256:px=1;py+=rowh+2;rowh=0
 assert py+h+1<=256,(py,h)
 x0,y0=px,py;px+=w+2;rowh=max(rowh,h)
 rng=random.Random(key+'/'+face);base=COLORS[mat];shade={'north':1.03,'south':.89,'east':.93,'west':.97,'up':1.10,'down':.76}[face]
 for y in range(h):
  for x in range(w):
   v=0
   # Sparse connected mottling, not white noise.
   cluster=((x//3)*19+(y//3)*31+sum(map(ord,key)))%17
   if mat in ['skin','bone','skull']:
    if cluster<2:v=-5
    elif cluster==8:v=3
   if mat in ['cloth','trousers','red']:
    # Broad tension planes, with a seam only on the actual folded edge.
    v=3 if x<w*.55 and y<h*.45 else -3
    if x>=w*.8:v-=12
    if y>h*.8:v-=10
    if y==0:v+=9
   elif mat in ['bone','skull','skin']:
    if x==0 or x==w-1:v-=8
    if y==0:v+=8
    if y>h*.75:v-=5
   elif mat=='wrap':
    if (y+x//3)%5==4:v-=14
    if (y+x//3)%5==0:v+=9
   elif mat=='leather':
    v+=7 if y==0 else (-14 if y>=h-2 else 0)
    if x==w//2 and y<h//2:v+=5
   elif mat=='bronze':v+=14 if x==0 or y==0 else (-18 if x==w-1 or y==h-1 else 0)
   elif mat=='soul':v=20 if x==w//2 else -12
   atlas.putpixel((x0+x,y0+y),tuple(max(0,min(255,round(c*shade+v))) for c in base)+(255,))
 d=ImageDraw.Draw(atlas)
 if decor=='tail' and face in ['north','south']:
  # A diagonal fold from belt tension, then a worn, irregular lower edge.
  d.polygon([(x0,y0),(x0+2,y0),(x0+w-2,y0+h-3),(x0+w-1,y0+h-1),(x0+w-3,y0+h-2)],fill=(34,46,57,255))
  d.line([(x0+2,y0+1),(x0+w-2,y0+h-3)],fill=(68,82,87,255))
  for x in range(w):
   if x%3!=1:d.point((x0+x,y0+h-1-(x%2)),fill=(92,95,80,255))
  if w>5:
   d.line((x0+1,y0+h-4,x0+2,y0+h-3),fill=(23,31,35,255))
 if decor=='chest_cloth' and face=='north':
  d.line([(x0+w-2,y0+3),(x0+w-2,y0+8),(x0+w-3,y0+10)],fill=(104,110,94,255))
  for y in [6,8,10]:d.point((x0+w-1,y0+y),fill=(160,141,107,255))
 if decor=='back' and face=='south':
  # Faded burial mark: broken ring and descending line, specific to this garment.
  motif=['..###..','.##.##.','##...#.','#..#...','#..#..#','.##.##.','..###..','...#...','..##...']
  ox=x0+4;oy=y0+8
  for yy,line in enumerate(motif):
   for xx,v in enumerate(line):
    if v=='#':d.point((ox+xx,oy+yy),fill=(115,112,86,255) if (xx+yy)%4 else (75,85,76,255))
  d.line([(x0+2,y0+9),(x0+2,y0+h-4),(x0+3,y0+h-2)],fill=(34,46,56,255))
 if decor=='mantle' and face=='south':
  d.polygon([(x0,y0),(x0+w-1,y0),(x0+w-1,y0+2),(x0+3,y0+3)],fill=(62,77,83,255))
  d.line([(x0,y0+h-2),(x0+4,y0+h-2),(x0+5,y0+h-3),(x0+w-2,y0+h-3)],fill=(123,116,87,255))
  d.line([(x0+2,y0+2),(x0+4,y0+3),(x0+5,y0+h-4)],fill=(24,34,45,255))
 if decor=='fingers' and face in ['north','south']:
  for x in range(1,w-1,2):d.line((x0+x,y0+h//2,x0+x,y0+h-1),fill=(73,83,69,255))
 if decor=='gaiter' and face=='north':
  d.line((x0+1,y0,x0+1,y0+h-1),fill=(109,79,55,255))
  for y in range(1,h,2):d.point((x0+2,y0+y),fill=(142,119,81,255))
 if decor=='skull':
  if face=='north':
   # Skull front (14x12): dark orbital cavities, sunken nose and temple cracks.
   for a,b in [(2,5),(8,11)]:
    d.rectangle((x0+a,y0+4,x0+b,y0+7),fill=(38,53,51,255))
    d.line((x0+a,y0+3,x0+b,y0+3),fill=(91,113,95,255))
   d.polygon([(x0+6,y0+7),(x0+7,y0+7),(x0+8,y0+9),(x0+6,y0+9)],fill=(40,49,44,255))
   d.line((x0+4,y0+11,x0+9,y0+11),fill=(42,51,46,255))
   d.line((x0+1,y0+8,x0+2,y0+9),fill=(185,183,150,255))
   d.point((x0+11,y0+9),fill=(185,183,150,255))
   d.line([(x0+8,y0),(x0+8,y0+1),(x0+7,y0+1),(x0+7,y0+3)],fill=(75,91,76,255),width=1)
   d.line([(x0+1,y0+9),(x0+3,y0+9),(x0+3,y0+10)],fill=(104,121,98,255))
  elif face in ['east','west','south']:
   d.line([(x0+w//2,y0+1),(x0+w//2,y0+3),(x0+w//2+1,y0+3),(x0+w//2+1,y0+5)],fill=(88,104,87,255))
 if decor=='jaw' and face=='north':
  d.line((x0,y0,x0+w-1,y0),fill=(60,68,56,255))
 if decor=='buckle' and face=='north' and w>2 and h>2:
  d.rectangle((x0+1,y0+1,x0+w-2,y0+h-2),fill=(62,48,38,255))
 return [x0,y0,x0+w,y0+h]
for c,mat,decor in spec:
 dx,dy,dz=[b-a for a,b in zip(c['from'],c['to'])]
 for face in ['north','east','south','west','up','down']:
  a,b=(dx,dy) if face in ['north','south'] else ((dz,dy) if face in ['east','west'] else (dx,dz))
  w,h=max(1,math.ceil(a*2)),max(1,math.ceil(b*2))
  c['faces'][face]={'uv':tile(w,h,mat,c['name'],face,decor),'texture':0}
# ModelEngine hitbox metadata bone. Kept invisible in editor, exported for parser.
group('hitbox',[0,28.35,0]);cube('player_hitbox',[-4.8,0,-4.8],[4.8,28.8,4.8],'dark','hitbox')
cubes[-1]['visibility']=False;cubes[-1]['faces']={f:{'uv':[0,0,1,1],'texture':None} for f in ['north','east','south','west','up','down']}
next(g for g in groups if g['name']=='hitbox')['visibility']=False
atlas.save(OUT/'race_nieumarly.png');buf=io.BytesIO();atlas.save(buf,format='PNG')
animations=[]
def anim(name,length,tracks,loop='loop',override=False):
 a={'uuid':uid('anim/'+name),'name':name,'loop':loop,'override':override,'length':length,'snapping':20,'animators':{}}
 for bone,channels in tracks.items():
  keys=[]
  for ch,frames in channels.items():
   for t,v in frames:
    keys.append({'uuid':uid(f'{name}/{bone}/{ch}/{t}'),'channel':ch,'time':t,'interpolation':'linear','data_points':[dict(zip(['x','y','z'],map(str,v)))]})
  a['animators'][uid(bone)]={'name':bone,'type':'bone','keyframes':keys}
 animations.append(a)
def rot(frames):return {'rotation':frames}
anim('idle',3,{'torso':rot([(0,[2,0,0]),(1.5,[3,0,0]),(3,[2,0,0])]),'right_arm':rot([(0,[0,0,-3]),(1.5,[2,0,-4]),(3,[0,0,-3])]),'left_arm':rot([(0,[0,0,3]),(1.5,[-2,0,4]),(3,[0,0,3])])})
# Bake a two-link leg solve at Minecraft tick resolution. Stance feet stay flat;
# forward travel is provided by the player, not by translating the model root.
walk={'root':{'position':[]},'torso':rot([])}
for side in ['right','left']:
 for part in ['leg','shin','foot','arm','forearm']:walk[side+'_'+part]=rot([])
for frame in range(21):
 t=frame/20; bob=-.65-.2*math.cos(4*math.pi*t)
 walk['root']['position'].append((t,[0,round(bob,5),0]))
 walk['torso']['rotation'].append((t,[3,round(3*math.sin(2*math.pi*t),4),round(1.4*math.sin(2*math.pi*t),4)]))
 for side,offset in [('right',0),('left',.5)]:
  phase=(t+offset)%1
  if phase<.5: z=-3+12*phase; lift=0
  else:
   u=(phase-.5)*2; z=3-6*(u*u*(3-2*u)); lift=1.8*math.sin(math.pi*u)**2
  down=10.5+bob-lift; radius=math.hypot(down,z)
  hip=math.degrees(math.atan2(-z,down)+math.acos(max(-1,min(1,(5.5**2+radius**2-5**2)/(11*radius)))))
  knee=-math.degrees(math.acos(max(-1,min(1,(radius**2-5.5**2-5**2)/(55)))))
  for part,angle in [('leg',hip),('shin',knee),('foot',-hip-knee)]:
   walk[side+'_'+part]['rotation'].append((t,[round(angle,5),0,0]))
  swing=math.cos(2*math.pi*phase)
  walk[side+'_arm']['rotation'].append((t,[-18*swing,0,-4 if side=='right' else 4]))
  walk[side+'_forearm']['rotation'].append((t,[7+4*swing,0,0]))
anim('walk',1,walk)
anim('jump_start',.2,{
 'root':{'position':[(0,[0,0,0]),(.1,[0,-.8,0]),(.2,[0,0,0])]},
 'torso':rot([(0,[2,0,0]),(.1,[10,0,0]),(.2,[0,0,0])]),
 'right_arm':rot([(0,[0,0,-3]),(.1,[-18,0,-8]),(.2,[20,0,-10])]),
 'left_arm':rot([(0,[0,0,3]),(.1,[-18,0,8]),(.2,[20,0,10])])},'once',True)
anim('jump',.6,{
 'torso':rot([(0,[0,0,0]),(.3,[3,0,0]),(.6,[0,0,0])]),
 'right_leg':rot([(0,[22,0,0]),(.3,[28,0,0]),(.6,[22,0,0])]),
 'right_shin':rot([(0,[-35,0,0]),(.3,[-42,0,0]),(.6,[-35,0,0])]),
 'left_leg':rot([(0,[10,0,0]),(.3,[15,0,0]),(.6,[10,0,0])]),
 'left_shin':rot([(0,[-25,0,0]),(.3,[-30,0,0]),(.6,[-25,0,0])]),
 'right_arm':rot([(0,[20,0,-10]),(.3,[12,0,-14]),(.6,[20,0,-10])]),
 'left_arm':rot([(0,[20,0,10]),(.3,[12,0,14]),(.6,[20,0,10])])},override=True)
anim('jump_end',.3,{
 'root':{'position':[(0,[0,0,0]),(.1,[0,-.7,0]),(.3,[0,0,0])]},
 'torso':rot([(0,[0,0,0]),(.1,[9,0,0]),(.3,[2,0,0])]),
 'right_leg':rot([(0,[0,0,0]),(.1,[18,0,0]),(.3,[0,0,0])]),
 'left_leg':rot([(0,[0,0,0]),(.1,[18,0,0]),(.3,[0,0,0])]),
 'right_shin':rot([(0,[0,0,0]),(.1,[-36,0,0]),(.3,[0,0,0])]),
 'left_shin':rot([(0,[0,0,0]),(.1,[-36,0,0]),(.3,[0,0,0])]),
 'right_foot':rot([(0,[0,0,0]),(.1,[18,0,0]),(.3,[0,0,0])]),
 'left_foot':rot([(0,[0,0,0]),(.1,[18,0,0]),(.3,[0,0,0])])},'once',True)
# Wind-up, strike, follow-through and recovery involve the chest and both arms.
anim('attack',.6,{
 'root':{'position':[(0,[0,0,0]),(.15,[.35,-.35,.25]),(.25,[-.25,-.6,-.35]),(.35,[-.15,-.4,-.25]),(.6,[0,0,0])]},
 'right_leg':rot([(0,[0,0,0]),(.15,[10,0,0]),(.25,[20,0,0]),(.35,[14,0,0]),(.6,[0,0,0])]),
 'right_shin':rot([(0,[0,0,0]),(.15,[-20,0,0]),(.25,[-40,0,0]),(.35,[-28,0,0]),(.6,[0,0,0])]),
 'right_foot':rot([(0,[0,0,0]),(.15,[10,0,0]),(.25,[20,0,0]),(.35,[14,0,0]),(.6,[0,0,0])]),
 'left_leg':rot([(0,[0,0,0]),(.15,[14,0,0]),(.25,[10,0,0]),(.35,[10,0,0]),(.6,[0,0,0])]),
 'left_shin':rot([(0,[0,0,0]),(.15,[-28,0,0]),(.25,[-20,0,0]),(.35,[-20,0,0]),(.6,[0,0,0])]),
 'left_foot':rot([(0,[0,0,0]),(.15,[14,0,0]),(.25,[10,0,0]),(.35,[10,0,0]),(.6,[0,0,0])]),
 'torso':rot([(0,[2,0,0]),(.15,[-4,-18,-4]),(.25,[9,14,3]),(.35,[7,20,2]),(.6,[2,0,0])]),
 'right_arm':rot([(0,[0,0,-3]),(.15,[110,-15,-22]),(.25,[55,10,-8]),(.35,[15,15,-5]),(.6,[0,0,-3])]),
 'right_forearm':rot([(0,[0,0,0]),(.15,[35,0,0]),(.25,[8,0,0]),(.35,[3,0,0]),(.6,[0,0,0])]),
 'left_arm':rot([(0,[0,0,3]),(.15,[12,0,15]),(.25,[-15,0,12]),(.6,[0,0,3])])},'once')
# Bake the attack support pose with both soles on the ground. The modest step
# opens the stance during the wind-up; no vertical foot penetration is intended.
attack=next(a for a in animations if a['name']=='attack')
root_keys=attack['animators'][uid('root')]['keyframes']
def sample_root(t):
 a=max((k for k in root_keys if k['time']<=t+1e-8),key=lambda k:k['time'])
 b=min((k for k in root_keys if k['time']>=t-1e-8),key=lambda k:k['time'])
 u=(t-a['time'])/(b['time']-a['time']) if b['time']>a['time'] else 0
 return [float(a['data_points'][0][ax])*(1-u)+float(b['data_points'][0][ax])*u for ax in 'xyz']
for k in root_keys:k['data_points'][0]['x']='0'
for side,sign in [('right',-1),('left',1)]:
 tracks={part:[] for part in ['leg','shin','foot']}
 for frame in range(13):
  t=frame/20; _,dy,dz=sample_root(t)
  target_z=sign*.8*math.sin(math.pi*t/.6); z=target_z-dz
  down=10.5+dy;r=math.hypot(down,z)
  hip=math.degrees(math.atan2(-z,down)+math.acos(max(-1,min(1,(5.5**2+r*r-25)/(11*r)))))
  knee=-math.degrees(math.acos(max(-1,min(1,(r*r-5.5**2-25)/55))))
  for part,angle in [('leg',hip),('shin',knee),('foot',-hip-knee)]:
   tracks[part].append({'uuid':uid(f'attack/{side}/{part}/{frame}'),'channel':'rotation','time':t,'interpolation':'linear','data_points':[{'x':str(round(angle,5)),'y':'0','z':'0'}]})
 for part,keys in tracks.items():attack['animators'][uid(side+'_'+part)]['keyframes']=keys
# Damage is a short upper-body reaction, intended to layer over locomotion.
# The integration must trigger hurt explicitly on a damage event.
anim('hurt',.35,{
 'torso':rot([(0,[2,0,0]),(.05,[-9,-7,-5]),(.15,[-5,-3,-2]),(.35,[2,0,0])]),
 'right_arm':rot([(0,[0,0,-3]),(.05,[-15,0,-12]),(.15,[-7,0,-7]),(.35,[0,0,-3])]),
 'left_arm':rot([(0,[0,0,3]),(.05,[-10,0,10]),(.15,[-4,0,6]),(.35,[0,0,3])])},'once')
# Collapse forward, settle and hold the final pose. ModelEngine uses death,
# not dead, for its default death state. Actual player death timing needs a test.
anim('death',1.2,{
 'root':{'rotation':[(0,[0,0,0]),(.2,[-8,0,0]),(.45,[-40,0,0]),(.7,[-82,0,0]),(.85,[-90,0,0]),(1.0,[-87,0,0]),(1.2,[-90,0,0])],
         'position':[(0,[0,0,0]),(.2,[0,0,0]),(.45,[0,1,0]),(.7,[0,4,0]),(.85,[0,6,0]),(1.0,[0,5.6,0]),(1.2,[0,6,0])]},
 'torso':rot([(0,[2,0,0]),(.2,[-10,0,0]),(.45,[-8,0,0]),(.7,[5,0,0]),(.85,[7,0,0]),(1.2,[7,0,0])]),
 'right_arm':rot([(0,[0,0,-3]),(.2,[22,0,-12]),(.45,[55,0,-18]),(.7,[35,0,-20]),(.85,[8,0,-18]),(1.2,[8,0,-18])]),
 'left_arm':rot([(0,[0,0,3]),(.2,[15,0,10]),(.45,[48,0,16]),(.7,[30,0,18]),(.85,[4,0,15]),(1.2,[4,0,15])]),
 'right_forearm':rot([(0,[0,0,0]),(.45,[25,0,0]),(.85,[12,0,0]),(1.2,[12,0,0])]),
 'left_forearm':rot([(0,[0,0,0]),(.45,[20,0,0]),(.85,[8,0,0]),(1.2,[8,0,0])])},'hold',True)
anim('spawn',.8,{
 'torso':rot([(0,[-14,0,-4]),(.3,[-8,0,-2]),(.6,[4,0,0]),(.8,[2,0,0])]),
 'right_arm':rot([(0,[12,0,-5]),(.3,[6,0,-3]),(.8,[0,0,-3])]),
 'left_arm':rot([(0,[8,0,5]),(.3,[4,0,3]),(.8,[0,0,3])])},'once',True)
# Ground offsets sampled from the final mesh in Blockbench at 20 Hz.
death_y=[0.0, 0.078524, 0.156952, 0.235189, 0.313139, 0.559552, 0.798991, 1.02847, 1.245131, 1.446272, 1.682546, 1.88272, 2.042499, 3.642976, 5.538999, 5.155511, 4.46593, 4.255567, 3.805659, 3.354592, 2.902503, 3.24166, 3.580262, 3.91825, 4.255567]
death=next(a for a in animations if a['name']=='death')
track=death['animators'][uid('root')]
track['keyframes']=[k for k in track['keyframes'] if k['channel']!='position']
for frame,y in enumerate(death_y):
 track['keyframes'].append({'uuid':uid(f'death/ground/{frame}'),'channel':'position','time':frame/20,'interpolation':'linear','data_points':[{'x':'0','y':str(y),'z':'0'}]})
model={'meta':{'format_version':'5.0','model_format':'free','box_uv':False},'name':'race_nieumarly','model_identifier':'race_nieumarly','resolution':{'width':256,'height':256},'elements':cubes,'groups':groups,'outliner':roots,'textures':[{'name':'race_nieumarly.png','namespace':'','folder':'','render_mode':'default','render_sides':'auto','frame_time':1,'frame_order_type':'loop','frame_order':'','frame_interpolate':False,'visible':True,'internal':True,'uuid':uid('texture'),'id':'0','width':256,'height':256,'uv_width':256,'uv_height':256,'mode':'bitmap','saved':False,'source':'data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()}],'animations':animations}
(OUT/'race_nieumarly.bbmodel').write_text(json.dumps(model,indent=2))
print(f'{len(cubes)-1} visible cubes, {len(groups)} bones, {len(animations)} animations; atlas used to row {py+rowh}/256')

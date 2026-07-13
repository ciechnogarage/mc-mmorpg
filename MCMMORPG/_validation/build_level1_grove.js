// Build lochu 1 "Kwietna Polana" -> scena "Drzewo-Serce Gaju" (v2: majestat).
// Flow (zwalidowany, memory mcmmorpg-md-template-mechanics):
//   bot join -> op -> /md edit level_1 (swiat-instancja edycji) -> komendy FAWE z
//   ABSOLUTNYMI wspolrzednymi (edit-mode zamraza tp bota -> zero operacji wzgl. pozycji
//   gracza: brak //brush; tylko //pos1 x,y,z + //pos2 + //set/replace/generate)
//   -> /md leave (zapis do template maps/level_1/).
// Orientacja: +z = poludnie = wejscie gracza (start 0,64,10). -z = polnoc = za bossem.
// NIE ruszac: 0,64,0 (spawn bossa), 0,64,10 (start), 0,64,3 (finish). Strefa walki r~12 @ 0,0
//   ma zostac otwarta/chodliwa (safety-clear na koncu).
// Parametryzacja: edytuj stale w SEKCJI PARAMS. Jeden skrypt, iterujemy tu (token-discipline).
const mineflayer = require('mineflayer');
const { Rcon } = require('rcon-client');
const HOST='127.0.0.1', PORT=25565, RPORT=25575, RPASS=process.env.RCON_PASS, USER='FableBuilder'; // unikalna nazwa: rownolegle sesje codex uzywaja 'Builder' -> duplicate_login kick
// INFRA (2026-07-06): bot laczy przez velocity (127.0.0.1:25565) -> ląduje na srv-items (try=[items,world,hub])
// -> musi /server world. Direct do srv-world odrzucony ("requires Velocity"). RCON tylko wewnetrznie:
// srv-world = kontener na mcmmorpg_net, RCON pod jego IP. Ustaw RHOST=IP srv-world (docker inspect).
const RHOST=process.env.RHOST||'172.18.0.3';   // srv-world container IP (mcmmorpg_net)
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const waitFor=async(fn,to=30000,st=300)=>{const t=Date.now();while(Date.now()-t<to){if(await fn())return true;await sleep(st);}return false;};

// ===================== PARAMS (iteruj tutaj) =====================
const R_ISLE    = 62;   // promien wyspy (fundament + trawa) — powiekszona (v4: rozmach)
const ISLE_CZ   = -18;  // srodek wyspy przesuniety NA POLNOC: rosnie N/E/W, pld krawedz zostaje z=ISLE_CZ+R_ISLE=44 (most z46 nietkniety)
const R_CLEAR   = R_ISLE+4; // promien czyszczenia nad ziemia (podaza za wyspa)
const Y_BASE    = 58;   // dol fundamentu (dirt)
const Y_TOP     = 64;   // powierzchnia (grass) = poziom triggerow
const Y_SKY     = 126;  // do jakiej wys. czyscic nad ziemia
const TREE_CX=0, TREE_CZ=-24; // srodek Drzewa-Serca (polnoc, za bossem) — glebiej w powiekszonej polnocy
const COMBAT_R  = 16;   // promien czystej strefy walki wokol 0,0 (powiekszona arena)
const RING_R    = 52;   // promien pierscienia wzgorz/drzew (hugujе nowy obrys, srodek ISLE_CZ)
// palety
const P_LEAF   = '46%oak_leaves,22%azalea_leaves,20%flowering_azalea_leaves,12%cherry_leaves';
const P_CROWN  = '40%oak_leaves,20%azalea_leaves,17%flowering_azalea_leaves,12%cherry_leaves,3%shroomlight,8%air'; // korona kolosa: blask + przeswity
const P_GRASS  = '82%grass_block,14%moss_block,4%podzol';
const P_CORRUPT= '40%sculk,24%deepslate,14%cracked_deepslate_bricks,12%cobbled_deepslate,10%rooted_dirt';
// ================================================================

const C=[]; // lista komend
const push=(...cs)=>cs.forEach(c=>C.push(c));
const sel=(x1,y1,z1,x2,y2,z2)=>push(`//pos1 ${x1},${y1},${z1}`,`//pos2 ${x2},${y2},${z2}`);
const box=(x1,y1,z1,x2,y2,z2,pat)=>{sel(x1,y1,z1,x2,y2,z2);push(`//set ${pat}`);};
const gen=(x1,y1,z1,x2,y2,z2,pat,expr)=>{sel(x1,y1,z1,x2,y2,z2);push(`//generate ${pat} ${expr}`);};
// walec pionowy (ignoruje y)
const cyl=(cx,z0,cz,r,y1,y2,pat)=>gen(cx-r,y1,cz-r,cx+r,y2,cz+r,pat,'x^2+z^2<1');
// elipsoida
const ell=(cx,cy,cz,rx,ry,rz,pat,expr='x^2+y^2+z^2<1')=>gen(cx-rx,cy-ry,cz-rz,cx+rx,cy+ry,cz+rz,pat,expr);
const gmask=(m='')=>push(`//gmask ${m}`.trim());
const line=(...a)=>push(...a);
const setb=(x,y,z,pat)=>{sel(x,y,z,x,y,z);push(`//set ${pat}`);};

// ---------- helper: kanoniczne drzewo z konarami (miniatura techniki Drzewa-Serca) ----------
// KONIEC LIZAKOW: zbiezna baza 2x2 + rozlane korzenie + 3-4 konary SCHODKOWE (nigdy box pien->koniec)
// + korona wielopłatowa (platy na koncach konarow). sp: oak|dark|dark_oak|cherry|azalea|dead.
// Reuzywane przez sciane gaju (ISLAND), trase (ZONES) i U1 (UPGRADE). gmask jest globalny -> czyscimy sami.
function groveTree(x,z,h,sp='oak'){
  gmask();
  const dark=(sp==='dark'||sp==='dark_oak');
  if(sp==='dead'){ // martwe skazone: goly wygiety pien, bez korony
    box(x,Y_TOP+1,z,x,Y_TOP+h,z,'stripped_dark_oak_log');
    setb(x+1,Y_TOP+h-1,z,'stripped_dark_oak_log'); setb(x+1,Y_TOP+h,z,'stripped_dark_oak_log');
    setb(x,Y_TOP+h,z-1,'stripped_dark_oak_log'); return;
  }
  const LOG=sp==='cherry'?'cherry_log':(dark?'82%dark_oak_log,18%dark_oak_wood':'88%oak_log,12%oak_wood');
  const LEAF=sp==='cherry'?'58%cherry_leaves,24%flowering_azalea_leaves,10%air,8%azalea_leaves'
    :sp==='azalea'?'52%flowering_azalea_leaves,36%azalea_leaves,12%air'
    :dark?'56%dark_oak_leaves,20%azalea_leaves,12%air,4%shroomlight,8%oak_leaves'
    :'48%oak_leaves,22%azalea_leaves,16%flowering_azalea_leaves,8%air,6%cherry_leaves';
  const top=Y_TOP+h;
  const flareTop=Y_TOP+Math.max(2,Math.round(h*0.30));
  box(x,Y_TOP,z,x+1,flareTop,z+1,LOG);                                   // zbiezna baza 2x2
  box(x,flareTop+1,z,x,top,z,LOG);                                       // slup 1x1 do korony
  for(const [dx,dz] of [[2,0],[-1,0],[0,2],[0,-1],[2,2],[-1,-1]]) setb(x+dx,Y_TOP,z+dz,LOG); // rozlane korzenie
  const nArms=3+(Math.abs(x+z)%2), arms=[], y0=Y_TOP+Math.round(h*0.62);
  for(let k=0;k<nArms;k++){                                              // 3-4 konary schodkowe (segmenty 1x1)
    const a=k*2*Math.PI/nArms + (x*0.7+z*0.3), reach=2+(k%2);
    const ax=x+Math.round(Math.sin(a)*reach), az=z+Math.round(Math.cos(a)*reach), ay=top-1+(k%2);
    for(let s=1;s<=3;s++){const t=s/3; setb(Math.round(x+(ax-x)*t),Math.round(y0+(ay-y0)*t),Math.round(z+(az-z)*t),LOG);}
    arms.push([ax,ay,az]);
  }
  gmask('air');                                                         // korona TYLKO w powietrze (chron pien/konary)
  ell(x,top+1,z,4,3,4,LEAF);                                            // plat centralny
  for(const [ax,ay,az] of arms) ell(ax,ay+1,az,3,2,3,LEAF);            // platy na koncach konarow
  ell(x,top+3,z,2,2,2,LEAF);                                            // czub
  gmask();
}

// FAZA: node build_level1_grove.js [island|zones|all|upgrade|quality]  (domyslnie: zones)
const PHASE=(process.argv[2]||'zones');
const ISLAND=(PHASE==='island'||PHASE==='all');
const ZONES=(PHASE==='zones'||PHASE==='all');
const UPGRADE=(PHASE==='upgrade');
const QUALITY=(PHASE==='quality');

if(ISLAND){
// ---------- 0. VOID-BABEL (2026-07-06) ----------
// Template przywrocony z backupu ma zapieczony NATURAL (±512 blokow terenu) — bez tego czyszczenia
// gracz widzi bedrock/teren w dali (skarga 07-05). Wycinamy CALY natural do bedrocku w obrebie
// wyspy+areny + margines VOID_M (sciana gaju domyka reszte sightline'ow). Ograniczone do z<=krawedz
// wyspy (ISLE_CZ+R_ISLE=44), zeby faza `island` byla samowystarczalna: korytarz (ZONES) nietkniety,
// zero ryzyka spawnu gracza nad pustka. Kierunek bossa/Drzewa (polnoc) = pelny void. [[md-void-generator-restart]]
const VOID_M=80;                                   // margines czyszczenia poza krawedzia wyspy
const bx=R_ISLE+VOID_M;                             // polszerokosc x babla (142)
const bzN=ISLE_CZ-R_ISLE-VOID_M;                    // polnocny kraniec (-160): void za Drzewem
const bzS=ISLE_CZ+R_ISLE;                           // poludniowy kraniec = krawedz wyspy (44): most/korytarz nietkniete
line('//gmask');                                   // bez maski: czyscimy natural (stone/dirt/bedrock) do powietrza
box(-bx,-64,bzN,bx,Y_SKY,bzS,'air');               // <<< VOID: bedrock->niebo, caly natural znika

// ---------- 1. FUNDAMENT WYSPY (na void) ----------
cyl(0,0,ISLE_CZ,R_ISLE,Y_BASE,Y_TOP-1,'dirt');     // bryla ziemi (przesunieta na polnoc)
cyl(0,0,ISLE_CZ,R_ISLE,Y_TOP,Y_TOP,P_GRASS);       // czapa trawy/mchu

// lagodne garby sredniego planu (poza strefa walki i sciezka; wypelniaja powiekszona polnoc)
for(const [mx,mz,mr] of [[20,10,6],[-19,12,7],[-24,-6,6],[24,-2,5],[-12,26,5],[14,24,5],[-34,-30,8],[36,-24,7],[0,-52,9],[-22,-46,7],[26,-44,7]]){
  ell(mx,Y_TOP,mz,mr,3,mr,'85%grass_block,15%moss_block','x^2+(y*2.2)^2+z^2<1');
}

// ---------- 2. PIERSCIEN WZGORZ + SCIANA GAJU (zamyka horyzont w "katedre") ----------
// srodek pierscienia = srodek wyspy (0,ISLE_CZ); luka od poludnia (worldz>14) na wejscie z korytarza
const ringZ=(a,r)=>ISLE_CZ+Math.round(r*Math.cos(a));
for(let i=0;i<16;i++){
  const a=i*Math.PI/8;
  const hz=ringZ(a,RING_R); const hx=Math.round(RING_R*Math.sin(a));
  if(hz>14 && Math.abs(hx)<18) continue;             // luka tylko na pas wejscia (most), reszta luku zostaje
  const hr=8+(i%3)*2, hh=5+(i%3);                    // r 8-12, wys 5-7
  ell(hx,Y_TOP,hz,hr,hh,hr,'62%grass_block,20%moss_block,10%stone,8%mossy_cobblestone','x^2+(y*1.35)^2+z^2<1');
}
// drzewa sciany gaju na wzgorzach (dwie warstwy koron, wyzsze — zamykaja void sylweta)
const WALL=[];
for(let i=0;i<16;i++){
  const a=i*Math.PI/8 + Math.PI/16;
  const z1=ringZ(a,RING_R-4), x1=Math.round((RING_R-4)*Math.sin(a));
  if(!(z1>14 && Math.abs(x1)<18)) WALL.push([x1, z1, 13+(i%4)*2, ['dark_oak','oak','cherry','azalea'][i%4]]);
  const b=i*Math.PI/8;
  const z2=ringZ(b,RING_R+1), x2=Math.round((RING_R+1)*Math.sin(b));
  if(!(z2>16 && Math.abs(x2)<18)) WALL.push([x2, z2, 15+(i%3)*2, ['oak','dark_oak','cherry'][i%3]]);
}
for(const [x,z,h,sp] of WALL){ groveTree(x,z,h,sp); } // koniec lizakow: konary + korona wielopłatowa

// ---------- 3. DRZEWO-SERCE (kolos: zbiezny pien, przypory, wielopłatowa korona) ----------
// pien zbiezny: stozek przez stackowane walce (KOLOS v4 — wyzszy i grubszy)
cyl(TREE_CX,0,TREE_CZ,5,Y_TOP,Y_TOP+9,'82%dark_oak_log,18%dark_oak_wood');
cyl(TREE_CX,0,TREE_CZ,4,Y_TOP+9,Y_TOP+20,'85%dark_oak_log,15%dark_oak_wood');
cyl(TREE_CX,0,TREE_CZ,3,Y_TOP+20,Y_TOP+31,'dark_oak_log');
cyl(TREE_CX,0,TREE_CZ,2,Y_TOP+31,Y_TOP+37,'dark_oak_log');
box(TREE_CX-1,Y_TOP+37,TREE_CZ-1,TREE_CX+1,Y_TOP+40,TREE_CZ+1,'dark_oak_log');
// ukryty rdzen swiatla
box(TREE_CX,Y_TOP+2,TREE_CZ,TREE_CX,Y_TOP+33,TREE_CZ,'sea_lantern');
// KOMORA SERCA: przeswit od poludnia (widoczny z areny!) ze shroomlight w glebi
box(TREE_CX-1,Y_TOP+5,TREE_CZ+2,TREE_CX+1,Y_TOP+8,TREE_CZ+4,'air');
box(TREE_CX-1,Y_TOP+5,TREE_CZ+1,TREE_CX+1,Y_TOP+8,TREE_CZ+1,'shroomlight');
setb(TREE_CX,Y_TOP+6,TREE_CZ+2,'amethyst_block');
setb(TREE_CX,Y_TOP+7,TREE_CZ+2,'amethyst_cluster');
// przypory korzeniowe: 8 kierunkow, schodkowe zjazdy 2x2 od pnia do ziemi (dluzsze — kolos)
for(let k=0;k<8;k++){
  const a=k*Math.PI/4;
  const dx=Math.sin(a), dz=Math.cos(a);
  for(let s=0;s<6;s++){
    const bx=Math.round(TREE_CX+dx*(5+s*1.7)), bz=Math.round(TREE_CZ+dz*(5+s*1.7));
    const by=Y_TOP+8-s*1.5;
    box(bx,Math.round(by)-2,bz,bx+1,Math.round(by),bz+1,'70%dark_oak_log,30%dark_oak_wood');
  }
}
// rozlane korzenie u podstawy
gmask('air');
ell(TREE_CX,Y_TOP+1,TREE_CZ,14,2,14,'40%mangrove_roots,25%muddy_mangrove_roots,15%rooted_dirt,20%air','x^2+(y*3)^2+z^2<1');
gmask();
// konary: 8 kierunkow z gory pnia, dluzsze na boki (wieksza rozpietosc, wyzej)
const arms=[[16,Y_TOP+26,-16],[-16,Y_TOP+26,-16],[13,Y_TOP+28,-40],[-13,Y_TOP+28,-40],[17,Y_TOP+30,-26],[-17,Y_TOP+30,-26],[0,Y_TOP+28,-44],[0,Y_TOP+26,-10]];
// konary jako SCHODKOWE segmenty 2x2 (NIE box od pnia do koncowki — to daje lite plyty!)
for(const [ax,ay,az] of arms){
  const y0=Y_TOP+24;
  for(let s=0;s<=6;s++){
    const t=s/6;
    const sx=Math.round(TREE_CX+(ax-TREE_CX)*t), sz=Math.round(TREE_CZ+(az-TREE_CZ)*t), sy=Math.round(y0+(ay-y0)*t);
    box(sx,sy,sz,sx+1,sy+1,sz+1,'dark_oak_log');
  }
}
// KORONA: wielopłatowa kopula (tylko w powietrze; chron pien/konary)
gmask('air');
ell(TREE_CX,Y_TOP+40,TREE_CZ,17,8,17,P_CROWN);                    // centralna kopula (wyzej, szersza)
ell(TREE_CX,Y_TOP+32,TREE_CZ,13,6,13,P_LEAF);                     // spodnica
for(const [ax,ay,az] of arms){ ell(ax,ay+3,az,7,5,7,P_CROWN); }   // platy na koncach konarow
ell(TREE_CX,Y_TOP+48,TREE_CZ,8,4,8,P_CROWN);                      // czub
gmask();

// ---------- 4. OLTARZ GAJU (miedzy arena a drzewem) ----------
const AX=0, AZ=-18;   // tuz za powiekszona arena (COMBAT_R=16), u podnoza Drzewa
cyl(AX,0,AZ,4,Y_TOP,Y_TOP,'80%mossy_stone_bricks,12%polished_deepslate,8%mossy_cobblestone');
cyl(AX,0,AZ,2,Y_TOP+1,Y_TOP+1,'mossy_stone_brick_slab');
box(AX,Y_TOP+1,AZ,AX,Y_TOP+2,AZ,'amethyst_block');
setb(AX,Y_TOP+3,AZ,'amethyst_cluster');
for(const [dx,dz] of [[3,3],[-3,3],[3,-3],[-3,-3]]){
  box(AX+dx,Y_TOP+1,AZ+dz,AX+dx,Y_TOP+2,AZ+dz,'mossy_stone_brick_wall');
  setb(AX+dx,Y_TOP+3,AZ+dz,'lantern');
}
for(const [dx,dz] of [[2,0],[-2,0],[0,2],[0,-2]]) setb(AX+dx,Y_TOP+1,AZ+dz,'sculk_sensor');

// ---------- 5. SKAZENIE (klin od pln-wsch, gradient, martwe drzewa, soul fire) ----------
// zewnetrzny pas sektora pln-wsch: podmien trawe/mech na palete skazenia (takze na wzgorzach)
gmask('grass_block,moss_block,podzol');
box(14,Y_TOP,-R_ISLE-6,R_ISLE+6,Y_TOP+8,-14,P_CORRUPT);                          // pelne skazenie (klin NE)
box(6,Y_TOP,-R_ISLE-6,14,Y_TOP+8,-6,'32%sculk,14%podzol,12%coarse_dirt,42%grass_block');   // pas przejscia (kraw. zachodnia)
box(14,Y_TOP,-14,R_ISLE+6,Y_TOP+8,-6,'32%sculk,14%podzol,12%coarse_dirt,42%grass_block');  // pas przejscia (kraw. poludniowa)
gmask();
// zyly sculk pelzajace po ziemi (przejscie)
gmask('air');
box(8,Y_TOP+1,-34,34,Y_TOP+1,-8,'12%sculk_vein[down=true],88%air');
gmask();
// zapadlisko z katalizatorami
ell(24,Y_TOP,-24,7,3,7,'air','x^2+(y*1.8)^2+z^2<1&&y>0');
ell(24,Y_TOP-1,-24,7,2,7,'60%sculk,22%sculk_catalyst,18%deepslate','x^2+(y*2.5)^2+z^2<1');
setb(24,Y_TOP,-24,'sculk_shrieker');
// martwe drzewa (gole pnie + wygietе konary)
for(const [tx,tz,th] of [[19,-31,8],[30,-16,7],[15,-19,6],[33,-28,9]]){
  box(tx,Y_TOP,tz,tx,Y_TOP+th,tz,'stripped_dark_oak_log');
  box(tx,Y_TOP+th-1,tz,tx+2,Y_TOP+th,tz,'stripped_dark_oak_log');
  box(tx,Y_TOP+th-3,tz,tx,Y_TOP+th-2,tz-2,'stripped_dark_oak_log');
}
// soul fire - upiorne ognie
for(const [fx,fz] of [[21,-14],[28,-22],[16,-26],[34,-19]]){
  setb(fx,Y_TOP,fz,'soul_soil');
  setb(fx,Y_TOP+1,fz,'soul_fire');
}
// jezyk skazenia pelznacy ku arenie (za bossem, po wschodniej stronie drzewa)
gmask('grass_block');
box(4,Y_TOP,-20,12,Y_TOP,-11,'45%sculk,30%grass_block,15%moss_block,10%podzol');
gmask();

// ---------- 6. DETAL: sadzawka, sciezka, brama, kwiaty klastrami ----------
// sadzawka zachodnia z azalia
box(-14,Y_TOP-1,-6,-10,Y_TOP-1,-2,'water');
box(-14,Y_TOP,-6,-10,Y_TOP,-2,'air');
box(-15,Y_TOP,-7,-15,Y_TOP,-1,'moss_block');
setb(-15,Y_TOP+1,-4,'flowering_azalea');
setb(-10,Y_TOP+1,-7,'azalea');
// sciezka od poludnia do areny (przez start 0,64,10 - to tylko podloga, funkcji nie rusza)
box(-1,Y_TOP,1,1,Y_TOP,20,'62%dirt_path,26%moss_block,12%coarse_dirt');
// brama pierwszego wrazenia (z=14, przejscie x0 wolne)
for(const gx of [-4,4]){
  box(gx,Y_TOP,14,gx,Y_TOP+6,14,'dark_oak_log');
  setb(gx,Y_TOP+7,14,'lantern');
}
gmask('air');
ell(0,Y_TOP+9,14,6,3,3,P_LEAF);   // luk lisci nad brama
gmask();
// kwiatowe klastry (zamiast globalnego spamu): typy naprzemiennie
const CLUSTERS=[[8,16,'w'],[-9,18,'b'],[16,4,'y'],[-17,2,'w'],[-6,-14,'b'],[7,-15,'w'],[22,16,'y'],[-23,10,'b'],[-26,-14,'w'],[12,28,'b'],[-13,30,'y'],[28,8,'w'],[-30,2,'b'],[6,32,'w']];
const MIX={w:'16%oxeye_daisy,12%azure_bluet,10%white_tulip,8%short_grass,54%air',
           b:'16%cornflower,10%blue_orchid,10%azure_bluet,8%short_grass,56%air',
           y:'14%dandelion,10%poppy,8%oxeye_daisy,10%short_grass,58%air'};
gmask('air');
for(const [cx,cz,t] of CLUSTERS){ cyl(cx,0,cz,3,Y_TOP+1,Y_TOP+1,MIX[t]); }
// delikatna trawa globalnie (rzadka, nie spam)
cyl(0,0,ISLE_CZ,R_ISLE-2,Y_TOP+1,Y_TOP+1,'6%short_grass,4%fern,90%air');
gmask();

// ---------- 7. SAFETY: strefa walki czysta i plaska ----------
line('//gmask');
cyl(0,0,0,COMBAT_R,Y_TOP+1,Y_TOP+7,'air');        // nic nie wlazi w strefe walki
cyl(0,0,0,COMBAT_R,Y_TOP,Y_TOP,'88%grass_block,12%moss_block'); // plaska podloga
// pierscien areny w podlodze (obwodka kregu walki)
gen(-COMBAT_R-1,Y_TOP,-COMBAT_R-1,COMBAT_R+1,Y_TOP,COMBAT_R+1,'70%mossy_stone_bricks,30%mossy_cobblestone','x^2+z^2<1&&x^2+z^2>0.8');
// sciezka przez arene do oltarza (flush, N-S — prowadzi wzrok na Drzewo)
box(-1,Y_TOP,-18,1,Y_TOP,1,'62%dirt_path,26%moss_block,12%coarse_dirt');
// kwiaty na arenie - tylko rzadkie
gmask('air');
cyl(0,0,0,COMBAT_R-2,Y_TOP+1,Y_TOP+1,'6%oxeye_daisy,5%azure_bluet,4%cornflower,85%air');
gmask();
// czysty krag wokol spawnu bossa i punktow funkcji
cyl(0,0,0,3,Y_TOP+1,Y_TOP+4,'air');
box(-1,Y_TOP+1,9,1,Y_TOP+4,11,'air');

// ---------- 8. PLYWAJACE FRAGMENTY LADU W VOID (wertykalny rozmach; zamyka horyzont sylweta) ----------
gmask();
for(let i=0;i<12;i++){
  const a=i*Math.PI/6 + Math.PI/12;
  const rad=R_ISLE+14+(i%3)*9;
  const fz=ISLE_CZ+Math.round(rad*Math.cos(a));
  const fx=Math.round(rad*Math.sin(a));
  if(fz>14 && Math.abs(fx)<26) continue;              // nie zaslaniaj wejscia od poludnia
  const fy=Y_TOP + [-14,8,-6,16,-10,12][i%6];         // rozne wysokosci = "katedra" w pionie
  const fr=6+(i%4)*2;                                  // r 6-12
  const ne = (fx>4 && fz<-4);                          // sektor skazenia NE (spojnosc tematyczna)
  for(let s=1;s<=5;s++){ const rr=Math.max(1,fr-s*2); ell(fx,fy-s,fz,rr,1,rr, ne?'70%deepslate,30%cobbled_deepslate':'75%dirt,25%coarse_dirt'); } // spodni stozek "wyrwanego" odlamka
  ell(fx,fy,fz,fr,2,fr, ne?P_CORRUPT:P_GRASS);        // czapa
  if(ne){
    ell(fx,fy+1,fz,Math.max(2,fr-2),1,Math.max(2,fr-2),'35%sculk,15%sculk_vein,50%air');
    box(fx,fy+1,fz,fx,fy+4,fz,'40%polished_basalt,60%basalt'); setb(fx,fy+5,fz,'soul_lantern'); // martwy drogowskaz
  } else {
    box(fx,fy+1,fz,fx,fy+3+(i%3),fz,'dark_oak_log');  // pien
    ell(fx,fy+4+(i%3),fz,fr-1,3,fr-1,P_LEAF);         // korona
    if(i%2) setb(fx+1,fy+1,fz,'lantern');
  }
}
} // koniec ISLAND

if(ZONES){
// ================================================================
// SEKCJA ZONES: poludniowy jezor wyspy — luk start->srodek->koniec
// Oboz (z~134) -> Skazona Sciezka (124..84) -> Polana Prob (72) ->
// Grota Zasobow (24,76) -> Wawoz+Most Gatekeepera (51..57) -> [wyspa: arena]
// + Komora Nagrod pod Drzewem-Sercem (za bossem, z -33..-42).
// Wspolrzedne BRAM musza zgadzac sie z fill w Packs/level_1/skills/zones.skill.yml!
// ================================================================
const P_PATH='62%dirt_path,26%moss_block,12%coarse_dirt';
// gradient skazenia wzdluz sciezki (najciemniej w srodku trasy)
const grassPal=z=> z>=120 ? '80%grass_block,15%moss_block,5%podzol'
                : z>=100 ? '66%grass_block,18%moss_block,10%podzol,6%coarse_dirt'
                : z>=84  ? '44%grass_block,22%moss_block,12%podzol,10%coarse_dirt,12%sculk'
                : z>=58  ? '72%grass_block,18%moss_block,10%podzol'
                : P_GRASS;

// --- Z0. czyszczenie korytarza poludniowego (od z=46: NIE ruszac poludnia wyspy r=44!) ---
line('//gmask');
box(-32,Y_TOP+1,46,38,Y_SKY,156,'air');

// --- A2a. LAKA KORYTARZA: zielone podloze na calym footprincie (koniec golej szarej plyty template) ---
box(-22,Y_BASE,46,22,Y_TOP-1,156,'dirt');
box(-22,Y_TOP,46,22,Y_TOP,156,P_GRASS);

// --- Z1. fundament jezora: kola wzdluz osi + wzgorze groty ---
const SPINE=[[0,140,16],[3,126,13],[6,114,12],[2,102,12],[-5,94,12],[-2,84,13],[0,72,15],[0,60,12],[0,48,13],[0,38,14]];
for(const [cx,cz,r] of SPINE){
  cyl(cx,0,cz,r,Y_BASE,Y_TOP-1,'dirt');
  cyl(cx,0,cz,r,Y_TOP,Y_TOP,grassPal(cz));
}
// wzgorze groty (wschod od polany)
cyl(24,0,76,12,Y_BASE,Y_TOP-1,'dirt');
cyl(24,0,76,12,Y_TOP,Y_TOP,'70%grass_block,20%moss_block,10%podzol');
ell(24,Y_TOP,76,12,7,12,'55%stone,20%grass_block,13%moss_block,8%andesite,4%mossy_cobblestone','x^2+(y*1.15)^2+z^2<1');

// --- Z2. przelecz przez pierscien wzgorz (wjazd na wyspe, z 26..48) ---
box(-5,Y_TOP+1,26,5,Y_TOP+26,48,'air');
sel(-7,Y_TOP,26,7,Y_TOP+16,48); push('//replace dirt moss_block');

// --- Z3. wawoz ze strumieniem (z 51..57) + zatyczki ---
box(-26,Y_TOP-5,51,26,Y_TOP-1,57,'air');
box(-26,Y_TOP-6,51,26,Y_TOP-6,57,'55%deepslate,30%cobbled_deepslate,15%tuff');
gmask('air');
box(-26,Y_TOP-6,50,26,Y_TOP-4,50,'60%stone,25%cobbled_deepslate,15%tuff');   // skarpa pld
box(-26,Y_TOP-6,58,26,Y_TOP-4,58,'60%stone,25%cobbled_deepslate,15%tuff');   // skarpa pln
gmask();
box(-16,Y_TOP-6,51,-14,Y_TOP,57,'70%stone,20%mossy_cobblestone,10%tuff');    // zatyczka W
box(14,Y_TOP-6,51,16,Y_TOP,57,'70%stone,20%mossy_cobblestone,10%tuff');      // zatyczka E
box(-13,Y_TOP-5,52,13,Y_TOP-5,56,'water');

// --- Z4. sciezka (segmenty co ~4z miedzy waypointami; omija wawoz 51..57) ---
const WPTS=[[0,146],[0,138],[2,128],[6,118],[7,110],[3,102],[-4,96],[-5,90],[-2,84],[0,78],[0,66],[0,60]];
const seg=(x1,z1,x2,z2)=>{const n=Math.max(1,Math.ceil(Math.abs(z1-z2)/4));for(let i=0;i<n;i++){
  const za=Math.round(z1+(z2-z1)*i/n), zb=Math.round(z1+(z2-z1)*(i+1)/n);
  const px=Math.round(x1+(x2-x1)*(i+0.5)/n);
  box(px-1,Y_TOP,Math.min(za,zb),px+1,Y_TOP,Math.max(za,zb),P_PATH);}};
for(let i=0;i<WPTS.length-1;i++) seg(WPTS[i][0],WPTS[i][1],WPTS[i+1][0],WPTS[i+1][1]);
seg(0,48,0,20);                                    // od mostu do bramy wyspy
box(-1,Y_TOP,74,1,Y_TOP,78,P_PATH);                // przez polane
seg(9,76,14,76);                                   // odnoga do groty (E-W)
box(9,Y_TOP,75,14,Y_TOP,77,P_PATH);

// --- Z5. OBOZ PRZYBYCIA (z 128..150) ---
// krag portalowy (przybycie z huba) za plecami startu
cyl(0,0,146,5,Y_TOP,Y_TOP,'55%polished_deepslate,30%stone_bricks,15%mossy_stone_bricks');
cyl(0,0,146,2,Y_TOP,Y_TOP,'polished_deepslate');
setb(0,Y_TOP+1,146,'amethyst_block'); setb(0,Y_TOP+2,146,'amethyst_cluster');
for(const [px,pz] of [[4,146],[-4,146],[0,150],[0,142]]){
  box(px,Y_TOP+1,pz,px,Y_TOP+3,pz,'chiseled_stone_bricks');
  setb(px,Y_TOP+4,pz,'lantern');
}
// ognisko z siedziskami
setb(5,Y_TOP+1,133,'campfire');
for(const [sx,sz] of [[7,133],[3,133],[5,131],[5,135]]) setb(sx,Y_TOP+1,sz,'stripped_oak_log[axis=x]');
setb(7,Y_TOP+1,131,'hay_block');
// namioty (A-frame z welny)
box(-7,Y_TOP+1,131,-5,Y_TOP+1,135,'white_wool');
box(-7,Y_TOP+2,132,-5,Y_TOP+2,134,'white_wool');
box(-7,Y_TOP+3,133,-5,Y_TOP+3,133,'brown_wool');
box(-6,Y_TOP+1,132,-6,Y_TOP+2,134,'air');          // wnetrze
box(-10,Y_TOP+1,128,-9,Y_TOP+1,130,'brown_wool');
setb(-9,Y_TOP+2,129,'brown_wool');
// zapasy
box(7,Y_TOP+1,137,8,Y_TOP+2,138,'barrel');
setb(6,Y_TOP+1,138,'oak_planks'); setb(6,Y_TOP+2,138,'hay_block');
// brama obozu (wyjscie na sciezke, z=124)
for(const gx of [-3,3]){ box(gx,Y_TOP+1,124,gx,Y_TOP+5,124,'dark_oak_log'); setb(gx,Y_TOP+6,124,'lantern'); }
box(-3,Y_TOP+6,124,3,Y_TOP+6,124,'dark_oak_log[axis=x]');
// latarnie obozowe
for(const [lx,lz] of [[2,136],[-3,138]]){ box(lx,Y_TOP+1,lz,lx,Y_TOP+2,lz,'oak_fence'); setb(lx,Y_TOP+3,lz,'lantern'); }

// --- Z6. SKAZONA SCIEZKA: las, POI, latarnie, skazenie narasta ---
// drzewa przydrozne (gatunek wg strefy; martwe w najbardziej skazonym odcinku)
const tree=(x,z,h,sp)=>groveTree(x,z,h,sp); // reuzycie kanonicznego drzewa (konary + korona wielopłatowa)
const TREES=[[ -5,141,6,'oak'],[8,138,7,'cherry'],[-8,127,6,'oak'],[12,122,8,'oak'],[-2,119,7,'cherry'],
  [12,112,8,'dark'],[0,108,7,'dark'],[13,104,6,'dead'],[-3,105,8,'dark'],[-10,97,7,'dead'],[2,92,6,'dead'],[-12,88,7,'dead'],
  [-12,82,7,'cherry'],[12,63,6,'oak'],[-11,60,7,'cherry'],[-10,50,8,'oak'],[10,44,7,'oak'],[-8,34,8,'cherry']];
for(const [x,z,h,sp] of TREES) tree(x,z,h,sp);
// wzgorza-sciany wzdluz krawedzi jezora (domykaja korytarz)
for(const [hx,hz,hr] of [[-15,132,7],[17,126,7],[-14,116,6],[16,106,7],[-15,102,6],[14,90,6],[-16,80,7],[15,64,6],[-14,58,6],[16,40,7],[-15,42,7]]){
  ell(hx,Y_TOP,hz,hr,4,hr,hz>=84&&hz<=106?'40%grass_block,25%moss_block,15%podzol,20%sculk':'62%grass_block,20%moss_block,10%stone,8%mossy_cobblestone','x^2+(y*1.4)^2+z^2<1');
}
// POI: woz zielarza (porzucony)
box(10,Y_TOP+1,115,12,Y_TOP+1,116,'oak_planks');
box(10,Y_TOP+2,115,10,Y_TOP+2,116,'barrel');
setb(12,Y_TOP+2,115,'flower_pot'); setb(12,Y_TOP+2,116,'composter');
setb(9,Y_TOP+1,115,'oak_trapdoor[facing=east,open=true]'); setb(13,Y_TOP+1,116,'oak_trapdoor[facing=west,open=true]'); // "kola"
setb(11,Y_TOP+2,115,'white_carpet');
// POI: zniszczona kapliczka gaju (sculk pelznie po ruinie)
box(-10,Y_TOP+1,98,-8,Y_TOP+1,100,'70%mossy_cobblestone,30%cobblestone');
box(-9,Y_TOP+2,99,-9,Y_TOP+3,99,'mossy_stone_brick_wall');
setb(-9,Y_TOP+4,99,'amethyst_cluster');
setb(-10,Y_TOP+2,98,'candle'); setb(-8,Y_TOP+2,100,'sculk_vein[down=true]');
setb(-10,Y_TOP+1,100,'sculk'); setb(-8,Y_TOP+1,98,'sculk');
// slady walki przy zasadzce A
box(5,Y_TOP,108,8,Y_TOP,110,'55%coarse_dirt,30%dirt_path,15%grass_block');
setb(7,Y_TOP+1,109,'dead_bush'); setb(5,Y_TOP+1,110,'bone_block');
// skazenie: zyly i plamy w srodkowym odcinku
gmask('grass_block,moss_block,podzol');
box(-12,Y_TOP,86,12,Y_TOP+6,104,'20%sculk,80%air');
gmask();
gmask('air');
box(-12,Y_TOP+1,86,12,Y_TOP+1,106,'10%sculk_vein[down=true],90%air');
gmask();
for(const [fx,fz] of [[9,99],[-7,91]]){ setb(fx,Y_TOP,fz,'soul_soil'); setb(fx,Y_TOP+1,fz,'soul_fire'); }
// latarnie wzdluz sciezki (soul w skazonym odcinku)
for(const [lx,lz,soul] of [[2,130,0],[8,118,0],[9,109,0],[5,103,1],[-2,99,1],[-7,92,1],[-4,87,1],[4,86,0],[2,63,0],[2,45,0],[2,30,0]]){
  box(lx,Y_TOP+1,lz,lx,Y_TOP+2,lz,'spruce_fence');
  setb(lx,Y_TOP+3,lz,soul?'soul_lantern':'lantern');
}

// --- A2b. DOMKNIECIE HORYZONTU KORYTARZA: wysoki grzbiet + sciana drzew po bokach + amfiteatr obozu ---
// (Z7 polana / Z8 grota rysuja sie PO tym i nadpisuja tam gdzie wchodza -> bezpieczne)
// grzbiet: nachodzace wzgorza r8 co 11z = ciagla sciana; wschod pomija wjazd do groty (z66-86)
for(let z=50; z<=152; z+=11){
  for(const side of [-1,1]){
    if(side>0 && z>=61 && z<=90) continue;            // wschod: zostaw miejsce na grote (24,76)
    const hx=20*side, hr=8, hh=8+(Math.abs(hx+z)%4);  // wys 8-11
    ell(hx,Y_TOP,z,hr,hh,hr,'58%grass_block,22%moss_block,12%stone,8%mossy_cobblestone','x^2+(y*1.3)^2+z^2<1');
  }
}
// dwuwarstwowa sciana drzew na grzbiecie ("katedra" korytarza; groveTree = konary+korona wielopłatowa)
const WSP=['dark','oak','cherry'];
for(let z=52; z<=150; z+=8){
  groveTree(-22, z,   12+(z%4), WSP[z%3]);
  groveTree(-28, z+3, 14+(z%3), WSP[(z+1)%3]);
  if(!(z>=61 && z<=90)){                              // wschod: omijaj grote
    groveTree(23, z+4, 12+(z%4), WSP[(z+2)%3]);
    groveTree(29, z,   14+(z%3), WSP[z%3]);
  }
}
// amfiteatr obozu: horseshoe wzgorz+drzew za przybyciem (0,146), otwarty na sciezke od pld
for(const [ax,az] of [[-14,151],[0,155],[14,151],[-17,143],[17,143],[-16,134],[17,134]]){
  ell(ax,Y_TOP,az,7,7,7,'60%grass_block,22%moss_block,10%stone,8%coarse_dirt','x^2+(y*1.25)^2+z^2<1');
  groveTree(ax, az, 12+(Math.abs(ax+az)%4), WSP[Math.abs(ax+az)%3]);
}

// --- Z7. POLANA PROB (0,72): krag przodkow ---
cyl(0,0,72,14,Y_TOP+1,Y_TOP+8,'air');              // przeswit
cyl(0,0,72,14,Y_TOP,Y_TOP,'72%grass_block,18%moss_block,10%podzol');
box(-1,Y_TOP,66,1,Y_TOP,78,P_PATH);                // sciezka przez polane (po czyszczeniu)
cyl(0,0,72,3,Y_TOP,Y_TOP,'60%cracked_stone_bricks,25%mossy_stone_bricks,15%stone_bricks');
setb(0,Y_TOP+1,72,'soul_campfire');
for(let i=0;i<8;i++){
  const a=i*Math.PI/4, px=Math.round(9*Math.sin(a)), pz=72+Math.round(9*Math.cos(a));
  const h=(i%3===2)?1:2+(i%2);                      // co trzeci filar zlamany
  box(px,Y_TOP+1,pz,px,Y_TOP+h,pz,'70%mossy_cobblestone_wall,30%cobblestone_wall');
  if(i%3===2){ setb(px+1,Y_TOP+1,pz,'mossy_cobblestone_slab'); setb(px-1,Y_TOP+1,pz,'cobblestone_slab'); }
}
for(const [bx,bz] of [[6,66],[-6,66],[6,78],[-6,78]]){
  setb(bx,Y_TOP+1,bz,'cobblestone_wall'); setb(bx,Y_TOP+2,bz,'campfire');
}

// --- Z8. GROTA ZASOBOW (wzgorze 24,76; wejscie od zachodu) ---
box(13,Y_TOP+1,75,20,Y_TOP+3,77,'air');            // tunel
ell(24,Y_TOP+2,76,5,3,5,'air');                    // komora
box(19,Y_TOP,75,29,Y_TOP,77,'55%moss_block,30%rooted_dirt,15%grass_block'); // podloga
box(15,Y_TOP,75,18,Y_TOP,77,'60%moss_block,40%dirt_path');
gmask('stone,dirt,andesite');
ell(24,Y_TOP+2,76,6,4,6,'50%stone,22%tuff,16%cobbled_deepslate,12%mossy_cobblestone'); // sciany komory
gmask();
ell(24,Y_TOP+2,76,5,3,5,'air');                    // re-carve po dressingu
setb(24,Y_TOP+3,76,'shroomlight');                 // swiatlo w stropie
for(const [gx,gz] of [[21,74],[26,78],[22,79],[27,73]]){ setb(gx,Y_TOP+1,gz,'glow_lichen[down=true]'); }
for(const [hx,hz] of [[22,76],[25,74],[26,77]]){ setb(hx,Y_TOP+3,hz,'hanging_roots'); }
// ziola i korzenie (fabularnie: cel objective "Korzenie Straznika")
for(const [px,pz,pl] of [[21,75,'fern'],[23,78,'large_fern[half=lower]'],[26,75,'fern'],[25,78,'sweet_berry_bush']]) setb(px,Y_TOP+1,pz,pl);
// sekret zielarza: skrytka w najglebszym punkcie (funkcja MD: 27,64,76 r=2.5)
setb(28,Y_TOP+1,76,'barrel[facing=up]');
setb(28,Y_TOP+2,76,'amethyst_cluster');
setb(27,Y_TOP+1,75,'flower_pot');

// --- Z9. MOST GATEKEEPERA + BRAMA KORZENI (fill: -3 64 46 -> 3 68 46!) ---
box(-2,Y_TOP,48,2,Y_TOP,60,'80%spruce_planks,20%stripped_spruce_log');       // pomost
box(-4,Y_TOP,52,4,Y_TOP,56,'75%spruce_planks,25%stripped_spruce_log');       // plac walki na srodku
for(const sz of [50,54,58]){ for(const sx of [-2,2]){ box(sx,Y_TOP-6,sz,sx,Y_TOP-1,sz,'spruce_log'); } }
box(-2,Y_TOP+1,48,-2,Y_TOP+1,51,'spruce_fence'); box(2,Y_TOP+1,48,2,Y_TOP+1,51,'spruce_fence');
box(-2,Y_TOP+1,57,-2,Y_TOP+1,60,'spruce_fence'); box(2,Y_TOP+1,57,2,Y_TOP+1,60,'spruce_fence');
box(-4,Y_TOP+1,52,-4,Y_TOP+1,56,'spruce_fence'); box(4,Y_TOP+1,52,4,Y_TOP+1,56,'spruce_fence');
setb(-2,Y_TOP+2,48,'lantern'); setb(2,Y_TOP+2,48,'lantern'); setb(-2,Y_TOP+2,60,'lantern'); setb(2,Y_TOP+2,60,'lantern');
// brama korzeni za mostem — DOKLADNIE region fill skilla level_1_open_root_gate_bridge
box(-3,Y_TOP,46,3,Y_TOP+4,46,'55%mangrove_roots,25%dark_oak_fence,20%moss_block');
for(const gx of [-4,4]){ box(gx,Y_TOP,46,gx,Y_TOP+5,46,'dark_oak_log'); }
box(-4,Y_TOP+5,46,4,Y_TOP+5,46,'dark_oak_log[axis=x]');
setb(-4,Y_TOP+6,46,'soul_lantern'); setb(4,Y_TOP+6,46,'soul_lantern');

// --- Z10. KOMORA NAGROD pod Drzewem-Sercem (wejscie za drzewem, z=-33) ---
// muszla komory w bryle ziemi
box(-5,Y_TOP-6,-43,5,Y_TOP,-34,'55%cobbled_deepslate,25%tuff,20%mossy_cobblestone');
box(-3,Y_TOP-5,-42,3,Y_TOP-1,-36,'air');                      // wnetrze
box(-3,Y_TOP-6,-42,3,Y_TOP-6,-36,'70%mossy_stone_bricks,30%polished_deepslate'); // posadzka
// zejscie schodkowe od powierzchni (z=-33 w dol na polnoc)
box(-1,Y_TOP,-34,1,Y_TOP+2,-34,'air'); box(-1,Y_TOP-1,-35,1,Y_TOP+1,-35,'air');
box(-1,Y_TOP-2,-36,1,Y_TOP,-36,'air'); box(-1,Y_TOP-3,-36,1,Y_TOP-3,-36,'mossy_stone_brick_stairs[facing=north]');
box(-1,Y_TOP-4,-37,1,Y_TOP-4,-37,'mossy_stone_brick_stairs[facing=north]');
box(-1,Y_TOP-5,-38,1,Y_TOP-5,-38,'mossy_stone_brick_stairs[facing=north]');
// korzenie serca schodza ze stropu + rdzen swiatla
for(const [kx,kz] of [[-2,-37],[2,-37],[-2,-41],[2,-41]]){ box(kx,Y_TOP-3,kz,kx,Y_TOP-1,kz,'dark_oak_log'); }
setb(0,Y_TOP-1,-39,'sea_lantern');
setb(0,Y_TOP-2,-39,'amethyst_cluster[facing=down]');
for(const [hx,hz] of [[-1,-38],[1,-40],[-1,-41]]) setb(hx,Y_TOP-2,hz,'hanging_roots');
// skarbiec: beczki + ametyst (nagroda mechaniczna idzie funkcja MD)
setb(-2,Y_TOP-5,-40,'barrel[facing=up]'); setb(-2,Y_TOP-4,-40,'amethyst_block');
setb(2,Y_TOP-5,-38,'barrel[facing=up]');
// portal powrotny (sciana polnocna, funkcja LeaveDungeon: 0,60,-42 r=2)
box(-1,Y_TOP-5,-42,1,Y_TOP-2,-42,'crying_obsidian');
box(0,Y_TOP-5,-42,0,Y_TOP-3,-42,'purple_stained_glass');
setb(-1,Y_TOP-1,-42,'soul_lantern'); setb(1,Y_TOP-1,-42,'soul_lantern');
// brama korzeni na wejsciu — DOKLADNIE region fill skilla level_1_open_root_gate_reward
box(-2,Y_TOP,-33,2,Y_TOP+3,-32,'55%mangrove_roots,25%dark_oak_fence,20%moss_block');

// --- Z11. SAFETY: przejezdnosc punktow funkcji MD ---
line('//gmask');
box(-1,Y_TOP+1,137,1,Y_TOP+3,139,'air');           // start (0,64,138)
box(-1,Y_TOP+1,71,1,Y_TOP+3,73,'air');             // waypointy polany
cyl(0,0,54,4,Y_TOP+1,Y_TOP+4,'air');               // srodek mostu (spawn wardena)
box(-1,Y_TOP+1,39,1,Y_TOP+3,41,'air');             // checkpoint za mostem
} // koniec ZONES

// ================================================================
// UPGRADE (V3): trasa "feel like an event" - korony wielopłatowe, brama
// widokowa, dramat mostu, monolity, komora nagród, wieża obozu, skażenie 2.0.
// Zasady: skill minecraft-dungeon-artistry (5s rule, konary schodkowe, 60-30-10,
// światło ukryte). Punkty funkcji MD i regiony bram NIETYKALNE.
// ================================================================
if(UPGRADE){
gmask();
// --- U1. DRZEWA TRASY 2.0: korony wielopłatowe zamiast 2 elipsoid ---
const LIVE=[[ -5,141,6,'oak'],[8,138,7,'cherry'],[-8,127,6,'oak'],[12,122,8,'oak'],[-2,119,7,'cherry'],
  [12,112,8,'dark'],[0,108,7,'dark'],[-3,105,8,'dark'],
  [-12,82,7,'cherry'],[12,63,6,'oak'],[-11,60,7,'cherry'],[-10,50,8,'oak'],[10,44,7,'oak'],[-8,34,8,'cherry']];
for(const [x,z,h,sp] of LIVE){
  // zdejmij starą koronę (tylko liście - POI/latarnie/sasiednie pnie nietknięte), potem kanoniczne drzewo
  sel(x-5,Y_TOP+2,z-5,x+5,Y_TOP+h+7,z+5);
  push('//replace oak_leaves,azalea_leaves,flowering_azalea_leaves,cherry_leaves,dark_oak_leaves air');
  groveTree(x,z,h,sp); // reuzycie kanonicznego drzewa (konary schodkowe + korona wielopłatowa)
}
// --- U2. BRAMA WIDOKOWA na przełęczy (~z=33): rama pierwszego pełnego widoku Drzewa-Serca ---
for(const px of [-4,4]){
  box(px,Y_TOP+1,33,px,Y_TOP+7,33,'55%cobbled_deepslate,30%polished_deepslate,15%mossy_cobblestone');
  setb(px,Y_TOP+8,33,'polished_deepslate_slab');
  setb(px>0?px-1:px+1,Y_TOP+6,33,'lantern[hanging=false]');
}
box(-3,Y_TOP+7,33,3,Y_TOP+7,33,'mangrove_roots');
box(-2,Y_TOP+6,33,2,Y_TOP+6,33,'35%mangrove_roots,25%hanging_roots,40%air');
setb(0,Y_TOP+7,33,'shroomlight');
// --- U3. MOST 2.0: wyższe wieże bramne + kurtyna korzeni nad regionem fill + wodospad ---
for(const gx of [-4,4]){
  box(gx,Y_TOP+5,46,gx,Y_TOP+8,46,'dark_oak_log');
  setb(gx,Y_TOP+9,46,'dark_oak_slab');
}
box(-3,Y_TOP+8,46,3,Y_TOP+8,46,'dark_oak_log');            // belka nad brama (fill konczy sie na y+4)
box(-3,Y_TOP+7,46,3,Y_TOP+7,46,'50%mangrove_roots,30%hanging_roots,20%air');
box(-3,Y_TOP+6,46,3,Y_TOP+6,46,'25%hanging_roots,15%vine,60%air');
setb(-2,Y_TOP+8,45,'lantern[hanging=true]'); setb(2,Y_TOP+8,45,'lantern[hanging=true]');
// wodospad z zatyczki E do strumienia wąwozu
box(15,Y_TOP+1,53,15,Y_TOP+1,55,'water');
// filary mostu (przypory od dna wąwozu, poza osią przejścia)
for(const fz of [52,56]){ box(-3,Y_TOP-6,fz,-3,Y_TOP-1,fz,'stripped_spruce_log'); box(3,Y_TOP-6,fz,3,Y_TOP-1,fz,'stripped_spruce_log'); }
// --- U4. POLANA PRÓB 2.0: monolity zamiast murków (krąg r=9 wokół 0,z=72) ---
{
  const R=9,CZ=72,H=[5,4,0,5,3,4,0,6]; // 0 = przewrócony
  for(let i=0;i<8;i++){
    const a=i*Math.PI/4+Math.PI/8, sx=Math.round(R*Math.sin(a)), sz=CZ+Math.round(R*Math.cos(a));
    box(sx-1,Y_TOP+1,sz-1,sx+1,Y_TOP+6,sz+1,'air');       // zdejmij stary murek
    if(H[i]===0){ // przewrócony monolit
      box(sx,Y_TOP+1,sz,sx+(sx>0?2:-2),Y_TOP+1,sz,'60%deepslate,25%tuff,15%mossy_cobblestone');
    } else {
      box(sx,Y_TOP+1,sz,sx,Y_TOP+H[i],sz,'55%deepslate,28%tuff,17%polished_deepslate');
      setb(sx,Y_TOP+1,sz,'polished_deepslate');
      if(i%3===0) setb(sx,Y_TOP+H[i]+1,sz,'amethyst_cluster');
      if(i%3===1) setb(sx,Y_TOP+H[i]+1,sz,'deepslate_tile_slab');
    }
  }
  // ukryte światło rytuału (niewidzialne bloki light)
  for(const [lx,lz] of [[3,69],[-3,75],[4,74],[-4,70]]) setb(lx,Y_TOP+1,lz,'light[level=13]');
  setb(2,Y_TOP+1,72,'soul_lantern'); setb(-2,Y_TOP+1,72,'soul_lantern'); // dais (poza boxem safety x-1..1)
}
// --- U5. KOMORA NAGRÓD 2.0: korzenie serca, froglight, piedestał w snopie światła ---
gmask('air');
box(-3,Y_TOP-2,-41,3,Y_TOP-2,-36,'20%mangrove_roots,80%air');   // korzenie zwisają z sufitu (tylko w air)
gmask();
setb(-2,Y_TOP-1,-40,'ochre_froglight'); setb(2,Y_TOP-1,-37,'ochre_froglight');
box(0,Y_TOP-5,-39,0,Y_TOP-4,-39,'polished_deepslate_wall');      // piedestał
setb(0,Y_TOP-3,-39,'amethyst_cluster');
setb(0,Y_TOP-1,-39,'ochre_froglight');                            // snop światła nad piedestałem
setb(-2,Y_TOP-5,-36,'moss_carpet'); setb(2,Y_TOP-5,-41,'moss_carpet');
// --- U6. OBÓZ 2.0: wieża zwiadowcy, palisada, proporce, ukryte światło ---
for(const [lx,lz] of [[9,141],[11,141],[9,143],[11,143]]) box(lx,Y_TOP+1,lz,lx,Y_TOP+4,lz,'spruce_log');
box(9,Y_TOP+5,141,11,Y_TOP+5,143,'spruce_planks');
box(9,Y_TOP+6,141,11,Y_TOP+6,143,'spruce_fence');
box(10,Y_TOP+6,142,10,Y_TOP+6,142,'air'); setb(10,Y_TOP+6,142,'lantern[hanging=false]');
setb(9,Y_TOP+7,141,'spruce_slab'); setb(11,Y_TOP+7,143,'spruce_slab');
// palisada łukiem za obozem (południe, nie zasłania widoku na północ)
for(const [px,pz,ph] of [[-8,152,3],[-5,154,4],[-1,155,3],[3,154,4],[7,152,3],[-11,149,4],[10,149,4]])
  box(px,Y_TOP+1,pz,px,Y_TOP+ph,pz,'spruce_log');
// proporce przy kręgu portalowym
for(const [bx,bz] of [[-5,143],[5,143]]){
  box(bx,Y_TOP+1,bz,bx,Y_TOP+4,bz,'spruce_fence');
  setb(bx,Y_TOP+4,bz,'green_wool'); setb(bx,Y_TOP+3,bz,'white_wool');
}
for(const [lx,lz] of [[0,146],[3,141],[-3,141]]) setb(lx,Y_TOP+1,lz,'light[level=12]');
// --- U7. SKAŻENIE 2.0: gięte martwe drzewa + jęzor sculku przez ścieżkę ---
for(const [mx,mz] of [[13,104],[-10,97],[2,92],[-12,88]]){
  box(mx-1,Y_TOP+1,mz-1,mx+1,Y_TOP+9,mz+1,'air');                 // zdejmij stary prosty pień
  box(mx,Y_TOP+1,mz,mx,Y_TOP+4,mz,'stripped_dark_oak_log');
  setb(mx+1,Y_TOP+5,mz,'stripped_dark_oak_log'); setb(mx+1,Y_TOP+6,mz,'stripped_dark_oak_log');
  setb(mx+2,Y_TOP+6,mz+1,'stripped_dark_oak_wood');               // wygięcie
  setb(mx,Y_TOP+5,mz,'sculk_vein[down=true]'); setb(mx+1,Y_TOP+4,mz,'sculk_catalyst');
}
box(-2,Y_TOP,93,2,Y_TOP,96,'45%sculk,40%dirt_path,15%coarse_dirt'); // jęzor przez ścieżkę
setb(-4,Y_TOP+1,94,'sculk_shrieker'); setb(4,Y_TOP+1,95,'sculk_sensor');
// --- U8. GROTA 2.0: kieszeń ametystu + nacieki + świecące jagody ---
ell(25,Y_TOP+2,76,2,2,2,'calcite');
ell(25,Y_TOP+2,76,1,1,1,'amethyst_block');
setb(24,Y_TOP+2,76,'amethyst_cluster'); setb(25,Y_TOP+3,76,'amethyst_cluster'); setb(25,Y_TOP+2,75,'budding_amethyst');
for(const [dx,dz,up] of [[19,75,0],[21,78,0],[18,77,1],[22,74,1]])
  setb(dx,up?Y_TOP+1:Y_TOP+4,dz,`pointed_dripstone[vertical_direction=${up?'up':'down'}]`);
setb(20,Y_TOP+4,76,'cave_vines[berries=true]'); setb(23,Y_TOP+4,77,'cave_vines[berries=true]');
// --- U9. ŚWIATŁO TRASY: ukryte punkty na osi (czytelność nocą, zero fixtures) ---
for(const [lx,lz] of [[0,126],[0,112],[0,98],[0,84],[0,60],[0,48],[0,40]]) setb(lx,Y_TOP+1,lz,'light[level=11]');
} // koniec UPGRADE

// ================================================================
// QUALITY (Faza 2 planu "ulepszona wersja lochu 001", 2026-07-04):
// wezly skazenia (dekoracje pod moby level_1_corruption_node), stanowisko
// zielarza w obozie, sightlines na Drzewo-Serce, dressing komory nagrod,
// sculk v2 z sensorami, oslony LoS w path_a. Punkty funkcji MD i regiony
// bram NIETYKALNE. Koordy wezlow MUSZA zgadzac sie z corruption_node.skill.yml.
// ================================================================
if(QUALITY){
gmask();
// --- Q1. WEZLY SKAZENIA: dekoracja 3 wezlow (mob = niewidzialny HUSK na y65) ---
// blok moba: N1 (8,65,-16) jezor przy arenie; N2 (20,65,-21) krawedz klina NE;
// N3 (17,65,-29) glab klina. Kokon niski (y65-66), front (poludnie) otwarty na cios.
const NODES=[[8,-16],[20,-21],[17,-29]];
for(const [nx,nz] of NODES){
  // plama sculk w podlozu r~2.5
  gen(nx-3,Y_TOP,nz-3,nx+3,Y_TOP,nz+3,'70%sculk,18%deepslate,12%rooted_dirt','x^2+z^2<1');
  // katalizator + shrieker w podlodze przy mobie (czytelny "rdzen")
  setb(nx-1,Y_TOP,nz,'sculk_catalyst');
  setb(nx+1,Y_TOP,nz-1,'sculk_shrieker');
  setb(nx,Y_TOP,nz+1,'sculk_sensor');
  // kokon: wieniec deepslate/rooted_dirt wokol moba, wyzszy od polnocy (plecy)
  setb(nx-1,Y_TOP+1,nz-1,'cobbled_deepslate'); setb(nx+1,Y_TOP+1,nz-1,'cobbled_deepslate');
  setb(nx,Y_TOP+1,nz-1,'deepslate_tiles');     setb(nx,Y_TOP+2,nz-1,'cracked_deepslate_tiles');
  setb(nx-1,Y_TOP+1,nz,'rooted_dirt');         setb(nx+1,Y_TOP+1,nz,'rooted_dirt');
  setb(nx-1,Y_TOP+2,nz-1,'sculk_vein[down=true]'); setb(nx+1,Y_TOP+2,nz-1,'sculk_vein[down=true]');
  // zyly na krawedziach plamy
  setb(nx-2,Y_TOP+1,nz+1,'sculk_vein[down=true]'); setb(nx+2,Y_TOP+1,nz,'sculk_vein[down=true]');
  setb(nx,Y_TOP+1,nz+2,'sculk_vein[down=true]');
  // DROGOWSKAZ: iglica od TYLU (polnoc) z blaskiem — czytelny cel z areny, nie blokuje ciosu od pld
  box(nx,Y_TOP+1,nz-2,nx,Y_TOP+4,nz-2,'72%polished_basalt,28%sculk');
  setb(nx,Y_TOP+5,nz-2,'soul_lantern');            // skazona latarnia = "tu jest wezel"
  setb(nx,Y_TOP+3,nz-2,'sculk_vein[south=true]');
}
// --- Q2. STANOWISKO ZIELARZA w obozie (hook do sekretu w grocie 27,64,76) ---
// przy malym namiocie brown_wool (-10..-9, 128..130); nie blokuje sciezki (os x0)
setb(-8,Y_TOP+1,132,'lectern[facing=east]');            // notatki zielarza
setb(-8,Y_TOP+1,134,'cauldron');                         // kociol
setb(-9,Y_TOP+1,135,'composter');
box(-11,Y_TOP+1,133,-11,Y_TOP+2,133,'oak_fence');        // suszarnia
setb(-11,Y_TOP+3,133,'hay_block');
setb(-10,Y_TOP+1,136,'sweet_berry_bush');
setb(-9,Y_TOP+1,132,'potted_fern');
setb(-11,Y_TOP+1,136,'barrel[facing=up]');
setb(-8,Y_TOP+1,136,'candle[candles=3,lit=true]');
// --- Q3. SIGHTLINES: kadrowane okna na Drzewo-Serce (0,-22) ---
// (a) GLIMPSE z polany prob (0,72): wytnij liscie drzew trasy przy osi w oknie
//     x -4..4, y wierzcholkow (Y_TOP+8..+16) miedzy z=40..52 (drzewa (10,44),(-10,50) i WALL pld)
sel(-4,Y_TOP+8,38,4,Y_TOP+16,54);
push('//replace oak_leaves,azalea_leaves,flowering_azalea_leaves,cherry_leaves,dark_oak_leaves air');
// rama konarow nad oknem (luk, nie plyta)
setb(-4,Y_TOP+12,46,'oak_log[axis=x]'); setb(-3,Y_TOP+13,46,'oak_log[axis=x]');
setb(3,Y_TOP+13,46,'oak_log[axis=x]');  setb(4,Y_TOP+12,46,'oak_log[axis=x]');
box(-2,Y_TOP+14,46,2,Y_TOP+14,46,'35%oak_leaves,30%azalea_leaves,35%air');
// (b) PARTIAL z mostu (0,54): poszerz klin widokowy w koronach sciany gaju przy przeleczy
//     (WALL pld-wsch/pld-zach przy z 28..40) — pien + komora serca widoczne z pomostu
sel(-6,Y_TOP+10,26,6,Y_TOP+22,40);
push('//replace oak_leaves,azalea_leaves,flowering_azalea_leaves,cherry_leaves,dark_oak_leaves air');
// --- Q4. KOMORA NAGROD: dressing "oczyszczone serce" + skrzynia na piedestale ---
setb(0,Y_TOP-3,-39,'chest[facing=south]');               // skrzynia nagrody na piedestale (nad wall y59-60)
setb(-1,Y_TOP-2,-38,'amethyst_cluster');                  // cluster przesuniety obok
gmask('air');
box(-3,Y_TOP-1,-42,3,Y_TOP-1,-36,'12%spore_blossom,88%air'); // kwiaty zarodnikow ze stropu
gmask();
setb(-3,Y_TOP-4,-41,'glow_lichen[south=true]'); setb(3,Y_TOP-4,-37,'glow_lichen[north=true]');
setb(-1,Y_TOP-5,-41,'moss_carpet'); setb(1,Y_TOP-5,-37,'moss_carpet'); setb(2,Y_TOP-5,-40,'moss_carpet');
// --- Q5. SCULK V2: jezor blizej trasy + lancuch sensorow (puls przy przejsciu) ---
gmask('grass_block,moss_block,podzol');
box(4,Y_TOP,-13,10,Y_TOP,-9,'40%sculk,35%grass_block,15%moss_block,10%podzol'); // przedluzenie jezora ku sciezce (y64, poza r12 w wiekszosci; podloga tylko)
gmask();
setb(7,Y_TOP,-13,'sculk_sensor'); setb(9,Y_TOP,-15,'sculk_sensor');
setb(11,Y_TOP,-17,'sculk_sensor'); setb(13,Y_TOP,-19,'sculk_sensor');
setb(5,Y_TOP+1,-11,'sculk_vein[down=true]'); setb(10,Y_TOP+1,-14,'sculk_vein[down=true]');
// --- Q6. OSLONY LoS w path_a (zasadzka @6,111 r10): glazy/krzew obok sciezki ---
// glaz E (za nim mozna zerwac LoS lucznika)
ell(11,Y_TOP+1,114,2,2,2,'55%stone,25%mossy_cobblestone,20%andesite','x^2+(y*1.2)^2+z^2<1');
setb(11,Y_TOP+3,114,'moss_carpet');
// glaz W
ell(0,Y_TOP+1,113,2,1,2,'60%stone,25%cobblestone,15%mossy_cobblestone','x^2+(y*1.3)^2+z^2<1');
// gruby zwalony pien (oslona pozioma)
box(9,Y_TOP+1,107,12,Y_TOP+1,107,'oak_log[axis=x]');
box(9,Y_TOP+2,107,10,Y_TOP+2,107,'55%moss_carpet,45%air');
// krzew 2-wysoki
setb(3,Y_TOP+1,117,'azalea'); setb(2,Y_TOP+1,117,'moss_block'); setb(2,Y_TOP+2,117,'flowering_azalea');
} // koniec QUALITY

// ================= WYKONANIE =================
// DRY-RUN: DRY=1 node build_level1_grove.js <faza>  -> zrzuca komendy do pliku, NIE laczy bota
if(process.env.DRY){
  const fs=require('fs');
  const out=`/tmp/claude-1000/-home-przemek/98025f4d-147d-4c03-9f0f-4f8fe77b0d95/scratchpad/cmds_${PHASE}.txt`;
  fs.writeFileSync(out, C.join('\n'));
  console.log(`[DRY] faza=${PHASE} komend=${C.length} -> ${out}`);
  process.exit(0);
}
(async()=>{
  const rcon=await Rcon.connect({host:RHOST,port:RPORT,password:RPASS}); // RCON -> srv-world (nie velocity)
  console.log('[rcon] ok (srv-world '+RHOST+'), komend do wykonania:',C.length);
  const bot=mineflayer.createBot({host:HOST,port:PORT,username:USER,auth:'offline',version:'1.21.11'});
  bot.on('resourcePack',()=>bot.acceptResourcePack());
  bot.on('kicked',r=>console.log('[KICK]',JSON.stringify(r)));
  bot.on('error',e=>console.log('[BOT-ERR]',e.message));
  bot._client.on('error',()=>{}); // edit-mode zalewa particlami -> PartialReadError
  // pack z wezlami skazenia (~onTimer particle FX) zalewa sesje edycji -> protodef rzuca na
  // deserializacji Particle/Chunk. Swallow CALY ten szum (inaczej 500MB+ stack-trace'ow/min na dysk).
  const NOISE=/PartialReadError|Read error|protodef|Particle|Chunk|deserialize|SizeOf|compiler\.js|RangeError|Cannot read prop/;
  process.on('uncaughtException',e=>{ const s=String(e&&(e.stack||e.message)||e); if(!NOISE.test(s)) console.log('[UNCAUGHT]',(e&&e.message)||s); });
  process.on('unhandledRejection',r=>{ const s=String(r&&(r.stack||r.message)||r); if(!NOISE.test(s)) console.log('[UNHANDLED]',s.slice(0,200)); });
  const WD=setTimeout(()=>{console.log('[FATAL] watchdog');process.exit(8);},4500000); // 75min: build urosl do ~8.5k komend

  let spawns=0; bot.on('spawn',()=>spawns++);
  let msgs=0; bot.on('message',m=>{ if(msgs++<40) console.log('[chat]',m.toString().slice(0,140)); });
  if(!await waitFor(()=>!!bot.entity,90000,500)){console.log('[FATAL] brak spawnu');process.exit(7);}
  // velocity (try=[items,world,hub]) ląduje bota na srv-items -> przełącz na world PRZED /md edit
  { const s0=spawns; console.log('[step] /server world (przełączam z domyślnego backendu)...'); bot.chat('/server world');
    const sw=await waitFor(()=>spawns>s0,15000,300); await sleep(3500);
    console.log('[bot] po /server world switched='+sw+' dim='+bot.game?.dimension+' pos='+bot.entity?.position); }
  await rcon.send(`op ${USER}`); await sleep(600);
  const before=spawns;
  console.log('[step] /md edit level_1 (czekam na zmiane swiata)...');
  bot.chat('/md edit level_1');
  // Bramka: zmiana swiata ALBO juz jestesmy w otwartej sesji edycji (bot loguje sie w level_1_0,
  // log serwera: "Autosave of level_1" co 5 min = sesja edycji otwarta). Weryfikacja koncowa i tak
  // idzie przez screenshoty template.
  const changed=await waitFor(()=>spawns>before,15000,300);
  if(!changed) console.log('[warn] brak zmiany swiata po /md edit — zakladam otwarta sesje edycji (login w level_1_0), kontynuuje.');
  await sleep(4000);
  console.log('[bot] weszlem do edycji. dim=',bot.game?.dimension,' pos=',bot.entity.position.toString());

  // === GUARD KRYTYCZNY (2026-07-06, po destrukcyjnym misfire) ===
  // Bot MUSI byc w instancji lochu (nazwa swiata zawiera "level_1"), NIE w glownym overworld (name="world").
  // Gdy /md edit faila (loch nie zaladowany / "No dungeon found"), bot zostaje w minecraft:overworld,
  // a FAWE //set zapisalby ZYWY swiat na spawnie. Sprawdzamy realna nazwe swiata bota z NBT Dimension.
  { let dim=''; try{ dim=await rcon.send(`execute as ${USER} run data get entity @s Dimension`); }catch(e){ dim='ERR '+((e&&e.message)||e); }
    console.log('[guard] Dimension bota ->', JSON.stringify(String(dim).slice(0,200)));
    if(!/level_1/.test(String(dim))){
      console.log('[FATAL] Bot NIE jest w instancji lochu (Dimension bez "level_1"). Loch pewnie sie nie zaladowal (sprawdz "md list" + logi InvocationTargetException). NIE buduje w zywym swiecie. ABORT.');
      try{bot.chat('/md leave');}catch{} await sleep(2000); clearTimeout(WD); process.exit(10);
    }
    console.log('[guard] OK — bot w instancji lochu. Buduje.');
  }

  let heavy=0;
  for(let i=0;i<C.length;i++){
    const c=C[i];
    bot.chat(c);
    const isHeavy=/^\/\/(set|generate|replace)/.test(c);
    await sleep(isHeavy?550:160);
    if(isHeavy) heavy++;
    if(i%25===0) console.log(`[cmd ${i}/${C.length}] ${c.slice(0,70)}`);
  }
  console.log('[build] komendy wyslane (ciezkich:',heavy,').');
  await sleep(2500);
  if(process.env.SKIP_LEAVE==='1'){
    console.log('[done] SKIP_LEAVE=1 — bez /md leave (autosave MD zapisze template).');
  } else {
    bot.chat('/md leave'); await sleep(8000); // zapis do template
    console.log('[done] /md leave wyslany. Koniec.');
  }
  clearTimeout(WD); try{await rcon.end();}catch{} try{bot.quit();}catch{}
  process.exit(0);
})().catch(e=>{console.error('FATAL',e);process.exit(1);});

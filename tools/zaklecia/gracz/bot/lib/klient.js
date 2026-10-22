'use strict';
const sleep = ms => new Promise(r => setTimeout(r, ms));

/** Model pakietów vanilla 1.21.11. Cel i komponenty są odświeżane przed każdą akcją. */
class Klient {
  constructor(bot, observer) {
    this.bot = bot; this.o = observer; this.down = {ppm:false,lpm:false}; this.using = false;
    this.destroying = false; this.screen = false; this.rightDelay = 0;
    this.movement = {}; this.timer = null; this.barOpen=false; this.barOpenedAt=0; this.barClosedAt=0;
  }
  item(hand='main') { return hand==='main' ? this.bot.heldItem : this.bot.inventory.slots[45]; }
  components(item) { return this.o.components(item); }
  gamemode() { return this.bot.game?.gameMode || 'survival'; }
  // ray_trace.js Mineflayer używa promienia od oczu i brył kolizji, tak jak
  // Minecraft.java:startAttack/startUseItem. Przeliczamy po każdym obrocie.
  target() {
    // Obserwator podłączony po spawn może nie dostać początkowego update_attributes.
    // Nośnik w prawej aktywuje tryb zaklęć: Rzucanie.java:159-161 ustawia oba zasięgi na 0.
    const spellMode=this.item()?.name==='debug_stick'&&this.components(this.item()).attack_range;
    const blockRange=this.o.attribute('minecraft:block_interaction_range') ?? (spellMode?0:4.5);
    const entityRange=this.o.attribute('minecraft:entity_interaction_range') ?? (spellMode?0:3);
    const attackRange=this.o.attribute('minecraft:entity_attack_range') ?? 3;
    const block=this.bot.blockAtCursor?.(blockRange);
    let entity=this.bot.entityAtCursor?.(Math.max(entityRange,attackRange));
    const eye=this.bot.entity.position.offset(0,this.bot.entity.eyeHeight||1.62,0);
    // Mineflayer 4.39 bywa bez width/height huska w tej wersji protokołu;
    // jego entityAtCursor zwraca wtedy null mimo widocznej istoty.
    if(!entity){const yaw=this.bot.entity.yaw||0,pitch=this.bot.entity.pitch||0;
      const dir={x:-Math.sin(yaw)*Math.cos(pitch),y:Math.sin(pitch),z:-Math.cos(yaw)*Math.cos(pitch)};
      let nearest=Infinity;for(const e of Object.values(this.bot.entities||{})){
        if(e.id===this.bot.entity.id||e.type==='object'||!e.position)continue;
        const h=e.height||1.95,w=e.width||0.6,center=e.position.offset(0,h/2,0),v=center.minus(eye);
        const along=v.x*dir.x+v.y*dir.y+v.z*dir.z,side=Math.sqrt(Math.max(0,v.x*v.x+v.y*v.y+v.z*v.z-along*along));
        if(along>0&&along<=Math.max(entityRange,attackRange)&&side<=Math.max(w/2,0.38)&&along<nearest){entity=e;nearest=along;}
      }
    }
    const bd=block?.intersect?.distanceTo(eye) ?? Infinity;
    const ed=entity?.position?.distanceTo(eye) ?? Infinity;
    if(entity && ed<=Math.min(Math.max(entityRange,attackRange),bd))return {type:'entity',entity,distance:ed,attackRange,interactable:ed<=entityRange};
    if(block && bd<=blockRange)return {type:'block',block,position:block.position,face:block.face??1,distance:bd};
    return {type:'air',distance:Infinity};
  }
  allow() { return !this.screen && !this.bot.currentWindow && this.gamemode()!=='spectator'; }
  swing() { this.bot.swingArm('right'); }
  // Minecraft.java:1998-2018: isUsingItem połyka klik ataku, a atak jest przed użyciem.
  lpmKlik() {
    if (!this.allow() || this.using) return false;
    const target=this.target();
    const c=this.components(this.item());
    if (c.piercing_weapon) { this.bot._client.write('block_dig',{status:7,location:{x:0,y:0,z:0},face:0,sequence:0}); this.swing(); return true; }
    const range=this.o.attackRange?.(this.item());
    const d=target.distance;
    if (target.type==='entity' && d<=target.attackRange && (!range||d>=range.min&&d<=range.max)) this.bot.attack(target.entity);
    else if (target.type==='block') {
      this.destroying=true;
      this.bot._client.write('block_dig',{status:0,location:target.position,face:target.face,sequence:0});
      // Pojedynczy klik kończy niszczenie po tiknięciu; bez ABORT kolejne PPM
      // były stale blokowane przez stan klienta aż do końca całej grupy.
      setTimeout(()=>{if(this.destroying){this.bot._client.write('block_dig',{status:1,location:target.position,face:target.face,sequence:0});this.destroying=false;}},50);
    } else this.swing(); // Minecraft.java:1639-1695: MISS emituje animację, nie atak istoty.
    return true;
  }
  async lpmTrzymaj(ms=300) { this.down.lpm=true; this.lpmKlik(); await sleep(ms); this.down.lpm=false; if(this.destroying){const t=this.target();this.bot._client.write('block_dig',{status:1,location:t.position||{x:0,y:0,z:0},face:t.face??1,sequence:0});this.destroying=false;} }
  // Minecraft.java:1700-1769: isDestroying blokuje PPM; obie ręce w kolejności,
  // pusta ręka + MISS nie wywołuje gameMode.useItem.
  ppmWcisnij() {
    this.down.ppm=true;
    if (!this.allow() || this.destroying || this.using) return false;
    const target=this.target();
    for(const hand of ['main','off']) {
      const item=this.item(hand), c=this.components(item);
      const physical=!!item && item.type!==0;
      if(target.type==='entity' && target.interactable) {
        // Minecraft.java:1718-1735 kontynuuje do useItem i drugiej dłoni,
        // gdy interactAt/interact na nieinteraktywnym celu zwróci PASS.
        this.bot._client.write('use_entity',{target:target.entity.id,mouse:0,sneaking:!!this.movement.sneak,hand:hand==='main'?0:1,location:{x:0,y:0,z:0}});
      }
      if(target.type==='block' && hand==='main') this.bot.activateBlock(target.block);
      if(!physical) continue;
      // Blokujący przedmiot/łuk/jedzenie przejmuje PPM. Komponenty z pakietów mają pierwszeństwo.
      this.bot.activateItem(hand==='off');
      const name=item.name||'';
      const consumes=!!(c.consumable || c.blocks_attacks || c.kinetic_weapon || c.use_duration || /shield|bow|crossbow|potion|bread|apple|food|map/.test(name));
      this.using=consumes;
      this.rightDelay=4;
      if(consumes || hand==='off')return true;
    }
    return false;
  }
  // Minecraft.java:1998-2002 i MultiPlayerGameMode.releaseUsingItem: RELEASE_USE_ITEM.
  ppmPusc() { this.down.ppm=false; if(this.using){this.bot.deactivateItem();this.using=false;} }
  async ppmTrzymaj(ms=500) { this.ppmWcisnij(); for(let elapsed=0;elapsed+200<=ms;elapsed+=200){await sleep(200);if(elapsed+200<ms&&!this.using)this.ppmWcisnij();} await sleep(ms%200);this.ppmPusc(); }
  // Minecraft.java:2032-2034: próba użycia co 4 tiki przy wciśniętym PPM.
  async obie(ms=500) { this.ppmWcisnij(); await sleep(70); this.lpmKlik();let elapsed=70;
    while(elapsed+200<ms){await sleep(200);elapsed+=200;if(!this.using)this.ppmWcisnij();}
    await sleep(ms-elapsed);this.ppmPusc(); }
  // Minecraft.java:1932-1944: cyfry ustawiają wybrany slot; kółko również aktualizuje wybrany slot.
  cyfra(n) { if(n<1||n>9)throw Error('slot 1..9');if(this.screen||this.bot.currentWindow)return false;const same=this.bot.quickBarSlot===n-1;this.bot.setQuickBarSlot(n-1);if(same)this.bot._client.write('held_item_slot',{slotId:n-1});return true; }
  kolko(delta=1) { if(this.screen||this.bot.currentWindow)return false;this.bot.setQuickBarSlot((this.bot.quickBarSlot+delta+9)%9);return true; }
  // Minecraft.java:1977-1987: F=SWAP_ITEM_WITH_OFFHAND; Q/Ctrl+Q=DROP/DROP_ALL.
  f() { if(!this.screen&&!this.bot.currentWindow&&this.gamemode()!=='spectator')this.bot._client.write('block_dig',{status:6,location:{x:0,y:0,z:0},face:0,sequence:0}); }
  // LocalPlayer.java:313-319: pakiet wyrzucenia idzie nawet przy pustym slocie;
  // machnięcie występuje tylko, gdy removeFromSelected zwrócił przedmiot.
  q(ctrl=false) { if(this.screen||this.bot.currentWindow||this.gamemode()==='spectator')return false; const held=!!this.item();this.bot._client.write('block_dig',{status:ctrl?3:4,location:{x:0,y:0,z:0},face:0,sequence:0}); if(held)this.swing();if(this.barOpen&&!this.movement.shift){this.barOpen=false;this.barClosedAt=Date.now();}return true; }
  // Inventory click/drop: Minecraft.java:445-470, w otwartym oknie Q jest kliknięciem DROP slotu.
  async qWMenu(slot,ctrl=false){if(!this.bot.currentWindow)return false;await this.bot.clickWindow(slot,ctrl?1:0,4);return true;}
  async shiftQ() { if(this.screen||this.bot.currentWindow)return false;const wasOpen=this.barOpen,at=Date.now();this.shift(true); await sleep(120); this.q(); await sleep(120); this.shift(false);this.barOpen=!wasOpen;if(wasOpen)this.barClosedAt=at;else this.barOpenedAt=at;return true; }
  // Minecraft.java:1832-1841: przy otwartym ekranie handleKeybinds nie działa.
  e() { if(this.bot.currentWindow)throw Error('E przy otwartym oknie');this.screen=true;return this.bot.inventory; }
  zamknij() { if(this.bot.currentWindow)this.bot.closeWindow(this.bot.currentWindow); else if(this.screen)this.bot.closeWindow(this.bot.inventory);this.screen=false;this.closedAt=Date.now(); }
  // LocalPlayer.java:231: realny klient wysyła ServerboundPlayerInputPacket z flagami ruchu.
  input(patch) { Object.assign(this.movement,patch); this.bot._client.write('player_input',{inputs:{...this.movement}}); }
  sprint(v=true){this.input({sprint:v});this.bot.setControlState('sprint',v);}
  skok(v=true){this.input({jump:v});this.bot.setControlState('jump',v);}
  shift(v=true){this.input({shift:v});this.bot.setControlState('sneak',v);}
  w(v=true){this.input({forward:v});this.bot.setControlState('forward',v);}
  async relog(wait=1200){this.ppmPusc();this.bot.quit();await sleep(Math.max(1000,wait));}
  close(){this.ppmPusc();this.bot.clearControlStates();}
}
module.exports={Klient,sleep};

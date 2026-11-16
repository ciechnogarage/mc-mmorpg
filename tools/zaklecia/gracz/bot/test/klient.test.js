'use strict';
const test=require('node:test');const assert=require('node:assert/strict');
const {Klient}=require('../lib/klient');
const {Vec3}=require('vec3');
function rig(main=null,off=null){
  const sent=[],activated=[];
  const bot={heldItem:main,inventory:{slots:Array(46).fill(null)},quickBarSlot:0,game:{gameMode:'survival'},
    entity:{id:1,position:new Vec3(0,0,0),eyeHeight:1.62,yaw:0,pitch:0},entities:{},blockAtCursor:()=>null,entityAtCursor:()=>null,
    _client:{write:(name,data)=>sent.push({name,data})},activateItem:hand=>activated.push(hand),deactivateItem:()=>sent.push({name:'release'}),
    swingArm:()=>sent.push({name:'swing'}),setControlState:()=>{},setQuickBarSlot:()=>{},clearControlStates:()=>{}};
  bot.inventory.slots[45]=off;const o={components:i=>i?.components||{},attribute:()=>null};
  return {k:new Klient(bot,o),sent,activated};
}
test('pusta ręka + PPM w powietrze nie wysyła use_item',()=>{const {k,activated}=rig();assert.equal(k.ppmWcisnij(),false);assert.deepEqual(activated,[]);});
test('nieużywalny nośnik w prawej dopuszcza próbę lewą',()=>{const {k,activated}=rig({type:1,name:'debug_stick'},{type:2,name:'debug_stick'});assert.equal(k.ppmWcisnij(),true);assert.deepEqual(activated,[false,true]);});
test('trzymane jedzenie połyka LPM i puszczenie wysyła release',()=>{const {k,sent}=rig({type:3,name:'bread'},{type:2,name:'debug_stick'});k.ppmWcisnij();assert.equal(k.lpmKlik(),false);k.ppmPusc();assert.equal(sent.filter(x=>x.name==='release').length,1);});
test('Q pustym slotem wysyła DROP_ITEM bez machnięcia',()=>{const {k,sent}=rig();k.q();assert.deepEqual(sent.map(x=>x.name),['block_dig']);assert.equal(sent[0].data.status,4);});
test('Q po pasku zapisuje moment jego zamknięcia',()=>{const {k}=rig();k.barOpen=true;k.q();assert.equal(k.barOpen,false);assert.ok(k.barClosedAt>0);});
test('F wysyła SWAP_ITEM_WITH_OFFHAND, sprint idzie w player_input',()=>{const {k,sent}=rig();k.f();k.sprint(true);assert.equal(sent[0].data.status,6);assert.deepEqual(sent[1].data.inputs,{sprint:true});});
test('okno blokuje Shift+Q',async()=>{const {k,sent}=rig();k.screen=true;assert.equal(await k.shiftQ(),false);assert.equal(sent.length,0);});

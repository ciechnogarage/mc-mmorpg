'use strict';
const test=require('node:test');const assert=require('node:assert/strict');
const {parseSlot,slotKlienta,clientItem}=require('../lib/niezmienniki');
test('odczyt serwerowego slotu bez komponentów i z komponentami',()=>{
  assert.deepEqual(parseSlot('MagTest has the following entity data: {Slot: 6b, id: "minecraft:stone_sword", count: 1, components: {...'),{id:'minecraft:stone_sword',count:1});
  assert.equal(parseSlot('No element matching Inventory[{Slot:6b}] was found'),null);
});
test('mapowanie slotów NBT do ekwipunku gracza',()=>{
  assert.equal(slotKlienta(0),36);assert.equal(slotKlienta(18),18);
  assert.equal(slotKlienta(-106),45);assert.equal(slotKlienta(103),5);
});
test('otwarte menu nie miesza swoich ikon z ekwipunkiem gracza',()=>{
  const slots=Array(90).fill(null);slots[81]={name:'paper',count:1};slots[54]={name:'stone',count:2};
  const own=Array(46).fill(null);own[36]={name:'book',count:1};
  const bot={inventory:{slots:own},currentWindow:{slots}};
  assert.equal(clientItem(bot,0).name,'book');assert.equal(clientItem(bot,9),null);
});

'use strict';
const test=require('node:test');const assert=require('node:assert/strict');
const {lore}=require('../lib/macierz');
test('parsuje faktyczne lore przedmiotu Płomienie',()=>{
  const x=lore('plomienie');assert.equal(x.cost,1);assert.equal(x.damage,8);assert.equal(x.range,6);
});
test('brak liczby w lore pozostaje brakiem, a nie zerem',()=>{
  assert.equal(lore('uzdrowienie_innego').cost,null);
});

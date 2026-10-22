'use strict';
const test=require('node:test');const assert=require('node:assert/strict');const {Wyniki}=require('../lib/wynik');
test('próg feelingu zamienia zbyt wolny PASS w FAIL',()=>{const w=new Wyniki('T');w.add('T1',true,'za wolno',{latencyTicks:11},{latencyTicks:'≤10'});assert.equal(w.rows[0].status,'FAIL');});
test('brak wymaganej metryki jest FAIL',()=>{const w=new Wyniki('T');w.add('T2',true,'brak pakietu',{latencyTicks:null},{latencyTicks:'≤10'});assert.equal(w.rows[0].status,'FAIL');});
test('poprawna metryka pozostaje PASS',()=>{const w=new Wyniki('T');w.add('T3',true,'szybko',{latencyTicks:4},{latencyTicks:'≤10'});assert.equal(w.rows[0].status,'PASS');});

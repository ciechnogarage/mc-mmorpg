'use strict';
module.exports=ctx=>{
  ctx.skipMinimize=true;
  ctx.finalInventoryCheck=true;
  return require('../lib/fuzzer').runFuzz(271828,ctx,1000);
};

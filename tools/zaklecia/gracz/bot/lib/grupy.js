'use strict';
function discoverGroups(files,filter=''){
  const all=files.filter(x=>/^[A-Z][A-Za-z0-9_-]*\.js$/.test(x)).map(x=>x.slice(0,-3)).sort();
  if(!filter)return all;
  const names=filter.split(',').filter(Boolean);
  const selected=new Set();
  for(const name of names){
    if(all.includes(name))selected.add(name);
    else if(/^[A-Z]+$/.test(name))for(const letter of name)if(all.includes(letter))selected.add(letter);
  }
  return all.filter(x=>selected.has(x));
}
module.exports={discoverGroups};

import re,sys,os
roots=sys.argv[1:]
for r in roots:
  for dp,dn,fn in os.walk(r):
    if '/build' in dp: continue
    for f in fn:
      if not f.endswith('.java'): continue
      p=os.path.join(dp,f); L=open(p,encoding='utf8').read().split('\n')
      for i,l in enumerate(L):
        if '@EventHandler' not in l: continue
        # find signature
        j=i; sig=''
        while j<min(i+4,len(L)):
          m=re.search(r'void\s+(\w+)\s*\(\s*([\w.]+)',L[j])
          if m: sig=m.group(2); name=m.group(1); break
          j+=1
        if not sig or not re.search(r'Damage|Attack',sig): continue
        pr=re.search(r'priority\s*=\s*EventPriority\.(\w+)',l+L[j]); ic='ignoreCancelled = true' in (l+L[j]) or 'ignoreCancelled=true' in (l+L[j])
        # body by brace count
        depth=0; body=[]; started=False
        for k in range(j,min(j+200,len(L))):
          body.append(L[k]); depth+=L[k].count('{')-L[k].count('}')
          if '{' in L[k]: started=True
          if started and depth<=0: break
        b='\n'.join(body)
        ops=sorted(set(re.findall(r'setDamage|setCancelled|setDamageModifier|getDamage\(\)|multiplicativeModifier|additiveModifier|getDamage\(\)\.|add\w*Modifier|\.damage\(',b)))
        calls=re.findall(r'(\w+)\(e\)|(\w+)\(event\)',b)
        print(f"{p.split('plugin-src/')[-1].split('trafienia/plugin/')[-1]}:{j+1} {name}({sig.split('.')[-1]}) {pr.group(1) if pr else 'NORMAL'}{' ic' if ic else ''} ops={ops} lines={len(body)}")

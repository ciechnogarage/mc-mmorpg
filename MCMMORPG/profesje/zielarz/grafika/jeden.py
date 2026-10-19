import sys
from PIL import Image
im=Image.open(sys.argv[1]).convert('RGBA'); s=int(sys.argv[3]) if len(sys.argv)>3 else 10
o=Image.new('RGB',(im.width*s*2+30,im.height*s+20),(122,160,88)); b=im.resize((im.width*s,im.height*s),Image.NEAREST)
o.paste(b,(10,10),b); t=Image.new('RGB',(im.width*s,im.height*s),(60,50,40)); t.paste(b,(0,0),b); o.paste(t,(20+im.width*s,10)); o.save(sys.argv[2])

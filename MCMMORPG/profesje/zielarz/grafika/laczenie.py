import sys
from PIL import Image
ims=[Image.open(p) for p in sys.argv[2:]]
w=max(i.width for i in ims); h=sum(i.height for i in ims)
o=Image.new('RGB',(w,h),(122,160,88)); y=0
for i in ims: o.paste(i,(0,y)); y+=i.height
o.save(sys.argv[1])

import zipfile, struct, zlib, sys, os
def extract(path, outdir):
    z=zipfile.ZipFile(path); f=open(path,'rb'); ok=bad=0
    for zi in z.infolist():
        if zi.is_dir(): continue
        f.seek(zi.header_offset); h=f.read(30)
        n,e=struct.unpack('<HH',h[26:30])
        f.seek(zi.header_offset+30+n+e); raw=f.read(zi.compress_size)
        try:
            data = zlib.decompress(raw,-15) if zi.compress_type==8 else raw
            p=os.path.join(outdir, zi.filename.lstrip('/'))
            if '..' in p: continue
            os.makedirs(os.path.dirname(p),exist_ok=True); open(p,'wb').write(data); ok+=1
        except Exception as ex: bad+=1
    print(path, 'ok',ok,'bad',bad)
extract(sys.argv[1], sys.argv[2])

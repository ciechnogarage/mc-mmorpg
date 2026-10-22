import socket, struct, json, sys, time, base64
def varint(n):
    out=b''
    while True:
        b=n&0x7f; n>>=7
        if n: out+=bytes([b|0x80])
        else: out+=bytes([b]); return out
def readvarint(s):
    n=0
    for i in range(5):
        b=s.recv(1)[0]; n|=(b&0x7f)<<(7*i)
        if not b&0x80: return n
def ping(host, port=25565, proto=774):
    s=socket.create_connection((host,port),timeout=10)
    h=host.encode()
    data=varint(0)+varint(proto)+varint(len(h))+h+struct.pack('>H',port)+varint(1)
    s.send(varint(len(data))+data); s.send(b'\x01\x00')
    t=time.time(); ln=readvarint(s); pid=readvarint(s); sl=readvarint(s)
    buf=b''
    while len(buf)<sl: buf+=s.recv(sl-len(buf))
    s.send(b'\x09\x01'+struct.pack('>q',1)); s.recv(10); rtt=(time.time()-t)*1000
    return json.loads(buf), rtt
host=sys.argv[1]; proto=int(sys.argv[2]) if len(sys.argv)>2 else 774
d,rtt=ping(host,proto=proto)
fav=d.pop('favicon',None)
if fav: open(sys.argv[3] if len(sys.argv)>3 else '/tmp/fav.png','wb').write(base64.b64decode(fav.split(',')[1]))
d['_rtt_ms']=round(rtt); d['_host']=host; d['_proto_sent']=proto
print(json.dumps(d,ensure_ascii=False))

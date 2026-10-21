"""Minimalny zapis NBT (big-endian) - tylko typy potrzebne do map_<id>.dat i idcounts.dat."""
import struct

class Byte(int): pass
class Int(int): pass
class IntArray(list): pass

def _tag(v):
    if isinstance(v, IntArray): return 11
    if isinstance(v, Byte): return 1
    if isinstance(v, Int) or (isinstance(v, int) and not isinstance(v, bool)): return 3
    if isinstance(v, (bytes, bytearray)): return 7
    if isinstance(v, str): return 8
    if isinstance(v, list): return 9
    if isinstance(v, dict): return 10
    raise TypeError(type(v))

def _str(s):
    b = s.encode("utf8"); return struct.pack(">H", len(b)) + b

def _payload(v):
    t = _tag(v)
    if t == 1: return struct.pack(">b", v)
    if t == 3: return struct.pack(">i", v)
    if t == 7: return struct.pack(">i", len(v)) + bytes(v)
    if t == 8: return _str(v)
    if t == 11: return struct.pack(">i", len(v)) + b"".join(struct.pack(">i", x) for x in v)
    if t == 9:
        et = _tag(v[0]) if v else 0
        return struct.pack(">bi", et, len(v)) + b"".join(_payload(x) for x in v)
    out = b""
    for k, x in v.items(): out += struct.pack(">b", _tag(x)) + _str(k) + _payload(x)
    return out + b"\x00"

def dumps(root):
    return b"\x0a" + _str("") + _payload(root)

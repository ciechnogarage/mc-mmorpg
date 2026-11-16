#!/usr/bin/env python3
"""Czytnik plikow regionow Anvil (MC 1.18+ / 1.21) - bez zaleznosci zewnetrznych.

Po co: zrzuty ekranu klamia (40% zrzutow poprzedniego projektu bylo pustym niebem),
a pliki regionow nie. Tym mierzymy, co Iris NAPRAWDE wygenerowal.

API:
    r = Region(sciezka_do_mca)
    r.chunks()                 -> [(cx, cz), ...] chunki obecne w pliku
    top_block(world, x, z)     -> (y, "minecraft:...") najwyzszy nie-powietrzny blok
"""
import os, struct, zlib, gzip, io, math

AIR = {"minecraft:air", "minecraft:cave_air", "minecraft:void_air"}

# ---------- minimalny czytnik NBT ----------
class _R:
    def __init__(self, b): self.b, self.i = b, 0
    def u1(self): v = self.b[self.i]; self.i += 1; return v
    def i1(self): v = struct.unpack_from(">b", self.b, self.i)[0]; self.i += 1; return v
    def i2(self): v = struct.unpack_from(">h", self.b, self.i)[0]; self.i += 2; return v
    def u2(self): v = struct.unpack_from(">H", self.b, self.i)[0]; self.i += 2; return v
    def i4(self): v = struct.unpack_from(">i", self.b, self.i)[0]; self.i += 4; return v
    def i8(self): v = struct.unpack_from(">q", self.b, self.i)[0]; self.i += 8; return v
    def f4(self): v = struct.unpack_from(">f", self.b, self.i)[0]; self.i += 4; return v
    def f8(self): v = struct.unpack_from(">d", self.b, self.i)[0]; self.i += 8; return v
    def s(self):
        n = self.u2(); v = self.b[self.i:self.i+n].decode("utf8", "replace"); self.i += n; return v

def _payload(r, t):
    if t == 1: return r.i1()
    if t == 2: return r.i2()
    if t == 3: return r.i4()
    if t == 4: return r.i8()
    if t == 5: return r.f4()
    if t == 6: return r.f8()
    if t == 7: n = r.i4(); v = r.b[r.i:r.i+n]; r.i += n; return v
    if t == 8: return r.s()
    if t == 9:
        it = r.u1(); n = r.i4(); return [_payload(r, it) for _ in range(n)]
    if t == 10:
        d = {}
        while True:
            tt = r.u1()
            if tt == 0: return d
            # UWAGA: nazwa MUSI byc odczytana do zmiennej przed payloadem.
            # W 'd[r.s()] = _payload(...)' Python liczy prawa strone PIERWSZA,
            # wiec nazwa czytalaby sie po zawartosci i strumien by sie rozjechal.
            key = r.s()
            d[key] = _payload(r, tt)
    if t == 11: n = r.i4(); return [r.i4() for _ in range(n)]
    if t == 12: n = r.i4(); return [r.i8() for _ in range(n)]
    raise ValueError(f"nieznany tag NBT {t}")

def parse_nbt(data):
    r = _R(data); t = r.u1()
    if t == 0: return {}
    r.s()
    return _payload(r, t)

# ---------- region ----------
class Region:
    def __init__(self, path):
        self.path = path
        with open(path, "rb") as f: self.raw = f.read()
        base = os.path.basename(path).split(".")          # r.<x>.<z>.mca
        self.rx, self.rz = int(base[1]), int(base[2])

    def _entry(self, cx, cz):
        i = ((cx & 31) + (cz & 31) * 32) * 4
        off = int.from_bytes(self.raw[i:i+3], "big"); cnt = self.raw[i+3]
        return off, cnt

    def chunks(self):
        out = []
        for cz in range(32):
            for cx in range(32):
                if self._entry(cx, cz)[0]:
                    out.append((self.rx * 32 + cx, self.rz * 32 + cz))
        return out

    def chunk(self, cx, cz):
        off, cnt = self._entry(cx, cz)
        if not off: return None
        p = off * 4096
        ln = struct.unpack_from(">I", self.raw, p)[0]
        comp = self.raw[p+4]
        blob = self.raw[p+5:p+4+ln]
        if comp == 1: blob = gzip.decompress(blob)
        elif comp == 2: blob = zlib.decompress(blob)
        elif comp == 3: pass
        else: raise ValueError(f"kompresja {comp} nieobslugiwana")
        return parse_nbt(blob)

def _section_block(sec, lx, ly, lz):
    bs = sec.get("block_states")
    if not bs: return None
    pal = bs.get("palette") or []
    if not pal: return None
    if len(pal) == 1: return pal[0].get("Name")
    data = bs.get("data")
    if not data: return pal[0].get("Name")
    bits = max(4, (len(pal) - 1).bit_length())
    per = 64 // bits                      # indeksy NIE przechodza przez granice longow (1.16+)
    idx = ly * 256 + lz * 16 + lx
    lo, sh = divmod(idx, per)
    v = (data[lo] >> (sh * bits)) & ((1 << bits) - 1)
    return pal[v].get("Name") if v < len(pal) else None

class World:
    def __init__(self, region_dir):
        self.dir = region_dir
        self.cache = {}
    def _region(self, cx, cz):
        rx, rz = cx >> 5, cz >> 5
        if (rx, rz) not in self.cache:
            p = os.path.join(self.dir, f"r.{rx}.{rz}.mca")
            self.cache[(rx, rz)] = Region(p) if os.path.exists(p) else None
        return self.cache[(rx, rz)]
    def top_block(self, x, z):
        cx, cz = x >> 4, z >> 4
        reg = self._region(cx, cz)
        if not reg: return (None, None)
        ch = reg.chunk(cx & 31, cz & 31)
        if not ch: return (None, None)
        secs = sorted(ch.get("sections", []), key=lambda s: s.get("Y", 0), reverse=True)
        lx, lz = x & 15, z & 15
        for sec in secs:
            sy = sec.get("Y", 0)
            for ly in range(15, -1, -1):
                nm = _section_block(sec, lx, ly, lz)
                if nm and nm not in AIR:
                    return (sy * 16 + ly, nm)
        return (None, None)

# ---------- kolumny i biomy (dopisane: weryfikacja wysp) ----------
def _chunk_for(world, x, z):
    reg = world._region(x >> 4, z >> 4)
    return reg.chunk((x >> 4) & 31, (z >> 4) & 31) if reg else None

def column(world, x, z):
    """Lista (y, blok) wszystkich NIE-powietrznych blokow w kolumnie, od gory."""
    ch = _chunk_for(world, x, z)
    if not ch: return None
    lx, lz = x & 15, z & 15
    out = []
    for sec in sorted(ch.get("sections", []), key=lambda s: s.get("Y", 0), reverse=True):
        sy = sec.get("Y", 0)
        for ly in range(15, -1, -1):
            nm = _section_block(sec, lx, ly, lz)
            if nm and nm not in AIR: out.append((sy * 16 + ly, nm))
    return out

def biome(world, x, y, z):
    """Biom w komorce 4x4x4 zawierajacej (x,y,z)."""
    ch = _chunk_for(world, x, z)
    if not ch: return None
    sy = y >> 4
    for sec in ch.get("sections", []):
        if sec.get("Y") != sy: continue
        bi = sec.get("biomes") or {}
        pal = bi.get("palette") or []
        if not pal: return None
        if len(pal) == 1: return pal[0]
        data = bi.get("data")
        bits = max(1, (len(pal) - 1).bit_length())
        per = 64 // bits
        idx = ((y & 15) >> 2) * 16 + ((z & 15) >> 2) * 4 + ((x & 15) >> 2)
        lo, sh = divmod(idx, per)
        v = (data[lo] >> (sh * bits)) & ((1 << bits) - 1)
        return pal[v] if v < len(pal) else None
    return None

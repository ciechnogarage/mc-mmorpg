#!/usr/bin/env bash
# Wgrywa wygenerowane skorki na mineskin.org i zapisuje podpisane tekstury do skins.yml.
#
# DLACZEGO PRZEZ MINESKIN
#   Klient Javy pobiera skorki WYLACZNIE z textures.minecraft.net — adres spoza tej domeny
#   jest ignorowany, takze przy online-mode=false. Zeby wlasny PNG dalo sie zalozyc graczowi,
#   musi najpierw trafic na konto Mojanga i wrocic z podpisem. Robi to mineskin.org.
#
# DLACZEGO NIE PRZEZ /grabskin Z LIBSDISGUISES (tak bylo do 2026-09-20)
#   MineSkin v2 KOLEJKUJE zlecenia i odpowiada {"messages":[{"code":"job_queued"}]}.
#   LibsDisguises 11.0.15 nie zna tej odpowiedzi — w logu serwera laduje wtedy
#   "Received an unknown response from MineSkin", a skorka nie zapisuje sie NIGDY.
#   Udawalo sie tylko dla obrazow, ktore mineskin mial juz w cache'u i zwracal od razu.
#   Dlatego gadamy z API wprost: widac status zlecenia, limit tempa i blad.
#
# UZYCIE
#   ./gen.sh gen_race_skins.py     # najpierw PNG-i do tools/out/skins/
#   ./upload_skins.sh              # potem to (serwer nie musi dzialac)
#   MINESKIN_KEY=<klucz> ./upload_skins.sh   # z kluczem API limity sa wyzsze
#   SKIP_UPLOAD=1 ./upload_skins.sh          # sam skins.yml z pliku cache
#
# Wynik: src/main/resources/skins.yml (do jara) + tools/out/skins/.mineskin-cache.json,
# czyli mapa "plik PNG -> gotowa tekstura". Cache jest po to, zeby powtorne uruchomienie
# nie zuzywalo limitu na skorki, ktore juz maja teksture.
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
src="${SKINS:-$here/out/skins}"
out="${OUT:-$here/../src/main/resources/skins.yml}"
cache="${CACHE:-$src/.mineskin-cache.json}"
races="${RACES:-czlowiek ork elf nieumarly}"
classes="${CLASSES:-wojownik mag akolita ninja}"

[ -d "$src" ] || { echo "BLAD: brak $src — uruchom najpierw ./gen.sh gen_race_skins.py"; exit 1; }

python3 - "$src" "$out" "$cache" "$races" "$classes" "${SKIP_UPLOAD:-0}" <<'PY'
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request

src, out, cache_path, races, classes, skip_upload = (
    sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4].split(), sys.argv[5].split(), sys.argv[6] == '1')

API = 'https://api.mineskin.org/v2'
AGENT = 'MMORPG-CharacterCreator/1.0 (kreator postaci, world1)'
KEY = os.environ.get('MINESKIN_KEY', '').strip()


def request(url, data=None, content_type=None, timeout=60):
    headers = {'User-Agent': AGENT, 'Accept': 'application/json'}
    if KEY:
        headers['Authorization'] = 'Bearer ' + KEY
    if content_type:
        headers['Content-Type'] = content_type
    req = urllib.request.Request(url, data=data, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            return json.loads(response.read())
    except urllib.error.HTTPError as error:
        body = error.read()
        try:
            return json.loads(body)
        except ValueError:
            return {'success': False, 'messages': [{'message': f'HTTP {error.code}: {body[:200]!r}'}]}


def multipart(path, name):
    """Zapytanie multipart skladane recznie — zeby nie ciagnac 'requests' do kontenera."""
    boundary = '----mineskin' + hashlib.md5(path.encode()).hexdigest()
    with open(path, 'rb') as handle:
        png = handle.read()
    parts = []
    for field, value in (('variant', 'classic'), ('visibility', 'public'), ('name', name[:20])):
        parts.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{field}"\r\n\r\n{value}\r\n'.encode())
    parts.append(
        f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}.png"\r\n'
        f'Content-Type: image/png\r\n\r\n'.encode() + png + b'\r\n')
    parts.append(f'--{boundary}--\r\n'.encode())
    return b''.join(parts), f'multipart/form-data; boundary={boundary}'


def wait_for_limit(payload):
    """MineSkin sam mowi, ile czekac. Bez klucza API: 10 zlecen/min, 60/h."""
    limit = (payload or {}).get('rateLimit') or {}
    seconds = ((limit.get('delay') or {}).get('seconds') or 0)
    relative = ((limit.get('next') or {}).get('relative') or 0) / 1000.0
    delay = max(seconds, relative)
    if delay > 0:
        time.sleep(min(delay + 1, 120))


def fetch_texture(skin_uuid):
    payload = request(f'{API}/skins/{skin_uuid}')
    data = (((payload.get('skin') or {}).get('texture') or {}).get('data') or {})
    if data.get('value') and data.get('signature'):
        return data['value'], data['signature']
    return None


def upload(path, name):
    """Zwraca (value, signature) albo None. Zlecenie moze wrocic gotowe albo w kolejce."""
    body, content_type = multipart(path, name)
    payload = request(f'{API}/queue', data=body, content_type=content_type, timeout=120)
    job = payload.get('job') or {}
    for message in payload.get('messages') or []:
        if message.get('code') in ('rate_limit', 'too_many_requests'):
            wait_for_limit(payload)
            return None
    deadline = time.time() + 180
    while True:
        if job.get('status') == 'completed' and job.get('result'):
            wait_for_limit(payload)
            return fetch_texture(job['result'])
        if job.get('status') in ('failed', 'canceled') or time.time() > deadline:
            reason = '; '.join(m.get('message', '') for m in payload.get('messages') or [])
            print(f'  {name}: zlecenie nie doszlo do skutku ({job.get("status")}) {reason}')
            wait_for_limit(payload)
            return None
        time.sleep(3)
        payload = request(f'{API}/queue/{job.get("id")}')
        job = payload.get('job') or job


cache = {}
if os.path.isfile(cache_path):
    with open(cache_path, encoding='utf-8') as handle:
        cache = json.load(handle)

entries, missing = [], []
for race in races:
    for cls in classes:
        skin_id = f'{race}_{cls}'
        path = os.path.join(src, f'cc_{skin_id}.png')
        if not os.path.isfile(path):
            missing.append(skin_id + ' (brak PNG)')
            continue
        with open(path, 'rb') as handle:
            digest = hashlib.sha256(handle.read()).hexdigest()
        cached = cache.get(skin_id)
        # Cache jest wiazany z TRESCIA pliku: po przerysowaniu skorki trzeba ja wgrac na nowo.
        if cached and cached.get('sha256') == digest:
            entries.append((skin_id, cached['value'], cached['signature']))
            print(f'{skin_id}: z cache')
            continue
        if skip_upload:
            missing.append(skin_id)
            continue
        result = None
        for attempt in range(1, 5):
            result = upload(path, f'cc_{skin_id}')
            if result:
                break
            print(f'{skin_id}: podejscie {attempt} bez skutku, ponawiam')
            time.sleep(10)
        if not result:
            missing.append(skin_id)
            print(f'{skin_id}: NIEUDANE')
            continue
        value, signature = result
        cache[skin_id] = {'sha256': digest, 'value': value, 'signature': signature}
        entries.append((skin_id, value, signature))
        with open(cache_path, 'w', encoding='utf-8') as handle:
            json.dump(cache, handle, indent=2, sort_keys=True)
        print(f'{skin_id}: OK')

header = """# Skorki postaci — JEDNA tekstura na pare rasa x klasa.
#
# PLIK JEST GENEROWANY: tools/gen_race_skins.py (PNG) + tools/upload_skins.sh (wgranie na
# mineskin.org i przepisanie stad podpisanych tekstur). Recznej edycji nie warto robic —
# 'value' to zakodowany base64 JSON z adresem tekstury, a 'signature' to podpis Mojanga
# dla dokladnie tej wartosci. Zmiana jednego bez drugiego daje skorke bez podpisu.
#
# Klucz to '<rasa>_<klasa>' i to ON ląduje w characters.yml jako 'skinSource' — od tego
# momentu postac trzyma sie tej skorki nawet po zmianie mapowania w config.yml.
# Plugin czyta ten plik z dysku, wiec '/cc reload' wystarcza.
schema: 1
skins:
"""
lines = [header]
for skin_id, value, signature in entries:
    lines.append(f'  {skin_id}:\n')
    lines.append(f"    value: '{value}'\n")
    lines.append(f"    signature: '{signature}'\n")
os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
with open(out, 'w', encoding='utf-8') as handle:
    handle.writelines(lines)

print(f'{len(entries)} skorek -> {out}')
if missing:
    print('BRAKUJE: ' + ', '.join(missing))
    sys.exit(1)
PY

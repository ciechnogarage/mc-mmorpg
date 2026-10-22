"""Sesja Blockbench MCP bez smiecenia kartami.

- js(): risky_eval; blad 'finishEdit' po zamknieciu karty jest falszywy (kod sie wykonal) - ignorowany.
- otworz(sciezka) -> uuid nowej karty; zamknij(uuid) zamyka TYLKO te karte.
- Karta startowa 'podklad' powstaje tylko, gdy nie ma zadnej (risky_eval bez karty nie wykonuje kodu),
  i jest zamykana przez pokaz_gotowe().
- pokaz_gotowe(): w Blockbenchu zostaja wylacznie gotowe modele z output/lochy/modele-wlasne/*/ (gdy model ma wersje/ - jego wersje).
"""
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'blockbench'))
from review_session import ReviewSession, literal, isolate_animation_js  # noqa: E402,F401

GOTOWE = Path('/opt/docker/mc/data/world1/output/lochy/modele-wlasne')
_s = None


def sesja():
    global _s
    if _s is None:
        _s = ReviewSession()
    return _s


def js(kod):
    try:
        return sesja().js(kod)
    except RuntimeError as e:
        if 'finishEdit' in str(e):
            return None
        raise


def karty():
    return js('JSON.stringify(ModelProject.all.map(p=>({n:p.name,u:p.uuid})))') or []


def _zapewnij_karte():
    if not karty():
        sesja().tool('create_project', {'name': 'podklad', 'format': 'free'})


def otworz(sciezka):
    # plik juz otwarty -> Blockbench tylko przelaczy karte; zamknij stara (gotowe wraca pokaz_gotowe())
    for k in karty():
        if k['n'] == Path(sciezka).stem:
            zamknij(k['u'])
    _zapewnij_karte()
    przed = {k['u'] for k in karty()}
    js('Blockbench.read([' + literal(str(sciezka)) + '],{readtype:"text"},f=>loadModelFile(f[0])); JSON.stringify(1)')
    for _ in range(40):
        time.sleep(0.5)
        nowe = [k for k in karty() if k['u'] not in przed]
        if nowe:
            time.sleep(1.5)  # tekstury
            u = nowe[0]['u']
            js('ModelProject.all.find(p=>p.uuid===' + literal(u) + ').select(); JSON.stringify(1)')
            return u
    raise RuntimeError(f'nie wczytal sie: {sciezka}')


def zamknij(uuid):
    js('const p=ModelProject.all.find(p=>p.uuid===' + literal(uuid) + '); if(p){p.select(); p.close(true);} JSON.stringify(1)')


def ukryj_techniczne(fx=True):
    """hitbox/ob_/shadow ModelEngine nie rysuje w grze; fx_* (efekty) w grze ukrywa skala 0 w kazdej animacji poza
    wlasna - ukryj je w podgladzie (bez zapisu). Klatki animacji pokazuja fx_ tam, gdzie animacja je skaluje."""
    wzor = '/^(hitbox|ob_|shadow|fx_)/i' if fx else '/^(hitbox|ob_|shadow)/i'
    js('Group.all.filter(g=>' + wzor + '.test(g.name)).forEach(g=>g.forEachChild(c=>{c.visibility=false},OutlinerElement,true)); Canvas.updateVisibility(); JSON.stringify(1)')


POKAZ = GOTOWE / 'POKAZ.txt'   # wlasciciel 2026-09-29: w Blockbenchu tylko to, co go teraz interesuje (1 nazwa katalogu w linii)


def pokaz_gotowe():
    """Zamyka wszystkie karty i otwiera tylko modele z POKAZ.txt (gdy brak pliku - wszystkie gotowe), swiezo z dysku."""
    # modele z katalogiem wersje/ -> otwieramy wersje do porownania (w1..wN); pozostale (np. strefa) -> sam model
    pliki = []
    tylko = {l.strip() for l in POKAZ.read_text().splitlines() if l.strip() and not l.startswith('#')} if POKAZ.exists() else None
    for kat in sorted(p for p in GOTOWE.iterdir() if p.is_dir() and not p.name.startswith('_') and (tylko is None or p.name in tylko)):
        wersje = sorted(kat.glob('wersje/*.bbmodel'))
        glowny = kat / f'{kat.name}.bbmodel'   # tylko model katalogu, bez plikow pobocznych (bryly, warianty)
        pliki += wersje or ([glowny] if glowny.exists() else [])
    sesja().tool('create_project', {'name': 'podklad', 'format': 'free'})  # karta startowa, zamykana na koncu
    start = [k['u'] for k in karty() if k['n'] == 'podklad'][-1]
    for k in karty():  # stare karty (w tym poprzednie wersje gotowych - otwarty plik BB tylko by przelaczyl)
        if k['u'] != start:
            zamknij(k['u'])
    for p in pliki:
        otworz(p)
        ukryj_techniczne()
    zamknij(start)
    return [k['n'] for k in karty()]


if __name__ == '__main__':
    print('w Blockbenchu:', pokaz_gotowe())

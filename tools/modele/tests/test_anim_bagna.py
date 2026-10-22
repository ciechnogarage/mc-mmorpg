import json
import importlib
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from anim_bagna import zastosuj


class NakladanieAnimacji(unittest.TestCase):
    def test_ponowny_eksport_geometrii_dostaje_animacje_z_aktualnym_uuid(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'mob.bbmodel'
            model = {'elements': [{'name': 'kostka'}], 'outliner': [
                {'name': 'korzen', 'uuid': 'nowy-korzen', 'children': [
                    {'name': 'glowa', 'uuid': 'nowa-glowa', 'children': []}]}],
                'animations': [{'name': 'idle', 'animators': {}}]}
            path.write_text(json.dumps(model))
            definicje = {'walk': {'dl': 1.0, 'petla': 'loop', 'kosci': {
                'glowa': {'rotation': [(0, (0, 0, 0)), (1, (10, 0, 0))]}}}}
            zastosuj(path, definicje)
            wynik = json.loads(path.read_text())
            self.assertEqual(wynik['elements'], model['elements'])
            self.assertEqual([a['name'] for a in wynik['animations']], ['idle', 'walk'])
            self.assertIn('nowa-glowa', wynik['animations'][1]['animators'])
            zastosuj(path, definicje)
            self.assertEqual(len(json.loads(path.read_text())['animations']), 2)

    def test_slimak_ma_pelny_kontrakt_i_chowa_glowe(self):
        from anim_slimak import DANE
        wymagane = {'idle', 'walk', 'attack', 'hide', 'hidden', 'emerge_spit',
                    'hurt', 'death', 'spawn'}
        self.assertEqual(set(DANE), wymagane)
        self.assertEqual(DANE['hide']['kosci']['szyja']['position'][-1][1][2] > 0, True)
        self.assertEqual(DANE['emerge_spit']['hit'], 0.85)
        self.assertEqual(DANE['emerge_spit']['skrypt'], 'mm:BAGNA_SLIMAK_EMERGE_SPIT_KLATKA')

    def test_skrypt_me_ma_animator_effects_jako_klucz(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / 'mob.bbmodel'
            path.write_text(json.dumps({'outliner': [{'name': 'korzen', 'uuid': 'u1',
                                                       'children': []}], 'animations': []}))
            zastosuj(path, {'attack': {'dl': .5, 'petla': 'once', 'hit': .2,
                                      'skrypt': 'mm:TEST_KLATKA', 'kosci': {}}})
            anim = json.loads(path.read_text())['animations'][0]
            self.assertEqual(anim['animators']['effects']['keyframes'][0]['data_points'][0]['script'],
                             'mm:TEST_KLATKA')

    def test_nowe_moby_maja_pelne_stany_i_klatki_uderzen(self):
        wymagania = {
            'kokoniatko': ({'idle', 'walk', 'attack', 'leap', 'hurt', 'death', 'spawn'},
                           {'attack': .12, 'leap': .7}),
            'pijawka': ({'idle', 'walk', 'attack', 'wyskok', 'hurt', 'death', 'spawn'},
                        {'attack': .15, 'wyskok': .9}),
            'ognik': ({'idle', 'walk', 'attack', 'flee', 'charge', 'explode', 'blink',
                       'hurt', 'death', 'spawn'},
                      {'attack': .2, 'explode': .1, 'blink': .2}),
            'krol_ropuch': ({'idle', 'walk', 'attack', 'swallow_prep', 'swallow', 'spit',
                            'tongue', 'leap_prep', 'leap', 'land', 'summon', 'hurt',
                            'death', 'spawn'},
                           {'attack': .35, 'swallow': .2, 'spit': .35,
                            'tongue': 1.15, 'land': .05, 'summon': 1.0}),
            'kokoniara': ({'idle', 'walk', 'attack', 'spin', 'leap_prep', 'leap',
                           'hatch', 'hurt', 'death', 'spawn'},
                          {'attack': .2, 'spin': 1.05, 'leap': .4, 'hatch': .9}),
            'mulowiec': ({'idle', 'walk', 'attack', 'throw', 'hurt', 'death', 'spawn'},
                         {'attack': .9, 'throw': 1.05}),
            'szeptucha': ({'idle', 'walk', 'attack', 'channel', 'throw', 'push',
                           'interrupted', 'hurt', 'death', 'spawn'},
                          {'attack': .2, 'throw': .85, 'push': .65,
                           'interrupted': .3}),
        }
        for mob, (stany, trafienia) in wymagania.items():
            with self.subTest(mob=mob):
                dane = importlib.import_module('anim_' + mob).DANE
                self.assertEqual(set(dane), stany)
                self.assertEqual({n: dane[n]['hit'] for n in trafienia}, trafienia)
                for nazwa in trafienia:
                    self.assertEqual(dane[nazwa]['skrypt'],
                                     f'mm:BAGNA_{mob.upper()}_{nazwa.upper()}_KLATKA')


if __name__ == '__main__':
    unittest.main()

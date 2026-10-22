import json
import tempfile
import unittest
from pathlib import Path

import animacje
import bb
import szkielet


class TestSzkielet(unittest.TestCase):
    def setUp(self):
        bb.wyczysc()

    def test_konczyna_pivoty_i_legalne_kostki(self):
        reka = szkielet.konczyna('reka', (0, 30, 0), (8, 20, 0), (12, 10, 0),
                                 stopa=((10, 7, -2), (15, 11, 2)))
        self.assertEqual(reka['origin'], [0, 30, 0])
        lokiec = next(c for c in reka['children'] if isinstance(c, dict))
        self.assertEqual(lokiec['origin'], [8, 20, 0])
        dlon = next(c for c in lokiec['children'] if isinstance(c, dict))
        self.assertEqual(dlon['origin'], [12, 10, 0])
        self.assertTrue(all(bb.legalny_obrot(e['rotation']) for e in bb.ELEMENTY))
        self.assertTrue(all(min(e['to'][i]-e['from'][i] for i in range(3)) >= 1 for e in bb.ELEMENTY))

    def test_lancuch_i_faza(self):
        k = szkielet.lancuch('pijawka', [(0, 4, 0), (6, 5, 0), (12, 4, 2)], [4, 3])
        self.assertEqual(k['origin'], [0, 4, 0])
        self.assertEqual(next(c for c in k['children'] if isinstance(c, dict))['origin'], [6, 5, 0])
        szkielet.fazuj('bark', (-6, 10, -4), (6, 19, 4), skos=1, kat=22.5)
        self.assertTrue(all(bb.legalny_obrot(e['rotation']) for e in bb.ELEMENTY))
        self.assertEqual(len([e for e in bb.ELEMENTY if e['name'].startswith('bark_')]), 3)


class TestAnimacje(unittest.TestCase):
    def test_interpolacja_i_tabela(self):
        a = {'attack': {'petla': 'once', 'dlugosc': 1.2,
                        'znaczniki': {'windup': 0.0, 'hit': 0.8, 'recover': 1.0},
                        'kosci': {'ramie': {'rotation': [(0, (0, 0, 0), 'step'),
                                                         (0.8, (30, 0, 0), 'linear'),
                                                         (1.2, (0, 0, 0))]}}}}
        wynik = animacje.zbuduj(a, {'ramie': 'id'}, do_me=True)
        klatki = wynik[0]['animators']['id']['keyframes']
        self.assertEqual([k['interpolation'] for k in klatki], ['step', 'linear', 'catmullrom'])
        self.assertEqual(klatki[1]['data_points'][0]['x'], '-30')
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp)/'timings.json'
            animacje.eksportuj_tabele_czasow(a, path)
            self.assertEqual(json.loads(path.read_text())['attack']['markers']['hit'], 0.8)

    def test_legacy_klatka(self):
        a = {'idle': {'petla': 'loop', 'dlugosc': 1,
                      'kosci': {'root': {'rotation': [(0, (1, 2, 3)), (1, (4, 5, 6))]}}}}
        wynik = animacje.zbuduj(a, {'root': 'id'}, do_me=True)
        self.assertEqual(wynik[0]['animators']['id']['keyframes'][0]['interpolation'], 'catmullrom')
        self.assertEqual(wynik[0]['animators']['id']['keyframes'][0]['data_points'][0],
                         {'x': '-1', 'y': '-2', 'z': '3'})


if __name__ == '__main__':
    unittest.main()

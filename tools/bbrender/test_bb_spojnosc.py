import unittest

from bb_spojnosc import komponenty, kontrola_ruchu, boxy_modelu, audyt, blad


class SpojnoscTest(unittest.TestCase):
    def test_odlaczenie_w_kosci_mimo_mostu_przez_inna_kosc(self):
        boxy = [
            ('a1', 'A', (0, 0, 0, 1, 1, 1)),
            ('a2', 'A', (4, 0, 0, 5, 1, 1)),
            ('most', 'B', (1, 0, 0, 4, 1, 1)),
        ]
        self.assertEqual(len(komponenty(boxy, 0.1)), 1)
        self.assertEqual(len(komponenty([b for b in boxy if b[1] == 'A'], 0.1)), 2)

    def test_animowany_emiter_jest_dozwolony_a_pusta_kosc_nie(self):
        model = {'outliner': [
            {'name': 'korzen', 'uuid': 'root', 'children': [
                {'name': 'cialo', 'uuid': 'body', 'children': ['cube']},
                {'name': 'p_dym', 'uuid': 'emitter', 'children': []},
                {'name': 'pusta', 'uuid': 'empty', 'children': []},
            ]}],
            'elements': [{'uuid': 'cube', 'name': 'kostka', 'from': [0, 0, 0], 'to': [1, 1, 1]}],
            'animations': [{'name': 'idle', 'animators': {
                'emitter': {'name': 'p_dym', 'type': 'bone', 'keyframes': [{'channel': 'position'}]},
                'empty': {'name': 'pusta', 'type': 'bone', 'keyframes': [{'channel': 'position'}]},
            }}],
        }
        ruch = kontrola_ruchu(model, {'p_dym'})
        self.assertIn('pusta', ruch['animowane_bez_geometrii'])
        self.assertNotIn('p_dym', ruch['animowane_bez_geometrii'])
        self.assertIn('cialo', ruch['geometria_bez_animacji'])

    def test_geometria_bez_animacji_jest_informacja(self):
        self.assertFalse(blad({'calosc': [[]], 'na_kosc': {}, 'pozy_odlaczone': {},
                              'ruch': {'animowane_bez_geometrii': [],
                                       'nieznane_animatory': [], 'geometria_bez_animacji': ['cialo']},
                              'brak_animacji': [], 'obce_animacje': [],
                              'brak_kosci': [], 'obce_kosci': [], 'puste_swiatlo': [],
                              'zla_nazwa': False, 'brak_kontraktu': False}))

    def test_wieloczlonowa_kosc_w_spojnej_calosci_jest_informacja(self):
        model = {'outliner': [
            {'name': 'A', 'uuid': 'a', 'children': ['e1', 'e2']},
            {'name': 'B', 'uuid': 'b', 'children': ['bridge']}],
            'elements': [
                {'uuid': 'e1', 'from': [0, 0, 0], 'to': [1, 1, 1]},
                {'uuid': 'e2', 'from': [4, 0, 0], 'to': [5, 1, 1]},
                {'uuid': 'bridge', 'from': [1, 0, 0], 'to': [4, 1, 1]}],
            'animations': []}
        w = audyt(model, 'test', kontrakt='## test\nKości specjalne: \n')
        self.assertIn('A', w['na_kosc'])
        self.assertFalse(blad(w))

    def test_ognik_iskry_sa_wyjatkiem(self):
        model = {'outliner': [
            {'name': 'rdzen', 'uuid': 'body', 'children': ['e1']},
            {'name': 'iskry', 'uuid': 'sparks', 'children': ['e2']}],
            'elements': [
                {'uuid': 'e1', 'from': [0, 0, 0], 'to': [1, 1, 1]},
                {'uuid': 'e2', 'from': [10, 0, 0], 'to': [11, 1, 1]}],
            'animations': []}
        w = audyt(model, 'ognik', kontrakt='## ognik\nKości specjalne: \n')
        self.assertEqual(len(w['calosc']), 1)
        self.assertFalse(blad(w))

    def test_obca_nazwa_kosci_specjalnej_jest_niezgodna_z_kontraktem(self):
        model = {'outliner': [{'name': 'swiatlo_inne', 'uuid': 'light', 'children': []}],
                 'elements': [], 'animations': []}
        wynik = audyt(model, 'test', kontrakt='## test\nKości specjalne: `swiatlo_oczy`\n')
        self.assertEqual(wynik['obce_kosci'], ['swiatlo_inne'])

    def test_hitbox_z_geometria_nie_wymaga_animacji(self):
        model = {'outliner': [{'name': 'hitbox', 'uuid': 'h', 'children': ['cube']}],
                 'elements': [{'uuid': 'cube', 'name': 'box', 'from': [0, 0, 0],
                               'to': [1, 1, 1]}], 'animations': []}
        self.assertEqual(kontrola_ruchu(model)['geometria_bez_animacji'], [])

    def test_puste_swiatlo_blokuje_odbior(self):
        model = {'outliner': [{'name': 'swiatlo_oczy', 'uuid': 'light', 'children': []}],
                 'elements': [], 'animations': []}
        wynik = audyt(model, 'test', kontrakt='## test\nKości specjalne: `swiatlo_oczy`\n')
        self.assertEqual(wynik['puste_swiatlo'], ['swiatlo_oczy'])
        self.assertTrue(blad(wynik))

    def test_animowany_efekt_moze_byc_ukryty_w_idle(self):
        model = {'outliner': [{'name': 'fx_blysk', 'uuid': 'fx', 'children': []}],
                 'elements': [], 'animations': [{'name': 'idle', 'animators': {
                     'fx': {'name': 'fx_blysk', 'type': 'bone',
                            'keyframes': [{'channel': 'scale'}]}}}]}
        self.assertEqual(kontrola_ruchu(model)['animowane_bez_geometrii'], [])

    def test_animacja_odlacza_kosc_od_calosci(self):
        model = {'outliner': [
            {'name': 'A', 'uuid': 'a', 'origin': [0, 0, 0], 'children': ['e1']},
            {'name': 'B', 'uuid': 'b', 'origin': [0, 0, 0], 'children': ['e2']}],
            'elements': [
                {'uuid': 'e1', 'name': 'a', 'from': [0, 0, 0], 'to': [1, 1, 1]},
                {'uuid': 'e2', 'name': 'b', 'from': [1, 0, 0], 'to': [2, 1, 1]}],
            'animations': [{'name': 'walk', 'length': 1, 'animators': {'b': {
                'name': 'B', 'type': 'bone', 'keyframes': [
                    {'channel': 'position', 'time': 0, 'data_points': [{'x': '0', 'y': '0', 'z': '0'}]},
                    {'channel': 'position', 'time': 1, 'data_points': [{'x': '3', 'y': '0', 'z': '0'}]},
                ]}}}]}
        self.assertEqual(len(komponenty(boxy_modelu(model), .1)), 1)
        self.assertEqual(len(komponenty(boxy_modelu(model, model['animations'][0], 1), .1)), 2)
        self.assertIn('walk', audyt(model, 'test', kontrakt='## test\n| `walk` | R |')['pozy_odlaczone'])


if __name__ == '__main__':
    unittest.main()

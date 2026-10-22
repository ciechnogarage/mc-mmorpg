package pl.mcmmorpg.trafienia;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GeometriaTest {
    double[] o = {0, 0, 0};

    @Test void trafiaPionowaKapsulePrzedSoba() {
        double t = Geometria.trafienie(o, new double[]{0, 0, 1}, new double[]{0, -1, 3}, new double[]{0, 1, 3}, .5, 6);
        assertEquals(2.5, t, 1e-6);
    }

    @Test void chybiaObok() {
        assertEquals(-1, Geometria.trafienie(o, new double[]{0, 0, 1}, new double[]{2, -1, 3}, new double[]{2, 1, 3}, .5, 6));
    }

    @Test void kulaZaZasiegiemChybia() {
        assertEquals(-1, Geometria.trafienie(o, new double[]{0, 0, 1}, new double[]{0, 0, 9}, new double[]{0, 0, 9}, .5, 6));
    }

    @Test void kulaZaPlecamiChybia() {
        assertEquals(-1, Geometria.trafienie(o, new double[]{0, 0, 1}, new double[]{0, 0, -3}, new double[]{0, 0, -3}, .5, 6));
    }

    @Test void koniecOdcinkaLiczySieJakKula() {
        // odcinek od (0,1,3) w górę; promień w y=0.7 trafia półkulę dolnego końca
        double t = Geometria.trafienie(new double[]{0, .7, 0}, new double[]{0, 0, 1}, new double[]{0, 1, 3}, new double[]{0, 3, 3}, .5, 6);
        assertTrue(t > 2.5 && t < 3, "t=" + t);
    }

    @Test void kierunekYaw0PatrzyNaPlusZ() {
        double[] d = Geometria.kierunek(0, 0);
        assertArrayEquals(new double[]{0, 0, 1}, d, 1e-9);
        double[] e = Geometria.kierunek(90, 0); // yaw 90 = -X
        assertEquals(-1, e[0], 1e-9);
        double[] f = Geometria.kierunek(0, -90); // pitch -90 = w górę
        assertEquals(1, f[1], 1e-9);
    }
}

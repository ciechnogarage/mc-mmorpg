package pl.mcmmorpg.trafienia;

/** Promień kontra kapsuła (odcinek a-b z promieniem r). Czyste double, bez Bukkita — testowalne. */
final class Geometria {
    private Geometria() {}

    /** Wynik najbliższego zbliżenia promienia (t na promieniu) do odcinka (s w [0,1]) i odległość między nimi. */
    record Zblizenie(double t, double s, double odleglosc) {}

    static Zblizenie zblizenie(double[] o, double[] d, double[] a, double[] b, double maxT) {
        double[] u = d, v = sub(b, a), w = sub(o, a);
        double uu = dot(u, u), uv = dot(u, v), vv = dot(v, v), uw = dot(u, w), vw = dot(v, w);
        double den = uu * vv - uv * uv;
        double t, s;
        if (vv < 1e-9) { // kapsuła = kula
            s = 0; t = clamp(-uw / uu, 0, maxT);
        } else if (den < 1e-9) { // równoległe
            s = 0; t = clamp(-uw / uu, 0, maxT);
            s = clamp((vw + t * uv) / vv, 0, 1);
        } else {
            t = clamp((uv * vw - vv * uw) / den, 0, maxT);
            s = clamp((vw + t * uv) / vv, 0, 1);
            // po przycięciu s popraw t i ponownie s
            t = clamp((s * uv - uw) / uu, 0, maxT);
            s = clamp((vw + t * uv) / vv, 0, 1);
        }
        double[] p = add(o, mul(d, t)), q = add(a, mul(v, s));
        return new Zblizenie(t, s, len(sub(p, q)));
    }

    /** t wejścia promienia w kapsułę albo -1, gdy chybia. d musi być jednostkowy. */
    static double trafienie(double[] o, double[] d, double[] a, double[] b, double r, double maxT) {
        Zblizenie z = zblizenie(o, d, a, b, maxT);
        if (z.odleglosc() > r) return -1;
        double wejscie = z.t() - Math.sqrt(Math.max(0, r * r - z.odleglosc() * z.odleglosc()));
        return Math.max(0, wejscie);
    }

    /** Kierunek z yaw/pitch w stopniach (konwencja Minecrafta). */
    static double[] kierunek(double yaw, double pitch) {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        return new double[]{-Math.sin(y) * Math.cos(p), -Math.sin(p), Math.cos(y) * Math.cos(p)};
    }

    static double[] sub(double[] a, double[] b) { return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]}; }
    static double[] add(double[] a, double[] b) { return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]}; }
    static double[] mul(double[] a, double k) { return new double[]{a[0] * k, a[1] * k, a[2] * k}; }
    static double dot(double[] a, double[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }
    static double len(double[] a) { return Math.sqrt(dot(a, a)); }
    static double clamp(double x, double lo, double hi) { return x < lo ? lo : Math.min(x, hi); }
}

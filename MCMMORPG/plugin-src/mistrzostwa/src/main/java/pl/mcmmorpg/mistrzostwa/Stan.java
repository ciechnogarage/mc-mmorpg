package pl.mcmmorpg.mistrzostwa;

import io.lumine.mythic.lib.api.player.MMOPlayerData;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.PlayerData;
import net.Indyuce.mmocore.experience.Profession;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Stan mistrzostw jednej POSTACI (profilu), trzymany w LuckPerms.
 *
 * Dlaczego LuckPerms z kontekstem, a nie goly wezel: MMOProfiles w trybie Classic nie rozdziela
 * uprawnien miedzy profile ("Permissions ... Classic: Not available" w jego config.yml), wiec
 * `lp user X permission set prof.master.mining` dostalyby WSZYSTKIE postacie konta. Wezel z
 * kontekstem profil=<uuid profilu> jest per postac, przezywa restart (baza LP), widac go w
 * `lp user X permission info`, a ContextCalculator tego pluginu sprawia, ze hasPermission
 * w innych pluginach tez widzi tylko biezaca postac.
 *
 * Wszystkie odczyty ida po WEZLACH (isPermissionSet-owo), nigdy przez hasPermission —
 * OP falszuje hasPermission (memory op-falszuje-haspermission).
 */
final class Stan {
    static final String PREFIKS = "prof.master.";
    static final String COOLDOWN = "mistrzostwa.cooldown";

    private final LuckPerms lp;
    private volatile Ustawienia u;

    Stan(LuckPerms lp, Ustawienia u) {
        this.lp = lp;
        this.u = u;
    }

    void ustawienia(Ustawienia u) { this.u = u; }

    /** uuid biezacego profilu postaci albo null, gdy gracz nie wybral jeszcze postaci. */
    static UUID profil(Player p) {
        try {
            MMOPlayerData d = MMOPlayerData.getOrNull(p.getUniqueId());
            if (d == null || !d.hasProfile()) return null;
            return d.getProfileId();
        } catch (Throwable t) {
            return null;
        }
    }

    User user(Player p) {
        return lp.getUserManager().getUser(p.getUniqueId());
    }

    private boolean tenProfil(Node n, UUID pid) {
        if (u.sab("kontekst")) return true;
        return n.getContexts().contains(u.kontekst, pid.toString());
    }

    /** Aktywne mistrzostwa biezacej postaci (tylko wezly z kontekstem jej profilu). */
    Set<String> mistrzostwa(Player p) {
        Set<String> s = new TreeSet<>();
        UUID pid = profil(p);
        User us = user(p);
        if (pid == null || us == null) return s;
        for (PermissionNode n : us.getNodes(NodeType.PERMISSION)) {
            if (!n.getValue() || n.hasExpired()) continue;
            String k = n.getPermission();
            if (!k.startsWith(PREFIKS) || !tenProfil(n, pid)) continue;
            String id = k.substring(PREFIKS.length()).toLowerCase(Locale.ROOT);
            if (u.mistrzowskie.contains(id)) s.add(id);
        }
        return s;
    }

    boolean jestMistrzem(Player p, String id) {
        return mistrzostwa(p).contains(id);
    }

    /** Koniec cooldownu zmiany albo null. */
    Instant cooldownDo(Player p) {
        UUID pid = profil(p);
        User us = user(p);
        if (pid == null || us == null) return null;
        Instant max = null;
        for (Node n : us.getNodes()) {
            if (!n.getKey().equals(COOLDOWN) || !n.hasExpiry() || n.hasExpired() || !tenProfil(n, pid)) continue;
            Instant e = n.getExpiry();
            if (max == null || e.isAfter(max)) max = e;
        }
        return max;
    }

    private Node wezelMistrza(String id, UUID pid) {
        if (u.sab("kontekst")) return Node.builder(PREFIKS + id).value(true).build();
        return Node.builder(PREFIKS + id).value(true).withContext(u.kontekst, pid.toString()).build();
    }

    /** Dodaje mistrzostwo biezacej postaci. Zwraca false, gdy nie ma profilu/uzytkownika. */
    boolean dodaj(Player p, String id) {
        UUID pid = profil(p);
        User us = user(p);
        if (pid == null || us == null) return false;
        us.data().add(wezelMistrza(id, pid));
        lp.getUserManager().saveUser(us);
        return true;
    }

    boolean usun(Player p, String id) {
        UUID pid = profil(p);
        User us = user(p);
        if (pid == null || us == null) return false;
        List<Node> doUsuniecia = new ArrayList<>();
        for (Node n : us.getNodes())
            if (n.getKey().equalsIgnoreCase(PREFIKS + id) && tenProfil(n, pid)) doUsuniecia.add(n);
        doUsuniecia.forEach(n -> us.data().remove(n));
        lp.getUserManager().saveUser(us);
        return !doUsuniecia.isEmpty();
    }

    /** Zamiana w JEDNEJ modyfikacji uzytkownika (bez stanu posredniego widocznego na zapisie). */
    boolean zamien(Player p, String z, String na, Duration cooldown) {
        UUID pid = profil(p);
        User us = user(p);
        if (pid == null || us == null) return false;
        List<Node> stare = new ArrayList<>();
        for (Node n : us.getNodes())
            if ((n.getKey().equalsIgnoreCase(PREFIKS + z) || n.getKey().equals(COOLDOWN)) && tenProfil(n, pid)) stare.add(n);
        stare.forEach(n -> us.data().remove(n));
        us.data().add(wezelMistrza(na, pid));
        if (!cooldown.isZero())
            us.data().add(Node.builder(COOLDOWN).value(true).withContext(u.kontekst, pid.toString()).expiry(cooldown).build());
        lp.getUserManager().saveUser(us);
        return true;
    }

    boolean zdejmijCooldown(Player p) {
        UUID pid = profil(p);
        User us = user(p);
        if (pid == null || us == null) return false;
        List<Node> c = new ArrayList<>();
        for (Node n : us.getNodes()) if (n.getKey().equals(COOLDOWN) && tenProfil(n, pid)) c.add(n);
        c.forEach(n -> us.data().remove(n));
        lp.getUserManager().saveUser(us);
        return !c.isEmpty();
    }

    /** Wezly prof.master.* BEZ kontekstu profilu — ignorowane, ale warto o nich wiedziec. */
    List<String> wezlyBezKontekstu(Player p) {
        List<String> l = new ArrayList<>();
        User us = user(p);
        if (us == null) return l;
        for (Node n : us.getNodes())
            if (n.getKey().startsWith(PREFIKS) && n.getContexts().getValues(u.kontekst).isEmpty()) l.add(n.getKey());
        return l;
    }

    // ---------------- MMOCore ----------------

    static Profession profesja(String id) {
        for (Profession pr : MMOCore.plugin.professionManager.getAll())
            if (pr.getId().equalsIgnoreCase(id)) return pr;
        return null;
    }

    static int poziom(Player p, String id) {
        Profession pr = profesja(id);
        if (pr == null || !PlayerData.has(p.getUniqueId())) return 0;
        return PlayerData.get(p).getCollectionSkills().getLevel(pr);
    }

    static String nazwa(String id) {
        Profession pr = profesja(id);
        return pr == null ? id : pr.getName();
    }
}

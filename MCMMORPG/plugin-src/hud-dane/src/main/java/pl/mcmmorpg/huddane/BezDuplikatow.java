package pl.mcmmorpg.huddane;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Logger;

import io.lumine.mythichud.api.HudHolder;
import io.lumine.mythichud.api.bossbar.BossBarHandler;
import net.kyori.adventure.text.Component;

/**
 * Nie wysyla graczowi HUD-a, ktory jest identyczny z ostatnio wyslanym (MythicHUD 1.3.4).
 *
 * HudHolder.update co check-update (5 tikow) oglasza zmiane, gdy cokolwiek w elementach "drgnie", i wtedy
 * send() buduje caly komponent i wysyla PacketPlayOutBoss UPDATE_NAME. Pomiar 26.10 (klon hud-opt, stojacy gracz):
 * 240 pakietow / 60 s, 239 z nich z tytulem identycznym jak poprzedni (~30 tys. znakow JSON). Kazdy taki pakiet
 * MythicHUD koduje drugi raz w swoim handlerze Netty (handleBossBarUpdate -> encode tylko po UUID), potem
 * koduje go serwer, przepisuje Nexo (ComponentTransformer) i kompresuje - razem ~3 s CPU / 90 s na gracza.
 *
 * Owijamy BossBarHandler gracza: setHud z komponentem rownym (Component.equals, strukturalnie) ostatnio wyslanemu
 * nic nie robi; kazde init() (nowe paski BLANK u klienta) zeruje pamiec, wiec po nim pierwszy HUD zawsze idzie.
 * Klient dostaje dokladnie te same tresci co wczesniej, tylko bez powtorek tego samego.
 */
final class BezDuplikatow implements BossBarHandler {
  private static Field pole;
  private static boolean wylaczony;

  private final BossBarHandler cel;
  private Component ostatni;

  private BezDuplikatow(BossBarHandler cel) { this.cel = cel; }

  /** Owija handler gracza, jesli jeszcze nie jest owiniety. Wolac na glownym watku; koszt: odczyt pola. */
  static void owin(HudHolder h, Logger log) {
    if (wylaczony) return;
    BossBarHandler b = h.getBarHandler();
    if (b == null || b instanceof BezDuplikatow) return;
    try {
      if (pole == null) {
        Field f = HudHolder.class.getDeclaredField("barHandler");
        f.setAccessible(true);
        pole = f;
      }
      pole.set(h, new BezDuplikatow(b));
    } catch (ReflectiveOperationException | RuntimeException ex) {
      wylaczony = true;   // inna wersja MythicHUD: HUD wysylany jak dotad
      log.warning("HudDane: nie owine BossBarHandler MythicHUD (" + ex + ")");
    }
  }

  @Override public synchronized void setHud(Component c) {
    if (c != null && c.equals(ostatni)) return;
    ostatni = c;
    cel.setHud(c);
  }

  @Override public synchronized void init() { ostatni = null; cel.init(); }
  @Override public synchronized void disable() { ostatni = null; cel.disable(); }
  @Override public void update() { cel.update(); }
  @Override public LinkedList<UUID> getIds() { return cel.getIds(); }
  @Override public boolean isHolding(UUID id) { return cel.isHolding(id); }
  @Override public void handle(UUID id, int op, Object packet) { cel.handle(id, op, packet); }
}

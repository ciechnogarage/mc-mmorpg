package pl.mcmmorpg.rzemioslo;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/** Dysk zielarza (F1). Jeden watek: zapisy i odczyty ida w kolejnosci zlecen, wiec odczyt konta po wyjsciu i szybkim
 *  powrocie zawsze widzi ostatni zapis. Plik tymczasowy + fsync + podmiana: przerwany zapis (crash, pelny dysk) zostawia
 *  stary plik, nie ucięty. Watek serwera tylko sklada tekst (male pliki: konto, region, postac). */
final class ZMagazyn {
    private final Logger log;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Rzemioslo-zielarz-dysk");
        t.setDaemon(true);
        return t;
    });
    /** Pomiar: zapisy wykonane / nieudane (rzemioslo magazyn). */
    volatile int zapisy, bledy;

    ZMagazyn(Logger log) { this.log = log; }

    /** Kolejka zapisu; tekst zlozony przez wolajacego na watku serwera (migawka). */
    void zapisz(File f, String tekst) { io.execute(() -> zapiszTeraz(f, tekst)); }

    void usun(File f) { io.execute(() -> { try { Files.deleteIfExists(f.toPath()); } catch (IOException e) { bledy++; log.warning("zielarz dysk: " + f + ": " + e); } }); }

    /** Dowolna praca w kolejce dysku (np. przeniesienie starego pliku PO zapisaniu jego tresci w nowym ukladzie). */
    void potem(Runnable r) { io.execute(r); }

    /** Odczyt w kolejce dysku; null = brak pliku. Zepsuty plik zostaje obok jako .zepsuty (nie nadpisujemy go pustym). */
    CompletableFuture<YamlConfiguration> czytaj(File f) { return CompletableFuture.supplyAsync(() -> czytajTeraz(f), io); }

    YamlConfiguration czytajTeraz(File f) {
        if (!f.exists()) return null;
        YamlConfiguration y = new YamlConfiguration();
        try {
            y.loadFromString(Files.readString(f.toPath(), StandardCharsets.UTF_8));
            return y;
        } catch (IOException | InvalidConfigurationException e) {
            bledy++;
            log.severe("zielarz dysk: zepsuty " + f + " (" + e.getMessage() + "), odkladam jako .zepsuty");
            try { Files.move(f.toPath(), new File(f.getPath() + ".zepsuty").toPath(), StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) { }
            return null;
        }
    }

    void zapiszTeraz(File f, String tekst) {
        File tmp = new File(f.getPath() + ".tmp");
        try {
            File dir = f.getParentFile();
            if (dir != null) dir.mkdirs();
            Files.writeString(tmp.toPath(), tekst, StandardCharsets.UTF_8);
            try (FileChannel ch = FileChannel.open(tmp.toPath(), StandardOpenOption.WRITE)) { ch.force(true); }
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            zapisy++;
        } catch (IOException e) {
            bledy++;
            log.warning("zielarz dysk: " + f + ": " + e);
        }
    }

    /** Wylaczanie: kolejka dopisana przed wywolaniem musi zejsc na dysk. */
    void zamknij() {
        io.shutdown();
        try {
            if (!io.awaitTermination(20, TimeUnit.SECONDS)) log.severe("zielarz dysk: zapisy nie zeszly w 20 s");
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}

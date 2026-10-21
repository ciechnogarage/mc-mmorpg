package pl.mcmmorpg.minioncore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

/** Main-thread Paper tick interval telemetry; bounded and independent of Spark/RCON. */
public final class PaperPerformanceTelemetry {
  private final ArrayDeque<Double> msptSamples = new ArrayDeque<>();
  private long previousNanos;

  public void sample(long nowNanos) {
    if (previousNanos != 0L) {
      double mspt = (nowNanos - previousNanos) / 1_000_000.0d;
      if (Double.isFinite(mspt) && mspt >= 0.0d && mspt < 10_000.0d) {
        if (msptSamples.size() >= 1200) msptSamples.removeFirst();
        msptSamples.addLast(mspt);
      }
    }
    previousNanos = nowNanos;
  }

  public PaperPerformanceSnapshot snapshot() {
    if (msptSamples.isEmpty()) return new PaperPerformanceSnapshot(0.0d, 0.0d, 0.0d, 0);
    ArrayList<Double> sorted = new ArrayList<>(msptSamples);
    Collections.sort(sorted);
    double total = sorted.stream().mapToDouble(Double::doubleValue).sum();
    int p95Index = Math.min(sorted.size() - 1, (int) Math.ceil(sorted.size() * 0.95d) - 1);
    return new PaperPerformanceSnapshot(total / sorted.size(), sorted.get(p95Index),
        sorted.get(sorted.size() - 1), sorted.size());
  }
}

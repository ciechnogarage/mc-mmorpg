package pl.mcmmorpg.minioncore;

public interface PvpContextAdapter {
  PvpDecision evaluate(PvpAttempt attempt);
}

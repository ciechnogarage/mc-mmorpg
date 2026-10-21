package pl.mcmmorpg.chowance.model;

import java.util.List;
import java.util.Map;

public record Wieza(String id, String nazwa, List<String> fale, int leczenieMiedzyFalami,
    Map<Integer, List<String>> nagrody) {}

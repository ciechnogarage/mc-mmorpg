package pl.mcmmorpg.companionbridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class HttpSupport {
  // serializeNulls: kontrakt (contracts/companion-bridge/v1) deklaruje pola
  // jako "type": ["string","null"] i wymaga ich OBECNOSCI (required) nawet
  // gdy wartosc jest null — domyslny Gson po cichu POMIJA klucze o wartosci
  // null w Map<String,Object>, co lamalo kontrakt (zweryfikowane na zywym
  // /v1/players/.../snapshot: "name" znikalo zamiast byc null).
  private static final Gson GSON = new GsonBuilder().serializeNulls().create();

  private HttpSupport() {
  }

  public static boolean isAuthorized(HttpExchange exchange, String expectedToken) {
    String header = exchange.getRequestHeaders().getFirst("Authorization");
    if (header == null) {
      return false;
    }
    String prefix = "Bearer ";
    if (!header.startsWith(prefix)) {
      return false;
    }
    return header.substring(prefix.length()).equals(expectedToken);
  }

  public static void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
    byte[] payload = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    exchange.sendResponseHeaders(status, payload.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(payload);
    }
  }

  public static void writeError(HttpExchange exchange, int status, String message) throws IOException {
    writeJson(exchange, status, Map.of("error", message));
  }
}

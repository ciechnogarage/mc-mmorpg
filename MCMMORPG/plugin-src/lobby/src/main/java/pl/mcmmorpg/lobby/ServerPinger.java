package pl.mcmmorpg.lobby;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Server List Ping (to samo, co lista serwerow w kliencie) wprost do backendu w sieci
 * dockera. Nie potrzebuje proxy ani gracza online - dlatego status nad portalem jest
 * prawdziwy takze wtedy, gdy nikt nie stoi w lobby.
 */
final class ServerPinger {

  private ServerPinger() {}

  /** Liczba graczy albo -1, gdy serwer nie odpowiada. */
  static int ping(String host, int port, int timeoutMs) {
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, port), timeoutMs);
      socket.setSoTimeout(timeoutMs);
      DataOutputStream out = new DataOutputStream(socket.getOutputStream());
      DataInputStream in = new DataInputStream(socket.getInputStream());

      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      DataOutputStream hs = new DataOutputStream(buf);
      writeVarInt(hs, 0x00);          // handshake
      writeVarInt(hs, 774);           // protokol 1.21.11 (dla statusu bez znaczenia)
      byte[] h = host.getBytes(StandardCharsets.UTF_8);
      writeVarInt(hs, h.length);
      hs.write(h);
      hs.writeShort(port);
      writeVarInt(hs, 1);             // nastepny stan: status
      writeVarInt(out, buf.size());
      out.write(buf.toByteArray());
      out.writeByte(1);               // dlugosc
      out.writeByte(0x00);            // status request
      out.flush();

      readVarInt(in);                 // dlugosc pakietu
      if (readVarInt(in) != 0x00) return -1;
      int len = readVarInt(in);
      if (len <= 0 || len > 1 << 20) return -1;
      byte[] json = new byte[len];
      in.readFully(json);
      JsonObject root = JsonParser.parseString(new String(json, StandardCharsets.UTF_8)).getAsJsonObject();
      JsonObject players = root.getAsJsonObject("players");
      return players == null ? 0 : players.get("online").getAsInt();
    } catch (IOException | RuntimeException e) {
      return -1;
    }
  }

  private static void writeVarInt(DataOutputStream out, int value) throws IOException {
    while ((value & ~0x7F) != 0) {
      out.writeByte((value & 0x7F) | 0x80);
      value >>>= 7;
    }
    out.writeByte(value);
  }

  private static int readVarInt(DataInputStream in) throws IOException {
    int value = 0;
    int shift = 0;
    byte b;
    do {
      b = in.readByte();
      value |= (b & 0x7F) << shift;
      shift += 7;
      if (shift > 35) throw new IOException("VarInt za dlugi");
    } while ((b & 0x80) != 0);
    return value;
  }
}

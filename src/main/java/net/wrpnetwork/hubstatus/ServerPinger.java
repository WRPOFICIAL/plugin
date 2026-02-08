package net.wrpnetwork.hubstatus;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ServerPinger {

    public static PingResult ping(String address) {
        String[] parts = address.split(":");
        String host = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Handshake
            out.writeByte(0x00); // Packet ID
            writeVarInt(out, 47); // Protocol version
            writeString(out, host);
            out.writeShort(port);
            writeVarInt(out, 1); // Next state (1 for status)

            // Request
            out.writeByte(0x01); // Packet length
            out.writeByte(0x00); // Packet ID

            // Response
            readVarInt(in); // Packet length
            int id = readVarInt(in);
            if (id == -1) throw new IOException("Premature end of stream");
            if (id != 0x00) throw new IOException("Invalid packet ID");

            String json = readString(in);

            // Minimalist JSON parsing to get players
            // Example: {"players":{"max":100,"online":5},...}
            int online = extractInt(json, "\"online\":");
            int max = extractInt(json, "\"max\":");

            return new PingResult(true, online, max);

        } catch (Exception e) {
            return new PingResult(false, 0, 0);
        }
    }

    private static int extractInt(String json, String key) {
        int index = json.indexOf(key);
        if (index == -1) return 0;
        int start = index + key.length();
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }
            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }

    private static int readVarInt(DataInputStream in) throws IOException {
        int i = 0;
        int j = 0;
        while (true) {
            int k = in.readByte();
            i |= (k & 0x7F) << j++ * 7;
            if (j > 5) throw new RuntimeException("VarInt too big");
            if ((k & 0x80) != 128) break;
        }
        return i;
    }

    private static void writeString(DataOutputStream out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        out.write(bytes);
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = readVarInt(in);
        byte[] bytes = new byte[len];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static class PingResult {
        public final boolean online;
        public final int playersOnline;
        public final int playersMax;

        public PingResult(boolean online, int playersOnline, int playersMax) {
            this.online = online;
            this.playersOnline = playersOnline;
            this.playersMax = playersMax;
        }
    }
}

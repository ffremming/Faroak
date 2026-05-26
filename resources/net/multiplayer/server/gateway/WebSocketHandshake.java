package resources.net.multiplayer.server.gateway;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Minimal websocket handshake helper.
 */
final class WebSocketHandshake {

    private static final String MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final int MAX_HEADER = 16_384;

    Request accept(InputStream in, OutputStream out) throws IOException {
        String header = readHeader(in);
        if (header == null || header.isBlank()) throw new IOException("empty handshake");
        String[] lines = header.split("\r\n");
        if (lines.length == 0) throw new IOException("invalid request");
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length < 2 || !"GET".equalsIgnoreCase(requestLine[0])) throw new IOException("invalid method");
        String path = requestLine[1];
        String key = headerValue(lines, "Sec-WebSocket-Key");
        if (key == null || key.isBlank()) throw new IOException("missing websocket key");
        String accept = computeAccept(key.trim());
        String response = ""
            + "HTTP/1.1 101 Switching Protocols\r\n"
            + "Upgrade: websocket\r\n"
            + "Connection: Upgrade\r\n"
            + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
        out.write(response.getBytes(StandardCharsets.US_ASCII));
        out.flush();
        return new Request(path);
    }

    private static String readHeader(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int state = 0;
        while (buf.size() < MAX_HEADER) {
            int b = in.read();
            if (b < 0) break;
            buf.write(b);
            if (state == 0 && b == '\r') state = 1;
            else if (state == 1 && b == '\n') state = 2;
            else if (state == 2 && b == '\r') state = 3;
            else if (state == 3 && b == '\n') break;
            else state = 0;
        }
        return buf.toString(StandardCharsets.US_ASCII);
    }

    private static String headerValue(String[] lines, String name) {
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int idx = line.indexOf(':');
            if (idx <= 0) continue;
            String key = line.substring(0, idx).trim();
            if (!name.equalsIgnoreCase(key)) continue;
            return line.substring(idx + 1).trim();
        }
        return null;
    }

    private static String computeAccept(String key) throws IOException {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest((key + MAGIC).getBytes(StandardCharsets.US_ASCII));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception ex) {
            throw new IOException("handshake digest failed", ex);
        }
    }

    static final class Request {
        final String path;
        Request(String path) { this.path = (path == null) ? "/" : path; }
    }
}

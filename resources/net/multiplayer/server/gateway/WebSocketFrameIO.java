package resources.net.multiplayer.server.gateway;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Minimal websocket frame reader/writer for binary traffic.
 */
final class WebSocketFrameIO {

    private static final int OPCODE_BINARY = 0x2;
    private static final int OPCODE_CLOSE = 0x8;
    private static final int OPCODE_PING = 0x9;
    private static final int OPCODE_PONG = 0xA;
    private static final int MAX_PAYLOAD = 1_000_000;

    Frame read(InputStream in) throws IOException {
        int b0 = in.read();
        if (b0 == -1) return null;
        int b1 = in.read();
        if (b1 == -1) throw new EOFException();

        int opcode = b0 & 0x0F;
        boolean masked = (b1 & 0x80) != 0;
        long length = (b1 & 0x7F);
        if (length == 126L) length = readUnsigned16(in);
        else if (length == 127L) length = readUnsigned64(in);
        if (length < 0L || length > MAX_PAYLOAD) throw new IOException("frame too large");

        byte[] maskKey = null;
        if (masked) {
            maskKey = in.readNBytes(4);
            if (maskKey.length != 4) throw new EOFException();
        }
        byte[] payload = in.readNBytes((int) length);
        if (payload.length != (int) length) throw new EOFException();
        if (masked) unmask(payload, maskKey);
        return new Frame(opcode, payload);
    }

    void writeBinary(OutputStream out, byte[] payload) throws IOException {
        write(out, OPCODE_BINARY, payload);
    }

    void writeClose(OutputStream out) throws IOException {
        write(out, OPCODE_CLOSE, new byte[0]);
    }

    void writePong(OutputStream out, byte[] payload) throws IOException {
        write(out, OPCODE_PONG, payload == null ? new byte[0] : payload);
    }

    boolean isClose(Frame frame) { return frame != null && frame.opcode == OPCODE_CLOSE; }
    boolean isPing(Frame frame) { return frame != null && frame.opcode == OPCODE_PING; }
    boolean isBinary(Frame frame) { return frame != null && frame.opcode == OPCODE_BINARY; }

    private void write(OutputStream out, int opcode, byte[] payload) throws IOException {
        byte[] data = (payload == null) ? new byte[0] : payload;
        int b0 = 0x80 | (opcode & 0x0F);
        out.write(b0);
        if (data.length <= 125) {
            out.write(data.length);
        } else if (data.length <= 0xFFFF) {
            out.write(126);
            out.write((data.length >>> 8) & 0xFF);
            out.write(data.length & 0xFF);
        } else {
            out.write(127);
            long len = data.length;
            for (int i = 7; i >= 0; i--) out.write((int) ((len >>> (i * 8)) & 0xFF));
        }
        out.write(data);
        out.flush();
    }

    private static void unmask(byte[] payload, byte[] key) {
        for (int i = 0; i < payload.length; i++) payload[i] = (byte) (payload[i] ^ key[i % 4]);
    }

    private static int readUnsigned16(InputStream in) throws IOException {
        int b0 = in.read();
        int b1 = in.read();
        if (b0 < 0 || b1 < 0) throw new EOFException();
        return ((b0 & 0xFF) << 8) | (b1 & 0xFF);
    }

    private static long readUnsigned64(InputStream in) throws IOException {
        long out = 0L;
        for (int i = 0; i < 8; i++) {
            int b = in.read();
            if (b < 0) throw new EOFException();
            out = (out << 8) | (b & 0xFFL);
        }
        return out;
    }

    static final class Frame {
        final int opcode;
        final byte[] payload;

        Frame(int opcode, byte[] payload) {
            this.opcode = opcode;
            this.payload = payload;
        }
    }
}

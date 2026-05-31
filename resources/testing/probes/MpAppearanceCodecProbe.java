package resources.testing.probes;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the per-player appearance/status fields (facing, moving, sprite,
 * displayName, alive) round-trip through the snapshot codec, and that a payload
 * lacking the trailing appearance section still decodes with sane defaults
 * (backward compatibility).
 */
public final class MpAppearanceCodecProbe implements Probe {

    @Override public String name() { return "mp-appearance-codec"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        ProtocolPayloadCodec codec = new ProtocolPayloadCodec();

        ArrayList<ProtocolPayloads.PlayerState> players = new ArrayList<>();
        players.add(new ProtocolPayloads.PlayerState(
            "alice", 10.0, 20.0, 1.0, -1.0, 5L, 18, 20,
            /*facing*/1, /*moving*/true, "red", "Alice", /*alive*/true));
        players.add(new ProtocolPayloads.PlayerState(
            "bob", 40.0, 80.0, 0.0, 0.0, 7L, 0, 20,
            /*facing*/3, /*moving*/false, "red", "Bob", /*alive*/false));
        ProtocolPayloads.Snapshot snap = new ProtocolPayloads.Snapshot(false, 5L, players);

        ProtocolPayloads.Snapshot decoded = codec.decodeSnapshot(codec.encodeSnapshot(snap));
        boolean roundTrip = decoded.players.size() == 2
            && decoded.players.get(0).facing == 1
            && decoded.players.get(0).moving
            && "Alice".equals(decoded.players.get(0).displayName)
            && decoded.players.get(0).alive
            && decoded.players.get(1).facing == 3
            && !decoded.players.get(1).moving
            && "Bob".equals(decoded.players.get(1).displayName)
            && !decoded.players.get(1).alive;

        // Old-format payload: header + one player row + tile sections, WITHOUT the
        // trailing appearance section. Must decode to defaults (facing=2, moving=false,
        // sprite="red", displayName="", alive=true).
        boolean legacyDefaults;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeBoolean(false);   // baseline
            out.writeLong(5L);         // ack
            out.writeInt(1);           // player count
            writeString(out, "carol");
            out.writeDouble(1.0); out.writeDouble(2.0);   // x,y
            out.writeDouble(0.0); out.writeDouble(0.0);   // vx,vy
            out.writeLong(3L);         // seq
            out.writeInt(20); out.writeInt(20);           // health,max
            out.writeInt(0);           // worldObjects count
            out.writeInt(0);           // entities count
            out.writeInt(0);           // inventories count
            out.writeInt(0);           // tile mutations count
            // (no appearance section)
            out.flush();
            ProtocolPayloads.Snapshot legacy = codec.decodeSnapshot(baos.toByteArray());
            ProtocolPayloads.PlayerState c = legacy.players.get(0);
            legacyDefaults = legacy.players.size() == 1
                && c.facing == 2 && !c.moving
                && "red".equals(c.spriteName)
                && c.displayName.isEmpty()
                && c.alive;
        } catch (Exception e) {
            return ProbeResult.fail(name() + " legacy-decode threw", String.valueOf(e));
        }

        boolean ok = roundTrip && legacyDefaults;
        String details = "roundTrip=" + roundTrip + " legacyDefaults=" + legacyDefaults;
        return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
    }

    /** Mirrors BinaryEnvelopeCodec.writeString (length-prefixed UTF-8). */
    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null) ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
}

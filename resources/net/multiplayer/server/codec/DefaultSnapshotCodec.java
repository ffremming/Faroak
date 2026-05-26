package resources.net.multiplayer.server.codec;

import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Snapshot codec backed by protocol payload codec.
 */
public final class DefaultSnapshotCodec implements SnapshotCodec {

    private final ProtocolPayloadCodec payloadCodec = new ProtocolPayloadCodec();

    @Override
    public byte[] encode(ProtocolPayloads.Snapshot snapshot) {
        return payloadCodec.encodeSnapshot(snapshot);
    }

    @Override
    public ProtocolPayloads.Snapshot decode(byte[] payload) {
        return payloadCodec.decodeSnapshot(payload);
    }
}

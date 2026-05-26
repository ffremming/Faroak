package resources.net.multiplayer.server.codec;

import resources.net.multiplayer.protocol.ProtocolPayloads;

/**
 * Server payload codec port for snapshot serialization.
 */
public interface SnapshotCodec {

    byte[] encode(ProtocolPayloads.Snapshot snapshot);

    ProtocolPayloads.Snapshot decode(byte[] payload);
}

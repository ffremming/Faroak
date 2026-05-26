package resources.net.multiplayer;

import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.server.GameServerRuntime;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.PersistenceStore;
import resources.net.multiplayer.server.persistence.PersistenceStoreFactory;
import resources.net.multiplayer.server.transport.InProcessRealtimeTransport;
import resources.net.multiplayer.server.transport.RealtimeTransport;

/**
 * Shared in-process authoritative server for loopback clients.
 */
final class LoopbackServerHub {

    private static LobbyRuntime lobby;
    private static GameServerRuntime runtime;
    private static RealtimeTransport transport;

    private LoopbackServerHub() {}

    static synchronized RealtimeTransport transport(MultiplayerConfig config) {
        if (transport != null) return transport;
        PersistenceStore store = PersistenceStoreFactory.createDefault(config.sqlitePath());
        lobby = new AuthoritativeLobbyRuntime(
            config, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        runtime = new GameServerRuntime(config, lobby);
        runtime.start();
        transport = new InProcessRealtimeTransport(lobby);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (runtime != null) runtime.close();
        }));
        return transport;
    }

    static synchronized void shutdown() {
        if (runtime != null) runtime.close();
        runtime = null;
        lobby = null;
        transport = null;
    }

    static List<ProtocolEnvelope> safeList(List<ProtocolEnvelope> envelopes) {
        if (envelopes == null) return new ArrayList<>();
        return envelopes;
    }
}

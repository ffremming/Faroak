package resources.net.multiplayer;

import java.util.ArrayList;
import java.util.List;

import resources.app.GameContext;
import resources.net.multiplayer.hostauth.HostAuthoritativeLobby;
import resources.net.multiplayer.hostauth.StableEntityIds;
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
    // Set by the host client before connecting so a host-authoritative lobby can read
    // the real engine. Null for legacy/guest paths.
    private static GameContext hostContext;
    private static HostAuthoritativeLobby hostLobby;

    private LoopbackServerHub() {}

    /** Register the host's engine context so {@code transport()} can build a
     *  HostAuthoritativeLobby when {@code game.multiplayer.lobby=hostauth}. */
    static synchronized void setHostContext(GameContext ctx) { hostContext = ctx; }

    /** The host-authoritative lobby, if one was built. The host frame pumps its
     *  {@code produceSnapshots()}; null under the legacy lobby. */
    static synchronized HostAuthoritativeLobby hostLobby() { return hostLobby; }

    static synchronized RealtimeTransport transport(MultiplayerConfig config) {
        if (transport != null) return transport;
        if ("hostauth".equals(config.lobby()) && hostContext != null) {
            HostAuthoritativeLobby hal = new HostAuthoritativeLobby(
                hostContext, config, new DefaultSnapshotCodec(), new StableEntityIds());
            hostLobby = hal;
            lobby = hal;
        } else {
            PersistenceStore store = PersistenceStoreFactory.createDefault(config.sqlitePath());
            lobby = new AuthoritativeLobbyRuntime(
                config, new DefaultAuthorityService(), store, new DefaultSnapshotCodec());
        }
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
        hostLobby = null;
        hostContext = null;
    }

    static List<ProtocolEnvelope> safeList(List<ProtocolEnvelope> envelopes) {
        if (envelopes == null) return new ArrayList<>();
        return envelopes;
    }
}

package resources.testing.probes;

import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.MultiplayerConfig;
import resources.net.multiplayer.MultiplayerMode;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.protocol.ProtocolMessageType;
import resources.net.multiplayer.protocol.ProtocolPayloadCodec;
import resources.net.multiplayer.protocol.ProtocolPayloads;
import resources.net.multiplayer.server.AuthoritativeLobbyRuntime;
import resources.net.multiplayer.server.LobbyRuntime;
import resources.net.multiplayer.server.authority.DefaultAuthorityService;
import resources.net.multiplayer.server.codec.DefaultSnapshotCodec;
import resources.net.multiplayer.server.persistence.InMemoryPersistenceStore;
import resources.testing.Probe;
import resources.testing.ProbeResult;
import resources.testing.TestHarness;

/**
 * Verifies the server relays player chat to all clients and emits a system line
 * when a player joins.
 */
public final class MpChatProbe implements Probe {

    @Override public String name() { return "mp-chat"; }

    @Override
    public ProbeResult run(TestHarness harness) {
        try {
            MultiplayerConfig cfg = new MultiplayerConfig(
                MultiplayerMode.HOST, "loopback", "host", 10, 30, 20, 1, 120, 20.0, 768.0, 1.0e9, "test.db");
            ProtocolPayloadCodec codec = new ProtocolPayloadCodec();
            LobbyRuntime lobby = new AuthoritativeLobbyRuntime(
                cfg, new DefaultAuthorityService(), new InMemoryPersistenceStore(), new DefaultSnapshotCodec());

            String a = "Alice-aaa111";
            String b = "Bob-bbb222";
            join(lobby, codec, a);
            // Joining B should produce a system "Bob joined" line delivered to A.
            join(lobby, codec, b);
            lobby.tick();
            List<String[]> aChats = chats(lobby.drainFor(a), codec);
            boolean systemJoinSeen = aChats.stream().anyMatch(
                c -> Boolean.parseBoolean(c[2]) && c[1].contains("Bob") && c[1].contains("joined"));

            // A sends a chat; B must receive it with sender "Alice".
            lobby.drainFor(b); // clear
            lobby.receive(new ProtocolEnvelope(1, a, 0L, 0L, 0L, ProtocolMessageType.CHAT,
                codec.encodeChat("hello world")));
            lobby.tick();
            List<String[]> bChats = chats(lobby.drainFor(b), codec);
            boolean relayed = bChats.stream().anyMatch(
                c -> !Boolean.parseBoolean(c[2]) && "Alice".equals(c[0]) && "hello world".equals(c[1]));

            boolean ok = systemJoinSeen && relayed;
            String details = "systemJoin=" + systemJoinSeen + " relayed=" + relayed;
            return ok ? ProbeResult.pass(name(), details) : ProbeResult.fail(name(), details);
        } catch (Exception e) {
            return ProbeResult.fail(name() + " threw", String.valueOf(e));
        }
    }

    private static void join(LobbyRuntime lobby, ProtocolPayloadCodec codec, String playerId) {
        lobby.receive(new ProtocolEnvelope(1, playerId, 0L, 0L, 0L, ProtocolMessageType.JOIN,
            codec.encodeJoinRequest(new ProtocolPayloads.JoinRequest(false, 0.0, 0.0))));
        lobby.tick();
    }

    private static List<String[]> chats(List<ProtocolEnvelope> envelopes, ProtocolPayloadCodec codec) {
        ArrayList<String[]> out = new ArrayList<>();
        for (ProtocolEnvelope e : envelopes) {
            if (ProtocolMessageType.CHAT_BROADCAST.equals(e.messageType())) {
                out.add(codec.decodeChatBroadcast(e.payload()));
            }
        }
        return out;
    }
}

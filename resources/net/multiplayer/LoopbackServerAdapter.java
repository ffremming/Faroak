package resources.net.multiplayer;

import java.util.ArrayList;
import java.util.List;

import resources.net.multiplayer.message.ClientMessage;
import resources.net.multiplayer.message.ServerMessage;
import resources.net.multiplayer.protocol.BinaryEnvelopeCodec;
import resources.net.multiplayer.protocol.ProtocolEnvelope;
import resources.net.multiplayer.server.transport.RealtimeTransport;

/**
 * In-process server adapter. Useful for local host mode, tests, and as a
 * reference adapter for external backends.
 */
public final class LoopbackServerAdapter implements MultiplayerServerAdapter {

    private final RealtimeTransport transport;
    private final ProtocolMessageTranslator translator;
    private final BinaryEnvelopeCodec envelopeCodec = new BinaryEnvelopeCodec();
    private final String playerId;
    private boolean connected;

    public LoopbackServerAdapter(MultiplayerConfig config) {
        this.transport = LoopbackServerHub.transport(config);
        this.translator = new ProtocolMessageTranslator(config.protocolVersion());
        this.playerId = config.playerId();
    }

    @Override
    public void connect(String playerId) {
        connected = true;
        transport.connect(this.playerId);
    }

    @Override
    public void disconnect(String playerId) {
        connected = false;
        transport.disconnect(this.playerId);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void submit(ClientMessage message) {
        if (!connected || message == null) return;
        ProtocolEnvelope envelope = translator.toEnvelope(message);
        if (envelope == null) return;
        // Exercise wire codec even in loopback so protocol regressions surface early.
        ProtocolEnvelope decoded = envelopeCodec.decode(envelopeCodec.encode(envelope));
        if (decoded != null) transport.sendToServer(decoded);
    }

    @Override
    public List<ServerMessage> poll() {
        if (!connected) return new ArrayList<>();
        ArrayList<ServerMessage> out = new ArrayList<>();
        for (ProtocolEnvelope envelope : LoopbackServerHub.safeList(transport.drainForClient(playerId))) {
            ProtocolEnvelope decoded = envelopeCodec.decode(envelopeCodec.encode(envelope));
            ServerMessage message = translator.fromEnvelope(decoded);
            if (message != null) out.add(message);
        }
        return out;
    }

    @Override
    public void tick() {
        // Dedicated in-process server loop runs independently.
    }
}

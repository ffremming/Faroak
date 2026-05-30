package resources.app;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import resources.domain.entity.Entity;
import resources.domain.inventory.Stack;
import resources.net.multiplayer.MultiplayerRuntime;
import resources.net.multiplayer.RemotePlayerAvatar;

/**
 * Opt-in local telemetry endpoint for GUI end-to-end tests.
 *
 * Disabled by default; starts only when game.test.telemetryPort > 0.
 */
final class TestTelemetryServer implements AutoCloseable {

    private final GamePanel panel;
    private final ServerSocket server;
    private final Thread thread;
    private volatile boolean running = true;

    private TestTelemetryServer(GamePanel panel, int port) throws Exception {
        this.panel = panel;
        this.server = new ServerSocket(port, 20, InetAddress.getLoopbackAddress());
        this.thread = new Thread(this::acceptLoop, "test-telemetry-" + port);
    }

    static TestTelemetryServer startIfConfigured(GamePanel panel) {
        int port = parsePort(System.getProperty("game.test.telemetryPort", ""));
        if (panel == null || port <= 0) return null;
        try {
            TestTelemetryServer telemetry = new TestTelemetryServer(panel, port);
            telemetry.thread.start();
            return telemetry;
        } catch (Exception ex) {
            System.out.println("telemetry disabled: " + ex.getMessage());
            return null;
        }
    }

    private void acceptLoop() {
        while (running) {
            try (Socket socket = server.accept()) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                String cmd = in.readLine();
                if ("PING".equalsIgnoreCase(cmd)) {
                    out.println("PONG");
                } else if ("STATUS".equalsIgnoreCase(cmd)) {
                    out.println(statusLine());
                } else if (cmd != null && cmd.startsWith("MOVE ")) {
                    String[] parts = cmd.trim().split("\\s+");
                    String dir = parts.length > 1 ? parts[1] : "";
                    long ms = parseDuration(parts.length > 2 ? parts[2] : "800");
                    out.println(applyMove(dir, ms) ? "OK" : "ERR move");
                } else if ("UI_CLOSE".equalsIgnoreCase(cmd)) {
                    out.println(closeUi() ? "OK" : "ERR ui");
                } else {
                    out.println("ERR unknown");
                }
            } catch (Exception ignored) {
                if (!running) break;
            }
        }
    }

    private String statusLine() {
        MultiplayerRuntime mp = panel.multiplayer();
        boolean online = mp != null && mp.isOnline();
        boolean joined = mp != null && mp.isJoined();
        double playerX = panel.player() == null ? 0.0 : panel.player().getWorldX();
        double playerY = panel.player() == null ? 0.0 : panel.player().getWorldY();
        double cameraX = panel.camera() == null ? 0.0 : panel.camera().getWorldX();
        double cameraY = panel.camera() == null ? 0.0 : panel.camera().getWorldY();
        int remotePlayers = mp == null ? 0 : mp.remotePlayerCount();
        int replicatedObjects = mp == null ? 0 : mp.replicatedObjectCount();
        double remoteMeanX = mp == null ? 0.0 : mp.remotePlayersMeanX();
        double remoteMeanY = mp == null ? 0.0 : mp.remotePlayersMeanY();
        long ackSeq = mp == null ? 0L : mp.lastAckedSequence();
        long localSeq = mp == null ? 0L : mp.localSequence();
        String equipped = equippedName();
        int visibleRemotePlayers = countVisibleRemotePlayers();
        return "ok"
            + " online=" + online
            + " joined=" + joined
            + " playerX=" + fmt(playerX)
            + " playerY=" + fmt(playerY)
            + " cameraX=" + fmt(cameraX)
            + " cameraY=" + fmt(cameraY)
            + " remotePlayers=" + remotePlayers
            + " visibleRemotePlayers=" + visibleRemotePlayers
            + " replicatedObjects=" + replicatedObjects
            + " remoteMeanX=" + fmt(remoteMeanX)
            + " remoteMeanY=" + fmt(remoteMeanY)
            + " localSeq=" + localSeq
            + " ackSeq=" + ackSeq
            + " equipped=" + equipped;
    }

    private int countVisibleRemotePlayers() {
        if (panel.camera() == null || panel.world == null) return 0;
        int count = 0;
        for (Entity entity : panel.world().getVisibleEntities(panel.camera())) {
            if (entity instanceof RemotePlayerAvatar) count++;
        }
        return count;
    }

    private String equippedName() {
        if (panel.player() == null) return "none";
        Stack equipped = panel.player().getEquipped();
        if (equipped == null || equipped.isEmpty()) return "empty";
        return equipped.getName();
    }

    private boolean applyMove(String direction, long ms) {
        if (panel.input() == null) return false;
        final boolean up = "up".equalsIgnoreCase(direction) || "w".equalsIgnoreCase(direction);
        final boolean down = "down".equalsIgnoreCase(direction) || "s".equalsIgnoreCase(direction);
        final boolean left = "left".equalsIgnoreCase(direction) || "a".equalsIgnoreCase(direction);
        final boolean right = "right".equalsIgnoreCase(direction) || "d".equalsIgnoreCase(direction);
        if (!up && !down && !left && !right) return false;

        panel.input().setUp(up);
        panel.input().setDown(down);
        panel.input().setLeft(left);
        panel.input().setRight(right);

        Thread stopper = new Thread(() -> {
            try { Thread.sleep(Math.max(60L, ms)); }
            catch (InterruptedException ignored) {}
            panel.input().setUp(false);
            panel.input().setDown(false);
            panel.input().setLeft(false);
            panel.input().setRight(false);
        }, "telemetry-move-stop");
        stopper.setDaemon(true);
        stopper.start();
        return true;
    }

    private boolean closeUi() {
        if (panel.userInterface() == null) return false;
        while (panel.userInterface().hasOpenOverlay()) {
            panel.userInterface().closeTopOverlay();
        }
        if (panel.userInterface().isMenuOpen()) {
            panel.userInterface().toggleMenu();
        }
        panel.userInterface().cleanUI();
        return true;
    }

    @Override
    public void close() {
        running = false;
        try { server.close(); } catch (Exception ignored) {}
    }

    private static int parsePort(String raw) {
        if (raw == null || raw.isBlank()) return -1;
        try {
            int p = Integer.parseInt(raw.trim());
            return (p <= 0 || p > 65535) ? -1 : p;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static long parseDuration(String raw) {
        if (raw == null || raw.isBlank()) return 800L;
        try {
            long v = Long.parseLong(raw.trim());
            return Math.max(60L, Math.min(10_000L, v));
        } catch (NumberFormatException ignored) {
            return 800L;
        }
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }
}

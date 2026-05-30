package resources.testing;

import java.awt.GraphicsEnvironment;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * True GUI multiplayer E2E:
 * - launches two real game processes
 * - drives real mouse/keyboard input via Robot
 * - verifies cross-client state through test telemetry sockets
 */
public final class MultiplayerGuiE2ERunner {

    private static final int W = 1280;
    private static final int H = 768;
    private static final int HOST_X = 40;
    private static final int HOST_Y = 40;
    private static final int JOIN_X = 1360;
    private static final int JOIN_Y = 40;

    private MultiplayerGuiE2ERunner() {}

    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("GUI E2E requires a non-headless desktop session.");
        }
        int gatewayPort = freePort();
        int hostTelemetryPort = freePort();
        int joinTelemetryPort = freePort();
        Process host = launchHost(HOST_X, HOST_Y, hostTelemetryPort, gatewayPort);
        Process join = launchJoin(JOIN_X, JOIN_Y, joinTelemetryPort, gatewayPort);
        pipe(host, "host");
        pipe(join, "join");
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(70);

            waitForJoined(host, join, hostTelemetryPort, joinTelemetryPort, Duration.ofSeconds(35));
            waitForRemotes(host, join, hostTelemetryPort, joinTelemetryPort, Duration.ofSeconds(25));

            double beforeMeanX = parseDouble(status(joinTelemetryPort).get("remoteMeanX"));
            focusGame(robot, HOST_X, HOST_Y);
            holdKey(robot, KeyEvent.VK_D, 1200);
            waitForRemoteMovement(host, join, joinTelemetryPort, beforeMeanX, Duration.ofSeconds(12));

            int hostObjectsBefore = parseInt(status(hostTelemetryPort).get("replicatedObjects"));
            int joinObjectsBefore = parseInt(status(joinTelemetryPort).get("replicatedObjects"));
            Map<String, String> hostStatus = status(hostTelemetryPort);
            int clickX = HOST_X + playerScreenX(hostStatus);
            int clickY = HOST_Y + playerScreenY(hostStatus);
            leftClick(robot, clickX, clickY);
            waitForReplicatedObjectDelta(host, join, hostTelemetryPort, joinTelemetryPort,
                hostObjectsBefore, joinObjectsBefore, Duration.ofSeconds(12));

            System.out.println("PASS multiplayer-gui-e2e "
                + "joined=true remotes-visible=true movement-replicated=true interactions-replicated=true");
        } finally {
            host.destroy();
            join.destroy();
        }
    }

    private static void waitForJoined(Process host, Process join, int hostPort, int joinPort, Duration timeout) throws Exception {
        waitUntil(timeout, host, join, () -> {
            Map<String, String> hs = status(hostPort);
            Map<String, String> js = status(joinPort);
            return bool(hs.get("joined")) && bool(js.get("joined"));
        }, "clients did not reach joined=true");
    }

    private static void waitForRemotes(Process host, Process join, int hostPort, int joinPort, Duration timeout) throws Exception {
        waitUntil(timeout, host, join, () -> {
            Map<String, String> hs = status(hostPort);
            Map<String, String> js = status(joinPort);
            return parseInt(hs.get("remotePlayers")) >= 1
                && parseInt(js.get("remotePlayers")) >= 1
                && parseInt(hs.get("visibleRemotePlayers")) >= 1
                && parseInt(js.get("visibleRemotePlayers")) >= 1;
        }, "remote players not visible in both clients");
    }

    private static void waitForRemoteMovement(Process host, Process join, int joinPort, double beforeMeanX, Duration timeout) throws Exception {
        waitUntil(timeout, host, join, () -> {
            Map<String, String> js = status(joinPort);
            double after = parseDouble(js.get("remoteMeanX"));
            return Math.abs(after - beforeMeanX) >= 10.0;
        }, "remote player movement did not replicate");
    }

    private static void waitForReplicatedObjectDelta(
            Process host, Process join, int hostPort, int joinPort,
            int hostBefore, int joinBefore, Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        Map<String, String> lastHost = new HashMap<>();
        Map<String, String> lastJoin = new HashMap<>();
        while (Instant.now().isBefore(deadline)) {
            ensureAlive(host, "host");
            ensureAlive(join, "join");
            try {
                lastHost = status(hostPort);
                lastJoin = status(joinPort);
                boolean ok = parseInt(lastHost.get("replicatedObjects")) > hostBefore
                    && parseInt(lastJoin.get("replicatedObjects")) > joinBefore;
                if (ok) return;
            } catch (Exception ignored) {}
            Thread.sleep(150L);
        }
        throw new IllegalStateException(
            "replicated object interaction did not appear on both clients"
            + " host=" + compact(lastHost)
            + " join=" + compact(lastJoin));
    }

    private static void waitUntil(Duration timeout, Process host, Process join, ProbeCondition condition, String failure) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            ensureAlive(host, "host");
            ensureAlive(join, "join");
            try {
                if (condition.ok()) return;
            } catch (Exception ignored) {}
            Thread.sleep(150L);
        }
        throw new IllegalStateException(failure);
    }

    private static void focusGame(Robot robot, int x, int y) {
        leftClick(robot, x + (W / 2), y + (H / 2));
    }

    private static void holdKey(Robot robot, int keyCode, long ms) throws Exception {
        robot.keyPress(keyCode);
        Thread.sleep(ms);
        robot.keyRelease(keyCode);
    }

    private static void leftClick(Robot robot, int x, int y) {
        robot.mouseMove(x, y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private static Process launchHost(int x, int y, int telemetryPort, int gatewayPort) throws Exception {
        return launchCommon(
            x, y, telemetryPort,
            "host",
            "ws://127.0.0.1:" + gatewayPort + "/ws",
            gatewayPort,
            "p_host");
    }

    private static Process launchJoin(int x, int y, int telemetryPort, int gatewayPort) throws Exception {
        return launchCommon(
            x, y, telemetryPort,
            "client",
            "ws://127.0.0.1:" + gatewayPort + "/ws",
            gatewayPort,
            "p_join");
    }

    private static Process launchCommon(
            int x,
            int y,
            int telemetryPort,
            String mode,
            String serverUrl,
            int gatewayPort,
            String playerId) throws Exception {
        String javaBin = System.getProperty("java.home") + "/bin/java";
        String cp = System.getProperty("java.class.path");
        ProcessBuilder pb = new ProcessBuilder(
            javaBin,
            "-cp", cp,
            "-Dgame.test.autostart=true",
            "-Dgame.test.undecorated=true",
            "-Dgame.window.x=" + x,
            "-Dgame.window.y=" + y,
            "-Dgame.test.telemetryPort=" + telemetryPort,
            "-Dgame.multiplayer.mode=" + mode,
            "-Dgame.multiplayer.backend=websocket",
            "-Dgame.multiplayer.playerId=" + playerId,
            "-Dgame.multiplayer.gateway.enabled=true",
            "-Dgame.multiplayer.gatewayPort=" + gatewayPort,
            "-Dgame.multiplayer.serverUrl=" + serverUrl,
            "-Dgame.multiplayer.maxPlayers=2",
            "-Dgame.multiplayer.reconcileLocal=false",
            "-Dgame.multiplayer.serverMoveSpeedPerTick=10.0",
            "-Dgame.test.hotbarItem=block",
            "resources.app.Main");
        pb.redirectErrorStream(true);
        return pb.start();
    }

    private static Map<String, String> status(int port) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(1500);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println("STATUS");
            String line = in.readLine();
            return parseStatus(line);
        }
    }

    private static Map<String, String> parseStatus(String line) {
        Map<String, String> map = new HashMap<>();
        if (line == null) return map;
        for (String token : line.trim().split("\\s+")) {
            int idx = token.indexOf('=');
            if (idx <= 0 || idx == token.length() - 1) continue;
            map.put(token.substring(0, idx), token.substring(idx + 1));
        }
        return map;
    }

    private static void pipe(Process process, String tag) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) System.out.println("[" + tag + "] " + line);
            } catch (Exception ignored) {}
        }, "pipe-" + tag);
        t.setDaemon(true);
        t.start();
    }

    private static void ensureAlive(Process p, String label) {
        if (p.isAlive()) return;
        throw new IllegalStateException(label + " process exited with code " + p.exitValue());
    }

    private static int freePort() throws Exception {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private static int parseInt(String raw) {
        try { return Integer.parseInt(raw == null ? "0" : raw); }
        catch (Exception ignored) { return 0; }
    }

    private static double parseDouble(String raw) {
        try { return Double.parseDouble(raw == null ? "0" : raw); }
        catch (Exception ignored) { return 0.0; }
    }

    private static boolean bool(String raw) {
        return "true".equalsIgnoreCase(raw);
    }

    private static int playerScreenX(Map<String, String> s) {
        double px = parseDouble(s.get("playerX"));
        double cx = parseDouble(s.get("cameraX"));
        int v = (int) Math.round((px - cx) + 24.0);
        return Math.max(32, Math.min(W - 32, v));
    }

    private static int playerScreenY(Map<String, String> s) {
        double py = parseDouble(s.get("playerY"));
        double cy = parseDouble(s.get("cameraY"));
        int v = (int) Math.round((py - cy) + 48.0);
        return Math.max(32, Math.min(H - 32, v));
    }

    @FunctionalInterface
    private interface ProbeCondition {
        boolean ok() throws Exception;
    }

    private static String compact(Map<String, String> m) {
        return "{joined=" + m.get("joined")
            + ", remotePlayers=" + m.get("remotePlayers")
            + ", visibleRemotePlayers=" + m.get("visibleRemotePlayers")
            + ", replicatedObjects=" + m.get("replicatedObjects")
            + ", localSeq=" + m.get("localSeq")
            + ", ackSeq=" + m.get("ackSeq")
            + "}";
    }
}

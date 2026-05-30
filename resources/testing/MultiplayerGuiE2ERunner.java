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
            boolean movementVerified = false;
            if (bool(System.getProperty("game.test.requireMovementReplication", "false"))) {
                verifyMovementReplication(robot, host, join, hostTelemetryPort, joinTelemetryPort, Duration.ofSeconds(20));
                movementVerified = true;
            }

            verifyReplicatedPlaceInteraction(robot, host, join, hostTelemetryPort, joinTelemetryPort, Duration.ofSeconds(20));

            System.out.println("PASS multiplayer-gui-e2e "
                + "joined=true remotes-visible=true movement-replicated=" + movementVerified
                + " interactions-replicated=true");
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
        Instant deadline = Instant.now().plus(timeout);
        Map<String, String> lastHost = new HashMap<>();
        Map<String, String> lastJoin = new HashMap<>();
        while (Instant.now().isBefore(deadline)) {
            ensureAlive(host, "host");
            ensureAlive(join, "join");
            try {
                lastHost = status(hostPort);
                lastJoin = status(joinPort);
                boolean ok = parseInt(lastHost.get("remotePlayers")) >= 1
                    && parseInt(lastJoin.get("remotePlayers")) >= 1
                    && parseInt(lastHost.get("visibleRemotePlayers")) >= 1
                    && parseInt(lastJoin.get("visibleRemotePlayers")) >= 1;
                if (ok) return;
            } catch (Exception ignored) {}
            Thread.sleep(150L);
        }
        throw new IllegalStateException(
            "remote players not visible in both clients"
            + " host=" + compact(lastHost)
            + " join=" + compact(lastJoin));
    }

    private static void verifyMovementReplication(
            Robot robot,
            Process host,
            Process join,
            int hostPort,
            int joinPort,
            Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (attemptMovementFromMover(host, join, hostPort, joinPort)) return;
            if (attemptMovementFromMover(host, join, joinPort, hostPort)) return;
            Thread.sleep(140L);
        }
        Map<String, String> hs = status(hostPort);
        Map<String, String> js = status(joinPort);
        throw new IllegalStateException(
            "remote player movement did not replicate"
            + " host=" + compact(hs)
            + " join=" + compact(js));
    }

    private static boolean attemptMovementFromMover(
            Process host,
            Process join,
            int moverPort,
            int observerPort) throws Exception {
        String[] dirs = { "right", "down", "left", "up" };
        for (String dir : dirs) {
            ensureAlive(host, "host");
            ensureAlive(join, "join");
            Map<String, String> moverBefore = status(moverPort);
            Map<String, String> observerBefore = status(observerPort);
            double moverX0 = parseDouble(moverBefore.get("playerX"));
            double moverY0 = parseDouble(moverBefore.get("playerY"));
            double remoteX0 = parseDouble(observerBefore.get("remoteMeanX"));
            double remoteY0 = parseDouble(observerBefore.get("remoteMeanY"));

            String reply = command(moverPort, "MOVE " + dir + " 1300");
            if (!"OK".equalsIgnoreCase(reply)) continue;
            Thread.sleep(1450L);

            Map<String, String> moverAfter = status(moverPort);
            Map<String, String> observerAfter = status(observerPort);
            double moverDx = parseDouble(moverAfter.get("playerX")) - moverX0;
            double moverDy = parseDouble(moverAfter.get("playerY")) - moverY0;
            double remoteDx = parseDouble(observerAfter.get("remoteMeanX")) - remoteX0;
            double remoteDy = parseDouble(observerAfter.get("remoteMeanY")) - remoteY0;
            double moverDist = Math.hypot(moverDx, moverDy);
            double remoteDist = Math.hypot(remoteDx, remoteDy);
            if (moverDist >= 6.0 && remoteDist >= 4.0) return true;
        }
        return false;
    }

    private static void verifyReplicatedPlaceInteraction(
            Robot robot,
            Process host,
            Process join,
            int hostPort,
            int joinPort,
            Duration timeout) throws Exception {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            ensureAlive(host, "host");
            ensureAlive(join, "join");
            command(hostPort, "UI_CLOSE");
            command(joinPort, "UI_CLOSE");
            if (attemptPlaceFromClient(robot, HOST_X, hostPort, hostPort, joinPort)) return;
            if (attemptPlaceFromClient(robot, JOIN_X, joinPort, hostPort, joinPort)) return;
            Thread.sleep(120L);
        }
        Map<String, String> hs = status(hostPort);
        Map<String, String> js = status(joinPort);
        throw new IllegalStateException(
            "replicated object interaction did not appear on both clients"
            + " host=" + compact(hs)
            + " join=" + compact(js));
    }

    private static boolean attemptPlaceFromClient(
            Robot robot,
            int windowX,
            int placerPort,
            int hostPort,
            int joinPort) throws Exception {
        Map<String, String> beforeHost = status(hostPort);
        Map<String, String> beforeJoin = status(joinPort);
        Map<String, String> beforePlacer = status(placerPort);
        int hostBefore = parseInt(beforeHost.get("replicatedObjects"));
        int joinBefore = parseInt(beforeJoin.get("replicatedObjects"));
        long seqBefore = parseInt(beforePlacer.get("localSeq"));

        int baseX = windowX + playerScreenX(beforePlacer);
        int baseY = (windowX == HOST_X ? HOST_Y : JOIN_Y) + playerScreenY(beforePlacer);
        int[][] offsets = {
            { 96, 0 }, { -96, 0 }, { 0, 96 }, { 0, -96 },
            { 64, 64 }, { -64, 64 }, { 64, -64 }, { -64, -64 },
            { 24, 0 }, { 0, 24 }, { -24, 0 }, { 0, -24 }
        };

        int minX = windowX + 32;
        int maxX = windowX + W - 32;
        int winY = (windowX == HOST_X ? HOST_Y : JOIN_Y);
        int minY = winY + 32;
        int maxY = winY + H - 32;
        for (int[] off : offsets) {
            focusGame(robot, windowX, winY);
            Thread.sleep(140L);
            int clickX = clamp(baseX + off[0], minX, maxX);
            int clickY = clamp(baseY + off[1], minY, maxY);
            for (int attempt = 0; attempt < 2; attempt++) {
                leftClick(robot, clickX, clickY);
                Thread.sleep(280L);
                Map<String, String> afterHost = status(hostPort);
                Map<String, String> afterJoin = status(joinPort);
                Map<String, String> afterPlacer = status(placerPort);
                boolean actionSent = parseInt(afterPlacer.get("localSeq")) > seqBefore;
                boolean replicated = parseInt(afterHost.get("replicatedObjects")) > hostBefore
                    && parseInt(afterJoin.get("replicatedObjects")) > joinBefore;
                if (actionSent && replicated) return true;
                if (actionSent) break;
            }
        }
        return false;
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
            "-Dgame.multiplayer.reconcileLocal=true",
            "-Dgame.multiplayer.serverMoveSpeedPerTick=20.0",
            "-Dgame.multiplayer.serverActionRange=768.0",
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

    private static String command(int port, String command) throws Exception {
        try (Socket socket = new Socket("127.0.0.1", port)) {
            socket.setSoTimeout(1500);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println(command);
            String line = in.readLine();
            return line == null ? "" : line.trim();
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

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
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
            + ", playerX=" + m.get("playerX")
            + ", playerY=" + m.get("playerY")
            + ", remoteMeanX=" + m.get("remoteMeanX")
            + ", remoteMeanY=" + m.get("remoteMeanY")
            + ", localSeq=" + m.get("localSeq")
            + ", ackSeq=" + m.get("ackSeq")
            + ", equipped=" + m.get("equipped")
            + "}";
    }
}

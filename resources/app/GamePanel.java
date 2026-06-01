package resources.app;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JFrame;
import javax.swing.JPanel;

import resources.core.event.EventBus;
import resources.core.time.GameClock;
import resources.domain.inventory.ItemManager;
import resources.domain.player.Playable;
import resources.domain.tile.TileManager;
import resources.environment.EnvironmentManager;
import resources.input.InputHandlingSystem;
import resources.input.Keys;
import resources.input.Mouse;
import resources.presentation.animation.AnimationLibrary;
import resources.presentation.camera.Camera;
import resources.presentation.image.ImageContainer;
import resources.presentation.lighting.LightField;
import resources.presentation.ui.Container;
import resources.presentation.ui.UserInterface;
import resources.net.AllowAllServerAuthority;
import resources.net.LoopbackNetworkChannel;
import resources.net.NetworkChannel;
import resources.net.ServerAuthority;
import resources.net.multiplayer.MultiplayerRuntime;
import resources.world.DimensionService;
import resources.world.MapHandler;
import resources.world.WorkingMemory;
import resources.world.WorldRuntime;

/**
 * Top-level Swing component + service holder. Hosts the camera-driven render
 * (in {@link #paintComponent}) and bootstraps the simulation services via
 * {@link #setUpObjects()}; the per-frame loop lives in {@link GameLoop}.
 *
 * Implements {@link GameContext} so subsystems can depend on the contract
 * instead of GamePanel directly.
 */
public class GamePanel extends JPanel implements GameContext {

    public  final int tileSize     = 64;
    public  final int screenHeight = tileSize * 12;
    public  final int screenWidth  = tileSize * 20;

    // World + simulation services
    GenerationManager generationM;
    public TileManager     tileM;
    public ItemManager     itemM;
    public ImageContainer  imageContainer;
    public EnvironmentManager environmentM;
    public MapHandler      mapH;
    public WorkingMemory   world;

    // Presentation / input
    public Camera        camera;
    public Container     UI;
    public UserInterface userInterface;
    public Playable      player;
    public Keys          keys;
    public Mouse         mouse;
    public InputHandlingSystem inputHandlingSystem;

    // Cross-cutting kernel
    private final EventBus         eventBus  = new EventBus();
    private final GameClock        clock     = new GameClock(
        GameClock.DEFAULT_TICKS_PER_DAY, GameClock.NOON_TICK_OF_DAY);
    private final NetworkChannel   network   = new LoopbackNetworkChannel();
    private final ServerAuthority  authority = new AllowAllServerAuthority();
    private final AnimationLibrary animations = new AnimationLibrary();
    private final LightField       lighting   = new LightField();
    private final AudioSettings    audio      = new AudioSettings();
    private final BackgroundMusicPlayer music  = new BackgroundMusicPlayer(audio);
    private DimensionService       dimensions;
    private MultiplayerRuntime     multiplayer;
    private TestTelemetryServer    telemetry;
    private double                 frameDelta = 1.0;

    public final JFrame frame;
    final boolean newGame;

    /** Live window dimensions (distinct from the constant screen size). Updated by EnvironmentManager. */
    public int width;
    public int height;

    private GameLoop loop;
    private Thread   loopThread;

    public GamePanel(JFrame frame, boolean newGame) {
        this.frame = frame;
        this.newGame = newGame;
        setPreferredSize(new Dimension(screenWidth, screenHeight));
        setBackground(Color.black);
        setDoubleBuffered(true);
        // The scene is fully repainted every frame (camera draws over the whole
        // panel before any UI). Marking opaque lets Swing skip the cleared
        // background fill that would otherwise happen in super.paintComponent.
        setOpaque(true);
        setFocusable(true);
        requestFocus();

        setUpObjects();
        wireInput();
    }

    private void setUpObjects() {
        resources.presentation.ui.LoadingScreen.setStatus("Generating world");
        generationM = new GenerationManager(this);
        generationM.generateMap();
        resources.presentation.ui.LoadingScreen.setStatus("Preparing environment");
        environmentM = new EnvironmentManager(this);
        resources.presentation.ui.LoadingScreen.setStatus("Wiring input");
        keys  = new Keys(this);
        mouse = new Mouse(this);
        inputHandlingSystem = new InputHandlingSystem(this);
        resources.presentation.ui.LoadingScreen.setStatus("Building UI");
        UI = new Container(this, 100, 100);
        userInterface = new UserInterface(this, 0, 0);
        userInterface.setVisible(true);
        userInterface.enable();
        resources.presentation.ui.LoadingScreen.setStatus("Placing player");
        generationM.initiate();
        resources.presentation.ui.LoadingScreen.setStatus("Spawning entities");
        generationM.seedWorldEntities();
        resources.presentation.ui.LoadingScreen.setStatus("Setting up camera");
        camera = new Camera(this, "camera", 0, 0,
            (short) (screenWidth + 400), (short) (screenHeight + 400));
        resources.presentation.ui.LoadingScreen.setStatus("Initializing multiplayer");
        multiplayer = MultiplayerRuntime.createDefault(this);
        telemetry = TestTelemetryServer.startIfConfigured(this);
        resources.presentation.ui.LoadingScreen.setStatus("Finalizing");
    }

    private void wireInput() {
        addKeyListener(keys);
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);

        // Escape is bound through Swing's InputMap/ActionMap with WHEN_IN_FOCUSED_WINDOW
        // scope rather than relying on the KeyListener. A KeyListener only fires when this
        // exact panel holds keyboard focus; if focus drifts (or never lands here after the
        // window is shown) Escape falls through to the platform default and the app loses
        // focus / closes. A window-scoped binding always reaches the menu toggle.
        javax.swing.InputMap im = getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "toggleEscapeMenu");
        im.put(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, 0), "toggleEscapeMenu");
        getActionMap().put("toggleEscapeMenu", new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                if (userInterface == null) return;
                // Escape closes an open container overlay (chest/crafting/barrel)
                // first; only when none is open does it toggle the pause menu.
                if (userInterface.closeTopOverlay()) return;
                userInterface.toggleMenu();
            }
        });
    }

    public void startGameThread() {
        music.startLoop();
        loop = new GameLoop(this);
        loopThread = new Thread(loop, "game-loop");
        loopThread.start();
    }

    public void stopGameThread() {
        if (loop != null) loop.stop();
        if (multiplayer != null) multiplayer.close();
        if (telemetry != null) telemetry.close();
        music.close();
    }

    /** One simulation step. Called by {@link GameLoop} from the EDT. */
    public void update(double delta) {
        this.frameDelta = (delta <= 0.0) ? 1.0 : delta;
        environmentM.updateTicks();
        inputHandlingSystem.update(delta);
        if (multiplayer != null) multiplayer.update(delta);
        music.syncSettings();
        world.simulate();
        // Clock drives water-wave animation, plant growth, and day-night mob spawning.
        // The day-night LIGHT cycle stays frozen independently in LightingPass.ambientAlpha()
        // (it hard-returns 0 = full daylight), so ticking here does not darken the scene.
        // Online: the server owns the clock and drives it via snapshots
        // (MultiplayerRuntime.applyWorldTime), so we must not also tick locally.
        if (multiplayer == null || !multiplayer.drivesWorldClock()) {
            clock.tick();
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        camera.draw(g);
    }

    public void newSeed() {
        generationM.newSeed();
        world.simulate();
        camera.follow(player);
    }

    public int getFrameHeight() { return (int) frame.getBounds().getHeight(); }
    public int getFrameWidth()  { return (int) frame.getBounds().getWidth(); }

    // ---- GameContext implementation: forwards to existing fields so the rest
    // of the codebase can depend on the GameContext interface instead of
    // GamePanel directly.

    @Override public EventBus            events()        { return eventBus; }
    @Override public GameClock           clock()         { return clock; }
    @Override public NetworkChannel      network()       { return network; }
    @Override public ServerAuthority     authority()     { return authority; }
    @Override public MultiplayerRuntime  multiplayer()   { return multiplayer; }

    /** Current frame's delta (1.0 == one 60 FPS frame). Used to make the online
     *  local player's walk speed frame-rate-independent so it doesn't drift from
     *  the fixed-rate authoritative server. */
    public double frameDelta() { return frameDelta; }

    /** True when the local player's movement should be time-corrected to match the
     *  server (i.e. we are online). Offline play keeps the original per-frame model. */
    public boolean movementIsTimeCorrected() {
        return multiplayer != null && multiplayer.isOnline();
    }

    @Override public WorldRuntime        world()         { return world; }
    @Override public TileManager         tiles()         { return tileM; }
    @Override public ItemManager         items()         { return itemM; }
    @Override public EnvironmentManager  environment()   { return environmentM; }
    @Override public MapHandler          mapHandler()    { return mapH; }
    @Override public DimensionService    dimensions()    { return dimensions; }
    public void setDimensions(DimensionService d)        { this.dimensions = d; }
    @Override public Playable            player()        { return player; }
    @Override public Camera              camera()        { return camera; }
    @Override public ImageContainer      images()        { return imageContainer; }
    @Override public AnimationLibrary    animations()    { return animations; }
    @Override public LightField          lighting()      { return lighting; }
    public AudioSettings                 audioSettings() { return audio; }
    public void                          syncAudio()     { music.syncSettings(); }
    @Override public UserInterface       userInterface() { return userInterface; }
    @Override public Keys                keys()          { return keys; }
    @Override public Mouse               mouse()         { return mouse; }
    @Override public InputHandlingSystem input()         { return inputHandlingSystem; }
    @Override public JFrame              frame()         { return frame; }
    @Override public int                 tileSize()      { return tileSize; }
    @Override public int                 screenWidth()   { return screenWidth; }
    @Override public int                 screenHeight()  { return screenHeight; }
}

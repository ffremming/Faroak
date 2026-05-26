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
    private DimensionService       dimensions;
    private MultiplayerRuntime     multiplayer;

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
        setFocusable(true);
        requestFocus();

        setUpObjects();
        wireInput();
    }

    private void setUpObjects() {
        generationM = new GenerationManager(this);
        generationM.generateMap();
        environmentM = new EnvironmentManager(this);
        keys  = new Keys(this);
        mouse = new Mouse(this);
        inputHandlingSystem = new InputHandlingSystem(this);
        UI = new Container(this, 100, 100);
        userInterface = new UserInterface(this, 0, 0);
        userInterface.setVisible(true);
        userInterface.enable();
        generationM.initiate();
        generationM.seedWorldEntities();
        camera = new Camera(this, "camera", 0, 0,
            (short) (screenWidth + 400), (short) (screenHeight + 400));
        multiplayer = MultiplayerRuntime.createDefault(this);
    }

    private void wireInput() {
        addKeyListener(keys);
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public void startGameThread() {
        loop = new GameLoop(this);
        loopThread = new Thread(loop, "game-loop");
        loopThread.start();
    }

    public void stopGameThread() {
        if (loop != null) loop.stop();
        if (multiplayer != null) multiplayer.close();
    }

    /** One simulation step. Called by {@link GameLoop} from the EDT. */
    public void update(double delta) {
        environmentM.updateTicks();
        inputHandlingSystem.update(delta);
        if (multiplayer != null) multiplayer.update(delta);
        world.simulate();
        clock.tick();
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
    @Override public UserInterface       userInterface() { return userInterface; }
    @Override public Keys                keys()          { return keys; }
    @Override public Mouse               mouse()         { return mouse; }
    @Override public InputHandlingSystem input()         { return inputHandlingSystem; }
    @Override public JFrame              frame()         { return frame; }
    @Override public int                 tileSize()      { return tileSize; }
    @Override public int                 screenWidth()   { return screenWidth; }
    @Override public int                 screenHeight()  { return screenHeight; }
}

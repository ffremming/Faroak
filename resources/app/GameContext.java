package resources.app;

import javax.swing.JFrame;

import resources.core.event.EventBus;
import resources.core.time.GameClock;
import resources.domain.player.Playable;
import resources.domain.tile.TileManager;
import resources.domain.inventory.ItemManager;
import resources.environment.EnvironmentManager;
import resources.input.InputHandlingSystem;
import resources.input.Keys;
import resources.input.Mouse;
import resources.presentation.animation.AnimationLibrary;
import resources.presentation.camera.Camera;
import resources.presentation.image.ImageContainer;
import resources.presentation.lighting.LightField;
import resources.presentation.ui.UserInterface;
import resources.world.MapHandler;
import resources.world.WorldRuntime;

/**
 * Service Locator for the runtime. Defines the contract of game-wide services
 * that any subsystem may need. {@link GamePanel} is the concrete implementation;
 * subsystems should depend on this interface rather than on GamePanel directly,
 * so that pieces can be tested or swapped in isolation.
 *
 * Existing code that accesses GamePanel fields directly continues to work — these
 * accessors are the recommended path for new code.
 */
public interface GameContext {

    // Cross-cutting kernel
    EventBus  events();
    GameClock clock();

    // World + simulation
    WorldRuntime world();
    TileManager tiles();
    ItemManager items();
    EnvironmentManager environment();
    MapHandler mapHandler();

    // Player + actors
    Playable player();

    // Presentation
    Camera camera();
    ImageContainer images();
    AnimationLibrary animations();
    LightField lighting();
    UserInterface userInterface();

    // Input
    Keys keys();
    Mouse mouse();
    InputHandlingSystem input();

    // Window dimensions / scale
    JFrame frame();
    int tileSize();
    int screenWidth();
    int screenHeight();
}

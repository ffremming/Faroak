package resources.domain.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import resources.app.GameContext;
import resources.core.id.Identifier;
import resources.domain.entity.component.EntityComponent;

/**
 * Definition of a non-player entity that can be spawned by name or by ID
 * (vegetation, mobs, placed objects). Held in {@link EntityTypeRegistry}.
 *
 * Holds the sprite key, hitbox/size defaults, and a list of component factories
 * that are run when an instance is built. This is what makes entities
 * data-driven: a new "torch" entity is one registration with a sprite name +
 * a {@code LightSourceComponent} factory, no new class required.
 */
public final class EntityType {

    private final Identifier id;
    private final String spriteName;
    private final int width;
    private final int height;
    private final int hitBoxWidth;
    private final int hitBoxHeight;
    private final boolean solid;
    private final List<Function<GameContext, EntityComponent>> componentFactories;

    private EntityType(Builder b) {
        this.id           = b.id;
        this.spriteName   = b.spriteName;
        this.width        = b.width;
        this.height       = b.height;
        this.hitBoxWidth  = b.hitBoxWidth;
        this.hitBoxHeight = b.hitBoxHeight;
        this.solid        = b.solid;
        this.componentFactories = Collections.unmodifiableList(new ArrayList<>(b.componentFactories));
    }

    public Identifier id()              { return id; }
    public String     spriteName()      { return spriteName; }
    public int        width()           { return width; }
    public int        height()          { return height; }
    public int        hitBoxWidth()     { return hitBoxWidth; }
    public int        hitBoxHeight()    { return hitBoxHeight; }
    public boolean    solid()           { return solid; }

    public List<Function<GameContext, EntityComponent>> componentFactories() {
        return componentFactories;
    }

    public static Builder builder(Identifier id, String spriteName) {
        return new Builder(id, spriteName);
    }

    public static final class Builder {
        private final Identifier id;
        private final String spriteName;
        private int width = 64, height = 64;
        private int hitBoxWidth = 64, hitBoxHeight = 64;
        private boolean solid = true;
        private final List<Function<GameContext, EntityComponent>> componentFactories = new ArrayList<>();

        private Builder(Identifier id, String spriteName) {
            this.id = id;
            this.spriteName = spriteName;
        }

        public Builder size(int width, int height) {
            this.width = width; this.height = height; return this;
        }

        public Builder hitBox(int w, int h) {
            this.hitBoxWidth = w; this.hitBoxHeight = h; return this;
        }

        public Builder solid(boolean v) { this.solid = v; return this; }

        /** Register a factory that adds a component to each spawned instance. */
        public Builder component(Function<GameContext, EntityComponent> factory) {
            componentFactories.add(factory);
            return this;
        }

        public EntityType build() { return new EntityType(this); }
    }
}

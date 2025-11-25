package org.infinitytwo.nyctotile.core.context;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.constants.LogicalSide;
import org.infinitytwo.nyctotile.core.constants.PhysicalSide;
import org.infinitytwo.nyctotile.core.renderer.Camera;

// TODO: ADD SOME MORE USEFUL STUFF
@Deprecated
public final class ClientContext extends Context {
    private final Camera camera;
    private final Window window;

    private ClientContext(Camera camera, Window window) {
        super(LogicalSide.CLIENT, PhysicalSide.LOCAL); // There is no way a Dedicated server handles a client
        this.camera = camera;
        this.window = window;
    }

    public static ClientContext of(Camera camera, Window window) {
        return new ClientContext(camera, window);
    }

    public Camera getCamera() {
        return camera;
    }

    public Window getWindow() {
        return window;
    }
}

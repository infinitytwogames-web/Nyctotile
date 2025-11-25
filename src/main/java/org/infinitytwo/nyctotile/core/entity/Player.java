package org.infinitytwo.nyctotile.core.entity;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.AABB;
import org.infinitytwo.nyctotile.core.data.InputManager;
import org.infinitytwo.nyctotile.core.data.Inventory;
import org.infinitytwo.nyctotile.core.data.PlayerData;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.input.VelocityChangedEvent;
import org.infinitytwo.nyctotile.core.renderer.Camera;
import org.infinitytwo.nyctotile.core.world.GMap;
import org.infinitytwo.nyctotile.core.world.dimension.Dimension;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static org.joml.Math.lerp;

public class Player extends Entity {
    protected PlayerData data = PlayerData.shell("Dev");
    @Nullable
    protected Camera camera;
    private InputManager input;
    private final EventBus bus = new EventBus("Player");
    
    public Player(PlayerData data, Dimension map, @Nullable Camera camera, Window window) {
        super("player", map, window, new Inventory(36));
        this.data = data;
        this.camera = camera;

        hitbox = new AABB(-0.3f,0,-0.3f,0.3f,1.8f,0.3f);
    }
    
    public Player(PlayerData data, Dimension map, Window window) {
        super("player", map, window, new Inventory(36));
        this.data = data;
        
        hitbox = new AABB(-0.3f,0,-0.3f,0.3f,1.8f,0.3f);
    }
    
    public void setInputHandler(InputManager input) {
        this.input = input;
    }

    public Player(Dimension map, Window window, @Nullable Camera camera) {
        super("player", map, window, new Inventory(36));
        this.camera = camera;

        this.hitbox = new AABB(-0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f);
    }

    private Player() {
        super("player", null, null, new Inventory(36), new AABB(-0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f));
    }
    
    @Override
    public void update(float deltaTime, GMap map) {
        if (input == null || camera == null) {
            super.update(deltaTime, map);
            return;
        }
        
        
        // --- 1. Calculate Wish Direction (Input Force) ---
        Vector3f forward = camera.getDirection();
        forward.y = 0;
        forward.normalize();
        
        Vector3f right = new Vector3f(forward).cross(new Vector3f(0, 1, 0)).normalize();
        
        // 💡 CRITICAL FIX: Create the pure input vector (Velocity Input)
        Vector3f velocityInput = new Vector3f(0,0,0);
        
        if (input.isKeyPressed(GLFW.GLFW_KEY_W)) velocityInput.add(forward);
        if (input.isKeyPressed(GLFW.GLFW_KEY_S)) velocityInput.sub(forward);
        if (input.isKeyPressed(GLFW.GLFW_KEY_D)) velocityInput.add(right);
        if (input.isKeyPressed(GLFW.GLFW_KEY_A)) velocityInput.sub(right);
        
        // Normalize and scale horizontal input
        if (velocityInput.lengthSquared() > 0) {
            velocityInput.normalize().mul(movementSpeed);
        }
        
        // 2. Handle Jump (Vertical Input)
        if (input.isKeyPressed(GLFW.GLFW_KEY_SPACE) && isGrounded()) {
            velocityInput.y = jumpStrength; // Only send jump intent
        }
        
        // --- 3. Apply Input to Local Velocity for Client-Side Prediction ---
        
        // Horizontal: Overwrite the local velocity's horizontal component with input (overrides friction)
        velocity.x = velocityInput.x;
        velocity.z = velocityInput.z;
        
        // Vertical: Only apply jump velocity if it was triggered. Gravity is handled in super.update.
        if (velocityInput.y > 0) {
            velocity.y = velocityInput.y;
        }
        
        // 4. Run base Entity physics (gravity, collision, friction/damping, and final move)
        super.update(deltaTime, map);
        
        // --- 5. Synchronize with Server (Send PURE INPUT VECTOR) ---
        // Send the pure, clean input vector to the server. The server will use this
        // to calculate its own authoritative position using its physics loop.
        bus.post(new VelocityChangedEvent(velocityInput.x, velocityInput.y, velocityInput.z));
    }
    
    @Override
    protected synchronized void moveAxis(float dx, float dy, float dz, GMap map) {
        super.moveAxis(dx, dy, dz, map);
        if (camera != null) camera.setPosition(position.x,position.y + 1.8f,position.z);
    }
    
    public void adjust() {
        for (int y = 0; y < 128; y++) {
            if (dimension.getWorld().getBlock((int) position.x, y, (int) position.z) == null) {
                position.y = y+1;
                break;
            }
        }
    }
    
    // Player.java - New method for smooth camera follow (or put this in your game loop)
    public void updateCamera(float alpha) {
        if (camera == null) return;
        
        // Use a smoothed position instead of the instant position.
        // The 'position' field is the authoritative position (either local or server-corrected).
        
        // To smooth movement, you'd typically use the previous position (prevPosition)
        // and the current position (position) and interpolate by a factor 'alpha' (0.0 to 1.0).
        // Lerp: A = start position, B = end position, alpha = interpolation factor.
        float targetX = lerp(getPrevPosition().x, position.x, alpha);
        float targetY = lerp(getPrevPosition().y, position.y, alpha);
        float targetZ = lerp(getPrevPosition().z, position.z, alpha);
        
        // Camera is always at player's eye level (position.y + 1.8f)
        camera.setPosition(targetX, targetY + hitbox.maxY, targetZ);
    }

    @Override
    public Entity newInstance() {
        return new Player();
    }
    
    public EventBus getEventBus() {
        return bus;
    }
    
    public void movePosition(float x, float y, float z) {
        position.add(x,y,z);
    }
    
    public PlayerData getData() {
        return data;
    }
}

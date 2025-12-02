package org.infinitytwo.nyctotile.core.entity;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.world.AABB;
import org.infinitytwo.nyctotile.core.data.io.InputManager;
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
    private final EventBus bus = new EventBus();
    private static final float EPSILON = 0.01f;
    
    public Player(PlayerData data, Dimension map, @Nullable Camera camera, Window window) {
        super("player", map, window, new Inventory(36));
        this.data = data;
        this.camera = camera;
        
        hitbox = new AABB(-0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f);
    }
    
    public Player(PlayerData data, Dimension map, Window window) {
        super("player", map, window, new Inventory(36));
        this.data = data;
        
        hitbox = new AABB(-0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f);
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
        Vector3f velocityInput = new Vector3f(0, 0, 0);
        
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
        
        if (Math.abs(position.x - prevPosition.x) > EPSILON ||
                Math.abs(position.y - prevPosition.y) > EPSILON ||
                Math.abs(position.z - prevPosition.z) > EPSILON)
        {
//          setPrevPosition();
        }
        bus.post(new VelocityChangedEvent(velocityInput.x, velocityInput.y, velocityInput.z));
    }
    
    @Override
    protected synchronized void moveAxis(float dx, float dy, float dz, GMap map) {
        super.moveAxis(dx, dy, dz, map);
    }
    
    public void adjust() {
        for (int y = 0; y < 128; y++) {
            if (dimension.getWorld().getBlock((int) position.x, y, (int) position.z) == null) {
                position.y = y + 1;
                break;
            }
        }
    }
    
    // Assuming this code is in your Player class
    
    public void updateCamera(float delta) {
        if (camera == null) return;
        
        // --- Camera Smoothing Constants ---
        // The LERP factor controls the speed of the follow.
        final float SMOOTH_FACTOR = 0.1f;
        
        // A small squared distance threshold (e.g., 0.0001f is 0.01 units)
        // If the player's position is closer than this to the camera's position,
        // we stop the camera movement.
        final float CAMERA_DEAD_ZONE_SQ = 0.0001f; // (0.01 * 0.01)
        
        // Calculate the target position (Player's current position at eye level)
        float playerEyeY = position.y + hitbox.maxY;
        
        // --- 1. Determine Target Vector ---
        Vector3f targetPos = new Vector3f(position.x, playerEyeY, position.z);
        Vector3f currentCamPos = camera.getPosition();
        
        // --- 2. Check Dead Zone ---
        // Only check the horizontal distance for the dead zone, as vertical jitter
        // is the main issue when standing.
        float distSqX = (targetPos.x - currentCamPos.x) * (targetPos.x - currentCamPos.x);
        float distSqZ = (targetPos.z - currentCamPos.z) * (targetPos.z - currentCamPos.z);
        
        // Check if the camera is already close enough to the player horizontally
        if (distSqX + distSqZ < CAMERA_DEAD_ZONE_SQ) {
            // If within the dead zone, keep the current horizontal position.
            // The camera should still smoothly follow the player's vertical movement (Y-axis)
            // because vertical jitter is handled by the low SMOOTH_FACTOR (LERP).
            
            float newCamY = lerp(currentCamPos.y, playerEyeY, SMOOTH_FACTOR);
            camera.setPosition(currentCamPos.x, newCamY, currentCamPos.z);
            return; // Skip LERP on X and Z
        }
        
        // --- 3. Apply Smooth LERP (If outside the dead zone) ---
        float newCamX = lerp(currentCamPos.x, targetPos.x, SMOOTH_FACTOR);
        float newCamY = lerp(currentCamPos.y, playerEyeY, SMOOTH_FACTOR);
        float newCamZ = lerp(currentCamPos.z, targetPos.z, SMOOTH_FACTOR);
        
        camera.setPosition(newCamX, newCamY, newCamZ);
    }
    
    @Override
    public Entity newInstance() {
        return new Player();
    }
    
    public EventBus getEventBus() {
        return bus;
    }
    
    public void movePosition(float x, float y, float z) {
        position.add(x, y, z);
    }
    
    public PlayerData getData() {
        return data;
    }
}

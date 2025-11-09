package org.infinitytwo.umbralore.core.entity;

import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.AABB;
import org.infinitytwo.umbralore.core.data.Inventory;
import org.infinitytwo.umbralore.core.data.PlayerData;
import org.infinitytwo.umbralore.core.debug.Main;
import org.infinitytwo.umbralore.core.renderer.Camera;
import org.infinitytwo.umbralore.core.renderer.Outline;
import org.infinitytwo.umbralore.core.renderer.ShaderProgram;
import org.infinitytwo.umbralore.core.world.GridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static org.joml.Math.lerp;

public class Player extends Entity {
    protected PlayerData data = PlayerData.shell("Dev");
    protected Camera camera;
    private boolean e;
    private final Vector3f color = new Vector3f(1,1,1);
    
    public Player(PlayerData data, Dimension map, Camera camera, Window window) {
        super("player", map, window, new Inventory(36));
        this.data = data;
        this.camera = camera;

        hitbox = new AABB(-0.3f,0,-0.3f,0.3f,1.8f,0.3f);
        camera.setPosition(position.x, position.y, position.z);
    }

    public Player(Dimension map, Window window, Camera camera) {
        super("player", map, window, new Inventory(36));
        this.camera = camera;

        this.hitbox = new AABB(-0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f);
        camera.setPosition(position.x, position.y, position.z);
    }

    private Player() {
        // This empty constructor is for the registry to create a blank shell
        super("player", null, null, new Inventory(36), new AABB(-0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f));
        this.camera = new Camera(); // Create a minimal camera shell
        // PlayerData is left as 'data' from the field initialization
    }

    @Override
    public void update(float deltaTime) {
        // 1. Handle player input and set desired horizontal velocity
        Vector3f forward = camera.getDirection();
        forward.y = 0;
        forward.normalize();

        Vector3f right = new Vector3f(forward).cross(new Vector3f(0, 1, 0)).normalize();
        Vector3f wishDir = new Vector3f();

        if (Main.isKeyPressed(GLFW.GLFW_KEY_W)) wishDir.add(forward);
        if (Main.isKeyPressed(GLFW.GLFW_KEY_S)) wishDir.sub(forward);
        if (Main.isKeyPressed(GLFW.GLFW_KEY_D)) wishDir.add(right);
        if (Main.isKeyPressed(GLFW.GLFW_KEY_A)) wishDir.sub(right);

        // If there is movement input, normalize the direction and apply speed.
        if (wishDir.lengthSquared() > 0) {
            wishDir.normalize().mul(movementSpeed);

            // Directly set the horizontal velocity components for controlled movement
            velocity.x = wishDir.x;
            velocity.z = wishDir.z;
        }
        // If no input, the base Entity.update() friction logic will handle damping.

        // 2. Handle Jump (Vertical Velocity)
        if (Main.isKeyPressed(GLFW.GLFW_KEY_SPACE) && isGrounded()) {
            velocity.y = jumpStrength;
        }

        // 3. Run base Entity physics (gravity, collision, friction/damping, and final move)
        super.update(deltaTime);
    }

    public void adjust() {
        for (int y = 0; y < 128; y++) {
            if (dimension.getWorld().getBlock((int) position.x, y, (int) position.z) == null) {
                position.y = y+1;
                break;
            }
        }
    }

    @Override
    public Entity newInstance() {
        return new Player();
    }

    @Override
    protected void moveAxis(float dx, float dy, float dz) {
        super.moveAxis(dx, dy, dz);
        camera.setPosition(position.x,position.y + 1.67f,position.z);
    }

    public void changeCam() {
        e = !e;
    }
}

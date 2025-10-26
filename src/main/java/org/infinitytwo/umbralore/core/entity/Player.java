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
    private final Outline outline = new Outline(new ShaderProgram(
            """
                    #version 330 core
                    layout (location = 0) in vec3 position;
                    uniform mat4 model, view, projection;
                    void main() {
                        gl_Position = projection * view * model * vec4(position, 1.0);
                    }
                    """,
            """
                    #version 330 core
                    out vec4 FragColor;
                    uniform vec3 outlineColor;
                    void main() {
                        FragColor = vec4(outlineColor, 1.0);
                    }
                    """
    ));

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

    public void handleInput(float delta) {
        Vector3f forward = camera.getDirection();
        forward.y = 0; // ignore pitch for walking
        forward.normalize();

        Vector3f right = new Vector3f(forward).cross(new Vector3f(0, 1, 0)).normalize();

        Vector3f move = new Vector3f();

        if (Main.isKeyPressed(GLFW.GLFW_KEY_W)) move.add(forward);
        if (Main.isKeyPressed(GLFW.GLFW_KEY_S)) move.sub(forward);
        if (Main.isKeyPressed(GLFW.GLFW_KEY_D)) move.add(right);
        if (Main.isKeyPressed(GLFW.GLFW_KEY_A)) move.sub(right);

        // Apply movement
//        if (move.lengthSquared() > 0) {
//            move.normalize().mul(movementSpeed);
//            velocity.x = move.x;
//            velocity.z = move.z;
//        } else {
//            velocity.x = lerp(velocity.x, 0, delta * 8f);
//            velocity.z = lerp(velocity.z, 0, delta * 8f);
//        }

        velocity.add(move);

        // Jump
        if (Main.isKeyPressed(GLFW.GLFW_KEY_SPACE) && isGrounded()) {
            velocity.y = jumpStrength;
        }
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
    protected void moveAxis(float dx, float dy, float dz) {
        super.moveAxis(dx, dy, dz);
        camera.setPosition(position.x,position.y + 1.67f,position.z);
    }

    public void draw() {
        outline.renderAABB(hitbox.offset(position),camera,window,color);
    }

    public void changeCam() {
        e = !e;
    }
}

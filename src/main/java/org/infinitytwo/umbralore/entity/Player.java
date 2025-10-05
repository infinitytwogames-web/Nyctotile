package org.infinitytwo.umbralore.entity;

import org.infinitytwo.umbralore.Window;
import org.infinitytwo.umbralore.data.AABB;
import org.infinitytwo.umbralore.data.PlayerData;
import org.infinitytwo.umbralore.debug.Main;
import org.infinitytwo.umbralore.renderer.Camera;
import org.infinitytwo.umbralore.renderer.Outline;
import org.infinitytwo.umbralore.renderer.ShaderProgram;
import org.infinitytwo.umbralore.world.GridMap;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import static org.infinitytwo.umbralore.AdvancedMath.lerp;

public class Player extends Entity {
    protected PlayerData data;
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

    public Player(PlayerData data, GridMap map, Camera camera, Window window) {
        super("player", map, window);
        this.data = data;
        this.camera = camera;

        hitbox = new AABB(-0.3f,0,-0.3f,0.3f,1.8f,0.3f);
        camera.setPosition(position.x, position.y, position.z);
    }

    public Player(String id, GridMap map, Window window, Camera camera) {
        super(id, map, window);
        this.camera = camera;

        // Example hitbox: 0.6x1.8 blocks
        this.hitbox = new AABB(-0.3f, 0, -0.3f, 0.3f, 1.8f, 0.3f);
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
        if (move.lengthSquared() > 0) {
            move.normalize().mul(movementSpeed);
            velocity.x = move.x;
            velocity.z = move.z;
        } else {
            velocity.x = lerp(velocity.x, 0, delta * 8f);
            velocity.z = lerp(velocity.z, 0, delta * 8f);
        }

        // Jump
        if (Main.isKeyPressed(GLFW.GLFW_KEY_SPACE) && isGrounded()) {
            velocity.y = jumpStrength;
        }
    }

    public void adjust() {
        for (int y = 0; y < 128; y++) {
            if (world.getBlock((int) position.x, y, (int) position.z) == null) {
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

    @Override
    public void draw() {
        outline.renderAABB(hitbox.offset(position),camera,window,color);
    }

    public void changeCam() {
        e = !e;
    }
}

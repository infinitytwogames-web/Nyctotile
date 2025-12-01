package org.infinitytwo.nyctotile.core.renderer;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.world.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.*;

public class Outline {

    private final int vaoId;
    private final int vboId;
    private final ShaderProgram shader;

    public Outline(ShaderProgram shader) {
        this.shader = shader;

        float[] wireCube = {
                0,0,0, 1,0,0, 1,0,0, 1,0,1, 1,0,1, 0,0,1, 0,0,1, 0,0,0, // bottom
                0,1,0, 1,1,0, 1,1,0, 1,1,1, 1,1,1, 0,1,1, 0,1,1, 0,1,0, // top
                0,0,0, 0,1,0, 1,0,0, 1,1,0, 1,0,1, 1,1,1, 0,0,1, 0,1,1  // sides
        };

        vaoId = GL30.glGenVertexArrays();
        vboId = GL15.glGenBuffers();

        GL30.glBindVertexArray(vaoId);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);

        FloatBuffer buffer = memAllocFloat(wireCube.length);
        buffer.put(wireCube).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);

        memFree(buffer);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }
    
    public Outline() {
        this(new ShaderProgram(
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
    }
    
    public void render(Vector3f blockPos, Camera camera, Window window, Vector3f color) {
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
//        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(10.0f);

        shader.bind();

        Matrix4f model = new Matrix4f()
                .translate(blockPos)
                .scale(1.001f); // Slightly bigger to avoid z-fighting

        shader.setUniformMatrix4fv("model", model);
        shader.setUniformMatrix4fv("view", camera.getViewMatrix());
        shader.setUniformMatrix4fv("projection", new Matrix4f().perspective(
                (float) Math.toRadians(camera.getFov()),
                (float) window.getWidth() / window.getHeight(),
                0.1f, 1024f));

        shader.setUniform3f("outlineColor", color);

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
        GL30.glBindVertexArray(0);

        shader.unbind();

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
//        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public void renderAABB(AABB box, Camera camera, Window window, Vector3f color) {
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        GL11.glLineWidth(2.0f);

        shader.bind();

        // Compute translation & scale
        Vector3f translation = new Vector3f((float) box.minX, (float) box.minY, (float) box.minZ);
        Vector3f scale = new Vector3f(
                (float) (box.maxX - box.minX),
                (float) (box.maxY - box.minY),
                (float) (box.maxZ - box.minZ)
        );

        Matrix4f model = new Matrix4f()
                .translate(translation)
                .scale(scale);

        shader.setUniformMatrix4fv("model", model);
        shader.setUniformMatrix4fv("view", camera.getViewMatrix());
        shader.setUniformMatrix4fv("projection", new Matrix4f().perspective(
                (float) Math.toRadians(camera.getFov()),
                (float) window.getWidth() / window.getHeight(),
                0.1f, 1024f));

        shader.setUniform3f("outlineColor", color);

        GL30.glBindVertexArray(vaoId);
        GL11.glDrawArrays(GL11.GL_LINES, 0, 24);
        GL30.glBindVertexArray(0);

        shader.unbind();

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
    }

    public void cleanup() {
        GL20.glDisableVertexAttribArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL15.glDeleteBuffers(vboId);
        GL30.glBindVertexArray(0);
        GL30.glDeleteVertexArrays(vaoId);
    }
}

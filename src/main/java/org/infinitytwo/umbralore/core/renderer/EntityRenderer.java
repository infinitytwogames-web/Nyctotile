package org.infinitytwo.umbralore.core.renderer;

import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.buffer.NFloatBuffer;
import org.infinitytwo.umbralore.core.entity.Entity;
import org.infinitytwo.umbralore.core.model.Model;
import org.infinitytwo.umbralore.core.model.TextureAtlas;
import org.infinitytwo.umbralore.core.registry.ModelRegistry;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.FloatBuffer;
import java.util.*;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawArraysInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;

public class EntityRenderer {
    private final int vao;
    private final int vbo;
    private final int instanceVbo;
    private final ShaderProgram program;
    private final Map<Integer, ArrayList<Entity>> entities = new HashMap<>();
    private final NFloatBuffer nBuffer = new NFloatBuffer();
    private final TextureAtlas atlas;

    private static final int MAX_INSTANCE = 1000;

    public EntityRenderer(TextureAtlas atlas) {
        this.atlas = atlas;
        program = new ShaderProgram("""
                #version 330 core

                // LAYOUTS
                layout (location = 0) in vec3 vPosition;
                layout (location = 1) in vec2 vTexCoords;
                layout (location = 2) in float vBrightness;
                layout (location = 3) in vec3 iPosition;

                // UNIFORMS
                uniform mat4 modelMat;
                uniform mat4 projectionMat;
                uniform mat4 viewMat;

                // OUTPUTS
                out vec2 texCoords;
                out float brightness;

                void main() {
                    texCoords = vTexCoords;
                    brightness = vBrightness;

                    vec4 worldPos = vec4(vPosition + iPosition, 1.0);

                    gl_Position = projectionMat * viewMat * modelMat * worldPos;
                }
                """, """
                #version 330 core

                // INPUT
                in vec2 texCoords;
                in float brightness;

                // UNIFORM
                uniform sampler2D uTexture;

                // OUTPUT
                out vec4 FragColor;

                void main() {
                    // FragColor = texture(uTexture, texCoords) * brightness;
                    FragColor = vec4(1.0,0.0,0.0,1.0);
                }
                """);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        glVertexAttribPointer(0, 3, GL_FLOAT, false, Float.BYTES * 6, 0L);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, Float.BYTES * 6, Float.BYTES * 3);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, Float.BYTES * 6, Float.BYTES * 5);

        glEnableVertexAttribArray(0);
        glEnableVertexAttribArray(1);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);

        instanceVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        glBufferData(GL_ARRAY_BUFFER, MAX_INSTANCE * Float.BYTES * 3, GL_DYNAMIC_DRAW);

        glVertexAttribPointer(3, 3, GL_FLOAT, false, Float.BYTES * 3, 0L);
        glEnableVertexAttribArray(3);
        glVertexAttribDivisor(3, 1);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void insert(Entity entity) {
        entities.computeIfAbsent(entity.getModelIndex(), k -> new ArrayList<>()).add(entity);
    }

    public void draw(Camera camera, Window window) {
        program.bind();
        atlas.getTexture().bind();

        glBindVertexArray(vao);
        glEnable(GL_DEPTH_TEST);

        program.setUniformMatrix4fv("projectionMat",new Matrix4f().perspective(
                (float) camera.getPov(),
                (float) window.getWidth() / window.getHeight(),
                1f, 1024f)
        );
        program.setUniformMatrix4fv("viewMat",camera.getViewMatrix());
        program.setUniformMatrix4fv("modelMat", new Matrix4f().identity()); // Model is now Identity
        program.setUniform1i("uTexture", 0); // Set the texture uniform

        for (int modelIndex : entities.keySet()) {
            List<Entity> entityList = entities.get(modelIndex);
            int instanceCount = entityList.size();

            if (instanceCount == 0) continue;

            Model model = ModelRegistry.get(modelIndex);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, model.toArray(), GL_STREAM_DRAW);
            // 2a. Update Instance VBO with current positions
            // You'll need a FloatBuffer to hold all (x, y, z) positions
            FloatBuffer instanceData = createPositionBuffer(entityList);

            glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0L, instanceData);


            glDrawArraysInstanced(
                    GL_TRIANGLES,
                    0,
                    3,
                    instanceCount
            );
        }

        // 3. Cleanup
        glBindVertexArray(0);
        atlas.getTexture().unbind();
        program.unbind();
    }

    private FloatBuffer createPositionBuffer(List<Entity> entityList) {
        nBuffer.reset();
        nBuffer.require(entityList.size() * 3);

        for (Entity entity : entityList) {
            Vector3f pos = entity.getPosition();

            nBuffer.put(pos.x); nBuffer.put(pos.y); nBuffer.put(pos.z);
        }
        return nBuffer.getBuffer();
    }

    public void cleanup() {
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        program.cleanup();
        nBuffer.cleanup();
    }
}

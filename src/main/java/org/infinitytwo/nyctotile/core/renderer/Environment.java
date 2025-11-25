package org.infinitytwo.nyctotile.core.renderer;

import org.infinitytwo.nyctotile.core.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Environment {
    private ShaderProgram shader;
    private int vao, vbo;
    private int vertexCount;

    private final Vector3f dayColor = new Vector3f(0.53f, 0.81f, 0.92f);   // Light blue
    private final Vector3f nightColor = new Vector3f(0.02f, 0.02f, 0.05f); // Almost black

    public Environment() {
        shader = new ShaderProgram(
                """
                #version 330 core
                layout (location = 0) in vec3 aPos;
                out vec3 fragWorldDir;
    
                uniform mat4 projection;
                uniform mat4 view;
    
                void main() {
                    fragWorldDir = aPos;
                    gl_Position = projection * view * vec4(aPos, 1.0);
                }
                """,
                """
#version 330 core
in vec3 fragWorldDir;

uniform vec3 sunDir;
uniform vec3 dayColor;
uniform vec3 nightColor;

out vec4 FragColor;

void main() {
    vec3 dir = normalize(fragWorldDir);
    vec3 sunDirection = normalize(sunDir);

    // Sky gradient blend
    float sunDot = dot(dir, sunDirection); // -1 to 1
    float t = clamp(sunDot * 0.5 + 0.5, 0.0, 1.0); // [0, 1]
    float curve = smoothstep(0.0, 1.0, t);
    vec3 skyColor = mix(nightColor, dayColor, curve);

    // Horizon glow logic
    float dirY = dir.y;
    float horizonAmount = 1.0 - abs(dirY); // strongest at horizon
    float sunAltitude = sunDirection.y;
    float sunsetAmount = 1.0 - abs(sunAltitude);
    float horizonGlow = smoothstep(0.05, 1.45, horizonAmount) * smoothstep(0.0, 0.3, sunsetAmount);
    vec3 horizonColor = vec3(1.0, 0.5, 0.2);
    skyColor = mix(skyColor, horizonColor, horizonGlow);

    // SUN RENDERING
    float sunSize = 0.05; // Size of the sun disk in radians
    float sunSharpness = 128.0; // Higher = crisper edge
    float sunIntensity = pow(max(dot(dir, sunDirection), 0.0), sunSharpness);
    vec3 sunColor = vec3(1.0, 1.0, 0.85); // pale yellowish sun

    vec3 finalColor = mix(skyColor, sunColor, sunIntensity);

    FragColor = vec4(finalColor, 1.0);
    gl_FragDepth = 1.0;
}

                """
        );

        setupSphere(32, 16); // segments, rings

        shader.bind();
        shader.setUniform3f("dayColor", dayColor);
        shader.setUniform3f("nightColor", nightColor);
        shader.unbind();
    }

    private void setupSphere(int segments, int rings) {
        List<Float> verts = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        for (int y = 0; y <= rings; y++) {
            float v = y / (float) rings;
            float phi = v * (float)Math.PI; // full vertical sweep from 0 to PI

            for (int x = 0; x <= segments; x++) {
                float u = x / (float) segments;
                float theta = u * (float)Math.PI * 2.0f;

                float xPos = (float)(Math.cos(theta) * Math.sin(phi));
                float yPos = (float)(Math.cos(phi)); // y should be up/down axis
                float zPos = (float)(Math.sin(theta) * Math.sin(phi));

                verts.add(xPos);
                verts.add(yPos);
                verts.add(zPos);
            }
        }

        for (int y = 0; y < rings; y++) {
            for (int x = 0; x < segments; x++) {
                int i0 = y * (segments + 1) + x;
                int i1 = i0 + segments + 1;

                indices.add(i0);
                indices.add(i1);
                indices.add(i0 + 1);

                indices.add(i1);
                indices.add(i1 + 1);
                indices.add(i0 + 1);
            }
        }

        vertexCount = indices.size();

        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(verts.size());
        verts.forEach(vertexBuffer::put);
        vertexBuffer.flip();

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);

        int[] indexArray = indices.stream().mapToInt(i -> i).toArray();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexArray, GL_STATIC_DRAW);

        glBindVertexArray(0);
    }

    public void render(Camera camera, Window window) {
        shader.bind();

        // Full cycle (day/night) now takes 5 minutes (300,000 milliseconds)
        long cycleDurationMillis = 720000L;
        float timeOfDay = (System.currentTimeMillis() % cycleDurationMillis) / (float)cycleDurationMillis;
        float angle = timeOfDay * 2.0f * (float)Math.PI; // Full 360°
        Vector3f sunDir = new Vector3f((float)Math.sin(angle), (float)Math.cos(angle), 0f).normalize();
        shader.setUniform3f("sunDir", sunDir);

        Matrix4f projection = new Matrix4f().perspective(
                (float)Math.toRadians(camera.getFov()),
                window.getWidth() / (float)window.getHeight(),
                0.1f, 100.0f);
        Matrix4f view = new Matrix4f(camera.getViewMatrix());
        view.m30(0).m31(0).m32(0); // Remove camera translation

        shader.setUniformMatrix4fv("projection", projection);
        shader.setUniformMatrix4fv("view", view);

        glDisable(GL_CULL_FACE); // Disable backface culling
        glDepthFunc(GL_LEQUAL);
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        glDepthFunc(GL_LESS);
        glEnable(GL_CULL_FACE);

        shader.unbind();
    }

    public void cleanup() {
        shader.cleanup();
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
    }
}

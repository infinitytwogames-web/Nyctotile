package org.infinitytwo.nyctotile.core.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL31.GL_INVALID_INDEX;

public class ShaderProgram {
    private final int programId;
    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertexShader = compileShader(vertexSource, GL_VERTEX_SHADER);
        int fragmentShader = compileShader(fragmentSource, GL_FRAGMENT_SHADER);

        programId = glCreateProgram();
        glAttachShader(programId, vertexShader);
        glAttachShader(programId, fragmentShader);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader linking failed: " + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
    }

    private int compileShader(String source, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Shader compile failed: " + glGetShaderInfoLog(shader));
        }

        return shader;
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void setUniform1i(String name, int value) {
        glUniform1i(glGetUniformLocation(programId, name), value);
    }

    public void setUniform4f(String name, float r, float g, float b, float a) {
        glUniform4f(glGetUniformLocation(programId, name), r, g, b, a);
    }

    public int getProgramId() {
        return programId;
    }

    public void cleanup() {
        glDeleteProgram(programId);
    }

    public static String load(String path) throws IOException {
        Path path2 = Paths.get(path);
        return new String(Files.readAllBytes(path2));
    }

    /**
     * Sets a mat4 uniform in the shader program.
     * @param name The name of the uniform in the shader (e.g., "uProjection").
     * @param matrix The JOML Matrix4f object to set.
     */
    public void setUniformMatrix4fv(String name, Matrix4f matrix) {
        int location = glGetUniformLocation(programId, name);
        if (location != GL_INVALID_INDEX) { // Check if uniform exists
            // Get the matrix data into the FloatBuffer
            matrix.get(matrixBuffer);
            // Upload the matrix to the uniform location
            glUniformMatrix4fv(location, false, matrixBuffer);
        } else {
            System.err.println("Warning: Uniform '" + name + "' not found in shader program " + programId);
        }
    }

    public void setUniform3f(String name, Vector3f value) {
        int location = glGetUniformLocation(programId, name);
        if (location != GL_INVALID_INDEX) {
            glUniform3f(location, value.x, value.y, value.z);
        }
    }
    
    public int getUniformLocation(String name) {
        return glGetUniformLocation(programId,name);
    }
}

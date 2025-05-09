package dev.merosssany.calculatorapp.core;

import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL11;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Shader {

    private int programId;
    private boolean isCompiled;
    private final Map<String, Integer> uniformLocations = new HashMap<>();
    private final Logger logger = Logger.getLogger(Shader.class.getName());

    /**
     * Constructor: Takes the paths to the vertex and fragment shader files.
     *
     * @param vertexShaderPath   The path to the vertex shader file.
     * @param fragmentShaderPath The path to the fragment shader file.
     */
    public Shader(String vertexShaderPath, String fragmentShaderPath) {
        try {
            // Load shader source code
            String vertexSource = loadShaderSource(vertexShaderPath);
            String fragmentSource = loadShaderSource(fragmentShaderPath);

            // Compile shaders
            int vertexShaderId = compileShader(GL20.GL_VERTEX_SHADER, vertexSource);
            int fragmentShaderId = compileShader(GL20.GL_FRAGMENT_SHADER, fragmentSource);

            // Link program
            linkProgram(vertexShaderId, fragmentShaderId);

            // Delete shaders after linking
            GL20.glDeleteShader(vertexShaderId);
            GL20.glDeleteShader(fragmentShaderId);

        } catch (IOException e) {
            // Log the error and re-throw as a RuntimeException
            logger.severe("Error creating shader: " + e.getMessage());
            throw new RuntimeException("Failed to create shader", e); // Wrap for a more general exception
        }
    }

    /**
     * Loads the shader source code from a file.
     *
     * @param path The path to the shader file.
     * @return The shader source code as a String.
     * @throws IOException If an error occurs while reading the file.
     */
    private String loadShaderSource(String path) throws IOException {
        StringBuilder source = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = reader.readLine()) != null) {
                source.append(line).append("\n");
            }
        }
        return source.toString();
    }

    /**
     * Compiles a single shader (vertex or fragment).
     *
     * @param type   The type of shader (GL20.GL_VERTEX_SHADER or GL20.GL_FRAGMENT_SHADER).
     * @param source The shader source code.
     * @return The ID of the compiled shader.
     */
    private int compileShader(int type, String source) {
        int shaderId = GL20.glCreateShader(type);
        GL20.glShaderSource(shaderId, source);
        GL20.glCompileShader(shaderId);

        int status = GL20.glGetShaderi(shaderId, GL20.GL_COMPILE_STATUS);
        if (status == GL11.GL_FALSE) {
            String errorLog = GL20.glGetShaderInfoLog(shaderId);
            GL20.glDeleteShader(shaderId); // Delete on failure
            throw new RuntimeException("Shader compilation failed: " + errorLog + "\nSource:\n" + source);
        }
        return shaderId;
    }

    /**
     * Links the compiled shaders into a program.
     */
    private void linkProgram(int vertexShaderId, int fragmentShaderId) {
        programId = GL20.glCreateProgram();
        GL20.glAttachShader(programId, vertexShaderId);
        GL20.glAttachShader(programId, fragmentShaderId);
        GL20.glLinkProgram(programId);

        int status = GL20.glGetProgrami(programId, GL20.GL_LINK_STATUS);
        if (status == GL11.GL_FALSE) {
            String errorLog = GL20.glGetProgramInfoLog(programId);
            GL20.glDeleteProgram(programId); // Delete the program
            throw new RuntimeException("Shader linking failed: " + errorLog);
        }
        isCompiled = true;
    }

    /**
     * Gets the location of a uniform variable.
     *
     * @param name The name of the uniform variable.
     * @return The location of the uniform variable, or -1 if not found.
     */
    public int getUniformLocation(String name) {
        if (uniformLocations.containsKey(name)) {
            return uniformLocations.get(name);
        }
        int location = GL20.glGetUniformLocation(programId, name);
        if (location == -1) {
            logger.warning("Uniform variable '" + name + "' not found in shader program.");
        } else {
            uniformLocations.put(name, location); // Cache the location
        }
        return location;
    }

    /**
     * Sets the value of a float uniform variable.
     *
     * @param name  The name of the uniform variable.
     * @param value The value to set.
     */
    public void setUniform1f(String name, float value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1f(location, value);
        }
    }

    /**
     * Sets the value of an integer uniform variable.
     *
     * @param name  The name of the uniform variable.
     * @param value The value to set.
     */
    public void setUniform1i(String name, int value) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniform1i(location, value);
        }
    }

    /**
     * Sets the value of a 4x4 matrix uniform variable.
     *
     * @param name   The name of the uniform variable.
     * @param matrix The matrix data in a FloatBuffer.
     */
    public void setUniformMatrix4f(String name, FloatBuffer matrix) {
        int location = getUniformLocation(name);
        if (location != -1) {
            GL20.glUniformMatrix4fv(location, false, matrix);
        }
    }

    /**
     * Uses (activates) the shader program.
     */
    public void use() {
        if (isCompiled) {
            GL30.glUseProgram(programId);
        } else {
            logger.warning("Shader program not compiled, cannot use.");
        }
    }

    /**
     * Unuses (deactivates) the shader program.
     */
    public void unuse() {
        GL30.glUseProgram(0);
    }

    /**
     * Cleans up (deletes) the shader program.
     */
    public void cleanup() {
        if (programId != 0) {
            GL20.glDeleteProgram(programId);
            programId = 0;
        }
    }

    public int getProgramId() {
        return programId;
    }
}
package dev.merosssany.calculatorapp.core.ui;

import dev.merosssany.calculatorapp.core.RGBA;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.core.logging.Logger;
import dev.merosssany.calculatorapp.core.render.ShaderFiles;
import dev.merosssany.calculatorapp.core.render.ShaderProgram;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class UI {
    private final String name;
    private UIVector2Df position;
    private float width, height;
    private RGBA background;
    private final Vector2D<Float> end;
    private final Logger logger = new Logger("UI Handler");

    // GL handles
    private int vaoId, vboPos, ebo;
    private boolean initialized = false;
    private int shaderProgramId; // Stores the ID of the compiled and linked shader program

    // Uniform locations
    private int locProj, locPos, locSize, locColor;

    public UI(String name, UIVector2Df pos, float w, float h, RGBA bg) {
        this.name = name;
        this.position = pos;
        this.width = w;
        this.height = h;
        this.background = bg;
        this.end = new Vector2D<>(pos.getX() + w, pos.getY() - h); // Y-axis direction might matter here for 'end'

        // 1. Create the ShaderProgram instance
        ShaderProgram uiShaderProgram = new ShaderProgram(
                ShaderFiles.uiVertex,
                ShaderFiles.uiFragment
        );

        // 2. Get the actual OpenGL program ID from the ShaderProgram object
        //    Assuming your ShaderProgram class has a method like getProgramID() or getID()
        //    that returns the integer ID after successful compilation and linking.
        this.shaderProgramId = uiShaderProgram.getProgramId(); // CRITICAL FIX

        // 3. Now that this.shaderProgramId is correctly set, get the uniform locations
        //    It's good practice to check if these are -1 (not found)
        locProj  = glGetUniformLocation(this.shaderProgramId, "uProj");
        locPos   = glGetUniformLocation(this.shaderProgramId, "uPosition");
        locSize  = glGetUniformLocation(this.shaderProgramId, "uSize");
        locColor = glGetUniformLocation(this.shaderProgramId, "uColor");

        logger.info(name + " created at " + pos + " size=" + w + "×" + h + " bg=" + bg);
        logger.info(name + " shader ID: " + this.shaderProgramId + ". Uniforms (proj,pos,size,color): (" +
                locProj + "," + locPos + "," + locSize + "," + locColor + ")");

        // Check if any uniform location is -1, which means it wasn't found
        if (locProj == -1 || locPos == -1 || locSize == -1 || locColor == -1) {
            logger.error(name + ": One or more uniform locations were not found! " +
                    "uProj: " + locProj + ", uPosition: " + locPos +
                    ", uSize: " + locSize + ", uColor: " + locColor +
                    ". Check shader uniform names and compilation status.");
        }
    }

    // This method is fine if you intend to allow changing shader programs dynamically.
    // However, the initial setup in the constructor should directly use the newly created program's ID.
    public void setShaderProgramAndFetchUniforms(int programId) {
        this.shaderProgramId = programId;
        locProj  = glGetUniformLocation(programId, "uProj");
        locPos   = glGetUniformLocation(programId, "uPosition");
        locSize  = glGetUniformLocation(programId, "uSize");
        locColor = glGetUniformLocation(programId, "uColor");

        logger.info(name + " shader MANUALLY SET to ID: " + programId + ", uniforms(proj,pos,size,color)=(" +
                locProj + "," + locPos + "," + locSize + "," + locColor + ")");
    }


    public void init() {
        if (initialized) return;

        float[] vertices = {
                // x,  y
                0f, 0f, // Top-left
                1f, 0f, // Top-right
                1f, 1f, // Bottom-right
                0f, 1f  // Bottom-left
        };
        // Note: OpenGL's default normalized device coordinates (NDC) have Y increasing upwards.
        // If your UI coordinates have Y increasing downwards, you might need to adjust
        // transformations in the shader or how you calculate Y positions.
        // For a simple quad from (0,0) to (1,1) local space, this is fine.
        // The vertex shader will then position and scale it.

        int[] indices = {
                0, 1, 2, // First triangle
                2, 3, 0  // Second triangle
        };

        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboPos = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboPos);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer vb = stack.mallocFloat(vertices.length);
            vb.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);
        }
        // Attribute location 0, 2 components (vec2), GL_FLOAT, not normalized,
        // stride = 2 floats, offset = 0
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0); // Enable vertex attribute location 0

        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ib = stack.mallocInt(indices.length);
            ib.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0); // Unbind VBO
        glBindVertexArray(0);             // Unbind VAO

        initialized = true;
        logger.info(name + " GL initialized (VAO=" + vaoId + ", VBO=" + vboPos + ", EBO=" + ebo + ")");
    }

    public void draw(Matrix4f projMatrix) {
        if (!initialized) init();
        if (this.shaderProgramId == 0 || locColor == -1) { // Basic check
            logger.error(name + ": Cannot draw, shader not properly initialized or uColor uniform not found.");
            return;
        }

        glUseProgram(shaderProgramId);

        // Set projection matrix uniform
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16); // Matrix4f has 16 elements
            projMatrix.get(fb);
            glUniformMatrix4fv(locProj, false, fb);
        }

        // OpenGL states for 2D UI rendering
        glDisable(GL_CULL_FACE); // Don't cull 2D UI
        glDisable(GL_DEPTH_TEST);
//        glEnable(GL_BLEND);
        glDisable(GL_BLEND);
        glDisable(GL_SCISSOR_TEST);

//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);


        // Set UI element's specific uniforms
        glUniform2f(locPos, position.getX(), position.getY());
        glUniform2f(locSize, width, height);

        // Set color uniform - ENSURE RGBA values are 0.0f to 1.0f
        glUniform4f(locColor,
                background.getRed(),   // e.g., 0.0f for blue
                background.getGreen(), // e.g., 0.0f for blue
                background.getBlue(),  // e.g., 1.0f for blue
                background.getAlpha()  // e.g., 1.0f for opaque
        );
        // For debugging, you can temporarily hardcode:
         glUniform4f(locColor, 0.0f, 0.0f, 1.0f, 1.0f); // Force blue

        // Bind VAO and draw
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0); // 6 indices for 2 triangles
        glBindVertexArray(0);

        glUseProgram(0); // Unbind shader program
    }

    public void cleanup() {
        if (!initialized) return;
        glDeleteBuffers(vboPos);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vaoId);
        // If ShaderProgram objects are managed and need explicit deletion, do it here or elsewhere.
        // glDeleteProgram(shaderProgramId); // Only if this UI owns the shader exclusively.
        initialized = false;
        logger.info(name + " cleaned up");
    }

    // Getter and Setter methods (ensure consistency)
    public Vector2D<Float> getEnd() { return end; }
    public String getName() { return name; }
    public UIVector2Df getPosition() { return position; }
    public void setPosition(UIVector2Df pos) {
        this.position = pos;
        end.setX(pos.getX() + width);
        end.setY(pos.getY() - height); // Adjust based on your Y-axis convention
        logger.info(name + " position updated to " + pos + ", end=" + end);
    }

    public float getWidth() { return width; }
    public void setWidth(float w) {
        this.width = w;
        end.setX(position.getX() + w);
    }

    public float getHeight() { return height; }
    public void setHeight(float h) {
        this.height = h;
        end.setY(position.getY() - height); // Adjust based on your Y-axis convention
    }

    public RGBA getBackgroundColor() { return background; }
    public void setBackgroundColor(RGBA bg) { this.background = bg; }
    // getBackground and setBackground are duplicates of getBackgroundColor and setBackgroundColor
    // public RGBA getBackground() { return background; }
    // public void setBackground(RGBA bg) { this.background = bg; }

    /**
     * Sets the background color.
     * IMPORTANT: Ensure your RGBA class constructor matches the order of parameters r, g, b, a.
     * If constructor is RGBA(r, g, b, a), then:
     * setBackgroundColor(0.0f, 0.0f, 1.0f, 1.0f) would be blue.
     * The method signature here is (r, b, g, a) which could be confusing.
     * Consider renaming parameters to match RGBA constructor or changing parameter order.
     */
    public void setBackgroundColor(float r, float g, float b, float a) { // Changed b and g order to be conventional
        this.background = new RGBA(r, g, b, a);
        logger.info(name + " background color set to: " + this.background.toString());
    }
}


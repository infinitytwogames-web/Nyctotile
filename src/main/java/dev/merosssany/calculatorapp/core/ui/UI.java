package dev.merosssany.calculatorapp.core.ui;

import dev.merosssany.calculatorapp.core.RGBA;
import dev.merosssany.calculatorapp.core.position.UIVector2Df;
import dev.merosssany.calculatorapp.core.position.Vector2D;
import dev.merosssany.calculatorapp.core.logging.Logger;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class UI {
    private UIVector2Df position;
    private final float width;
    private final float height;
    protected RGBA backgroundRGBA;
    private Vector2D<Float> end;
    private final Logger logger = new Logger("UI Handler");
    private final String name;

    private int vaoId;
    private final Map<String, Integer> vboIds = new HashMap<>(); // Store VBO IDs with names
    private boolean initialized = false;
    private int vertexCount; // Store the number of vertices to draw

    // Shader program ID (added for shader support)
    private int shaderProgramId;

    public void setBackgroundColor(RGBA color) {
        backgroundRGBA = color;
        logger.info(name, ": Background color set to ", color);
    }

    public RGBA getBackgroundColor() {
        return backgroundRGBA;
    }

    public UI(String name, UIVector2Df position, float width, float height, RGBA background) {
        this.name = name;
        this.height = height;
        this.width = width;
        this.position = position;
        this.backgroundRGBA = background;
        float topLeftX = position.getX();
        float topLeftY = position.getY();
        this.end = new Vector2D<>(topLeftX + width, topLeftY - height);
        logger.info(name, " created with position: ", position, ", width: ", width, ", height: ", height, ", background: ", background);
    }

    public Vector2D<Float> getPosition() {
        return position;
    }

    public void setPosition(UIVector2Df position) {
        this.position = position;
        float topLeftX = position.getX();
        float topLeftY = position.getY();
        this.end = new Vector2D<>(topLeftX + width, topLeftY - height);
        logger.info(name, ": Position set to ", position);
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Vector2D<Float> getEnd() {
        return end;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getName() {
        return name;
    }

    // New method to change background color using individual components
    public void changeBackgroundColor(float red, float green, float blue, float alpha) {
        this.backgroundRGBA = new RGBA(red, green, blue, alpha);
//        logger.info(name, ": Background color changed to RGBA(", red, ", ", green, ", ", blue, ", ", alpha, ")");
    }

    /**
     * Initializes the VAO and VBO. This should be called on the main OpenGL thread
     * before the UI element is drawn for the first time.
     */
    public void init() {
        if (initialized) return; // Only initialize once

        // Vertex data for a quad (two triangles)
        float[] vertices = {
                position.getX(), position.getY(),             // Top-left
                position.getX() + width, position.getY(),      // Top-right
                position.getX() + width, position.getY() - height, // Bottom-right
                position.getX(), position.getY() - height       // Bottom-left
        };
        vertexCount = 4; // 4 vertices for a quad

        // Create a FloatBuffer from the vertex data
        FloatBuffer verticesBuffer = MemoryUtil.memAllocFloat(vertices.length);
        verticesBuffer.put(vertices).flip();

        // Create VAO
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // Create VBO for positions and upload data
        createAndBindVBO("position", verticesBuffer, 2); // 2 components (x, y)

        // Clean up
        GL30.glBindVertexArray(0); // Unbind VAO after configuration
        MemoryUtil.memFree(verticesBuffer);

        initialized = true;
        logger.info(name, " initialized. VAO ID: ", vaoId, ", VBO IDs: ", vboIds);
    }

    /**
     * Creates a VBO, uploads data to it, and stores its ID with a name.
     *
     * @param bufferName The name to associate with the VBO (e.g., "position", "color").
     * @param buffer     The FloatBuffer containing the data.
     * @param size       The number of components per vertex attribute (e.g., 2 for x, y; 3 for r, g, b).
     */
    private void createAndBindVBO(String bufferName, FloatBuffer buffer, int size) {
        int vboId = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);

        // Define vertex attributes (position) - location 0 for position
        if (bufferName.equals("position")) {
            GL30.glVertexAttribPointer(0, size, GL11.GL_FLOAT, false, 0, 0);
            GL30.glEnableVertexAttribArray(0);
        } else if (bufferName.equals("color")){
            GL30.glVertexAttribPointer(1, size, GL11.GL_FLOAT, false, 0, 0);
            GL30.glEnableVertexAttribArray(1);
        }
        vboIds.put(bufferName, vboId); // Store the VBO ID with its name
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0); // Unbind after setting up the attribute
        logger.info(name, " created VBO: ", bufferName, ", ID: ", vboId, ", Size: ", size);
    }

    /**
     * Sets the shader program to be used for rendering this UI element.
     * @param shaderProgramId The ID of the compiled and linked shader program.
     */
    public void setShaderProgramId(int shaderProgramId) {
        this.shaderProgramId = shaderProgramId;
        logger.info(name, " shader program set to: ", shaderProgramId);
    }

    public void draw() {
        if (!initialized) {
            init(); // Initialize if not already initialized.
        }

        glEnable(GL11.GL_BLEND);
        glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // OpenGL states for 2D UI rendering
        glDisable(GL_CULL_FACE); // Don't cull 2D UI
        glDisable(GL_DEPTH_TEST);
//        glEnable(GL_BLEND);
        glDisable(GL_BLEND);
        glDisable(GL_SCISSOR_TEST);

        if (backgroundRGBA != null) {
            GL11.glColor4f(backgroundRGBA.getRed(), backgroundRGBA.getGreen(), backgroundRGBA.getBlue(), backgroundRGBA.getAlpha());
        } else {
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f); // Default to opaque white
        }

        // Use the shader program
        if (shaderProgramId != 0) {
            GL30.glUseProgram(shaderProgramId);
        }

        // Bind the VAO for drawing
        GL30.glBindVertexArray(vaoId);

        // Draw the quad using the data in the VBO
        GL11.glDrawArrays(GL11.GL_QUADS, 0, vertexCount);

        // Unbind VAO and shader program
        GL30.glBindVertexArray(0);
        if (shaderProgramId != 0) {
            GL30.glUseProgram(0);
        }

        glDisable(GL11.GL_BLEND);
//        logger.info(name, " drawn at position: ", position);
    }

    /**
     * Cleans up the VAO and VBO. Call this when the UI object is no longer needed.
     */
    public void cleanup() {
        if (initialized) {
            // Delete all VBOs
            for (Integer vboId : vboIds.values()) {
                GL15.glDeleteBuffers(vboId);
            }
            vboIds.clear(); // Clear the VBO ID map
            GL30.glDeleteVertexArrays(vaoId);
            initialized = false;
            logger.info(name, " cleaned up. VAO ID: ", ""+vaoId);
        }
    }

    public void setBackgroundColor(float r, float g, float b, float a) {
        this.backgroundRGBA = new RGBA(r,g,b,a);
    }
}


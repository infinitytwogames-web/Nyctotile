package dev.merosssany.calculatorapp.core.ui.font;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class FontMatrices {

    private Matrix4f modelMatrix;
    private Matrix4f viewMatrix;
    private Matrix4f projectionMatrix;

    public FontMatrices(int screenWidth, int screenHeight, boolean originAtCenter) {
        modelMatrix = new Matrix4f(); // Initialize as identity
        viewMatrix = new Matrix4f();  // Initialize as identity
        projectionMatrix = new Matrix4f();

        setupViewMatrix();
        setupProjectionMatrix(screenWidth, screenHeight, originAtCenter);
    }

    private void setupViewMatrix() {
        // In 2D, the view matrix is often just an identity matrix.
        // If you have a 2D camera, you might translate it.
        // For example, to simulate scrolling:
        // viewMatrix.translate(-cameraX, -cameraY, 0);
        viewMatrix.identity(); // Set to identity for basic 2D
    }

    private void setupProjectionMatrix(int screenWidth, int screenHeight, boolean originAtCenter) {
        // Create an orthographic projection matrix.
        // The parameters are:
        // left, right, bottom, top, near, far
        if (originAtCenter) {
            projectionMatrix.setOrtho(
                    -screenWidth / 2, screenWidth / 2,
                    -screenHeight / 2, screenHeight / 2,
                    -1, 1);
        } else {
            projectionMatrix.setOrtho(0, screenWidth, screenHeight, 0, -1, 1);
        }
        // In 2D, near and far are typically -1 and 1.
    }

    public void setCharacterPosition(float x, float y) {
        // Set the model matrix to translate (position) the character.
        modelMatrix.identity(); // Reset first
        modelMatrix.translate(x, y, 0); // Translate to the desired position
    }

    public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    public Matrix4f getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

//    public static void main(String[] args) {
//        // Example usage:
//        int screenWidth = 800;
//        int screenHeight = 600;
//        boolean originAtCenter = true; // Or false
//
//        FontMatrices fontMatrices = new FontMatrices(screenWidth, screenHeight, originAtCenter);
//
//        // Get the matrices
//        Matrix4f model = fontMatrices.getModelMatrix();
//        Matrix4f view = fontMatrices.getViewMatrix();
//        Matrix4f projection = fontMatrices.getProjectionMatrix();
//
//        // Print the matrices (for demonstration)
//        System.out.println("Model Matrix:\n" + model);
//        System.out.println("View Matrix:\n" + view);
//        System.out.println("Projection Matrix:\n" + projection);
//
//        // Set the position of a character
//        fontMatrices.setCharacterPosition(100, 200);
//        System.out.println("\nModel Matrix after setting position:\n" + fontMatrices.getModelMatrix());
//    }
}

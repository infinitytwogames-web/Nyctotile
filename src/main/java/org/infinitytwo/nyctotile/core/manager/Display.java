package org.infinitytwo.nyctotile.core.manager;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.state.WindowResizedEvent;
import org.infinitytwo.nyctotile.core.renderer.Camera;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.lwjgl.opengl.*;

import java.nio.FloatBuffer;

import static org.infinitytwo.nyctotile.core.constants.Constants.UI_DESIGN_HEIGHT;
import static org.lwjgl.opengl.GL11.*;

public final class Display {
    public static int width;
    public static int height = (int) UI_DESIGN_HEIGHT;
    public static Matrix4f projection = new Matrix4f();
    private static volatile boolean enabled = true;
    
    public static int getWidth() {
        return width;
    }
    
    public static void init() {
        EventBus.connect(Display.class);
    }
    
    public static Vector2i transformVirtualToWindow(Window window, int virtualX, int virtualY) {
        float scale = (float) window.getHeight() / Display.height;
        
        int screenX = (int) (virtualX * scale);
        int screenY = (int) (virtualY * scale);
        return new Vector2i(screenX, screenY);
    }
    
    public static Vector2i transformVirtualToWindow(Window window, Vector2i pos) {
        float scale = (float) window.getHeight() / Display.height;
        
        int screenX = (int) (pos.x * scale);
        int screenY = (int) (pos.y * scale);
        return new Vector2i(screenX, screenY);
    }
    
    public static int transformVirtualToWindow(Window window, int c) {
        float scale = (float) window.getHeight() / Display.height;
        return (int) (c * scale);
    }
    
    public static Matrix4f get3DProjectionMatrix(Camera camera, Window window) {
        return projection = new Matrix4f().perspective((float) Math.toRadians(camera.getFov()), (float) window.getWidth() / window.getHeight(), 0.1f, 1024f);
    }
    
    public static Vector2i transformWindowToVirtual(Window window, int windowX, int windowY) {
        float scale = (float) Display.height / window.getHeight();
        
        int virtualX = (int) (windowX * scale);
        int virtualY = (int) (windowY * scale);
        
        return new Vector2i(virtualX, virtualY);
    }
    
    public static int transformWindowToVirtual(Window window, int c) {
        float scale = (float) Display.height / window.getHeight();
        return (int) (c * scale);
    }
    
    public static Vector2i transformWindowToVirtual(Window window, Vector2i pos) {
        float scale = (float) Display.height / window.getHeight();
        
        int virtualX = (int) (pos.x * scale);
        int virtualY = (int) (pos.y * scale);
        
        return new Vector2i(virtualX, virtualY);
    }
    
    
    @SubscribeEvent
    public static void onWindowResize(WindowResizedEvent e) {
        float currentWindowWidth = e.width;
        float currentWindowHeight = e.height;
        
        width = (int) ((currentWindowWidth / currentWindowHeight) * UI_DESIGN_HEIGHT);
        projection.setOrtho(
                0.0f,               // Left
                width, // Right
                UI_DESIGN_HEIGHT,   // Bottom (max Y)
                0.0f,               // Top (min Y)
                -1.0f,              // Near
                1.0f                // Far
        );
    }
    
    public static void enable() {
        enabled = true;
    }
    
    public static void disable() {
        enabled = false;
    }
    
    public static boolean isEnabled() {
        return enabled;
    }
    
    // --- DELEGATED GL STATE MANAGEMENT ---
    
    // New wrappers for the specific GL calls you requested delegation for:
    public static void glEnable(int cap) {
        GL11.glEnable(cap);
    }
    
    public static void glDisable(int cap) {
        GL11.glDisable(cap);
    }
    
    public static void glBlendFunc(int sfactor, int dfactor) {
        GL11.glBlendFunc(sfactor, dfactor);
    }
    
    // prepare2d() now uses the new delegated methods
    public static void prepare2d() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
//        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
    }
    
    public static void prepare3d() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glFrontFace(GL_CW);
    }
    
    public static void resetGL() {
        glDisable(GL_BLEND);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
    }
    
    public static void glBindVertexArray(int vaoId) {
        GL30.glBindVertexArray(vaoId);
    }
    
    public static void glBindBuffer(int target, int buffer) {
        GL15.glBindBuffer(target, buffer);
    }
    
    public static void glBufferData(int target, long size, int usage) {
        GL15.glBufferData(target,size,usage);
    }
    
    public static void glBufferSubData(int target, int offset, FloatBuffer data) {
        GL15.glBufferSubData(target,offset,data);
    }
    
    public static void glEnableVertexAttribArray(int index) {
        GL20.glEnableVertexAttribArray(index);
    }
    
    public static void glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        GL20.glVertexAttribPointer(index, size, type, normalized, stride, pointer);
    }
    
    // --- Drawing and Frame Management ---
    
    public static void glDrawElements(int mode, int count, int type, long indices) {
        GL11.glDrawElements(mode, count, type, indices);
    }
    
    public static void glViewport(int x, int y, int width, int height) {
        GL11.glViewport(x, y, width, height);
    }
    
    public static void glClearColor(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
    }
    
    public static void glClear(int mask) {
        GL11.glClear(mask);
    }
    
    public static void glUseProgram(int program) {
        GL20.glUseProgram(program);
    }
    
    public static void glUniform1i(int location, int v0) {
        GL20.glUniform1i(location,v0);
    }
    
    public static int glGetUniformLocation(int program, CharSequence name) {
        // NOTE: The hardcoded uniform name here is likely wrong and should use the passed 'name' argument.
        return GL20.glGetUniformLocation(program, name);
    }
    
    public static void glUniformMatrix4fv(int location, boolean transpose, FloatBuffer buffer) {
        GL20.glUniformMatrix4fv(location,transpose,buffer);
    }
    
    public static void glActiveTexture(int target) {
        GL13.glActiveTexture(target);
    }
    
    public static void glBindTexture(int target, int texture) {
        GL11.glBindTexture(target,texture);
    }
    
    public static void glCullFace(int mode) {
        GL11.glCullFace(mode);
    }
    
    public static void glFrontFace(int mode) {
        GL11.glFrontFace(mode);
    }
    
}
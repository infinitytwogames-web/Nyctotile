package org.infinitytwo.nyctotile.core.ui.input;

import org.infinitytwo.nyctotile.core.event.bus.EventBus;
import org.infinitytwo.nyctotile.core.event.SubscribeEvent;
import org.infinitytwo.nyctotile.core.event.input.keyboard.CharacterInputEvent;
import org.infinitytwo.nyctotile.core.event.input.keyboard.KeyPressEvent;
import org.infinitytwo.nyctotile.core.event.input.mouse.MouseButtonEvent;
import org.infinitytwo.nyctotile.core.ui.Label;
import org.infinitytwo.nyctotile.core.ui.layout.Scene;
import org.infinitytwo.nyctotile.core.ui.position.Anchor;
import org.infinitytwo.nyctotile.core.ui.position.Pivot;
import org.joml.Vector2i;

import java.nio.file.Path;

import static org.infinitytwo.nyctotile.core.manager.Display.transformWindowToVirtual;
import static org.joml.Math.clamp;
import static org.lwjgl.glfw.GLFW.*;

public abstract class TextInput extends Label {
    private int index = 0;
    private final Caret caret;
    private boolean input;
    private final StringBuilder builder = new StringBuilder();
    private boolean submitted;
    private boolean disabled;
    private final Scene scene;
    
    public TextInput(Scene scene, Path path) {
        super(scene, path);
        this.scene = scene;
        
        caret = new Caret(scene.getUIBatchRenderer());
        caret.setActive(false);
        caret.setHeight((int) ((textRenderer.getFontHeight()) + 3));
        caret.setParent(this);
        caret.setPosition(new Anchor(0, 0.5f), new Pivot(0, 0.5f));
        caret.setWidth(10);
        caret.setBackgroundColor(0, 0, 0, 1);
        
        scene.register(caret);
        setTextPosition(new Anchor(0, 0f), new Pivot(0, 0f), new Vector2i(5, 0));
        
        EventBus.connect(this);
    }
    
    @Override
    public void setHeight(int height) {
        super.setHeight(height);
        caret.setHeight(height - 25);
    }
    
    @Override
    public void onMouseClicked(MouseButtonEvent e) {
        if (submitted) {
            setText("");
            submitted = false;
        }
        
        index = getCaretIndexAtMouse(transformWindowToVirtual(scene.getWindow(), e.x, e.y).x, getPosition().x + 5);
        System.out.println(index);
        focus();
    }
    
    @Override
    public void draw() {
        super.draw();
        caret.draw();
    }
    
    @Override
    public void setBackgroundColor(float r, float g, float b, float a) {
        super.setBackgroundColor(r, g, b, a);
        caret.setBackgroundColor(backgroundColor.getContrastColor());
    }
    
    @SubscribeEvent
    public void onKeyPress(KeyPressEvent e) {
        if (e.getAction() == GLFW_PRESS ||
                e.getAction() == GLFW_REPEAT
        ) {
            caret.reset();
            boolean submittedLocally = false; // New flag to track submission
            
            if (e.getKey() == GLFW_KEY_ESCAPE) unfocus();
            else if (e.getKey() == GLFW_KEY_RIGHT) index = clamp(0, builder.length(), index + 1);
            else if (e.getKey() == GLFW_KEY_LEFT) index = clamp(0, builder.length(), index - 1);
            else if (e.getKey() == GLFW_KEY_BACKSPACE) backspace();
            else if (e.getKey() == GLFW_KEY_HOME) index = 0;
            else if (e.getKey() == GLFW_KEY_END) index = builder.length();
            else if (e.mods == GLFW_MOD_CONTROL) {
                if (e.getKey() == GLFW_KEY_A) {
                    // Control + A logic
                }
            } else if (e.getKey() == GLFW_KEY_ENTER) {
                submitted = true;
                submittedLocally = true; // Set flag
                submit(builder.toString());
                clean();
            }
            
            // CRITICAL FIX: Only update cursor if the command wasn't "Enter" (submission)
            if (!submittedLocally) {
                updateCursorPosition();
            }
        }
    }
    
    private void unfocus() {
        input = false;
        caret.setActive(false);
        String visible = getVisibleText(textRenderer, builder.toString(), 0, width - 10);
        setText(visible);
        updateCursorPosition();
    }
    
    private void focus() {
        if (disabled) {
            unfocus();
            return;
        }
        input = true;
        caret.setActive(true);
        String visible = getVisibleText(textRenderer, builder.toString(), index, width - 10);
        text.setText(visible);
        updateCursorPosition();
    }
    
    public int getCaretIndexAtMouse(float mouseX, float textX) {
        // 1. Get the visible text and the global starting index (the scroll offset)
        String visible = text.getText();
        String fullText = builder.toString();
        
        // Calculate the global offset of the visible text within the full string
        int globalOffset = getVisibleTextStartIndex(fullText, visible);
        
        float x = textX;
        
        // 2. Iterate through the visible characters
        for (int i = 0; i < visible.length(); i++) {
            char c = visible.charAt(i);
            float charWidth = textRenderer.getStringWidth(String.valueOf(c));
            
            // If mouseX is within this character's bounds
            if (mouseX < x + charWidth / 2f) {
                // Return the global index (global offset + local index i)
                return globalOffset + i;
            }
            
            x += charWidth;
        }
        
        // 3. If clicked past the end of the visible text, return the global length of the text.
        return fullText.length();
    }
    
    @SubscribeEvent
    public void onCharacterPressed(CharacterInputEvent e) {
        if (input) {
            caret.forceDraw();
            // NEW: If the previous state was submitted, clear the builder before inserting.
            if (submitted) {
                builder.setLength(0);
                index = 0;
                submitted = false;
            }
            
            // Original insertion logic
            builder.insert(index, e.character); // insert at index
            index++;
            setText(builder.toString());
            updateCursorPosition();
            caret.reset();
        }
    }
    
    // Inside TextInput.java
    public void updateCursorPosition() {
        String visible = getVisibleText(textRenderer, builder.toString(), index, width - 10);
        setText(visible);
        
        String fullText = builder.toString(); // For clarity
        
        // 1. Calculate the Global Offset (where the visible text starts in the full text)
        // If the visible text starts with "...", the start index (left) is > 0.
        int globalOffset = 0;
        if (visible.startsWith(ellipsis)) {
            // This is complex. The simplest way to find the left index (globalOffset)
            // is to reverse the logic of getVisibleText() in Label:
            
            // Find the full text segment (strip the possible ellipses)
            String segment = visible;
            if (visible.startsWith(ellipsis)) segment = segment.substring(ellipsis.length());
            if (visible.endsWith(ellipsis)) segment = segment.substring(0, segment.length() - ellipsis.length());
            
            // Now, find the index of this segment in the full text.
            globalOffset = fullText.indexOf(segment);
            // Fallback check, if index not found or is -1, globalOffset remains 0.
            if (globalOffset == -1) globalOffset = 0;
        }
        // If it doesn't start with "...", globalOffset is 0.
        
        // This is safer than the original helper, but still relies on string matching.
        
        // --- Cursor Position Calculation (This is where the index failure occurs) ---
        
        // FIX: Swap parameter order to match (min, max, val).
        // The clamp value should be 'index - globalOffset'
        int localIndex = clamp(0, visible.length(), index - globalOffset);
        
        // This line should now be safer as it uses the calculated localIndex:
        String leftPart = visible.substring(0, localIndex);
        int cursorX = (int) textRenderer.getStringWidth(leftPart);
        
        caret.setOffset(new Vector2i(cursorX + 5, 0));
    }
    
    private int getVisibleTextStartIndex(String full, String visible) {
        int fullLen = full.length();
        int visibleLen = visible.replace("...", "").length(); // remove ellipsis
        
        for (int i = 0; i <= fullLen - visibleLen; i++) {
            String candidate = full.substring(i, i + visibleLen);
            if (visible.contains(candidate)) {
                return i;
            }
        }
        
        return 0;
    }
    
    private void backspace() {
        caret.draw();
        // 1. Check if there is anything to delete (index > 0 means cursor is NOT at the start)
        if (index == 0 || builder.isEmpty()) return;
        
        // 2. The character to delete is the one *before* the cursor position (index - 1)
        int deleteIndex = index - 1;
        
        // 3. Delete the character
        builder.deleteCharAt(deleteIndex);
        
        // 4. Move the cursor back one position
        index = deleteIndex;
        
        // 5. Update UI
        setText(builder.toString());
        updateCursorPosition();
    }
    
    private void clean() {
        unfocus();
        builder.setLength(0);
        // Explicitly reset index for safety
        index = 0;
    }
    
    
    public abstract void submit(String data);
    
    public boolean isDisabled() {
        return disabled;
    }
    
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}

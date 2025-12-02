package org.infinitytwo.nyctotile.core.data.buffer;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class IntPacker {
    private final Map<String, Integer> bitSize = new LinkedHashMap<>();
    private final Map<String, Integer> shiftAmount = new LinkedHashMap<>();
    private final Map<String, Integer> values = new HashMap<>();
    
    private int currentShift = 0; // Tracks the cumulative shift for the next field
    private int remaining = 32;
    
    // --- Public Registration and Value Setting ---
    
    /**
     * Registers a new data field with a specified bit size.
     */
    public void register(String name, int bits) {
        name = name.toLowerCase();
        if (remaining < bits || bits <= 0) {
            // Throw an exception or handle the error gracefully
            throw new IllegalArgumentException("Cannot register field '" + name + "' with " + bits + " bits. Remaining bits: " + remaining);
        }
        
        // 1. Store the size
        bitSize.put(name, bits);
        
        // 2. Store the shift amount (current position)
        shiftAmount.put(name, currentShift);
        
        // 3. Update internal counters
        currentShift += bits;
        remaining -= bits;
    }
    
    /**
     * Sets the value for a registered data field.
     */
    public void setValue(String name, int value) {
        if (!bitSize.containsKey(name)) {
            throw new IllegalArgumentException("Field '" + name + "' is not registered.");
        }
        values.put(name, value);
    }
    
    public int getValue(String name) {
        if (!bitSize.containsKey(name)) {
            throw new IllegalArgumentException("Field '" + name + "' is not registered.");
        }
        return values.get(name);
    }
    
    /**
     * Updates the value of a specific field within an already packed integer.
     * @param currentPackedInt The integer to be modified.
     * @param name The name of the field to update.
     * @param newValue The new integer value for that field.
     * @return The updated packed integer.
     */
    public int setValue(int currentPackedInt, String name, int newValue) {
        name = name.toLowerCase();
        
        if (!bitSize.containsKey(name)) {
            throw new IllegalArgumentException("Field '" + name + "' is not registered.");
        }
        
        int bits = bitSize.get(name);
        int shift = shiftAmount.get(name);
        
        // The mask is a sequence of 'bits' number of 1s at the far right (e.g., 0xFF)
        int dataMask = (1 << bits) - 1;
        
        // --- Step 1: Clear the Old Bits ---
        // 1. Shift the Data Mask to the field's position.
        int clearMask = dataMask << shift;
        
        // 2. Invert the mask (turns the field's 1s to 0s, and all other 0s to 1s).
        // Using AND with the inverted mask clears the field's bits.
        int clearedInt = currentPackedInt & (~clearMask);
        
        // --- Step 2: Prepare the New Bits ---
        // 1. Clamp the new value (ensures it doesn't overflow the bit allocation).
        int maskedNewValue = newValue & dataMask;
        
        // 2. Shift the new value to the field's position.
        int newBits = maskedNewValue << shift;
        
        // --- Step 3: Insert the New Bits ---
        // Use Bitwise OR to combine the cleared integer and the new bits.
        return clearedInt | newBits;
    }
    
    // --- Core Packing Logic ---
    
    /**
     * Computes and returns the final packed integer.
     */
    public int getPackedInt() {
        int packed = 0;
        
        // Iterate through the fields in the order they were registered (thanks to LinkedHashMap)
        for (Map.Entry<String, Integer> entry : bitSize.entrySet()) {
            String name = entry.getKey();
            int bits = entry.getValue();
            
            // Get the value and the shift amount for this field
            Integer value = values.get(name);
            Integer shift = shiftAmount.get(name);
            
            if (value == null) {
                value = 0;
            }
            
            // 1. Create a mask to ensure the value doesn't exceed its allocated bits.
            // (1 << bits) - 1 creates a mask of 'bits' number of 1s (e.g., bits=4 -> 15 -> 0xF)
            int mask = (1 << bits) - 1;
            
            // 2. Apply the mask and shift the value to its correct position.
            int shiftedValue = (value & mask) << shift;
            
            // 3. Combine with the main packed integer using Bitwise OR.
            packed = packed | shiftedValue;
        }
        
        return packed;
    }
    
    /**
     * Extracts a field's value from an already packed integer.
     * @param packedInt The integer containing the packed data.
     * @param name The name of the field to extract.
     * @return The integer value of the extracted field.
     */
    public int getValue(int packedInt, String name) {
        name = name.toLowerCase();
        
        // 1. Check if the field is registered
        if (!bitSize.containsKey(name)) {
            throw new IllegalArgumentException("Field '" + name + "' is not registered.");
        }
        
        // 2. Retrieve the necessary bitwise constants
        int bits = bitSize.get(name);
        int shift = shiftAmount.get(name);
        
        // 3. Create the Mask
        //  is a sequence of 'bits' number of 1s at the far right.
        // E.g., if bits is 8, the mask is 0xFF (255).
        int mask = (1 << bits) - 1;
        
        // --- The Unpacking Formula ---
        
        // A. Right Shift (move the target bits to the rightmost position)
        int shiftedValue = packedInt >> shift;
        
        // B. Bitwise AND (mask to isolate ONLY the target bits)
        return shiftedValue & mask;
    }
    
    // --- Utility Methods ---
    
    public int getRemaining() {
        return remaining;
    }
}
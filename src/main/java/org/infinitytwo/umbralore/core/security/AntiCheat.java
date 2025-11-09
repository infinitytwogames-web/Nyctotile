package org.infinitytwo.umbralore.core.security;

import org.infinitytwo.umbralore.core.entity.Entity;
import org.joml.Vector3f;

import static org.infinitytwo.umbralore.core.data.io.WorldData.CHUNK_SIZE;

public class AntiCheat {
    private final int playerViewDistance;
    
    public AntiCheat(int playerViewDistance) {
        this.playerViewDistance = playerViewDistance;
    }
    
    
    public boolean shouldSendRequestedChunk(int requestX, int requestZ, Vector3f playerPosition) {
        
        // 1. Convert player world coordinates to player chunk coordinates
        int playerChunkX = (int) Math.floor(playerPosition.x / CHUNK_SIZE);
        int playerChunkZ = (int) Math.floor(playerPosition.z / CHUNK_SIZE);
        
        // 2. Calculate the absolute difference in chunk space (Manhattan Distance)
        int diffX = Math.abs(requestX - playerChunkX);
        int diffZ = Math.abs(requestZ - playerChunkZ);
        
        // 3. Compare to the view distance
        // (Note: requestY is typically the Z coordinate in a 2D chunk map)
        return diffX <= playerViewDistance && diffZ <= playerViewDistance;
    }
    
    public boolean isAcceptableVelocity(float velX, float velY, float velZ, Entity entity) {
        
        // --- 1. Horizontal Speed Check (XZ Plane) ---
        // Calculate the squared horizontal magnitude (avoids costly square root)
        float horizSpeedSquared = velX * velX + velZ * velZ;
        
        float maxMovementSpeed = entity.getMovementSpeed();
        // Allow a small buffer for movement (e.g., 5% extra speed or 0.5 units)
        final float HORIZ_BUFFER = 1.0f;
        float maxHorizSpeed = maxMovementSpeed + HORIZ_BUFFER;
        float maxHorizSpeedSquared = maxHorizSpeed * maxHorizSpeed;
        
        if (horizSpeedSquared > maxHorizSpeedSquared + 0.0001f) {
            // Horizontal speed too high (e.g., walking faster than allowed)
            return false;
        }
        
        // --- 2. Vertical Speed Check (Y Axis) ---
        float maxJumpSpeed = entity.getJumpStrength();
        float maxFallSpeed = 50.0f; // Set a high but reasonable terminal velocity limit
        
        // Check for extreme upward velocity
        if (velY > maxJumpSpeed + 1.0f) {
            // Upward speed too high (e.g., super jump)
            return false;
        }
        
        // Check for extreme downward velocity (e.g., falling through the world)
        if (velY < -maxFallSpeed) {
            // Downward speed too high
            return false;
        }
        
        // If both checks pass, the velocity is considered acceptable
        return true;
    }
}

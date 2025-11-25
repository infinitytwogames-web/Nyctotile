package org.infinitytwo.nyctotile.core.security;

import org.infinitytwo.nyctotile.core.entity.Entity;
import org.infinitytwo.nyctotile.core.entity.Player;
import org.joml.Vector3f;

import static org.infinitytwo.nyctotile.core.data.io.WorldData.CHUNK_SIZE;

public class AntiCheat {
    private static final float MAX_TERMINAL_VELOCITY = 50f;
    // FIX 1: Increased the override value to 0.75f to safely accommodate the observed jump impulse (dy=0.694).
    private static final float MAX_JUMP_IMPULSE_OVERRIDE = 0.75f;
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
        return diffX <= playerViewDistance && diffZ <= playerViewDistance;
    }
    
    @Deprecated
    public boolean isAcceptableVelocity(float velX, float velY, float velZ, Entity entity) { // I will leave it just in case
        
        // --- 1. Horizontal Speed Check (XZ Plane) ---
        // Calculate the squared horizontal magnitude (avoids costly square root)
        float horizontalSpeedSquared = velX * velX + velZ * velZ;
        
        float maxMovementSpeed = entity.getMovementSpeed();
        // Allow a small buffer for movement (e.g., 5% extra speed or 0.5 units)
        final float H_BUFFER = 1.0f;
        float maxHorSpeed = maxMovementSpeed + H_BUFFER;
        float maxHorSpeedSquared = maxHorSpeed * maxHorSpeed;
        
        if (horizontalSpeedSquared > maxHorSpeedSquared + 0.0001f) {
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
        
        return !(velY < -maxFallSpeed);
    }
    
    public boolean isAcceptablePosition(float x, float y, float z, Player player) {
        
        final float FIXED_DELTA = 1.0f / 60.0f;
        Vector3f original = player.getPosition();
        
        // --- 1. Calculate Actual Movement ---
        float dx = x - original.x;
        float dy = y - original.y;
        float dz = z - original.z;
        float distanceSq = dx * dx + dy * dy + dz * dz;
        
        // --- 2. Check for Teleportation (Uses a more lenient buffer) ---
        float maxAllowedDistanceSq = getMaxAllowedDistanceSq(player);
        if (distanceSq > maxAllowedDistanceSq) {
            return false;
        }
        
        // --- 3. Mid-Air Jump Check (REMOVED: The block check is too unreliable and causing false positives.) ---
        // We rely on the speed check in Section 4 to catch suspicious vertical movement.
        /*
        final float JUMP_THRESHOLD = player.getJumpStrength() * FIXED_DELTA * 0.75f;
        if (dy > JUMP_THRESHOLD) {
            // ... (Grounding check removed)
        }
        */
        
        // --- 4. Vertical Fly Check (Now robust against normal jumps with high impulse) ---
        
        // Calculate the maximum allowed vertical distance.
        final float JUMP_STRENGTH_LIMIT = player.getJumpStrength() * 1.25f;
        // FINAL_JUMP_LIMIT is now guaranteed to be 0.75f (or higher if jump strength is massive).
        final float FINAL_JUMP_LIMIT = Math.max(JUMP_STRENGTH_LIMIT, MAX_JUMP_IMPULSE_OVERRIDE);
        
        if (dy > FINAL_JUMP_LIMIT) {
            // Exceeded the generous impulse limit (Super Jump/Flight).
            // Only allow this if they are fixing a grounding error (snapping to a surface).
            
            // Calculate the block under the *new* position (x, y, z)
            int newBlockY = (int) Math.floor(y - 1.0f);
            Object blockBelowNew = player.getDimension().getWorld().getBlock((int) Math.floor(x), newBlockY, (int) Math.floor(z));
            
            if (blockBelowNew == null) {
                System.out.println("FLYING!");
                return false; // Confirmed Super Jump/Flight.
            }
        }
        
        return true; // No violation detected.
    }
    
    private static float getMaxAllowedDistanceSq(Player player) {
        float maxSpeed = getMaxSpeed(player);
        
        // Retain the generous buffer of 6.0f to cover max movement + jump + network jitter.
        final float SPEED_BUFFER = 6.0f;
        
        float maxSpeedWithBuffer = maxSpeed + SPEED_BUFFER;
        
        float maxAllowedDistance = maxSpeedWithBuffer * (float) 0.016666668;
        return maxAllowedDistance * maxAllowedDistance;
    }
    
    private static float getMaxSpeed(Player player) {
        float maxHorizontalSpeed = player.getMovementSpeed();
        // Assuming MAX_TERMINAL_VELOCITY is defined elsewhere
        float maxVerticalSpeed = Math.max(player.getJumpStrength(), MAX_TERMINAL_VELOCITY);
        
        return (float) Math.sqrt(
                (maxHorizontalSpeed * maxHorizontalSpeed) +
                        (maxVerticalSpeed * maxVerticalSpeed)
        );
    }
}
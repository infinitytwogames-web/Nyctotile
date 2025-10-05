package org.infinitytwo.umbralore.entity;

import org.infinitytwo.umbralore.Window;
import org.infinitytwo.umbralore.block.Block;
import org.infinitytwo.umbralore.data.AABB;
import org.infinitytwo.umbralore.world.GridMap;
import org.joml.Vector3f;

public abstract class Entity {
    protected final String id;
    protected final Window window;
    protected AABB hitbox = new AABB(0,0,0,0,0,0); // Local-space AABB size
    protected Vector3f velocity = new Vector3f(); // But we also have this...
    protected Vector3f position = new Vector3f();
    protected float gravity = -22.7f; // Increased gravity for a more noticeable effect (original -0.08f was too small)
    protected GridMap world;
    private boolean isGrounded = false;
    protected AABB currentHitbox = hitbox;
    private Vector3f prevPos = position;
    protected float movementSpeed = 7;
    protected float jumpStrength = 7.2f;

    protected Entity(String id, GridMap map, Window window) {
        this.id = id;
        this.world = map;
        this.window = window;
    }

    public abstract void draw();

    // Implement the collision detection here.
    // Note: This is a voxel game. GridMap here means my world.
    // How to use GridMap: You can get block by using GridMap.getBlock(int x, int y, int z)
    // For now, let's assume every cube is a solid "new AABB(0,0,0,1,1,1);"

    public void update(float deltaTime) {
        // Reset grounded state
        isGrounded = false;

        // Apply gravity
        velocity.y += gravity * deltaTime;

        // Move axis by axis
        moveAxis(velocity.x * deltaTime, 0, 0);
        moveAxis(0, velocity.y * deltaTime, 0);
        moveAxis(0, 0, velocity.z * deltaTime);
    }

    protected void moveAxis(float dx, float dy, float dz) {
        float moveX = dx;
        float moveY = dy;
        float moveZ = dz;

        // Check blocks around entity
        AABB base = hitbox.offset(position.x, position.y, position.z); // how do i render this in real time?

        int minX = (int)Math.floor(base.minX + dx);
        int minY = (int)Math.floor(base.minY + dy);
        int minZ = (int)Math.floor(base.minZ + dz);

        int maxX = (int)Math.floor(base.maxX + dx);
        int maxY = (int)Math.floor(base.maxY + dy);
        int maxZ = (int)Math.floor(base.maxZ + dz);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlock(x, y, z);
                    if (block == null) continue;

                    for (AABB blockBox : block.getType().getBoundingBoxes()) {
                        // Convert block-local AABB to world coords
                        AABB worldBox = blockBox.offset(x, y, z);

                        // Entity’s attempted new box
                        AABB moved = hitbox.offset(position.x + moveX, position.y + moveY, position.z + moveZ);

                        if (!moved.isIntersecting(worldBox)) continue;

                        if (dx != 0) {
                            if (dx > 0) moveX = Math.min(moveX, worldBox.minX - base.maxX - 0.0001f);
                            else        moveX = Math.max(moveX, worldBox.maxX - base.minX + 0.0001f);
                            velocity.x = 0;
                        }
                        if (dy != 0) {
                            if (dy > 0) moveY = Math.min(moveY, worldBox.minY - base.maxY - 0.0001f);
                            else {
                                moveY = Math.max(moveY, worldBox.maxY - base.minY + 0.0001f);
                                if (velocity.y < 0) isGrounded = true; // only if falling
                            }
                            velocity.y = 0;
                        }
                        if (dz != 0) {
                            if (dz > 0) moveZ = Math.min(moveZ, worldBox.minZ - base.maxZ - 0.0001f);
                            else        moveZ = Math.max(moveZ, worldBox.maxZ - base.minZ + 0.0001f);
                            velocity.z = 0;
                        }

                        // Refresh base AABB after resolving
                        base = hitbox.offset(position.x + moveX, position.y + moveY, position.z + moveZ);
                    }
                }
            }
        }

        // Apply final resolved movement
        position.add(moveX, moveY, moveZ);
    }

    public boolean isGrounded() {
        return isGrounded;
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(float x, float y, float z) {
        position.set(x,y,z);
        prevPos.set(x,y,z);
    }

    public void savePrevPosition() {
        prevPos.set(position);
    }

    public void setPosition(Vector3f pos) {
        setPosition(pos.x, pos.y, pos.z);
    }

    public Vector3f getPrevPosition() {
        return prevPos;
    }

    public AABB getHitbox() {
        return hitbox;
    }
}
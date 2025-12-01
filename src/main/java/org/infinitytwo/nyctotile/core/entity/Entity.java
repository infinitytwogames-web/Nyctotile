package org.infinitytwo.nyctotile.core.entity;

import org.infinitytwo.nyctotile.core.Window;
import org.infinitytwo.nyctotile.core.data.world.Block;
import org.infinitytwo.nyctotile.core.data.world.AABB;
import org.infinitytwo.nyctotile.core.data.Inventory;
import org.infinitytwo.nyctotile.core.data.Item;
import org.infinitytwo.nyctotile.core.registry.Registerable;
import org.infinitytwo.nyctotile.core.world.GMap;
import org.infinitytwo.nyctotile.core.world.dimension.Dimension;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.UUID;

import static org.joml.Math.lerp;

public abstract class Entity implements Registerable {
    protected final String id;
    protected UUID uuid;
    protected final Window window;
    protected AABB hitbox = new AABB(0,0,0,0,0,0); // Local-space AABB size
    protected final Vector3f velocity = new Vector3f();
    protected final Vector3f position = new Vector3f();
    protected final Vector3f prevPosition = new Vector3f().set(position);
    protected float gravity = -22.7f;
    protected Dimension dimension;
    protected Inventory inventory;
    private boolean isGrounded = false;
    protected int modelIndex;
    protected float movementSpeed = 7;
    protected float jumpStrength = 7.2f;
    private final Vector3f scale = new Vector3f(1,1,1);
    private final Vector3f rotation = new Vector3f();
    private static float airResistance = 7;
    
    private static final float COLLISION_EPSILON = 0.005f;

    public static synchronized float getAirResistance() {
        return airResistance;
    }

    public static synchronized void setAirResistance(float airResistance) {
        Entity.airResistance = airResistance;
    }

    protected Entity(@NotNull String id, Dimension map, Window window, Inventory inventory) {
        this.id = id;
        this.dimension = map;
        this.window = window;
        this.inventory = inventory;
        uuid = UUID.randomUUID();
    }

    public Entity(@NotNull String id, Window window, Dimension dimension, Inventory inventory, AABB hitbox) {
        this.id = id;
        this.window = window;
        this.dimension = dimension;
        this.inventory = inventory;
        this.hitbox = hitbox;
        uuid = UUID.randomUUID();
    }
    
    public void update(float deltaTime) {
        update(deltaTime, dimension.getWorld());
    }
    
    public void update(float deltaTime, GMap map) {
        synchronized (this) {
        setPrevPosition();
        
        // Reset grounded state
        isGrounded = false;

        Block block = map.getBlock((int) position.x, (int) (position.y - 1), (int) position.z);
        float friction = block == null? 1 : block.getType().getFriction();
        
        // Apply gravity
        velocity.y += gravity * deltaTime;
        velocity.x = lerp(velocity.x, 0, deltaTime * (friction * airResistance));
        velocity.z = lerp(velocity.z, 0, deltaTime * (friction * airResistance));
        
        // Move axis by axis
        moveAxis(velocity.x * deltaTime, 0, 0,map);
        moveAxis(0, velocity.y * deltaTime, 0,map);
        moveAxis(0, 0, velocity.z * deltaTime,map);
        }
    }
    
    protected synchronized void moveAxis(float dx, float dy, float dz, GMap map) {
        float moveX = dx;
        float moveY = dy;
        float moveZ = dz;

        // Check blocks around entity
        AABB base = hitbox.offset(position.x, position.y, position.z);

        float sweepMinX = Math.min(base.minX, base.minX + dx);
        float sweepMinY = Math.min(base.minY, base.minY + dy);
        float sweepMinZ = Math.min(base.minZ, base.minZ + dz);
        
        float sweepMaxX = Math.max(base.maxX, base.maxX + dx);
        float sweepMaxY = Math.max(base.maxY, base.maxY + dy);
        float sweepMaxZ = Math.max(base.maxZ, base.maxZ + dz);

        int minX = (int)Math.floor(sweepMinX);
        int minY = (int)Math.floor(sweepMinY);
        int minZ = (int)Math.floor(sweepMinZ);
        
        int maxX = (int)Math.floor(sweepMaxX);
        int maxY = (int)Math.floor(sweepMaxY);
        int maxZ = (int)Math.floor(sweepMaxZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = map.getBlock(x, y, z);
                    if (block == null) continue;

                    if (block.getType() == null) throw new IllegalStateException("Somehow, the block at "+x+" "+y+" "+z+" has undefined block type");
                    for (AABB blockBox : block.getType().getBoundingBoxes()) {
                        // Convert block-local AABB to world coords
                        AABB worldBox = blockBox.offset(x, y, z);

                        // Entity’s attempted new box
                        AABB moved = hitbox.offset(position.x + moveX, position.y + moveY, position.z + moveZ);

                        if (!moved.isIntersecting(worldBox)) continue;

                        if (dx != 0) {
                            if (dx > 0) moveX = Math.min(moveX, worldBox.minX - base.maxX - COLLISION_EPSILON);
                            else        moveX = Math.max(moveX, worldBox.maxX - base.minX + COLLISION_EPSILON);
                            velocity.x = 0;
                        }
                        if (dy != 0) {
                            if (dy > 0) moveY = Math.min(moveY, worldBox.minY - base.maxY - COLLISION_EPSILON);
                            else {
                                moveY = Math.max(moveY, worldBox.maxY - base.minY + COLLISION_EPSILON);
                                if (velocity.y < 0) isGrounded = true; // only if falling
                            }
                            velocity.y = 0;
                        }
                        if (dz != 0) {
                            if (dz > 0) moveZ = Math.min(moveZ, worldBox.minZ - base.maxZ - COLLISION_EPSILON);
                            else        moveZ = Math.max(moveZ, worldBox.maxZ - base.minZ + COLLISION_EPSILON);
                            velocity.z = 0;
                        }
                    }
                }
            }
        }

        // Apply final resolved movement
        position.add(moveX, moveY, moveZ);
    }


    public static <E extends Entity> Entity copy(E entity) {
        Entity e = new Entity(entity.id,entity.window, entity.getDimension(),entity.inventory,entity.hitbox) {
            @Override
            public Entity newInstance() {
                return null;
            }
        };
        e.gravity = entity.gravity;
        e.modelIndex = entity.modelIndex;
        e.jumpStrength = entity.jumpStrength;
        e.movementSpeed = entity.movementSpeed;
        e.scale.set(entity.getScale());
        return e;
    }

    public static Entity nullType() {
        return new Entity("",null,null,null) {
            @Override
            public Entity newInstance() {
                return null;
            }
        };
    }

    @Override
    public String getId() {
        return id;
    }

    public UUID getUUID() {
        return uuid;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int giveItem(Item item) {
        return inventory.add(item);
    }

    public void removeItem(int slot) {
        inventory.remove(slot);
    }

    public void addItem(int slot, int change) {
        inventory.add(slot,change);
    }

    public int getModelIndex() {
        return modelIndex;
    }

    public void setModelIndex(int modelIndex) {
        this.modelIndex = modelIndex;
    }

    public void setItem(int slot, Item item) {
        inventory.set(slot, item);
    }

    public int addItem(Item item) {
        return inventory.add(item);
    }

    public void remove(int slot) {
        inventory.remove(slot);
    }

    public int getItemCount(int slot) {
        return inventory.getCount(slot);
    }

    public int getMaxSlots() {
        return inventory.getMaxSlots();
    }

    public boolean isInventoryFull() {
        return inventory.isFull();
    }

    public void clearInventory() {
        inventory.clear();
    }

    public boolean isInventoryEmpty() {
        return inventory.isEmpty();
    }
    
    public synchronized boolean isGrounded() {
        return isGrounded;
    }

    public synchronized Vector3f getPosition() {
        return new Vector3f(position);
    }

    public synchronized void setPosition(float x, float y, float z) {
        position.set(x,y,z);
    }

    public synchronized void setPosition(Vector3f pos) {
        setPosition(pos.x, pos.y, pos.z);
    }

    public synchronized AABB getHitbox() {
        return hitbox;
    }

    public synchronized Vector3f getRotation() {
        return new Vector3f(rotation);
    }

    public synchronized void setRotation(Vector3f rotation) {
        this.rotation.set(rotation);
    }

    public synchronized Vector3f getScale() {
        return scale;
    }

    public synchronized void setScale(Vector3f scale) {
        this.scale.set(scale);
    }

    public synchronized Vector3f getVelocity() {
        return new Vector3f(velocity);
    }

    public synchronized void setVelocity(Vector3f velocity) {
        this.velocity.set(velocity);
    }

    public synchronized float getGravity() {
        return gravity;
    }

    public synchronized void setGravity(float gravity) {
        this.gravity = gravity;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

    public float getMovementSpeed() {
        return movementSpeed;
    }

    public void setMovementSpeed(float movementSpeed) {
        this.movementSpeed = movementSpeed;
    }

    public float getJumpStrength() {
        return jumpStrength;
    }

    public void setJumpStrength(float jumpStrength) {
        this.jumpStrength = jumpStrength;
    }

    public synchronized Matrix4f getModelMatrix() {
        return new Matrix4f()
                .translate(position)
                .rotateXYZ(rotation.x, rotation.y, rotation.z)
                .scale(scale);
    }
    
    public synchronized void setVelocity(float x, float y, float z) {
        velocity.set(x,y,z);
    }
    
    public synchronized Vector3f getPrevPosition() {
        return prevPosition;
    }
    
    public synchronized void setPrevPosition() {
        this.prevPosition.set(position);
    }
    
    public void setUUID(UUID id) {
        uuid = id;
    }
    
    public abstract Entity newInstance();
}

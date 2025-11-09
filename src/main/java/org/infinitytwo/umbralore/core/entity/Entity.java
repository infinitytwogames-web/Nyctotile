package org.infinitytwo.umbralore.core.entity;

import org.infinitytwo.umbralore.core.Window;
import org.infinitytwo.umbralore.core.data.Block;
import org.infinitytwo.umbralore.core.data.AABB;
import org.infinitytwo.umbralore.core.data.Inventory;
import org.infinitytwo.umbralore.core.data.Item;
import org.infinitytwo.umbralore.core.registry.Registerable;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.UUID;

import static org.joml.Math.lerp;

public abstract class Entity implements Registerable {
    protected final String id;
    protected final UUID uuid;
    protected final Window window;
    protected AABB hitbox = new AABB(0,0,0,0,0,0); // Local-space AABB size
    protected Vector3f velocity = new Vector3f();
    protected Vector3f position = new Vector3f();
    protected float gravity = -22.7f;
    protected Dimension dimension;
    protected Inventory inventory;
    private boolean isGrounded = false;
    protected int modelIndex;
    protected float movementSpeed = 7;
    protected float jumpStrength = 7.2f;
    private Vector3f scale = new Vector3f(1,1,1);
    private Vector3f rotation = new Vector3f();
    private static float airResistance = 7;

    public static float getAirResistance() {
        return airResistance;
    }

    public static void setAirResistance(float airResistance) {
        Entity.airResistance = airResistance;
    }

    protected Entity(String id, Dimension map, Window window, Inventory inventory) {
        this.id = id;
        this.dimension = map;
        this.window = window;
        this.inventory = inventory;
        uuid = UUID.randomUUID();
    }

    public Entity(String id, Window window, Dimension dimension, Inventory inventory, AABB hitbox) {
        this.id = id;
        this.window = window;
        this.dimension = dimension;
        this.inventory = inventory;
        this.hitbox = hitbox;
        uuid = UUID.randomUUID();
    }

    public abstract Entity newInstance();

    public static <E extends Entity> Entity copy(E entity) {
        Entity e = new Entity(entity.id,entity.window, entity.getDimension(),entity.inventory,entity.hitbox) {
            @Override
            public Entity newInstance() {
                return null;
            }
        };
        e.gravity = entity.gravity;
        e.velocity = entity.velocity;
        e.isGrounded = entity.isGrounded();
        e.modelIndex = entity.modelIndex;
        e.jumpStrength = entity.jumpStrength;
        e.movementSpeed = entity.movementSpeed;
        e.scale = entity.getScale();
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

    public void update(float deltaTime) {
        // Reset grounded state
        isGrounded = false;

        Block block = dimension.getWorld().getBlock((int) position.x, (int) (position.y - 1), (int) position.z);
        float friction = block == null? 1 : block.getType().getFriction();
        // Apply gravity
        velocity.y += gravity * deltaTime;
        velocity.x = lerp(velocity.x, 0, deltaTime * (friction * airResistance));
        velocity.z = lerp(velocity.z, 0, deltaTime * (friction * airResistance));

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
                    Block block = dimension.getWorld().getBlock(x, y, z);
                    if (block == null) continue;

                    if (block.getType() == null) throw new IllegalStateException("Somehow, the block at "+x+" "+y+" "+z+" has undefined block type");
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
//                        base = hitbox.offset(position.x + moveX, position.y + moveY, position.z + moveZ);
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
    }

    public void setPosition(Vector3f pos) {
        setPosition(pos.x, pos.y, pos.z);
    }

    public AABB getHitbox() {
        return hitbox;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(Vector3f rotation) {
        this.rotation = rotation;
    }

    public Vector3f getScale() {
        return scale;
    }

    public void setScale(Vector3f scale) {
        this.scale = scale;
    }

    public Vector3f getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector3f velocity) {
        this.velocity = velocity;
    }

    public float getGravity() {
        return gravity;
    }

    public void setGravity(float gravity) {
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

    public Matrix4f getModelMatrix() {
        return new Matrix4f()
                .translate(position)
                .rotateXYZ(rotation.x, rotation.y, rotation.z)
                .scale(scale);
    }
    
    public void setVelocity(float x, float y, float z) {
        velocity.set(x,y,z);
    }
}
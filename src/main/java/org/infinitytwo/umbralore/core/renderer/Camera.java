package org.infinitytwo.umbralore.core.renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.joml.Math.clamp;

public class Camera {
    private Vector3f position = new Vector3f(0, 0, 3);
    private Vector3f targetPosition = new Vector3f(position);

    private float pitch = 0;
    private float yaw = -90; // Start looking toward Z-

    private float targetPitch = pitch;
    private float targetYaw = yaw;
    public boolean doLerp = true;

    private float moveSpeed = 5f;
    private float rotateSpeed = 15f;

    private final Vector3f worldUp = new Vector3f(0, 1, 0);
    private double POV = 70;

    public void update(float delta) {
        // Smoothly interpolate rotation
        if (doLerp) {
            pitch = lerp(pitch, targetPitch, rotateSpeed * delta);
            yaw = lerp(yaw, targetYaw, rotateSpeed * delta);

            // Smoothly interpolate position
            position.lerp(targetPosition, moveSpeed * delta);
        } else {
            pitch = targetPitch;
            yaw = targetYaw;
            position = targetPosition;
        }
    }

    public Matrix4f getViewMatrix() {
        Vector3f front = getDirection();
        return new Matrix4f().lookAt(position, new Vector3f(position).add(front), worldUp);
    }

    public Vector3f getDirection() {
        Vector3f dir = new Vector3f();
        dir.x = (float) Math.cos(Math.toRadians(pitch)) * (float) Math.cos(Math.toRadians(yaw));
        dir.y = (float) Math.sin(Math.toRadians(pitch));
        dir.z = (float) Math.cos(Math.toRadians(pitch)) * (float) Math.sin(Math.toRadians(yaw));
        return dir.normalize();
    }

    // Movement
    public void moveForward(float delta) {
        Vector3f front = getDirection().mul(delta * moveSpeed);
        targetPosition.add(front);
    }

    public void moveBackward(float delta) {
        Vector3f back = getDirection().mul(-delta * moveSpeed);
        targetPosition.add(back);
    }

    public void moveRight(float delta) {
        Vector3f right = getDirection().cross(worldUp, new Vector3f()).normalize().mul(delta * moveSpeed);
        targetPosition.add(right);
    }

    public void moveLeft(float delta) {
        Vector3f left = getDirection().cross(worldUp, new Vector3f()).normalize().mul(-delta * moveSpeed);
        targetPosition.add(left);
    }

    public void moveUp(float delta) {
        targetPosition.add(new Vector3f(worldUp).mul(delta * moveSpeed));
    }

    public void moveDown(float delta) {
        targetPosition.add(new Vector3f(worldUp).mul(-delta * moveSpeed));
    }

    // Rotation input
    public void rotate(float yawOffset, float pitchOffset) {
        targetYaw += yawOffset;
        targetPitch += pitchOffset;

        // Clamp pitch
        targetPitch = Math.max(-89.9f, Math.min(89.9f, targetPitch));
    }

    // Util lerp
    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getPitch() {
        return pitch;
    }

    public  float getYaw() {
        return yaw;
    }

    public double getFov() {
        return POV;
    }

    public void setPov(double pov) {
        POV = clamp(pov, 40, 120);
    }

    public void setRawPov(double pov) {
        POV = pov;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public float getRotateSpeed() {
        return rotateSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public void setRotateSpeed(float rotateSpeed) {
        this.rotateSpeed = rotateSpeed;
    }

    public void setPosition(Vector3f position) {
        this.position.set(
                position.x,
                position.y,
                position.z
        );
    }

    public void setPosition(float x, float y, float z) {
        this.position.set(x,y,z);
    }

    public void follow(Vector3f position, Vector3f offset) {
        this.position.set(position).add(offset);
    }

    public double getPov() {
        return POV;
    }
}

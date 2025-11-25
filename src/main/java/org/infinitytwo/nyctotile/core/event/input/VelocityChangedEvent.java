package org.infinitytwo.nyctotile.core.event.input;

import org.infinitytwo.nyctotile.core.event.Event;
import org.joml.Vector3f;

public class VelocityChangedEvent extends Event {
    public final float x,y,z;
    
    public VelocityChangedEvent(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public VelocityChangedEvent(Vector3f wishDir) {
        x = wishDir.x;
        y = wishDir.y;
        z = wishDir.z;
    }
}

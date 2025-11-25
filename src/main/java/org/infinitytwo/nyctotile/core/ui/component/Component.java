package org.infinitytwo.nyctotile.core.ui.component;

public interface Component {
    void draw();
    
    void setAngle(float angle);
    
    void setDrawOrder(int z);
    int getDrawOrder();
}

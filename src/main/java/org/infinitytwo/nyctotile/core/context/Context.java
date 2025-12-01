package org.infinitytwo.nyctotile.core.context;

import org.infinitytwo.nyctotile.core.ServerThread;
import org.infinitytwo.nyctotile.core.constants.LogicalSide;
import org.infinitytwo.nyctotile.core.constants.PhysicalSide;

@Deprecated
public abstract class Context {
    public final LogicalSide logicalSide;
    public final PhysicalSide physicalSide;
    public ServerThread server;

    protected Context(LogicalSide logicalSide, PhysicalSide physicalSide) {
        this.logicalSide = logicalSide;
        this.physicalSide = physicalSide;
    }

    public boolean isServerSide() {
        return logicalSide == LogicalSide.SERVER;
    }

    public boolean isClientSide() {
        return logicalSide == LogicalSide.CLIENT;
    }

    public boolean isDedicatedServer() {
        return physicalSide == PhysicalSide.DEDICATED_SERVER;
    }

    public boolean isLocal() {
        return physicalSide == PhysicalSide.LOCAL;
    }

    public ServerThread getServer() {
        return server;
    }
}

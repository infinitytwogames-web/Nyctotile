package org.infinitytwo.umbralore.core.context;

import org.infinitytwo.umbralore.core.manager.Players;
import org.infinitytwo.umbralore.core.constants.LogicalSide;
import org.infinitytwo.umbralore.core.constants.PhysicalSide;
import org.infinitytwo.umbralore.core.world.ServerGridMap;
import org.infinitytwo.umbralore.core.world.dimension.Dimension;

public class ServerContext extends Context {
    protected final ServerGridMap world;
    protected final Dimension dimension;
    protected final Players players;

    protected ServerContext(PhysicalSide physicalSide, ServerGridMap world, Dimension dimension, Players players) {
        super(LogicalSide.SERVER, physicalSide);
        this.world = world;
        this.dimension = dimension;
        this.players = players;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public ServerGridMap getWorld() {
        return world;
    }

    public Players getPlayers() {
        return players;
    }

    public static ServerContext of(PhysicalSide side, ServerGridMap world, Dimension dimension, Players players) {
        return new ServerContext(side, world, dimension, players);
    }
}

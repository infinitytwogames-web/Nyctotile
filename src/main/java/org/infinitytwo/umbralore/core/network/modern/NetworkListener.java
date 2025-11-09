package org.infinitytwo.umbralore.core.network.modern;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

public abstract class NetworkListener extends Listener {
    public abstract void connected(Connection connection);
    public abstract void received(Connection connection, Object object);
    public abstract void disconnected(Connection connection);
}

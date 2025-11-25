package org.infinitytwo.nyctotile.core.network.data;

import com.esotericsoftware.kryonet.Connection;
import org.infinitytwo.nyctotile.core.data.io.DataSchematica;

@FunctionalInterface
public interface NetworkCommandProcessor {
    void process(DataSchematica.Data packet, Connection client);
}

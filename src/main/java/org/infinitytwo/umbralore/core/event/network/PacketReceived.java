package org.infinitytwo.umbralore.core.event.network;

import org.infinitytwo.umbralore.core.event.Event;
import org.infinitytwo.umbralore.core.network.NetworkThread;

import java.net.InetAddress;

public class PacketReceived extends Event {
    public final NetworkThread.Packet packet;
    public final InetAddress address;

    public PacketReceived(NetworkThread.Packet message, InetAddress address) {
        this.packet = message;
        this.address = address;
    }
    
    public PacketReceived(NetworkThread.Packet packet) {
        address = packet.address();
        this.packet = packet;
    }
}

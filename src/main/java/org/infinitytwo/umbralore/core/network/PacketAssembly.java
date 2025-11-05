package org.infinitytwo.umbralore.core.network;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class PacketAssembly {
    
    // Map of <Packet ID, Map of <Fragment Index, Packet Fragment>>
    private final Map<Integer, Map<Short, NetworkThread.Packet>> assemblies = new ConcurrentHashMap<>();
    
    public void addPacket(int packetId, NetworkThread.Packet data) {
        // Use computeIfAbsent to initialize the inner map, and then store the packet
        // storing by index automatically handles and discards duplicate fragments.
        assemblies.computeIfAbsent(packetId, i -> new ConcurrentHashMap<>())
                .put(data.index(), data);
    }
    
    public NetworkThread.Packet getData(int packetId) throws MissingResourceException, NullPointerException {
        if (!assemblies.containsKey(packetId)) {
            throw new NullPointerException("The packet id does not exist in the registry.");
        }
        
        Map<Short, NetworkThread.Packet> fragmentMap = assemblies.get(packetId);
        
        // Get the total expected fragments from any fragment (the first one is a safe choice)
        NetworkThread.Packet firstPacket = fragmentMap.values().iterator().next();
        int expectedChunks = firstPacket.total();
        
        // CRITICAL CHECK: Ensure we have all unique fragments
        if (fragmentMap.size() != expectedChunks) {
            throw new MissingResourceException("Packet ID " + packetId + " is incomplete or has missing indexes. Found " + fragmentMap.size() + " of " + expectedChunks + " fragments.",
                    NetworkThread.Packet.class.getName(),
                    String.valueOf(packetId)
            );
        }
        
        // Reconstruct final payload by iterating fragments in order (using a TreeMap-like iteration of keys)
        int totalSize = 0;
        
        // Calculate total size first
        for (NetworkThread.Packet fragment : fragmentMap.values()) {
            totalSize += fragment.payload().length;
        }
        
        byte[] finalPayload = new byte[totalSize];
        int offset = 0;
        
        // Iterate from index 0 up to total-1 to ensure correct order
        for (short i = 0; i < expectedChunks; i++) {
            byte[] payload = fragmentMap.get(i).payload();
            System.arraycopy(payload, 0, finalPayload, offset, payload.length);
            offset += payload.length;
        }
        
        // Create the final assembled packet using data from the first fragment
        NetworkThread.Packet newPacket = new NetworkThread.Packet(
                packetId,
                firstPacket.nonce(),
                finalPayload.length,
                (short) 0,
                (short) 1, // Total is 1 for an assembled packet
                firstPacket.type(),
                finalPayload,
                firstPacket.address(),
                firstPacket.port()
        );
        
        discard(packetId);
        
        return newPacket;
    }
    
    public void discard(int packetId) {
        assemblies.remove(packetId);
    }
    
    public List<Integer> getFilledPacketsId() {
        List<Integer> ids = new ArrayList<>();
        
        for (Map.Entry<Integer, Map<Short, NetworkThread.Packet>> entry : assemblies.entrySet()) {
            Map<Short, NetworkThread.Packet> fragmentMap = entry.getValue();
            
            if (fragmentMap.isEmpty()) continue;
            
            // Get total expected fragments from any fragment in the map
            NetworkThread.Packet firstPacket = fragmentMap.values().iterator().next();
            short total = firstPacket.total();
            
            // CRITICAL FIX: Check the size of the unique fragment map against the total expected.
            if (fragmentMap.size() == total) {
                ids.add(entry.getKey());
            }
        }
        
        return ids;
    }
    
    public void dropAll() {
        assemblies.clear();
    }
    
    public PacketResendData getMissingPackets(int id) {
        if (!assemblies.containsKey(id)) throw new NullPointerException("The packet id does not exist in the registry.");
        
        Map<Short, NetworkThread.Packet> receivedPackets = assemblies.get(id);
        
        // If the map is empty, we can't determine the total, but this shouldn't happen
        // if called by NetworkHandler, which checks for existence first.
        if (receivedPackets.isEmpty()) return null;
        
        short total = receivedPackets.values().iterator().next().total();
        List<Short> missingIndexes = new ArrayList<>();
        
        // Iterate through all expected indices (0 to total-1)
        for (short i = 0; i < total; i++) {
            // Check if the map contains a fragment for this specific index
            if (!receivedPackets.containsKey(i)) {
                missingIndexes.add(i);
            }
        }
        
        PacketResendData data = null;
        if (!missingIndexes.isEmpty()) {
            short[] d = new short[missingIndexes.size()];
            for (int j = 0; j < d.length; j++) {
                d[j] = missingIndexes.get(j);
            }
            data = new PacketResendData(id,d);
        }
        
        return data;
    }
    
    public InetAddress getAddress(int id) {
        if (assemblies.containsKey(id) && !assemblies.get(id).isEmpty()) {
            // Get the address from the first fragment in the assembly
            return assemblies.get(id).values().iterator().next().address();
        } else return null;
    }
    
    public boolean exists(int id) {
        return assemblies.containsKey(id) && !assemblies.get(id).isEmpty();
    }
    
    public NetworkThread.Packet getFirstPacket(int id) {
        Map<Short, NetworkThread.Packet> p = assemblies.get(id);
        if (p == null || p.isEmpty()) return null;
        
        // Return the fragment at index 0, which holds the original metadata
        return p.get((short) 0);
    }
    
    public void addPacket(NetworkThread.Packet packet) {
        addPacket(packet.id(),packet);
    }
}
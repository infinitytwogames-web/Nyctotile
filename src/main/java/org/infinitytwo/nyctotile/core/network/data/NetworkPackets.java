package org.infinitytwo.nyctotile.core.network.data;

public abstract class NetworkPackets {
    public static abstract class MPacket {}
    
    public static class PKey extends MPacket {
        public byte[] rsaKey;
        public PKey() {}
        
        public PKey(byte[] rsaKey) {
            this.rsaKey = rsaKey;
        }
    }
    
    public static class PUnencrypted extends MPacket {
        public String command;
        public PUnencrypted() {}
        
        public PUnencrypted(String command) {
            this.command = command;
        }
    }
    
    public static class PUserData extends MPacket {
        public String token;
        public String name;
        
        public PUserData(String token, String name) {
            this.token = token;
            this.name = name;
        }
        
        public PUserData() {}
    }
    
    public static class PConnection extends MPacket {
        public String name;
        public String uid;
        
        public PConnection() {
        }
        
        public PConnection(String name, String uid) {
            this.name = name;
            this.uid = uid;
        }
    }
    
    public static class PHandshakeComplete extends MPacket {}
    
    public static class EncryptedPacket extends MPacket {
        public byte[] encrypted;
        public int nonce;
        public short index;
        public short total;
        public int id;
        
        public EncryptedPacket(byte[] encrypted, int nonce, short index, short total, int id) {
            this.encrypted = encrypted;
            this.nonce = nonce;
            this.index = index;
            this.total = total;
            this.id = id;
        }
        
        public EncryptedPacket() {}
    }
    
    public static class Failure extends MPacket {
        public String msg;
        
        public Failure() {}
        
        public Failure(String msg) {
            this.msg = msg;
        }
    }
    
}
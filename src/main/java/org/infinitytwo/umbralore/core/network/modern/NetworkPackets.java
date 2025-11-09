package org.infinitytwo.umbralore.core.network.modern;

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
        
        public Failure() {
        }
        
        public Failure(String msg) {
            this.msg = msg;
        }
    }
    
    public static class NACK extends MPacket {
        public short[] indices;
        public int id;
        
        public NACK(short[] indices, int id) {
            this.indices = indices;
            this.id = id;
        }
        
        public NACK() {}
    }
}
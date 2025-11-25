package org.infinitytwo.nyctotile.core.data.io;

import org.infinitytwo.nyctotile.core.data.ChunkData;
import org.joml.Vector2i;

import java.io.*;
import java.nio.ByteBuffer;

public final class ChunkSerializer {
    public static byte[] serialize(ChunkData data) throws IOException {
        ByteArrayOutputStream arrayOutStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(arrayOutStream);
        ByteBuffer buffer = ByteBuffer.allocate(data.getBlockIds().length * Integer.BYTES);

        for (int block : data.getBlockIds()) {
            buffer.putInt(block);
        }

        outStream.writeInt(data.getPosition().x);
        outStream.writeInt(data.getPosition().y);
        outStream.write(buffer.array());

        outStream.flush();
        return arrayOutStream.toByteArray();
    }

    public static ChunkData deserialize(byte[] data) throws IOException {
        ByteArrayInputStream arrayInputStream = new ByteArrayInputStream(data);
        DataInputStream inStream = new DataInputStream(arrayInputStream);

        int x = inStream.readInt(), y = inStream.readInt();
        ChunkData chunk = new ChunkData(new Vector2i(x,y));
        int[] blocks = new int[chunk.getBlockIds().length];

        for (int i = 0; i < chunk.getBlockIds().length; i++) {
            blocks[i] = inStream.readInt();
        }

        chunk.modify(blocks);
        return chunk;
    }
}

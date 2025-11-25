package org.infinitytwo.nyctotile.core.model;

import org.infinitytwo.nyctotile.core.data.buffer.NFloatBuffer;
import org.infinitytwo.nyctotile.core.registry.Registerable;
import org.joml.Vector3i;

import java.nio.FloatBuffer;

public class Model implements AutoCloseable, Registerable {
    protected NFloatBuffer vertices = new NFloatBuffer(), normals = new NFloatBuffer();
    private final String id;

    public Model(String id) {
        this.id = id;
    }

    public void vertex(float x, float y, float z) {
        vertices.put(x);
        vertices.put(y);
        vertices.put(z);
    }

    public float[] toArray() {
        return vertices.array();
    }

    public void uv(float x, float y, float brightness) {
        vertices.put(x); vertices.put(y);
        vertices.put(brightness);
    }

    public void normal(float x, float y, float z) {
        normals.put(x);normals.put(y);normals.put(z);
    }

    public FloatBuffer getVertices() {
        return vertices.getBuffer();
    }

    public void getVertices(NFloatBuffer buffer) {
        buffer.put(getVertices());
    }

    public void getVertices(TextureAtlas atlas, int textureCoords, Vector3i pos,  NFloatBuffer data) {
        getVertices(atlas.getUVCoords(textureCoords),pos,data);
    }

    public void getVertices(float[] uvs, Vector3i pos, NFloatBuffer data) {
        getVertices(uvs,pos.x, pos.y, pos.z, data);
    }

    public void getVertices(float[] uvs, int x, int y, int z, NFloatBuffer data) {
        // [ x, y, z, u, v, brightness ]
        int vertexCount = vertices.size() / 6;
        for (int i = 0; i < vertexCount; i++) {
            data.put(vertices.get(i * 6) + x);     // x
            data.put(vertices.get(i * 6 + 1) + y); // y
            data.put(vertices.get(i * 6 + 2) + z); // z

            float uOrig = vertices.get(i * 6 + 3);
            float vOrig = vertices.get(i * 6 + 4);
            float u = uvs[0] + uOrig * (uvs[2] - uvs[0]);
            float v = uvs[1] + vOrig * (uvs[3] - uvs[1]);

            data.put(u);
            data.put(v);
            data.put(vertices.get(i * 6 + 5)); // brightness
        }
    }

    public void getVertices(TextureAtlas atlas, int textureIndex, int x, int y, int z, NFloatBuffer buffer) {
        getVertices(atlas.getUVCoords(textureIndex),x,y,z,buffer);
    }

    public void setVertices(NFloatBuffer buffer) {
        vertices.close();
        vertices = buffer;
    }

    public void setVertices(FloatBuffer buffer) {
        vertices.reset();
        vertices.put(buffer);
    }

    public void cleanup() {
        vertices.close();
        normals.close();
    }

    @Override
    public void close() {
        cleanup();
    }

    public NFloatBuffer getVerticesBuffer() {
        return vertices;
    }

    @Override
    public String getId() {
        return "";
    }
}

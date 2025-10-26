package org.infinitytwo.umbralore.core.model.builder;

import org.infinitytwo.umbralore.core.model.Model;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;

import java.io.File;
import java.io.IOException;

import static org.lwjgl.assimp.Assimp.*;

public class ModelImporter {
    public static Model loadModel(String path) throws IOException {
        File file = new File(path);
        if (!file.exists())
            throw new IOException("File Not Found: \"" + path + "\"");

        int flags = aiProcess_Triangulate | aiProcess_FlipUVs | aiProcess_GenNormals | aiProcess_JoinIdenticalVertices;
        AIScene scene = aiImportFile(path, flags);
        if (scene == null || scene.mNumMeshes() == 0)
            throw new IOException("Failed to load model: \"" + path + "\" Error: " + aiGetErrorString());

        AIMesh mesh = AIMesh.create(scene.mMeshes().get(0));
        Model model = new Model(file.getName());

        AIVector3D.Buffer vertices = mesh.mVertices();
        AIVector3D.Buffer texCoords = mesh.mTextureCoords(0);
        AIVector3D.Buffer normals = mesh.mNormals();

        for (int i = 0; i < mesh.mNumVertices(); i++) {
            AIVector3D pos = vertices.get(i);
            AIVector3D uv = texCoords != null ? texCoords.get(i) : null;
            AIVector3D normal = normals != null? normals.get(i) : null;

            model.vertex(pos.x(), pos.y(), pos.z());
            float brightness = normal != null? Math.max(0.2f, normal.y() * 0.8f + 0.2f) : 0.8f;
            if (normal != null) model.normal(normal.x(),normal.y(),normal.z()); // Just in case

            if (uv != null)
                model.uv(uv.x(), uv.y(),brightness);
            else
                model.uv(0f, 0f,brightness); // default UV if missing
        }

        aiFreeScene(scene);
        return model;
    }
}

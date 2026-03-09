package net.benbenmc.records;

import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.nio.file.Paths;

public record ClientJar(
        String id,
        String sha1,
        int size,
        String url
) implements Downloadable {
    public static ClientJar fromJsonObject(JsonObject clientJar, String id) {
        return new ClientJar(
                id,
                clientJar.get("sha1").getAsString(),
                clientJar.get("size").getAsInt(),
                clientJar.get("url").getAsString()
        );
    }

    @Override
    public Path targetFile(String rootDir) {
        return Paths.get(rootDir, "versions", id(), id() + ".jar");
    }
}

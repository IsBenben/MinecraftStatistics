package net.benbenmc.records;

import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.nio.file.Paths;

public record Version(
        String id,
        String type,
        String url,
        String time,
        String releaseTime,
        String sha1,
        int complianceLevel
) implements Downloadable {
    public static Version fromJsonObject(JsonObject version) {
        return new Version(
            version.get("id").getAsString(),
            version.get("type").getAsString(),
            version.get("url").getAsString(),
            version.get("time").getAsString(),
            version.get("releaseTime").getAsString(),
            version.get("sha1").getAsString(),
            version.get("complianceLevel").getAsInt()
        );
    }

    @Override
    public Path targetFile(String rootDir) {
        return Paths.get(rootDir, "versions", id(), id() + ".json");
    }
}

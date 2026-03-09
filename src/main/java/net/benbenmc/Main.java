package net.benbenmc;

import com.google.gson.*;
import net.benbenmc.records.ClientJar;
import net.benbenmc.records.Version;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    public static final HttpClient client = HttpClient.newBuilder()
            .build();

    public static final String rootDir = "run";

    public static final Set<String> excludeVersions = Set.of(
            "25w14craftmine",
            "24w14potato",
            "23w13a_or_b",
            "22w13oneblockatatime",
            "20w14infinite",
            "3D Shareware v1.34",
            "1.RV-Pre1"
    );

    // 白名单模式：如果设置了环境变量 RUN_VERSIONS，则只处理列表中的版本
    private static final Set<String> whiteList;

    static {
        String env = System.getenv("RUN_VERSIONS");
        if (env != null && !env.isBlank()) {
            String[] parts = env.split(",");
            whiteList = new HashSet<>();
            for (String part : parts) {
                whiteList.add(part.trim());
            }
            System.out.println("白名单模式已启用，将处理版本: " + whiteList);
        } else {
            whiteList = null;
        }
    }

    private static boolean shouldIgnoreVersion(String versionId) {
        if (whiteList == null) {
            return excludeVersions.contains(versionId);
        } else {
            return !whiteList.contains(versionId);
        }
    }

    public static void main(String[] args) {
        try {
            List<Version> versions = getVersions();
            if (versions == null) return;
            downloadVersionJson(versions);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        List<ClientJar> clientJars = getClientJars();
        downloadClientJars(clientJars);
        JsonArray jsonElements = statisticsClientJars();
        store(Paths.get(rootDir, "result.json"), jsonElements);
    }

    private static @Nullable List<Version> getVersions() throws IOException, InterruptedException {
        HttpRequest manifestRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"))
                .build();
        HttpResponse<String> manifestResponse = client.send(manifestRequest, HttpResponse.BodyHandlers.ofString());

        if (manifestResponse.statusCode() != 200) {
            System.err.println("获取版本清单失败，HTTP 状态码: " + manifestResponse.statusCode());
            return null;
        }

        JsonObject jsonRoot = JsonParser.parseString(manifestResponse.body()).getAsJsonObject();
        JsonArray jsonVersions = jsonRoot.getAsJsonArray("versions");
        List<Version> versions = new ArrayList<>();
        for (JsonElement jsonVersion : jsonVersions) {
            Version version = Version.fromJsonObject(jsonVersion.getAsJsonObject());
            versions.add(version);
        }
        return versions;
    }

    private static void downloadVersionJson(List<Version> versions) {
        System.out.println("开始检查/下载版本JSON文件...");
        int succeed = 0;
        for (Version version : versions) {
            try {
                if (version.download(client, rootDir)) {
                    succeed++;
                }
            } catch (IOException | InterruptedException e) {
                System.err.printf("下载 %s 时出错: %s%n", version, e.getMessage());
            }
        }
        System.out.printf("完成！已完成 %d 个文件%n", succeed);
    }

    private static @Nullable List<ClientJar> getClientJars() {
        // 遍历已下载的版本JSON，解析client下载信息
        System.out.println("\n开始解析版本JSON中的client信息...");
        Path versionsDir = Paths.get(rootDir, "versions");
        if (Files.exists(versionsDir) && Files.isDirectory(versionsDir)) {
            try (var stream = Files.newDirectoryStream(versionsDir, Files::isDirectory)) {
                List<ClientJar> clientJars = new ArrayList<>();
                for (Path versionDir : stream) {
                    String versionId = versionDir.getFileName().toString();
                    if (shouldIgnoreVersion(versionId)) {
                        System.out.printf("跳过 %s: 位于排除版本列表中%n", versionId);
                        continue;
                    }
                    Path jsonFile = versionDir.resolve(versionId + ".json");
                    if (!Files.exists(jsonFile)) {
                        System.err.printf("警告: 版本 %s 的JSON文件不存在: %s%n", versionId, jsonFile);
                        continue;
                    }

                    String content = Files.readString(jsonFile);
                    JsonObject root = JsonParser.parseString(content).getAsJsonObject();
                    JsonObject jsonDownload = root
                            .getAsJsonObject("downloads")
                            .getAsJsonObject("client");
                    ClientJar clientJar = ClientJar.fromJsonObject(jsonDownload, versionId);
                    clientJars.add(clientJar);
                }
                return clientJars;
            } catch (IOException e) {
                System.err.println("遍历versions目录失败: " + e.getMessage());
                return null;
            }
        } else {
            System.out.println("versions目录不存在，请先下载版本JSON。");
            return null;
        }
    }

    private static void downloadClientJars(List<ClientJar> clientJars) {
        System.out.println("开始检查/下载client.jar文件...");
        int succeed = 0;
        for (ClientJar clientJar : clientJars) {
            try {
                if (clientJar.download(client, rootDir)) {
                    succeed++;
                }
            } catch (IOException | InterruptedException e) {
                System.err.printf("下载 %s 时出错: %s%n", clientJar, e.getMessage());
            }
        }
        System.out.printf("完成！已完成 %d 个文件%n", succeed);
    }

    private static @Nullable JsonArray statisticsClientJars() {
        // 遍历已下载的版本JSON，解析client下载信息
        Gson gson = new GsonBuilder().create();
        System.out.println("\n开始解析版本jar信息...");
        Path versionsDir = Paths.get(rootDir, "versions");
        if (Files.exists(versionsDir) && Files.isDirectory(versionsDir)) {
            try (var stream = Files.newDirectoryStream(versionsDir, Files::isDirectory)) {
                JsonArray result = new JsonArray();
                for (Path versionDir : stream) {
                    String versionId = versionDir.getFileName().toString();
                    if (shouldIgnoreVersion(versionId)) {
                        System.out.printf("跳过 %s: 位于排除版本列表中%n", versionId);
                        continue;
                    }
                    Path jsonFile = versionDir.resolve(versionId + ".jar");
                    if (!Files.exists(jsonFile)) {
                        System.err.printf("警告: 版本 %s 的JAR文件不存在: %s%n", versionId, jsonFile);
                        continue;
                    }

                    JarStatistics statistics = JarStatistics.analyze(jsonFile);
                    JsonObject jsonStatistics = statistics.toJsonObject();
                    jsonStatistics.addProperty("versionId", versionId);
                    System.out.println(gson.toJson(jsonStatistics));
                    result.add(jsonStatistics);
                }
                return result;
            } catch (IOException e) {
                System.err.println("遍历versions目录失败: " + e.getMessage());
                return null;
            }
        } else {
            System.out.println("versions目录不存在，请先下载版本JSON。");
            return null;
        }
    }

    public static void store(Path targetFile, JsonElement jsonElement) {
        try {
            Files.createDirectories(targetFile.getParent()); // 确保 run 目录存在
            String jsonString = new Gson().toJson(jsonElement); // 紧凑模式
            Files.writeString(targetFile, jsonString);
            System.out.printf("统计完成！文件已保存至%s。%n", targetFile.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("保存结果文件失败: " + e.getMessage());
        }
    }
}
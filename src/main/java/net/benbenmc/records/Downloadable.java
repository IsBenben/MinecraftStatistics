package net.benbenmc.records;

import net.benbenmc.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Downloadable {
    String id();
    String sha1();
    String url();
    Path targetFile(String rootDir);

    default boolean download(HttpClient client, String rootDir)
            throws IOException, InterruptedException {
        Path targetFile = targetFile(rootDir);
        String sha1 = sha1();
        String url = url();
        String id = id();

        if (Files.exists(targetFile)) {
            try {
                String actualSha1 = Utils.computeSha1(targetFile);
                if (actualSha1.equalsIgnoreCase(sha1)) {
                    System.out.printf("跳过 %s: 已存在且哈希匹配%n", id);
                    return true;
                } else {
                    System.out.printf("  %s: 哈希不匹配 (期望 %s, 实际 %s)，重新下载%n", id, sha1, actualSha1);
                }
            } catch (IOException e) {
                System.err.printf("  %s: 无法计算本地文件哈希 (%s)，将重新下载%n", id, e.getMessage());
            }
        }

        System.out.printf("正在下载: %s (%s)%n", id, url);
        HttpRequest versionRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .build();
        HttpResponse<byte[]> versionResponse = client.send(versionRequest,
                HttpResponse.BodyHandlers.ofByteArray());

        if (versionResponse.statusCode() != 200) {
            System.err.printf("  跳过 %s: HTTP %d%n", id, versionResponse.statusCode());
            return false;
        }

        Files.createDirectories(targetFile.getParent());
        Files.write(targetFile, versionResponse.body());
        System.out.printf("  已保存: %s%n", targetFile);
        return true;
    }
}

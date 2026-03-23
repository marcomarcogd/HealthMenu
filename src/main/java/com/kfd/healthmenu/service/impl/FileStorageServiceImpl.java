package com.kfd.healthmenu.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements com.kfd.healthmenu.service.FileStorageService {

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Path directory = ensureDateDirectory();
            String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "file.bin";
            String extension = resolveExtension(original, null);
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = directory.resolve(fileName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return toWebPath(target);
        } catch (IOException ex) {
            throw new IllegalStateException("文件上传失败", ex);
        }
    }

    @Override
    public String downloadToLocal(String fileUrl, String extensionHint) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .GET()
                    .uri(URI.create(fileUrl))
                    .timeout(Duration.ofSeconds(60))
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("下载远程图片失败，状态码：" + response.statusCode());
            }
            Path directory = ensureDateDirectory();
            String contentType = response.headers().firstValue("Content-Type").orElse(null);
            String extension = resolveExtension(fileUrl, extensionHint);
            if (!StringUtils.hasText(extension) || ".bin".equals(extension)) {
                extension = resolveExtensionByContentType(contentType);
            }
            String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
            Path target = directory.resolve(fileName);
            try (InputStream inputStream = response.body()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return toWebPath(target);
        } catch (Exception ex) {
            throw new IllegalStateException("下载并保存图片失败", ex);
        }
    }

    private Path ensureDateDirectory() throws IOException {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path directory = Paths.get(uploadDir, datePath.split("/")).toAbsolutePath().normalize();
        Files.createDirectories(directory);
        return directory;
    }

    private String toWebPath(Path target) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        String relative = root.relativize(target.toAbsolutePath().normalize()).toString().replace("\\", "/");
        return "/uploads/" + relative;
    }

    private String resolveExtension(String source, String extensionHint) {
        if (StringUtils.hasText(extensionHint)) {
            return extensionHint.startsWith(".") ? extensionHint : "." + extensionHint;
        }
        if (!StringUtils.hasText(source)) {
            return ".bin";
        }
        String clean = source;
        int queryIndex = clean.indexOf('?');
        if (queryIndex >= 0) {
            clean = clean.substring(0, queryIndex);
        }
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0) {
            clean = clean.substring(lastSlash + 1);
        }
        if (clean.contains(".")) {
            return clean.substring(clean.lastIndexOf('.'));
        }
        return ".bin";
    }

    private String resolveExtensionByContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return ".png";
        }
        String guessed = URLConnection.guessContentTypeFromName("x." + contentType.substring(contentType.lastIndexOf('/') + 1));
        if (guessed != null && guessed.contains("png")) {
            return ".png";
        }
        if (contentType.contains("jpeg") || contentType.contains("jpg")) {
            return ".jpg";
        }
        if (contentType.contains("png")) {
            return ".png";
        }
        if (contentType.contains("webp")) {
            return ".webp";
        }
        return ".png";
    }
}

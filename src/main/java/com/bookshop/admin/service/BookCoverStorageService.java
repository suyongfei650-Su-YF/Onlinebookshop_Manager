package com.bookshop.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class BookCoverStorageService {

    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList(".jpg", ".jpeg", ".png", ".gif", ".webp"));

    private final Path coverDirectory;

    public BookCoverStorageService(
            ServletContext servletContext,
            @Value("${app.book-covers-dir:}") String configuredDir) throws IOException {
        this.coverDirectory = resolveDirectory(servletContext, configuredDir);
        Files.createDirectories(coverDirectory);
    }

    public Path getCoverDirectory() {
        return coverDirectory;
    }

    public String save(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择图片文件");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("图片不能超过 5MB");
        }
        String ext = resolveExtension(file);
        String filename = "upload_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        Path target = coverDirectory.resolve(filename).normalize();
        if (!target.startsWith(coverDirectory)) {
            throw new IllegalArgumentException("非法文件名");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return "/book-covers/" + filename;
    }

    private static String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null) {
            String lower = original.toLowerCase(Locale.ROOT);
            for (String ext : ALLOWED_EXT) {
                if (lower.endsWith(ext)) {
                    return ext;
                }
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            switch (contentType.toLowerCase(Locale.ROOT)) {
                case "image/jpeg":
                    return ".jpg";
                case "image/png":
                    return ".png";
                case "image/gif":
                    return ".gif";
                case "image/webp":
                    return ".webp";
                default:
                    break;
            }
        }
        throw new IllegalArgumentException("仅支持 JPG、PNG、GIF、WebP 格式");
    }

    private static Path resolveDirectory(ServletContext servletContext, String configuredDir) {
        if (configuredDir != null && !configuredDir.trim().isEmpty()) {
            return Paths.get(configuredDir.trim()).toAbsolutePath().normalize();
        }
        Path devWebapp = Paths.get(System.getProperty("user.dir"), "src", "main", "webapp", "book-covers");
        if (Files.isDirectory(devWebapp.getParent())) {
            return devWebapp.toAbsolutePath().normalize();
        }
        if (servletContext != null) {
            String realPath = servletContext.getRealPath("/book-covers");
            if (realPath != null && !realPath.isEmpty()) {
                return Paths.get(realPath).toAbsolutePath().normalize();
            }
        }
        return Paths.get(System.getProperty("user.home"), "bookshop-book-covers").toAbsolutePath().normalize();
    }
}

package com.example.pdfreview.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class KindleService {

    private static final Logger log = LoggerFactory.getLogger(KindleService.class);
    private static final List<String> EBOOK_EXTENSIONS = List.of(".epub", ".mobi", ".azw", ".azw3", ".kfx", ".azw.md");

    private final Path kindleContentPath;

    public KindleService(@Value("${app.kindle.content-path:}") String configuredPath) {
        this.kindleContentPath = resolveKindlePath(configuredPath);
        try {
            log.info("Kindle content path resolved to: {} (exists={}, isDirectory={})",
                    kindleContentPath.toAbsolutePath(),
                    Files.exists(kindleContentPath),
                    Files.isDirectory(kindleContentPath));
        } catch (Exception e) {
            log.warn("Could not verify Kindle path at startup: {}", e.getMessage());
        }
    }

    private Path resolveKindlePath(String configured) {
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured.trim());
        }
        // Default: Windows "Documents\My Kindle Content"
        String userHome = System.getProperty("user.home");
        return Path.of(userHome, "Documents", "My Kindle Content");
    }

    /**
     * Lists ebook files in the Kindle content folder, sorted by last modified (newest first).
     * Scans both the root folder and one level of subdirectories (e.g. B0GQ3LGCDQ_EBOK/).
     */
    public List<KindleBookInfo> listBooks() {
        log.debug("listBooks() called for path: {}", kindleContentPath.toAbsolutePath());

        if (!Files.exists(kindleContentPath) || !Files.isDirectory(kindleContentPath)) {
            log.warn("Kindle content path does not exist or is not a directory: {}", kindleContentPath.toAbsolutePath());
            return List.of();
        }

        try (Stream<Path> entries = Files.list(kindleContentPath)) {
            List<KindleBookInfo> books = entries
                    .flatMap(this::collectEbookPaths)
                    .map(this::toBookInfo)
                    .sorted(Comparator.comparing(KindleBookInfo::lastModified).reversed())
                    .toList();
            log.info("Found {} Kindle book(s) in {}", books.size(), kindleContentPath.toAbsolutePath());
            books.forEach(b -> log.debug("  - {} ({})", b.fileName(), b.absolutePath()));
            return books;
        } catch (IOException e) {
            log.error("Failed to list Kindle books: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to list Kindle books: " + e.getMessage(), e);
        }
    }

    private Stream<Path> collectEbookPaths(Path entry) {
        if (Files.isRegularFile(entry) && isEbookFile(entry)) {
            return Stream.of(entry);
        }
        if (Files.isDirectory(entry)) {
            try (Stream<Path> sub = Files.list(entry)) {
                return sub
                        .filter(Files::isRegularFile)
                        .filter(this::isEbookFile)
                        .min(this::compareEbookPreference)
                        .stream();
            } catch (IOException e) {
                log.warn("Could not list subdirectory {}: {}", entry.getFileName(), e.getMessage());
                return Stream.empty();
            }
        }
        return Stream.empty();
    }

    /** Prefer .azw over .azw.md: .azw.md in KFX is binary metadata, not readable text. */
    private int compareEbookPreference(Path a, Path b) {
        String aName = a.getFileName().toString().toLowerCase();
        String bName = b.getFileName().toString().toLowerCase();
        boolean aMd = aName.endsWith(".azw.md");
        boolean bMd = bName.endsWith(".azw.md");
        if (aMd && !bMd) return 1;
        if (!aMd && bMd) return -1;
        return 0;
    }

    public Path getKindleContentPath() {
        return kindleContentPath;
    }

    public boolean isPathUnderKindleFolder(Path path) {
        try {
            return path != null && path.startsWith(kindleContentPath.toAbsolutePath().normalize());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isEbookFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return EBOOK_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private KindleBookInfo toBookInfo(Path path) {
        try {
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            return new KindleBookInfo(
                    path.getFileName().toString(),
                    path.toAbsolutePath().toString(),
                    lastModified
            );
        } catch (IOException e) {
            return new KindleBookInfo(path.getFileName().toString(), path.toAbsolutePath().toString(), 0L);
        }
    }

    public record KindleBookInfo(String fileName, String absolutePath, long lastModified) {}
}

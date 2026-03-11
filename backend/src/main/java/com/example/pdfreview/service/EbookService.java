package com.example.pdfreview.service;

import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class EbookService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final int MAX_TEXT_LENGTH = 1_000_000; // ~1M chars for summarization

    private final Tika tika = new Tika();

    /**
     * Extracts plain text from ebook files (EPUB, MOBI, AZW, AZW3, AZW.MD).
     * Kindle DRM-protected files may fail to extract; returns null or partial content.
     * .azw.md files (Kindle export) are read as plain UTF-8 text.
     */
    public ExtractedEbook extractText(Path path) {
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalArgumentException("File not found: " + path);
        }
        try {
            long size = Files.size(path);
            if (size > MAX_FILE_SIZE) {
                throw new IllegalArgumentException("File too large (max 50 MB): " + path.getFileName());
            }

            String fileName = path.getFileName().toString().toLowerCase();
            if (fileName.endsWith(".azw.md")) {
                Path effectivePath = resolveAzwMd(path);
                return extractFromAzwMdOrEbook(effectivePath, path);
            }

            String contentType = tika.detect(path);
            if (!isSupportedEbookType(contentType, path)) {
                throw new IllegalArgumentException("Unsupported ebook format: " + path.getFileName());
            }

            var handler = new BodyContentHandler(MAX_TEXT_LENGTH);
            var metadata = new Metadata();
            var parser = new AutoDetectParser();

            try (InputStream stream = Files.newInputStream(path)) {
                parser.parse(stream, handler, metadata);
            }

            String text = handler.toString();
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Could not extract text. The file may be DRM-protected.");
            }

            String title = metadata.get("title");
            String author = metadata.get("dc:creator");
            if (author == null) {
                author = metadata.get("Author");
            }

            return new ExtractedEbook(
                    text.trim(),
                    title != null && !title.isBlank() ? title : null,
                    author != null && !author.isBlank() ? author : null
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            String hint = isCharsetOrEncodingError(e)
                    ? " The file may have encoding issues or be DRM-protected. Try exporting as .azw.md from Kindle, or use an EPUB/MOBI from a different source."
                    : "";
            throw new IllegalStateException("Failed to extract text from ebook: " + e.getMessage() + hint, e);
        }
    }

    /**
     * .azw.md can be either (a) plain-text markdown export, or (b) KFX binary metadata.
     * If binary, use the main .azw file in the same folder instead.
     */
    private Path resolveAzwMd(Path azwMdPath) throws IOException {
        if (!isBinaryFile(azwMdPath)) {
            return azwMdPath;
        }
        Path parent = azwMdPath.getParent();
        if (parent == null) {
            return azwMdPath;
        }
        String folderName = parent.getFileName().toString();
        if (folderName.endsWith("_EBOK")) {
            Path mainAzw = parent.resolve(folderName + ".azw");
            if (Files.exists(mainAzw) && Files.isRegularFile(mainAzw)) {
                return mainAzw;
            }
        }
        return azwMdPath;
    }

    private boolean isBinaryFile(Path path) throws IOException {
        byte[] head = Files.readAllBytes(path);
        int toCheck = Math.min(2048, head.length);
        for (int i = 0; i < toCheck; i++) {
            if (head[i] == 0) {
                return true;
            }
        }
        return false;
    }

    private ExtractedEbook extractFromAzwMdOrEbook(Path effectivePath, Path originalPath) throws Exception {
        if (effectivePath.getFileName().toString().toLowerCase().endsWith(".azw.md")) {
            return extractFromPlainTextMarkdown(effectivePath);
        }
        return extractWithTika(effectivePath);
    }

    private ExtractedEbook extractFromPlainTextMarkdown(Path path) throws IOException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE);
        String text = decoder.decode(ByteBuffer.wrap(Files.readAllBytes(path))).toString();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Markdown file is empty.");
        }
        return new ExtractedEbook(text.trim(), null, null);
    }

    private ExtractedEbook extractWithTika(Path path) throws Exception {
        var handler = new BodyContentHandler(MAX_TEXT_LENGTH);
        var metadata = new Metadata();
        var parser = new AutoDetectParser();
        try (InputStream stream = Files.newInputStream(path)) {
            parser.parse(stream, handler, metadata);
        }
        String text = handler.toString();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Could not extract text. The file may be DRM-protected.");
        }
        String title = metadata.get("title");
        String author = metadata.get("dc:creator");
        if (author == null) {
            author = metadata.get("Author");
        }
        return new ExtractedEbook(
                text.trim(),
                title != null && !title.isBlank() ? title : null,
                author != null && !author.isBlank() ? author : null
        );
    }

    private boolean isCharsetOrEncodingError(Throwable e) {
        if (e instanceof MalformedInputException) return true;
        String msg = e.getMessage();
        if (msg != null && (msg.contains("Input length") || msg.contains("MalformedInput") || msg.contains("charset"))) {
            return true;
        }
        return e.getCause() != null && isCharsetOrEncodingError(e.getCause());
    }

    private boolean isSupportedEbookType(String contentType, Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return (contentType != null && (
                contentType.contains("epub") ||
                contentType.contains("mobi") ||
                contentType.contains("x-mobipocket") ||
                contentType.contains("application/x-mobipocket")
        )) || name.endsWith(".epub") || name.endsWith(".mobi") || name.endsWith(".azw") || name.endsWith(".azw3");
    }

    public record ExtractedEbook(String text, String title, String author) {
        public boolean hasText() {
            return text != null && !text.isBlank();
        }
    }
}

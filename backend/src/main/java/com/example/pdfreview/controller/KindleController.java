package com.example.pdfreview.controller;

import com.example.pdfreview.dto.DocumentDetailResponse;
import com.example.pdfreview.dto.ImportKindleRequest;
import com.example.pdfreview.service.DocumentService;
import com.example.pdfreview.service.KindleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kindle")
public class KindleController {

    private static final Logger log = LoggerFactory.getLogger(KindleController.class);

    private final KindleService kindleService;
    private final DocumentService documentService;

    public KindleController(KindleService kindleService, DocumentService documentService) {
        this.kindleService = kindleService;
        this.documentService = documentService;
    }

    @GetMapping("/books")
    public KindleBooksResponse listBooks() {
        log.debug("GET /api/kindle/books requested");
        List<KindleService.KindleBookInfo> books = kindleService.listBooks();
        String contentPath = kindleService.getKindleContentPath().toAbsolutePath().toString();
        log.debug("Returning {} book(s) from path: {}", books.size(), contentPath);
        return new KindleBooksResponse(contentPath, books);
    }

    @PostMapping("/import")
    public DocumentDetailResponse importBook(@RequestBody ImportKindleRequest request) {
        return documentService.importKindleFromPath(
                request.filePath(),
                request.bookTitle(),
                request.author(),
                request.bookSize(),
                request.amazonUrl()
        );
    }

    public record KindleBooksResponse(String contentPath, List<KindleService.KindleBookInfo> books) {}
}

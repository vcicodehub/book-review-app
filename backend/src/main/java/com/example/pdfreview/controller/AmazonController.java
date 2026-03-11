package com.example.pdfreview.controller;

import com.example.pdfreview.dto.AmazonBookInfo;
import com.example.pdfreview.service.AmazonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/amazon")
public class AmazonController {

    private final AmazonService amazonService;

    public AmazonController(AmazonService amazonService) {
        this.amazonService = amazonService;
    }

    @GetMapping("/book-info")
    public AmazonBookInfo getBookInfo(@RequestParam("url") String url) throws IOException {
        return amazonService.fetchBookInfo(url);
    }
}

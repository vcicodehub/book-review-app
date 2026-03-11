package com.example.pdfreview.service;

import com.example.pdfreview.dto.AmazonBookInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Service
public class AmazonService {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public AmazonBookInfo fetchBookInfo(String urlString) throws IOException {
        String normalized = urlString.trim();
        if (!isValidAmazonUrl(normalized)) {
            throw new IllegalArgumentException("Invalid Amazon URL. Use a link like https://www.amazon.com/dp/...");
        }

        Document doc = Jsoup.connect(normalized)
                .userAgent(USER_AGENT)
                .referrer("https://www.google.com/")
                .timeout(10000)
                .followRedirects(true)
                .get();

        String title = extractTitle(doc);
        String author = extractAuthor(doc);
        String bookSize = extractBookSize(doc);

        return new AmazonBookInfo(
                title != null ? title.trim() : "",
                author != null ? author.trim() : "",
                bookSize != null ? bookSize.trim() : ""
        );
    }

    /** Strips colons, Unicode direction marks (RLM/LRM), and normalizes whitespace for label matching. */
    private String normalizeLabel(String raw) {
        if (raw == null) return "";
        return raw.replace(":", "")
                .replace("\u200E", "")
                .replace("\u200F", "")
                .trim();
    }

    private boolean isValidAmazonUrl(String url) {
        if (url == null || url.isBlank()) return false;
        try {
            URL u = new URL(url);
            String host = u.getHost().toLowerCase().replaceFirst("^www\\.", "");
            return host.equals("amazon.com") || host.endsWith(".amazon.com") ||
                    host.equals("amzn.to") || host.equals("a.co");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    private String extractTitle(Document doc) {
        Element titleEl = doc.selectFirst("#productTitle");
        if (titleEl != null) {
            return titleEl.text();
        }
        titleEl = doc.selectFirst("span[id=productTitle]");
        return titleEl != null ? titleEl.text() : null;
    }

    private String extractAuthor(Document doc) {
        Element byline = doc.selectFirst("#bylineInfo");
        if (byline != null) {
            String text = byline.text();
            if (text.startsWith("by ")) {
                return text.substring(3).trim();
            }
            Element authorLink = byline.selectFirst("a");
            return authorLink != null ? authorLink.text() : text;
        }
        Element contributor = doc.selectFirst("a.contributorNameID, a[data-asin][href*='/author/']");
        return contributor != null ? contributor.text() : null;
    }

    private String extractBookSize(Document doc) {
        // Try #detailBullets_feature_div (current Amazon format: ul.detail-bullet-list li)
        Element details = doc.selectFirst("#detailBullets_feature_div");
        if (details != null) {
            Elements items = details.select("li");
            for (Element item : items) {
                Element labelEl = item.selectFirst(".a-text-bold");
                if (labelEl != null) {
                    String label = normalizeLabel(labelEl.text());
                    if ("Dimensions".equalsIgnoreCase(label) || "Product Dimensions".equalsIgnoreCase(label)) {
                        Element valueSpan = item.select("span").last();
                        if (valueSpan != null) {
                            String value = valueSpan.text().trim();
                            if (!value.isEmpty()) return value;
                        }
                    }
                }
            }
        }
        // Try "Product Dimensions" in product details tables (older format)
        for (Element li : doc.select("table#productDetailsTable div.content ul li")) {
            Element b = li.selectFirst("b");
            if (b != null && "Product Dimensions".equals(b.text().trim().replace(":", ""))) {
                String full = li.text();
                String value = full.replaceFirst("(?i)Product Dimensions:?\\s*", "").trim();
                if (!value.isEmpty()) return value;
            }
        }
        // Try newer format: table rows with th for Product Dimensions
        for (Element tr : doc.select("table#productDetails_detailBullets_sections1 tr, table#productDetails_techSpec_section_1 tr")) {
            Element th = tr.selectFirst("th");
            if (th != null && "Product Dimensions".equals(th.text().trim().replace(":", ""))) {
                Element td = tr.selectFirst("td");
                if (td != null && td.hasText()) return td.text().trim();
            }
        }
        // Try generic product details table (various Amazon layouts)
        for (Element tr : doc.select("table.a-keyvalue tr")) {
            Element th = tr.selectFirst("th");
            if (th != null && "Product Dimensions".equals(th.text().trim().replace(":", ""))) {
                Element td = tr.selectFirst("td");
                if (td != null && td.hasText()) return td.text().trim();
            }
        }
        return null;
    }
}

package com.shop.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.shop.model.Product;
import com.shop.service.ProductService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductImageController {
    private final ProductService productService;

    public ProductImageController(ProductService productService) { this.productService = productService; }

    @GetMapping(value = "/{id}/image.svg", produces = "image/svg+xml")
    public ResponseEntity<String> image(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("image/svg+xml"))
                .cacheControl(CacheControl.noCache()).body(render(productService.getProductById(id)));
    }

    private String render(Product product) {
        String category = product.getCategory() == null ? "Everyday finds" : product.getCategory();
        String name = product.getName() == null ? "ShopEase find" : product.getName();
        String[] palette = palette(category, name);
        String icon = icon(category);
        int hash = stableHash(product.getId() + ":" + name);
        int x = 75 + Math.floorMod(hash, 71);
        int y = 62 + Math.floorMod(hash >>> 6, 53);
        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 320 240" role="img" aria-label="%s illustration">
                  <rect width="320" height="240" rx="16" fill="%s"/>
                  <circle cx="250" cy="42" r="56" fill="%s" opacity=".5"/>
                  <circle cx="%d" cy="%d" r="70" fill="#ffffff" opacity=".72"/>
                  <text x="160" y="118" text-anchor="middle" font-family="Arial,sans-serif" font-size="67" fill="%s">%s</text>
                  <rect x="60" y="158" width="200" height="42" rx="21" fill="#ffffff" opacity=".85"/>
                  <text x="160" y="184" text-anchor="middle" font-family="Arial,sans-serif" font-size="13" font-weight="600" fill="#24364b">%s</text>
                  <text x="160" y="220" text-anchor="middle" font-family="Arial,sans-serif" font-size="9" letter-spacing="2" fill="%s">SHOPEASE · %s</text>
                </svg>
                """.formatted(escape(name), palette[0], palette[1], x, y, palette[2], icon,
                escape(truncate(name, 26)), palette[2], escape(truncate(category.toUpperCase(), 24)));
    }

    private String[] palette(String category, String name) {
        int hue = Math.floorMod(stableHash(category + name), 360);
        return new String[]{"hsl(" + hue + ", 32%, 92%)", "hsl(" + Math.floorMod(hue + 28, 360) + ", 48%, 78%)", "hsl(" + Math.floorMod(hue + 205, 360) + ", 28%, 38%)"};
    }

    private String icon(String category) {
        String value = category.toLowerCase();
        if (value.contains("mobile") || value.contains("phone")) return "▯";
        if (value.contains("computer") || value.contains("laptop")) return "▣";
        if (value.contains("audio") || value.contains("headphone")) return "♫";
        if (value.contains("shoe")) return "◒";
        if (value.contains("watch")) return "◷";
        if (value.contains("fashion") || value.contains("cloth")) return "✧";
        if (value.contains("home") || value.contains("furniture")) return "⌂";
        if (value.contains("beauty")) return "✿";
        if (value.contains("sport") || value.contains("fitness")) return "↗";
        if (value.contains("book") || value.contains("stationery")) return "▤";
        if (value.contains("toy") || value.contains("game")) return "✳";
        if (value.contains("travel")) return "✈";
        return "⌁";
    }

    private int stableHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.nio.ByteBuffer.wrap(digest).getInt();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private String truncate(String value, int max) { return value.length() <= max ? value : value.substring(0, max - 1) + "…"; }
    private String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;"); }
}


package com.shop.controller;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shop.model.Product;
import com.shop.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // GET: View all products
    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/browse")
    public Map<String, Object> browse(@RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "false") boolean inStock,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        if (page < 0 || size < 1 || size > 60) throw new IllegalArgumentException("Invalid page or page size");
        if (minPrice != null && minPrice < 0 || maxPrice != null && maxPrice < 0 ||
                minPrice != null && maxPrice != null && minPrice > maxPrice)
            throw new IllegalArgumentException("Price range is invalid");
        String term = q.trim().toLowerCase(Locale.ROOT);
        String selectedCategory = category.trim().toLowerCase(Locale.ROOT);
        List<Product> filtered = productService.getAllProducts().stream()
                .filter(p -> term.isEmpty() || p.getName().toLowerCase(Locale.ROOT).contains(term))
                .filter(p -> selectedCategory.isEmpty() || p.getCategory() != null && p.getCategory().toLowerCase(Locale.ROOT).equals(selectedCategory))
                .filter(p -> minPrice == null || p.getPrice() >= minPrice)
                .filter(p -> maxPrice == null || p.getPrice() <= maxPrice)
                .filter(p -> !inStock || p.getStock() > 0).toList();
        java.util.Comparator<Product> comparator = switch (sort) {
            case "price-asc" -> java.util.Comparator.comparing(Product::getPrice);
            case "price-desc" -> java.util.Comparator.comparing(Product::getPrice).reversed();
            case "name" -> java.util.Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER);
            case "newest" -> java.util.Comparator.comparing(Product::getCreatedAt,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()));
            case "featured" -> java.util.Comparator.comparing(Product::getRating,
                    java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder()));
            default -> throw new IllegalArgumentException("Unsupported sort option: " + sort);
        };
        List<Product> sorted = filtered.stream().sorted(comparator).toList();
        int start = (int) Math.min((long) page * size, sorted.size());
        int end = Math.min(start + size, sorted.size());
        return Map.of("products", sorted.subList(start, end), "total", sorted.size(), "page", page, "size", size);
    }

    // GET: View one product
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // POST: Add a product
    @PostMapping
    public ResponseEntity<Product> addProduct(
            @Valid @RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.addProduct(product));
    }

    // PUT: Update a product
    @PutMapping("/{id}")
    public Product updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product) {
        return productService.updateProduct(id, product);
    }

    // DELETE: Delete a product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // GET: Search by name
    @GetMapping("/search")
    public List<Product> searchProducts(@RequestParam String name) {
        return productService.searchProducts(name);
    }

    // GET: Filter by category
    @GetMapping("/category/{category}")
    public List<Product> getProductsByCategory(
            @PathVariable String category) {
        return productService.getProductsByCategory(category);
    }
}

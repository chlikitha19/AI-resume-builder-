
package com.shop.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.shop.model.Product;
import com.shop.repository.ProductRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Get all products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // Get product by ID
    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Product not found: " + id));
    }

    // Add a product
    public Product addProduct(Product product) {
        product.setId(null);
        return productRepository.save(product);
    }

    // Update a product
    public Product updateProduct(Long id, Product updatedProduct) {
        Product existing = getProductById(id);

        existing.setName(updatedProduct.getName());
        existing.setDescription(updatedProduct.getDescription());
        existing.setPrice(updatedProduct.getPrice());
        if (updatedProduct.getOriginalPrice() != null) existing.setOriginalPrice(updatedProduct.getOriginalPrice());
        existing.setStock(updatedProduct.getStock());
        existing.setCategory(updatedProduct.getCategory());
        if (updatedProduct.getBrand() != null) existing.setBrand(updatedProduct.getBrand());
        if (updatedProduct.getImageUrl() != null) existing.setImageUrl(updatedProduct.getImageUrl());
        if (updatedProduct.getRating() != null) existing.setRating(updatedProduct.getRating());
        if (updatedProduct.getReviewCount() != null) existing.setReviewCount(updatedProduct.getReviewCount());

        return productRepository.save(existing);
    }

    // Delete a product
    public void deleteProduct(Long id) {
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    // Search products by name
    public List<Product> searchProducts(String name) {
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    // Filter products by category
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategoryIgnoreCase(category);
    }
}

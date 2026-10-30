package com.shop.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.shop.model.CartEntry;
import com.shop.model.CartItem;
import com.shop.model.Product;
import com.shop.repository.CartEntryRepository;
import com.shop.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartService {
    private final ProductRepository productRepository;
    private final CartEntryRepository cartEntryRepository;

    public CartService(ProductRepository productRepository, CartEntryRepository cartEntryRepository) {
        this.productRepository = productRepository;
        this.cartEntryRepository = cartEntryRepository;
    }

    @Transactional
    public Collection<CartItem> getCart(String cartToken) {
        List<CartEntry> entries = cartEntryRepository.findByCartToken(cartToken);
        if (entries.isEmpty()) return List.of();
        Map<Long, Product> products = productRepository.findAllById(
                entries.stream().map(CartEntry::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Long> removedIds = entries.stream().map(CartEntry::getProductId)
                .filter(id -> !products.containsKey(id)).toList();
        if (!removedIds.isEmpty()) cartEntryRepository.deleteByCartTokenAndProductIdIn(cartToken, removedIds);
        return entries.stream().filter(entry -> products.containsKey(entry.getProductId()))
                .map(entry -> toCartItem(entry, products.get(entry.getProductId()))).toList();
    }

    @Transactional
    public CartItem addToCart(String cartToken, Long productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        Product product = findProduct(productId);
        CartEntry entry = cartEntryRepository.findByCartTokenAndProductId(cartToken, productId).orElse(null);
        long nextQuantity = (long) quantity + (entry == null ? 0 : entry.getQuantity());
        validateStock(nextQuantity, product);
        if (entry == null) {
            entry = new CartEntry();
            entry.setCartToken(cartToken);
            entry.setProductId(productId);
        }
        entry.setQuantity((int) nextQuantity);
        return toCartItem(cartEntryRepository.save(entry), product);
    }

    @Transactional
    public CartItem setQuantity(String cartToken, Long productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        Product product = findProduct(productId);
        CartEntry entry = cartEntryRepository.findByCartTokenAndProductId(cartToken, productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not present in the cart"));
        validateStock(quantity, product);
        entry.setQuantity(quantity);
        return toCartItem(cartEntryRepository.save(entry), product);
    }

    @Transactional
    public void removeFromCart(String cartToken, Long productId) {
        if (cartEntryRepository.deleteByCartTokenAndProductId(cartToken, productId) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not present in the cart");
        }
    }

    @Transactional
    public double getTotal(String cartToken) {
        return getCart(cartToken).stream().mapToDouble(CartItem::getSubtotal).sum();
    }

    @Transactional
    public void clearCart(String cartToken) {
        cartEntryRepository.deleteByCartToken(cartToken);
    }

    private Product findProduct(Long productId) {
        return productRepository.findById(productId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + productId));
    }

    private void validateStock(long quantity, Product product) {
        if (quantity > product.getStock()) throw new IllegalArgumentException("Requested quantity exceeds available stock");
    }

    private CartItem toCartItem(CartEntry entry, Product product) {
        return new CartItem(product.getId(), product.getName(), product.getPrice(), entry.getQuantity());
    }
}

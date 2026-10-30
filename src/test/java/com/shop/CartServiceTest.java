package com.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import com.shop.model.CartEntry;
import com.shop.model.Product;
import com.shop.repository.CartEntryRepository;
import com.shop.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CartServiceTest {
    private ProductRepository productRepository;
    private CartEntryRepository cartRepository;
    private CartService service;
    private final List<CartEntry> storedEntries = new ArrayList<>();
    private Product product;

    @BeforeEach
    void setUp() {
        storedEntries.clear();
        productRepository = org.mockito.Mockito.mock(ProductRepository.class);
        cartRepository = org.mockito.Mockito.mock(CartEntryRepository.class);
        service = new CartService(productRepository, cartRepository);
        product = new Product();
        product.setId(7L); product.setName("Lamp"); product.setPrice(5.0); product.setStock(2);
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productRepository.findAllById(anyIterable())).thenAnswer(call -> {
            Iterable<Long> ids = call.getArgument(0);
            List<Product> found = new ArrayList<>();
            ids.forEach(id -> { if (id.equals(product.getId())) found.add(product); });
            return found;
        });
        when(cartRepository.findByCartToken(anyString())).thenAnswer(call -> storedEntries.stream()
                .filter(entry -> entry.getCartToken().equals(call.getArgument(0))).toList());
        when(cartRepository.findByCartTokenAndProductId(anyString(), eq(7L))).thenAnswer(call -> storedEntries.stream()
                .filter(entry -> entry.getCartToken().equals(call.getArgument(0)) && entry.getProductId().equals(7L)).findFirst());
        when(cartRepository.save(any(CartEntry.class))).thenAnswer(call -> {
            CartEntry entry = call.getArgument(0);
            if (!storedEntries.contains(entry)) storedEntries.add(entry);
            return entry;
        });
        when(cartRepository.deleteByCartTokenAndProductId(anyString(), eq(7L))).thenAnswer(call -> {
            boolean removed = storedEntries.removeIf(entry -> entry.getCartToken().equals(call.getArgument(0)) && entry.getProductId().equals(7L));
            return removed ? 1 : 0;
        });
        when(cartRepository.deleteByCartToken(anyString())).thenAnswer(call -> {
            int before = storedEntries.size(); storedEntries.removeIf(entry -> entry.getCartToken().equals(call.getArgument(0)));
            return before - storedEntries.size();
        });
        when(cartRepository.deleteByCartTokenAndProductIdIn(anyString(), any())).thenReturn(0);
    }

    @Test
    void rejectsQuantityAboveAvailableStockIncludingExistingQuantity() {
        service.addToCart("visitor-a", 7L, 1);
        assertThrows(IllegalArgumentException.class, () -> service.addToCart("visitor-a", 7L, 2));
    }

    @Test
    void rejectsMissingProduct() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.addToCart("visitor-a", 99L, 1));
    }

    @Test
    void cartPersistsAcrossServiceRecreationAndRemovalUpdatesTotal() {
        service.addToCart("visitor-a", 7L, 2);
        CartService afterRestart = new CartService(productRepository, cartRepository);
        assertEquals(10.0, afterRestart.getTotal("visitor-a"));
        afterRestart.removeFromCart("visitor-a", 7L);
        assertEquals(0.0, afterRestart.getTotal("visitor-a"));
    }

    @Test
    void separateBrowserTokensDoNotShareCartItems() {
        service.addToCart("visitor-a", 7L, 1);
        assertEquals(1, service.getCart("visitor-a").size());
        assertEquals(0, service.getCart("visitor-b").size());
    }

    @Test
    void quantityUpdatesAreStockCheckedAndReflectedInTotals() {
        service.addToCart("visitor-a", 7L, 1);
        service.setQuantity("visitor-a", 7L, 2);
        assertEquals(10.0, service.getTotal("visitor-a"));
        assertThrows(IllegalArgumentException.class, () -> service.setQuantity("visitor-a", 7L, 3));
    }
}

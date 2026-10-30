package com.shop.controller;

import java.util.Collection;
import java.util.Map;

import com.shop.dto.AddToCartRequest;
import com.shop.model.CartItem;
import com.shop.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;
    private final CartCookie cartCookie;

    public CartController(CartService cartService, CartCookie cartCookie) {
        this.cartService = cartService;
        this.cartCookie = cartCookie;
    }

    @GetMapping
    public Collection<CartItem> getCart(HttpServletRequest request, HttpServletResponse response) {
        return cartService.getCart(cartCookie.resolve(request, response));
    }

    @PostMapping("/add")
    public CartItem addToCart(@Valid @RequestBody AddToCartRequest body, HttpServletRequest request, HttpServletResponse response) {
        return cartService.addToCart(cartCookie.resolve(request, response), body.productId(), body.quantity());
    }

    @PutMapping("/{productId}")
    public CartItem setQuantity(@PathVariable Long productId, @Valid @RequestBody QuantityRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        return cartService.setQuantity(cartCookie.resolve(request, response), productId, body.quantity());
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long productId, HttpServletRequest request, HttpServletResponse response) {
        cartService.removeFromCart(cartCookie.resolve(request, response), productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/total")
    public Map<String, Double> getTotal(HttpServletRequest request, HttpServletResponse response) {
        return Map.of("total", cartService.getTotal(cartCookie.resolve(request, response)));
    }

    @DeleteMapping
    public Map<String, String> clearCart(HttpServletRequest request, HttpServletResponse response) {
        cartService.clearCart(cartCookie.resolve(request, response));
        return Map.of("message", "Cart cleared successfully");
    }

    public record QuantityRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Integer quantity) { }
}

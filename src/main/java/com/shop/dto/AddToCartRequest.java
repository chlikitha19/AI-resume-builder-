package com.shop.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddToCartRequest(
        @NotNull(message = "productId is required") Long productId,
        @NotNull(message = "quantity is required")
        @Positive(message = "Quantity must be greater than zero") Integer quantity) {
}

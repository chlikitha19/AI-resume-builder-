package com.shop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shop.model.CartEntry;

public interface CartEntryRepository extends JpaRepository<CartEntry, Long> {
    List<CartEntry> findByCartToken(String cartToken);
    Optional<CartEntry> findByCartTokenAndProductId(String cartToken, Long productId);
    int deleteByCartTokenAndProductId(String cartToken, Long productId);
    int deleteByCartTokenAndProductIdIn(String cartToken, Collection<Long> productIds);
    int deleteByCartToken(String cartToken);
}

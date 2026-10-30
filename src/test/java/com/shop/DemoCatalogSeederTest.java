package com.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;
import org.junit.jupiter.api.Test;

class DemoCatalogSeederTest {
    @Test
    void seedingTwiceAddsOnlyTheMissingUniqueProducts() throws Exception {
        ProductRepository repository = org.mockito.Mockito.mock(ProductRepository.class);
        List<Product> stored = new ArrayList<>();
        when(repository.findByNameContainingIgnoreCase(anyString())).thenAnswer(call -> {
            String term = call.getArgument(0);
            return stored.stream().filter(product -> product.getName().toLowerCase().contains(term.toLowerCase())).toList();
        });
        when(repository.save(any(Product.class))).thenAnswer(call -> {
            Product product = call.getArgument(0);
            if (product.getId() == null) product.setId((long) stored.size() + 1);
            if (stored.stream().noneMatch(existing -> existing.getId().equals(product.getId()))) stored.add(product);
            return product;
        });
        var runner = new DemoCatalogSeeder().seedDemoCatalog(repository, true);
        runner.run();
        runner.run();
        assertEquals(56, stored.size());
        assertEquals(56, stored.stream().map(Product::getName).distinct().count());
    }
}

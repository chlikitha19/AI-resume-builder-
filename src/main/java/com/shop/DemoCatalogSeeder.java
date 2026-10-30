package com.shop;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.shop.model.Product;
import com.shop.repository.ProductRepository;

@Configuration
class DemoCatalogSeeder {
    private record Row(String category, String brand, int basePrice, String[] names) { }

    @Bean
    CommandLineRunner seedDemoCatalog(ProductRepository repository,
            @Value("${shopease.seed-demo-catalog:false}") boolean enabled) {
        return args -> {
            if (!enabled) return;
            List<Row> rows = List.of(
                new Row("Electronics", "boAt", 1299, new String[]{"Pulse Smart Speaker", "Aero Desk Fan", "Nova LED Monitor Light", "Orbit Power Bank"}),
                new Row("Computers & Laptops", "Lenovo", 42999, new String[]{"IdeaPad Slim Laptop", "ThinkVision 24 Monitor", "Ergo Wireless Keyboard", "Precision USB-C Hub"}),
                new Row("Mobile Accessories", "Ambrane", 499, new String[]{"Flex Braided USB-C Cable", "Snap MagSafe Stand", "Rapid 30W Charger", "Shield Clear Phone Case"}),
                new Row("Audio & Headphones", "JBL", 1499, new String[]{"Tune Wireless Headphones", "Wave Bluetooth Earbuds", "Studio USB Microphone", "Mini Portable Speaker"}),
                new Row("Fashion & Clothing", "H&M", 799, new String[]{"Everyday Cotton Shirt", "Relaxed Fit Trousers", "Classic Denim Jacket", "Soft Knit Pullover"}),
                new Row("Shoes & Footwear", "Campus", 1199, new String[]{"Urban Walk Sneakers", "Trail Runner Shoes", "Cloud Comfort Slides", "Classic Canvas Trainers"}),
                new Row("Watches & Wearables", "Noise", 1999, new String[]{"ColorFit Active Watch", "Minimal Analog Watch", "Move Fitness Band", "Leather Strap Smartwatch"}),
                new Row("Home & Kitchen", "Prestige", 349, new String[]{"Stainless Steel Cookware Set", "Brew Easy French Press", "Everyday Ceramic Mug Set", "Compact Electric Kettle"}),
                new Row("Furniture & Home Decor", "Wakefit", 1499, new String[]{"Oak Finish Side Table", "Linen Cushion Pair", "Arc Table Lamp", "Wall Shelf Set"}),
                new Row("Beauty & Personal Care", "Mamaearth", 299, new String[]{"Hydrating Face Wash", "Daily Care Shampoo", "Aloe Vera Moisturizer", "Gentle Body Lotion"}),
                new Row("Sports & Fitness", "Decathlon", 599, new String[]{"Flex Yoga Mat", "Adjustable Jump Rope", "Resistance Band Set", "Insulated Sports Bottle"}),
                new Row("Travel Accessories", "Safari", 799, new String[]{"Cabin Travel Backpack", "Packing Cube Set", "Compact Neck Pillow", "Hard Shell Luggage Tag"}),
                new Row("Books & Stationery", "Classmate", 199, new String[]{"Daily Planner Notebook", "Watercolor Sketch Pad", "Fine Tip Pen Set", "Reading Journal"}),
                new Row("Toys & Games", "Funskool", 499, new String[]{"Wooden Building Blocks", "Family Strategy Board Game", "Creative Art Kit", "Junior Puzzle Set"})
            );
            for (Row row : rows) {
                for (int i = 0; i < row.names().length; i++) {
                    String name = row.names()[i];
                    Product existing = repository.findByNameContainingIgnoreCase(name).stream()
                            .filter(product -> product.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
                    if (existing != null) {
                        String expectedDescription = description(name, row.brand());
                        if (existing.getOriginalPrice() == null && row.brand().equals(existing.getBrand())
                                && row.category().equals(existing.getCategory())
                                && expectedDescription.equals(existing.getDescription())) {
                            existing.setOriginalPrice(Math.round(existing.getPrice() * 1.18 * 100.0) / 100.0);
                            repository.save(existing);
                        }
                        continue;
                    }
                    Product product = new Product();
                    product.setName(name);
                    product.setDescription(description(name, row.brand()));
                    product.setCategory(row.category());
                    product.setBrand(row.brand());
                    product.setPrice((double) row.basePrice() + (i * Math.max(75, row.basePrice() / 5)));
                    product.setOriginalPrice(Math.round(product.getPrice() * 1.18 * 100.0) / 100.0);
                    product.setStock(12 + i * 3);
                    product.setRating(4.1 + i * 0.2);
                    product.setReviewCount(24 + i * 17);
                    product = repository.save(product);
                    product.setImageUrl("/api/products/" + product.getId() + "/image.svg");
                    repository.save(product);
                }
            }
        };
    }

    private String description(String name, String brand) {
        return name + " by " + brand + ". A thoughtfully selected everyday essential, built for dependable use and lasting value.";
    }
}


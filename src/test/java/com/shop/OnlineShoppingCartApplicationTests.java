package com.shop;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import com.shop.controller.ApiExceptionHandler;
import com.shop.controller.CartCookie;
import com.shop.controller.ProductImageController;
import com.shop.controller.CartController;
import com.shop.controller.ProductController;
import com.shop.model.Product;
import com.shop.service.CartService;
import com.shop.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OnlineShoppingCartApplicationTests {
    private MockMvc mvc;
    private ProductService productService;
    private CartService cartService;

    @BeforeEach
    void setUp() {
        productService = org.mockito.Mockito.mock(ProductService.class);
        cartService = org.mockito.Mockito.mock(CartService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(productService), new ProductImageController(productService), new CartController(cartService, new CartCookie()))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void browseCombinesCaseInsensitiveSearchCategoryAndPriceFilters() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of(product(1L), product(2L)));
        mvc.perform(get("/api/products/browse").param("q", "PHONE").param("category", "electronics")
                .param("minPrice", "8").param("maxPrice", "12").param("size", "12"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.products[0].id").value(1));
    }

    @Test
    void searchReturnsEmptyArrayWhenNothingMatches() throws Exception {
        when(productService.searchProducts("missing")).thenReturn(List.of());
        mvc.perform(get("/api/products/search").param("name", "missing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void servesUniqueProductSpecificSvgImage() throws Exception {
        Product illustratedProduct = product(1L);
        illustratedProduct.setName("Phone & Case");
        when(productService.getProductById(1L)).thenReturn(illustratedProduct);
        mvc.perform(get("/api/products/1/image.svg"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/svg+xml"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.containsString("Phone &amp; Case")));
    }

    @Test
    void searchAndCategoryEndpointsDelegateQueries() throws Exception {
        when(productService.searchProducts("phone")).thenReturn(List.of(product(1L)));
        when(productService.getProductsByCategory("Electronics")).thenReturn(List.of(product(1L)));
        mvc.perform(get("/api/products/search").param("name", "phone"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        mvc.perform(get("/api/products/category/Electronics"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(1));
        verify(productService).searchProducts("phone");
        verify(productService).getProductsByCategory("Electronics");
    }

    @Test
    void rejectsInvalidCartQuantitiesWithMeaningfulBadRequest() throws Exception {
        mvc.perform(post("/api/cart/add").contentType("application/json")
                .content("{\"productId\":1,\"quantity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Quantity must be greater than zero"));
        mvc.perform(post("/api/cart/add").contentType("application/json")
                .content("{\"productId\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cartRemovalEndpointDelegatesRemoval() throws Exception {
        mvc.perform(delete("/api/cart/1")).andExpect(status().isNoContent())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists("Set-Cookie"));
        verify(cartService).removeFromCart(anyString(), eq(1L));
    }

    private Product product(Long id) {
        Product product = new Product();
        product.setId(id); product.setName(id == 1L ? "Phone" : "Tablet"); product.setCategory("Electronics");
        product.setPrice(10.0); product.setStock(4);
        return product;
    }
}


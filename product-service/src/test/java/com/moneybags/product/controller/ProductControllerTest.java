package com.moneybags.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneybags.product.dto.ProductRequest;
import com.moneybags.product.dto.ProductResponse;
import com.moneybags.product.enums.ProductStatus;
import com.moneybags.product.enums.ProductType;
import com.moneybags.product.exception.GlobalExceptionHandler;
import com.moneybags.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ProductController.class, GlobalExceptionHandler.class})
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockBean
    private ProductService productService;

    @Test
    void createReturns201() throws Exception {
        ProductRequest request = new ProductRequest("SAV-001", "Savings", ProductType.SAVINGS,
                "Retail savings", new BigDecimal("3.50"), new BigDecimal("1000.00"),
                new BigDecimal("50000.00"), 5, ProductStatus.ACTIVE);
        ProductResponse response = new ProductResponse("SAV-001", "Savings", ProductType.SAVINGS,
                "Retail savings", new BigDecimal("3.50"), new BigDecimal("1000.00"),
                new BigDecimal("50000.00"), 5, ProductStatus.ACTIVE);
        when(productService.create(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productCode").value("SAV-001"));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        ProductRequest request = new ProductRequest("", "", null, null, null, null, null, -1, null);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }
}

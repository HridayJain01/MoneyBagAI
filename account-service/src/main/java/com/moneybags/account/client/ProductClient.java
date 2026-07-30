package com.moneybags.account.client;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/v1/products/{productCode}")
    ProductSummary findProduct(@PathVariable String productCode);
    record ProductSummary(String productCode, BigDecimal minBalance, String status) {}
}

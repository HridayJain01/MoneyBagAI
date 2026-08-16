package com.example.productservice.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductRequest {

    @Size(max = 50, message = "productCode must not exceed 50 characters")
    private String productCode;

    @Size(max = 150, message = "name must not exceed 150 characters")
    private String name;

    private String productCategory;

    private String productType;

    private String status;

    private LocalDateTime effectiveFrom;

    private LocalDateTime effectiveTo;

    private Map<String, Object> terms;

    @AssertTrue(message = "effectiveTo must be after effectiveFrom")
    public boolean isEffectiveDateRangeValid() {
        return effectiveFrom == null || effectiveTo == null || effectiveTo.isAfter(effectiveFrom);
    }

    @AssertTrue(message = "terms cannot be empty")
    public boolean isTermsPresent() {
        return terms == null || !terms.isEmpty();
    }
}

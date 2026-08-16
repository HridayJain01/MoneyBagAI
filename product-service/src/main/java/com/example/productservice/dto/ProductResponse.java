package com.example.productservice.dto;

import java.time.LocalDateTime;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductResponse {

    private Long id;
    private String productCode;
    private String name;
    private String productCategory;
    private String productType;
    private Long productVersionId;
    private Integer version;
    private String status;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> terms;
}

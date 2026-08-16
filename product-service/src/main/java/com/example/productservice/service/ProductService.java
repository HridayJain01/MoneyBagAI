package com.example.productservice.service;

import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.ProductStatusUpdateRequest;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductAttributeDefinition;
import com.example.productservice.entity.ProductVersion;
import com.example.productservice.entity.ProductVersionAttribute;
import com.example.productservice.exception.BadRequestException;
import com.example.productservice.exception.ResourceNotFoundException;
import com.example.productservice.repository.ProductAttributeDefinitionRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.repository.ProductVersionAttributeRepository;
import com.example.productservice.repository.ProductVersionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final ProductRepository productRepository;
    private final ProductVersionRepository productVersionRepository;
    private final ProductAttributeDefinitionRepository attributeDefinitionRepository;
    private final ProductVersionAttributeRepository productVersionAttributeRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        validateCreateRequest(request);

        Product product = productRepository.findByProductCode(request.getProductCode())
                .orElseGet(() -> productRepository.save(Product.builder()
                        .id(nextProductId())
                        .productCode(request.getProductCode())
                        .name(request.getName())
                        .productCategory(normalize(request.getProductCategory()))
                        .productType(normalize(request.getProductType()))
                        .build()));

        return createVersion(product, request);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts(String productCategory, String productType, String status) {
        String normalizedCategory = normalize(productCategory);
        String normalizedType = normalize(productType);
        String normalizedStatus = normalize(status);
        List<Product> products;

        if (normalizedCategory != null) {
            products = productRepository.findByProductCategory(normalizedCategory);
        } else if (normalizedType != null) {
            products = productRepository.findByProductType(normalizedType);
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .filter(product -> normalizedType == null || normalizedType.equals(product.getProductType()))
                .map(this::toCurrentResponseOrNull)
                .filter(Objects::nonNull)
                .filter(response -> normalizedStatus == null || normalizedStatus.equals(response.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        return toActiveResponse(findProductById(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductByCode(String productCode) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with code: " + productCode));

        return toActiveResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductByVersionId(Long productVersionId) {
        ProductVersion productVersion = productVersionRepository.findById(productVersionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product version not found with id: " + productVersionId
                ));

        return toResponse(productVersion.getProduct(), productVersion);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductVersions(Long id) {
        Product product = findProductById(id);

        return productVersionRepository.findByProduct_IdOrderByVersionNumberDesc(product.getId())
                .stream()
                .map(productVersion -> toResponse(product, productVersion))
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        validateVersionRequest(request);
        Product product = findProductById(id);

        updateProductIdentity(product, request);

        return createVersion(product, request);
    }

    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatusUpdateRequest request) {
        Product product = findProductById(id);
        ProductVersion productVersion = findActiveVersion(product);
        productVersion.setStatus(request.getStatus());

        return toResponse(product, productVersionRepository.save(productVersion));
    }

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse toActiveResponse(Product product) {
        return toResponse(product, findActiveVersion(product));
    }

    private ProductResponse toCurrentResponseOrNull(Product product) {
        return productVersionRepository
                .findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(product.getId(), ACTIVE)
                .or(() -> productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(product.getId()))
                .map(productVersion -> toResponse(product, productVersion))
                .orElse(null);
    }

    private ProductVersion findActiveVersion(Product product) {
        return productVersionRepository
                .findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(product.getId(), ACTIVE)
                .or(() -> productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(product.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product version not found for product id: " + product.getId()
                ));
    }

    private ProductResponse createVersion(Product product, ProductRequest request) {
        ProductVersion latestVersion = productVersionRepository
                .findFirstByProduct_IdOrderByVersionNumberDesc(product.getId())
                .orElse(null);
        String status = normalize(request.getStatus()) == null ? ACTIVE : normalize(request.getStatus());

        if (ACTIVE.equals(status)) {
            productVersionRepository
                    .findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(product.getId(), ACTIVE)
                    .ifPresent(activeVersion -> {
                        activeVersion.setStatus(INACTIVE);
                        activeVersion.setEffectiveTo(request.getEffectiveFrom());
                        productVersionRepository.save(activeVersion);
                    });
        }

        ProductVersion productVersion = ProductVersion.builder()
                .id(nextProductVersionId())
                .product(product)
                .versionNumber(latestVersion == null ? 1 : latestVersion.getVersionNumber() + 1)
                .status(status)
                .effectiveFrom(request.getEffectiveFrom())
                .effectiveTo(request.getEffectiveTo())
                .build();

        ProductVersion savedVersion = productVersionRepository.save(productVersion);
        saveAttributes(savedVersion, request.getTerms());

        return toResponse(product, savedVersion);
    }

    private void saveAttributes(ProductVersion productVersion, Map<String, Object> terms) {
        long[] nextAttributeId = {nextProductVersionAttributeId()};
        terms.forEach((attributeCode, value) -> {
            ProductAttributeDefinition definition = attributeDefinitionRepository.findByAttributeCode(attributeCode)
                    .orElseThrow(() -> new BadRequestException("Unknown product attribute: " + attributeCode));

            productVersionAttributeRepository.save(ProductVersionAttribute.builder()
                    .id(nextAttributeId[0]++)
                    .productVersion(productVersion)
                    .attributeDefinition(definition)
                    .attributeValue(toStorageValue(definition, value))
                    .build());
        });
    }

    private ProductResponse toResponse(Product product, ProductVersion productVersion) {
        return ProductResponse.builder()
                .id(product.getId())
                .productCode(product.getProductCode())
                .name(product.getName())
                .productCategory(product.getProductCategory())
                .productType(product.getProductType())
                .productVersionId(productVersion.getId())
                .version(productVersion.getVersionNumber())
                .status(productVersion.getStatus())
                .effectiveFrom(productVersion.getEffectiveFrom())
                .effectiveTo(productVersion.getEffectiveTo())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .terms(toTerms(productVersion))
                .build();
    }

    private Map<String, Object> toTerms(ProductVersion productVersion) {
        Map<String, Object> terms = new LinkedHashMap<>();

        productVersionAttributeRepository.findByProductVersion_Id(productVersion.getId())
                .forEach(attribute -> {
                    ProductAttributeDefinition definition = attribute.getAttributeDefinition();
                    terms.put(
                            toCamelCase(definition.getAttributeCode()),
                            fromStorageValue(definition, attribute.getAttributeValue())
                    );
                });

        return terms;
    }

    private void updateProductIdentity(Product product, ProductRequest request) {
        if (hasText(request.getName())) {
            product.setName(request.getName());
        }
        if (request.getProductCategory() != null) {
            product.setProductCategory(normalize(request.getProductCategory()));
        }
        if (request.getProductType() != null) {
            product.setProductType(normalize(request.getProductType()));
        }
    }

    private void validateCreateRequest(ProductRequest request) {
        if (!hasText(request.getProductCode())) {
            throw new BadRequestException("productCode is required");
        }
        if (!hasText(request.getName())) {
            throw new BadRequestException("name is required");
        }
        if (request.getProductCategory() == null) {
            throw new BadRequestException("productCategory is required");
        }
        if (request.getProductType() == null) {
            throw new BadRequestException("productType is required");
        }
        validateVersionRequest(request);
    }

    private void validateVersionRequest(ProductRequest request) {
        if (request.getEffectiveFrom() == null) {
            throw new BadRequestException("effectiveFrom is required");
        }
        if (request.getEffectiveTo() != null && !request.getEffectiveTo().isAfter(request.getEffectiveFrom())) {
            throw new BadRequestException("effectiveTo must be after effectiveFrom");
        }
        if (request.getTerms() == null || request.getTerms().isEmpty()) {
            throw new BadRequestException("terms cannot be null or empty");
        }
    }

    private String toStorageValue(ProductAttributeDefinition definition, Object value) {
        if (value == null) {
            throw new BadRequestException(definition.getAttributeCode() + " cannot be null");
        }

        return switch (definition.getDataType()) {
            case "STRING" -> value.toString();
            case "INTEGER" -> String.valueOf(toLong(value, definition.getAttributeCode()));
            case "DECIMAL" -> toBigDecimal(value, definition.getAttributeCode()).toPlainString();
            case "BOOLEAN" -> String.valueOf(toBoolean(value, definition.getAttributeCode()));
            case "DATE" -> toLocalDate(value, definition.getAttributeCode()).toString();
            default -> throw new BadRequestException("Unsupported attribute data type: " + definition.getDataType());
        };
    }

    private Object fromStorageValue(ProductAttributeDefinition definition, String value) {
        return switch (definition.getDataType()) {
            case "STRING" -> value;
            case "INTEGER" -> {
                long parsed = Long.parseLong(value);
                if (parsed >= Integer.MIN_VALUE && parsed <= Integer.MAX_VALUE) {
                    yield (int) parsed;
                }
                yield parsed;
            }
            case "DECIMAL" -> new BigDecimal(value);
            case "BOOLEAN" -> Boolean.valueOf(value);
            case "DATE" -> LocalDate.parse(value);
            default -> value;
        };
    }

    private BigDecimal toBigDecimal(Object value, String attributeCode) {
        try {
            return value instanceof BigDecimal bigDecimal ? bigDecimal : new BigDecimal(value.toString());
        } catch (NumberFormatException ex) {
            throw new BadRequestException(attributeCode + " must be a decimal value");
        }
    }

    private Long toLong(Object value, String attributeCode) {
        try {
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            throw new BadRequestException(attributeCode + " must be an integer value");
        }
    }

    private Boolean toBoolean(Object value, String attributeCode) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = value.toString().toLowerCase(Locale.ROOT);
        if ("true".equals(text) || "false".equals(text)) {
            return Boolean.valueOf(text);
        }
        throw new BadRequestException(attributeCode + " must be a boolean value");
    }

    private LocalDate toLocalDate(Object value, String attributeCode) {
        try {
            if (value instanceof LocalDate localDate) {
                return localDate;
            }
            return LocalDate.parse(value.toString());
        } catch (RuntimeException ex) {
            throw new BadRequestException(attributeCode + " must be a date value");
        }
    }

    private String toCamelCase(String attributeCode) {
        String[] parts = attributeCode.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)));
                result.append(parts[i].substring(1));
            }
        }

        return result.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Long nextProductId() {
        return productRepository.findMaxId() + 1;
    }

    private Long nextProductVersionId() {
        return productVersionRepository.findMaxId() + 1;
    }

    private Long nextProductVersionAttributeId() {
        return productVersionAttributeRepository.findMaxId() + 1;
    }

    private String normalize(String value) {
        return hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : null;
    }
}

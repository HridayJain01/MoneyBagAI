package com.example.productservice.service;

import com.example.productservice.dto.ProductRequest;
import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.ProductStatusUpdateRequest;
import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductAttributeDefinition;
import com.example.productservice.entity.ProductVersion;
import com.example.productservice.entity.ProductVersionAttribute;
import com.example.productservice.exception.BadRequestException;
import com.example.productservice.repository.ProductAttributeDefinitionRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.repository.ProductVersionAttributeRepository;
import com.example.productservice.repository.ProductVersionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVersionRepository productVersionRepository;

    @Mock
    private ProductAttributeDefinitionRepository attributeDefinitionRepository;

    @Mock
    private ProductVersionAttributeRepository productVersionAttributeRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_shouldCreateProductVersionAndTerms() {
        ProductRequest request = createHomeLoanRequest();

        when(productRepository.findByProductCode("HL-STANDARD")).thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(9L);
            return product;
        });
        when(productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(9L)).thenReturn(Optional.empty());
        when(productRepository.findMaxId()).thenReturn(8L);
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(productVersionRepository.findMaxId()).thenReturn(900L);
        when(productVersionAttributeRepository.findMaxId()).thenReturn(1800L);
        when(productVersionRepository.save(any(ProductVersion.class))).thenAnswer(invocation -> {
            ProductVersion productVersion = invocation.getArgument(0);
            productVersion.setId(901L);
            return productVersion;
        });
        stubHomeLoanDefinitions();
        when(productVersionAttributeRepository.findByProductVersion_Id(901L)).thenReturn(List.of(
                attribute("INTEREST_RATE", "DECIMAL", "8.25"),
                attribute("MAX_TENURE_MONTHS", "INTEGER", "360")
        ));

        ProductResponse response = productService.createProduct(request);

        assertEquals(9L, response.getId());
        assertEquals(901L, response.getProductVersionId());
        assertEquals(1, response.getVersion());
        assertEquals("LOAN", response.getProductCategory());
        assertEquals("HOME_LOAN", response.getProductType());
        assertEquals(new BigDecimal("8.25"), response.getTerms().get("interestRate"));
        assertEquals(360, response.getTerms().get("maxTenureMonths"));
        verify(productVersionAttributeRepository, atLeastOnce()).save(any(ProductVersionAttribute.class));
    }

    @Test
    void createProduct_whenProductCodeExists_shouldCreateSecondVersionAndClosePreviousActiveVersion() {
        Product product = product();
        ProductVersion activeVersion = version(product, 901L, 1, "ACTIVE");
        ProductRequest request = createHomeLoanRequest();
        request.setEffectiveFrom(LocalDateTime.of(2026, 8, 1, 0, 0));

        when(productRepository.findByProductCode("HL-STANDARD")).thenReturn(Optional.of(product));
        when(productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(9L))
                .thenReturn(Optional.of(activeVersion));
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.of(activeVersion));
        when(productVersionRepository.findMaxId()).thenReturn(901L);
        when(productVersionAttributeRepository.findMaxId()).thenReturn(1800L);
        when(productVersionRepository.save(any(ProductVersion.class))).thenAnswer(invocation -> {
            ProductVersion productVersion = invocation.getArgument(0);
            if (productVersion.getId() == null) {
                productVersion.setId(902L);
            }
            return productVersion;
        });
        stubHomeLoanDefinitions();
        when(productVersionAttributeRepository.findByProductVersion_Id(902L)).thenReturn(List.of());

        ProductResponse response = productService.createProduct(request);

        assertEquals(902L, response.getProductVersionId());
        assertEquals(2, response.getVersion());
        assertEquals("INACTIVE", activeVersion.getStatus());
        assertEquals(request.getEffectiveFrom(), activeVersion.getEffectiveTo());
    }

    @Test
    void getProductById_shouldReturnActiveVersionWithTypedCamelCaseTerms() {
        Product product = product();
        ProductVersion version = version(product, 901L, 1, "ACTIVE");

        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.of(version));
        when(productVersionAttributeRepository.findByProductVersion_Id(901L)).thenReturn(List.of(
                attribute("INTEREST_RATE", "DECIMAL", "8.250"),
                attribute("MAX_TENURE_MONTHS", "INTEGER", "360")
        ));

        ProductResponse response = productService.getProductById(9L);

        assertEquals(901L, response.getProductVersionId());
        assertEquals(new BigDecimal("8.250"), response.getTerms().get("interestRate"));
        assertInstanceOf(Integer.class, response.getTerms().get("maxTenureMonths"));
    }

    @Test
    void getProductById_whenNoActiveVersionExists_shouldReturnLatestVersion() {
        Product product = product();
        ProductVersion latestVersion = version(product, 903L, 2, "DISCONTINUED");

        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(9L))
                .thenReturn(Optional.of(latestVersion));
        when(productVersionAttributeRepository.findByProductVersion_Id(903L)).thenReturn(List.of());

        ProductResponse response = productService.getProductById(9L);

        assertEquals(9L, response.getId());
        assertEquals(903L, response.getProductVersionId());
        assertEquals("DISCONTINUED", response.getStatus());
    }

    @Test
    void getAllProducts_shouldIncludeLatestDiscontinuedProductWhenNoStatusFilterIsProvided() {
        Product product = product();
        ProductVersion latestVersion = version(product, 903L, 2, "DISCONTINUED");

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(9L))
                .thenReturn(Optional.of(latestVersion));
        when(productVersionAttributeRepository.findByProductVersion_Id(903L)).thenReturn(List.of());

        List<ProductResponse> responses = productService.getAllProducts(null, null, null);

        assertEquals(1, responses.size());
        assertEquals(9L, responses.get(0).getId());
        assertEquals(903L, responses.get(0).getProductVersionId());
        assertEquals("DISCONTINUED", responses.get(0).getStatus());
    }

    @Test
    void getAllProducts_withDiscontinuedStatus_shouldReturnLatestDiscontinuedProduct() {
        Product product = product();
        ProductVersion latestVersion = version(product, 903L, 2, "DISCONTINUED");

        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(9L))
                .thenReturn(Optional.of(latestVersion));
        when(productVersionAttributeRepository.findByProductVersion_Id(903L)).thenReturn(List.of());

        List<ProductResponse> responses = productService.getAllProducts(null, null, "discontinued");

        assertEquals(1, responses.size());
        assertEquals(9L, responses.get(0).getId());
        assertEquals("DISCONTINUED", responses.get(0).getStatus());
    }

    @Test
    void getProductByCode_shouldReturnActiveVersion() {
        Product product = product();
        ProductVersion version = version(product, 901L, 1, "ACTIVE");

        when(productRepository.findByProductCode("HL-STANDARD")).thenReturn(Optional.of(product));
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.of(version));
        when(productVersionAttributeRepository.findByProductVersion_Id(901L)).thenReturn(List.of());

        ProductResponse response = productService.getProductByCode("HL-STANDARD");

        assertEquals(9L, response.getId());
        assertEquals(901L, response.getProductVersionId());
    }

    @Test
    void getProductByVersionId_shouldReturnExactHistoricalVersion() {
        Product product = product();
        ProductVersion historicalVersion = version(product, 901L, 1, "INACTIVE");

        when(productVersionRepository.findById(901L)).thenReturn(Optional.of(historicalVersion));
        when(productVersionAttributeRepository.findByProductVersion_Id(901L)).thenReturn(List.of());

        ProductResponse response = productService.getProductByVersionId(901L);

        assertEquals("INACTIVE", response.getStatus());
        assertEquals(901L, response.getProductVersionId());
    }

    @Test
    void createProduct_whenAttributeCodeIsUnknown_shouldFailValidation() {
        ProductRequest request = createHomeLoanRequest();
        request.setTerms(Map.of("UNKNOWN_ATTRIBUTE", 1));

        when(productRepository.findByProductCode("HL-STANDARD")).thenReturn(Optional.of(product()));
        when(productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(9L)).thenReturn(Optional.empty());
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(productVersionRepository.findMaxId()).thenReturn(901L);
        when(productVersionRepository.save(any(ProductVersion.class))).thenAnswer(invocation -> {
            ProductVersion productVersion = invocation.getArgument(0);
            productVersion.setId(902L);
            return productVersion;
        });
        when(attributeDefinitionRepository.findByAttributeCode("UNKNOWN_ATTRIBUTE")).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> productService.createProduct(request));
    }

    @Test
    void updateProduct_shouldCreateNewVersionInsteadOfOverwritingExistingVersion() {
        Product product = product();
        ProductVersion latestVersion = version(product, 901L, 1, "ACTIVE");
        ProductRequest request = createHomeLoanRequest();
        request.setName("Standard Home Loan Updated");
        request.setProductType("HOME_LOAN");

        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(9L))
                .thenReturn(Optional.of(latestVersion));
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.of(latestVersion));
        when(productVersionRepository.findMaxId()).thenReturn(901L);
        when(productVersionAttributeRepository.findMaxId()).thenReturn(1800L);
        when(productVersionRepository.save(any(ProductVersion.class))).thenAnswer(invocation -> {
            ProductVersion productVersion = invocation.getArgument(0);
            if (productVersion.getId() == null) {
                productVersion.setId(902L);
            }
            return productVersion;
        });
        stubHomeLoanDefinitions();
        when(productVersionAttributeRepository.findByProductVersion_Id(902L)).thenReturn(List.of());

        ProductResponse response = productService.updateProduct(9L, request);

        assertEquals("Standard Home Loan Updated", product.getName());
        assertEquals(9L, response.getId());
        assertEquals(2, response.getVersion());
        assertEquals(902L, response.getProductVersionId());
        assertEquals("INACTIVE", latestVersion.getStatus());
        verify(productRepository, never()).save(product);
    }

    @Test
    void updateProductStatus_shouldUpdateActiveVersionStatus() {
        Product product = product();
        ProductVersion activeVersion = version(product, 901L, 1, "ACTIVE");
        ProductStatusUpdateRequest request = new ProductStatusUpdateRequest();
        request.setStatus("DISCONTINUED");

        when(productRepository.findById(9L)).thenReturn(Optional.of(product));
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.of(activeVersion));
        when(productVersionRepository.save(activeVersion)).thenReturn(activeVersion);
        when(productVersionAttributeRepository.findByProductVersion_Id(901L)).thenReturn(List.of());

        ProductResponse response = productService.updateProductStatus(9L, request);

        assertEquals("DISCONTINUED", activeVersion.getStatus());
        assertEquals("DISCONTINUED", response.getStatus());
    }

    @Test
    void createProduct_shouldPersistAttributeValuesAsStrings() {
        Product product = product();
        ProductRequest request = createHomeLoanRequest();

        when(productRepository.findByProductCode("HL-STANDARD")).thenReturn(Optional.of(product));
        when(productVersionRepository.findFirstByProduct_IdOrderByVersionNumberDesc(9L)).thenReturn(Optional.empty());
        when(productVersionRepository.findFirstByProduct_IdAndStatusOrderByVersionNumberDesc(9L, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(productVersionRepository.findMaxId()).thenReturn(901L);
        when(productVersionAttributeRepository.findMaxId()).thenReturn(1800L);
        when(productVersionRepository.save(any(ProductVersion.class))).thenAnswer(invocation -> {
            ProductVersion productVersion = invocation.getArgument(0);
            productVersion.setId(902L);
            return productVersion;
        });
        stubHomeLoanDefinitions();
        when(productVersionAttributeRepository.findByProductVersion_Id(902L)).thenReturn(List.of());

        productService.createProduct(request);

        ArgumentCaptor<ProductVersionAttribute> attributeCaptor = ArgumentCaptor.forClass(ProductVersionAttribute.class);
        verify(productVersionAttributeRepository, atLeastOnce()).save(attributeCaptor.capture());
        assertNotNull(attributeCaptor.getValue().getAttributeValue());
    }

    private ProductRequest createHomeLoanRequest() {
        ProductRequest request = new ProductRequest();
        request.setProductCode("HL-STANDARD");
        request.setName("Standard Home Loan");
        request.setProductCategory("LOAN");
        request.setProductType("HOME_LOAN");
        request.setStatus("ACTIVE");
        request.setEffectiveFrom(LocalDateTime.of(2026, 7, 1, 0, 0));
        request.setTerms(Map.of(
                "INTEREST_RATE", new BigDecimal("8.25"),
                "MAX_TENURE_MONTHS", 360
        ));
        return request;
    }

    private Product product() {
        Product product = new Product();
        product.setId(9L);
        product.setProductCode("HL-STANDARD");
        product.setName("Standard Home Loan");
        product.setProductCategory("LOAN");
        product.setProductType("HOME_LOAN");
        product.setCreatedAt(LocalDateTime.of(2026, 7, 1, 9, 40));
        product.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 9, 0));
        return product;
    }

    private ProductVersion version(Product product, Long id, Integer versionNumber, String status) {
        ProductVersion productVersion = new ProductVersion();
        productVersion.setId(id);
        productVersion.setProduct(product);
        productVersion.setVersionNumber(versionNumber);
        productVersion.setStatus(status);
        productVersion.setEffectiveFrom(LocalDateTime.of(2026, 7, 1, 0, 0));
        return productVersion;
    }

    private ProductVersionAttribute attribute(String code, String dataType, String value) {
        ProductVersionAttribute attribute = new ProductVersionAttribute();
        attribute.setAttributeDefinition(ProductAttributeDefinition.builder()
                .id(1L)
                .attributeCode(code)
                .displayName(code)
                .dataType(dataType)
                .build());
        attribute.setAttributeValue(value);
        return attribute;
    }

    private void stubHomeLoanDefinitions() {
        when(attributeDefinitionRepository.findByAttributeCode("INTEREST_RATE"))
                .thenReturn(Optional.of(ProductAttributeDefinition.builder()
                        .id(1L)
                        .attributeCode("INTEREST_RATE")
                        .displayName("Interest Rate")
                        .dataType("DECIMAL")
                        .unit("PERCENT")
                        .build()));
        when(attributeDefinitionRepository.findByAttributeCode("MAX_TENURE_MONTHS"))
                .thenReturn(Optional.of(ProductAttributeDefinition.builder()
                        .id(9L)
                        .attributeCode("MAX_TENURE_MONTHS")
                        .displayName("Maximum Tenure Months")
                        .dataType("INTEGER")
                        .unit("MONTHS")
                        .build()));
    }
}

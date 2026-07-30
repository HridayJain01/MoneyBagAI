package com.moneybags.product.mapper;

import com.moneybags.product.dto.ProductRequest;
import com.moneybags.product.dto.ProductResponse;
import com.moneybags.product.dto.ProductUpdateRequest;
import com.moneybags.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    Product toEntity(ProductRequest request);

    ProductResponse toResponse(Product product);

    @Mapping(target = "productCode", ignore = true)
    void update(ProductUpdateRequest request, @MappingTarget Product product);
}

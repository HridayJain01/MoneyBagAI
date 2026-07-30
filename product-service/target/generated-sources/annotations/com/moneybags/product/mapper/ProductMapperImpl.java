package com.moneybags.product.mapper;

import com.moneybags.product.dto.ProductRequest;
import com.moneybags.product.dto.ProductResponse;
import com.moneybags.product.dto.ProductUpdateRequest;
import com.moneybags.product.entity.Product;
import com.moneybags.product.enums.ProductStatus;
import com.moneybags.product.enums.ProductType;
import java.math.BigDecimal;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-29T16:51:28+0530",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.12 (Oracle Corporation)"
)
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public Product toEntity(ProductRequest request) {
        if ( request == null ) {
            return null;
        }

        Product.ProductBuilder product = Product.builder();

        product.productCode( request.productCode() );
        product.productName( request.productName() );
        product.productType( request.productType() );
        product.description( request.description() );
        product.interestRate( request.interestRate() );
        product.minBalance( request.minBalance() );
        product.maxWithdrawalPerDay( request.maxWithdrawalPerDay() );
        product.freeTxnPerMonth( request.freeTxnPerMonth() );
        product.status( request.status() );

        return product.build();
    }

    @Override
    public ProductResponse toResponse(Product product) {
        if ( product == null ) {
            return null;
        }

        String productCode = null;
        String productName = null;
        ProductType productType = null;
        String description = null;
        BigDecimal interestRate = null;
        BigDecimal minBalance = null;
        BigDecimal maxWithdrawalPerDay = null;
        Integer freeTxnPerMonth = null;
        ProductStatus status = null;

        productCode = product.getProductCode();
        productName = product.getProductName();
        productType = product.getProductType();
        description = product.getDescription();
        interestRate = product.getInterestRate();
        minBalance = product.getMinBalance();
        maxWithdrawalPerDay = product.getMaxWithdrawalPerDay();
        freeTxnPerMonth = product.getFreeTxnPerMonth();
        status = product.getStatus();

        ProductResponse productResponse = new ProductResponse( productCode, productName, productType, description, interestRate, minBalance, maxWithdrawalPerDay, freeTxnPerMonth, status );

        return productResponse;
    }

    @Override
    public void update(ProductUpdateRequest request, Product product) {
        if ( request == null ) {
            return;
        }

        product.setProductName( request.productName() );
        product.setProductType( request.productType() );
        product.setDescription( request.description() );
        product.setInterestRate( request.interestRate() );
        product.setMinBalance( request.minBalance() );
        product.setMaxWithdrawalPerDay( request.maxWithdrawalPerDay() );
        product.setFreeTxnPerMonth( request.freeTxnPerMonth() );
        product.setStatus( request.status() );
    }
}

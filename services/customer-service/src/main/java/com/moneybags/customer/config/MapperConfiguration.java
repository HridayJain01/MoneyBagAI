package com.moneybags.customer.config;

import com.moneybags.customer.mapper.CustomerMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfiguration {

    @Bean
    @ConditionalOnMissingBean(CustomerMapper.class)
    CustomerMapper customerMapper() {
        return Mappers.getMapper(CustomerMapper.class);
    }
}

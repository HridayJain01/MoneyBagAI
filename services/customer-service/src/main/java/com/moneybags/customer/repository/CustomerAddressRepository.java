package com.moneybags.customer.repository;
import com.moneybags.customer.entity.CustomerAddress;
import com.moneybags.customer.enums.AddressType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {
    List<CustomerAddress> findByCustomerCifNo(String cifNo);
    Optional<CustomerAddress> findFirstByCustomerCifNoAndAddressTypeAndIsCurrentTrue(String cifNo, AddressType addressType);
    boolean existsByCustomerCifNo(String cifNo);
    boolean existsByCustomerCifNoAndCountryIgnoreCase(String cifNo, String country);
}

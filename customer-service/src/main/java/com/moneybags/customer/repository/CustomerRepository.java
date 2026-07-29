package com.moneybags.customer.repository;
import com.moneybags.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByPanNo(String panNo);
}

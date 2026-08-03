package com.moneybags.customer.repository;
import com.moneybags.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerRepository extends JpaRepository<Customer, String> {
    boolean existsByPanNo(String panNo);
    boolean existsByUserId(Long userId);
    java.util.List<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrMobileContainingOrEmailContainingIgnoreCase(String firstName, String lastName, String mobile, String email);
    java.util.List<Customer> findByRelationshipManagerEmpId(Long empId);
}

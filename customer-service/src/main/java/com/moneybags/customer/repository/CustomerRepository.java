package com.moneybags.customer.repository;
import com.moneybags.customer.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, String> {
    boolean existsByPanNo(String panNo);
    boolean existsByUserId(Long userId);
    List<Customer> findByRelationshipManagerEmpId(Long empId);

    @Query("""
            select c from Customer c
            where lower(c.cifNo) like lower(concat('%', :query, '%'))
               or lower(c.firstName) like lower(concat('%', :query, '%'))
               or lower(coalesce(c.lastName, '')) like lower(concat('%', :query, '%'))
               or lower(coalesce(c.email, '')) like lower(concat('%', :query, '%'))
               or c.mobile like concat('%', :query, '%')
               or c.panNo like concat('%', upper(:query), '%')
            """)
    List<Customer> search(@Param("query") String query);
}

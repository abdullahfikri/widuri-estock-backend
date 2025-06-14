package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Integer> {
    boolean existsBySupplierName(String supplierName);

    boolean existsByEmail(String email);
}

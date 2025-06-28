package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.incoming_product.IncomingProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface IncomingProductRepository extends JpaRepository<IncomingProduct, Integer> {
    Page<IncomingProduct> findByDateInBetween(LocalDate dateInAfter, LocalDate dateInBefore, Pageable pageable);
}

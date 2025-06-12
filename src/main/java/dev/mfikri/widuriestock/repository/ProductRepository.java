package dev.mfikri.widuriestock.repository;

import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.entity.product.Product;
import dev.mfikri.widuriestock.entity.product.ProductPhoto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    boolean existsByName(String name);

    Page<ProductSummary> findBy(Pageable pageable);
}


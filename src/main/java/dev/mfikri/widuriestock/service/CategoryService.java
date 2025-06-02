package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryCreateRequest request);
    CategoryResponse get(Integer id);
    List<CategoryResponse> getList();
    CategoryResponse update(Integer id);
    void delete(Integer id);
}

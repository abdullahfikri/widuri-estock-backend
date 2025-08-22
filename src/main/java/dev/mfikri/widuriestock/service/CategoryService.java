package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryResponse;
import dev.mfikri.widuriestock.model.product.CategoryUpdateRequest;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryCreateRequest request);
    CategoryResponse get(Integer id);
    List<CategoryResponse> getList();
    CategoryResponse update(CategoryUpdateRequest request);
    void delete(Integer id);
}

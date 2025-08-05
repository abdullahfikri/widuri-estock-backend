package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryResponse;
import dev.mfikri.widuriestock.model.product.CategoryUpdateRequest;
import dev.mfikri.widuriestock.repository.CategoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {
    private final ValidationService validationService;
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(ValidationService validationService, CategoryRepository categoryRepository) {
        this.validationService = validationService;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        log.info("Processing request to create a new category.");

        validationService.validate(request);

        log.debug("Verifying if category name is already exists. categoryName={}", request.getName());
        boolean isExists = categoryRepository.existsByName(request.getName());
        if (isExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category name is already exists.");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        log.debug("Saving new category entity to the database.");
        categoryRepository.save(category);

        log.info("Successfully create a new category. categoryId={}", category.getId());
        return toCategoryResponse(category);
    }

    @Override
    public CategoryResponse get(Integer id) {
        log.info("Processing request to get a category. categoryId={}", id);

        Category category = findCategoryByIdOrThrows(id);

        log.info("Successfully get a category. categoryId={}", id);
        return toCategoryResponse(category);
    }

    @Override
    public List<CategoryResponse> getList() {
        log.info("Processing request to get list of categories.");

        log.debug("Finding list of category in database.");
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Order.asc("name")));

        log.info("Successfully get list of categories. count={}", categories.size());
        return categories.stream().map(this::toCategoryResponse).toList();
    }

    @Override
    public CategoryResponse update(CategoryUpdateRequest request) {
        log.info("Processing request to update a category. categoryId={}", request.getId());
        validationService.validate(request);

        Category category = findCategoryByIdOrThrows(request.getId());
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        log.debug("Saving updated category entity to the database.");
        categoryRepository.save(category);

        log.info("Successfully update a category. categoryId={}", request.getId());
        return toCategoryResponse(category);
    }

    @Override
    public void delete(Integer id) {
        log.info("Processing request to delete a category. categoryId={}", id);

        Category category = findCategoryByIdOrThrows(id);

        log.debug("Deleting category entity from the database.");
        categoryRepository.deleteById(category.getId());

        log.info("Successfully delete a category. categoryId={}", id);
    }

    private Category findCategoryByIdOrThrows(Integer id) {
        log.debug("Finding category by id. categoryId={}", id);
        return categoryRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category is not found."));
    }

    private CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}

package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryResponse;
import dev.mfikri.widuriestock.model.product.CategoryUpdateRequest;
import dev.mfikri.widuriestock.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping(path = "/categories",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<CategoryResponse> create(@RequestBody CategoryCreateRequest request) {
        log.info("Receiving request to create new category.");

        CategoryResponse response = categoryService.create(request);

        return WebResponse.<CategoryResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/categories",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<CategoryResponse>> getList() {
        log.info("Receiving request to get all available category.");

        List<CategoryResponse> response = categoryService.getList();

        return WebResponse.<List<CategoryResponse>>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/categories/{categoryId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<CategoryResponse> get(@PathVariable Integer categoryId) {
        log.info("Receiving request to get a category. categoryId={}.", categoryId);

        CategoryResponse response = categoryService.get(categoryId);

        return WebResponse.<CategoryResponse>builder()
                .data(response)
                .build();
    }

    @PutMapping(path = "/categories/{categoryId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<CategoryResponse> update(@RequestBody CategoryUpdateRequest request, @PathVariable Integer categoryId) {
        log.info("Receiving request to update a category. categoryId={}.", categoryId);

        request.setId(categoryId);
        CategoryResponse response = categoryService.update(request);

        return WebResponse.<CategoryResponse>builder()
                .data(response)
                .build();
    }

    @DeleteMapping(path = "/categories/{categoryId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<String> delete(@PathVariable Integer categoryId) {
        log.info("Receiving request to delete a category. categoryId={}.", categoryId);

        categoryService.delete(categoryId);

        return WebResponse.<String>builder()
                .data("OK")
                .build();
    }
}

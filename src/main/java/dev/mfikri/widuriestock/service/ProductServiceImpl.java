package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.product.*;
import dev.mfikri.widuriestock.model.product.ProductCreateRequest;
import dev.mfikri.widuriestock.model.product.ProductResponse;
import dev.mfikri.widuriestock.model.product.ProductsGetListResponse;
import dev.mfikri.widuriestock.repository.*;
import dev.mfikri.widuriestock.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final ValidationService validationService;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductPhotoRepository productPhotoRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductVariantAttributeRepository productVariantAttributeRepository;

    public ProductServiceImpl(ValidationService validationService, CategoryRepository categoryRepository, ProductRepository productRepository, ProductPhotoRepository productPhotoRepository, ProductVariantRepository productVariantRepository, ProductVariantAttributeRepository productVariantAttributeRepository) {
        this.validationService = validationService;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productPhotoRepository = productPhotoRepository;
        this.productVariantRepository = productVariantRepository;
        this.productVariantAttributeRepository = productVariantAttributeRepository;
    }

    @Override
    @Transactional
    public ProductResponse create(ProductCreateRequest request) {
        validationService.validate(request);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category is not found."));

        boolean productNameIsExists = productRepository.existsByName(request.getName());

        if (productNameIsExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is already exists.");
        }

        if (!request.getHasVariant() && (request.getPrice() == null || request.getStock() == null )) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price and Stock must be included when 'hasVariant' is false.");
        }

        if (request.getHasVariant() && (request.getPrice() != null || request.getStock() != null )) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price and Stock must not be included when 'hasVariant' is true.");
        }


        if (!request.getHasVariant() && request.getVariants() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variants must not be included when 'hasVariant' is false.");
        }

        if (request.getHasVariant() && request.getVariants() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variant must be included when 'hasVariant' is true.");
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setHasVariant(request.getHasVariant());
        product.setStock(request.getStock());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        productRepository.save(product);

        List<ProductResponse.ProductPhoto> productPhotosResponse = new ArrayList<>();

        if (request.getProductPhotos() != null) {
            List<ProductPhoto> productPhotos = new ArrayList<>();
            for (int i = 0; i < request.getProductPhotos().size(); i++) {
                ProductCreateRequest.ProductPhoto productPhoto = request.getProductPhotos().get(i);

                Path path = ImageUtil.uploadPhoto(productPhoto.getImage(), product.getName() + "-" + i, true);

                ProductPhoto photo = new ProductPhoto();
                photo.setProduct(product);
                photo.setImageLocation(path.toString());
                productPhotos.add(photo);

                ProductResponse.ProductPhoto pPhotoResponse = new ProductResponse.ProductPhoto(photo.getImageLocation());
                productPhotosResponse.add(pPhotoResponse);
            }
            productPhotoRepository.saveAll(productPhotos);
        }

        List<ProductResponse.ProductVariant> productVariantResponse = new ArrayList<>();

        List<ProductVariant> productVariants = new ArrayList<>();
        if (request.getVariants() != null) {

            Set<String> skuVariantProductSet = new HashSet<>();

            request.getVariants().forEach(productVariant -> {

                ProductVariant pVariant = new ProductVariant();
                pVariant.setSku(productVariant.getSku());
                pVariant.setStock(productVariant.getStock());
                pVariant.setPrice(productVariant.getPrice());
                pVariant.setProduct(product);
                productVariants.add(pVariant);
                skuVariantProductSet.add(productVariant.getSku());
            });

            if (skuVariantProductSet.size() != productVariants.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variants 'sku' must be unique in a product.");
            }

            productVariantRepository.saveAll(productVariants);


            List<ProductVariantAttribute> productVariantAttributes = new ArrayList<>();
            int attributeSize = 0;
            for (int i = 0; i < productVariants.size(); i++) {

                ProductCreateRequest.ProductVariant productVariant = request.getVariants().get(i);
                if (i != 0 && productVariant.getAttributes().size() != attributeSize){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'Attribute' size must be same for each 'Variant'.");
                }
                attributeSize = productVariant.getAttributes().size();

                // pVariant
                ProductVariant pVariant = productVariants.get(i);

                productVariant.getAttributes().forEach(productVariantAttribute -> {
                    ProductVariantAttribute attribute = new ProductVariantAttribute();
                    attribute.setAttributeKey(productVariantAttribute.getAttributeKey());
                    attribute.setAttributeValue(productVariantAttribute.getAttributeValue());
                    attribute.setProductVariant(pVariant);

                    productVariantAttributes.add(attribute);
                });

                // write product variant response
                ProductResponse.ProductVariant pVariantResponse = new ProductResponse.ProductVariant();
                pVariantResponse.setId(pVariant.getId());
                pVariantResponse.setSku(pVariant.getSku());
                pVariantResponse.setStock(pVariant.getStock());
                pVariantResponse.setPrice(pVariant.getPrice());

                productVariantResponse.add(pVariantResponse);
            }
            productVariantAttributeRepository.saveAll(productVariantAttributes);
        }


        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .hasVariant(product.getHasVariant())
                .stock(product.getStock())
                .price(product.getPrice())
                .category(dev.mfikri.widuriestock.model.product.Category
                        .builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build())
                .photos(productPhotosResponse)
                .variants(productVariantResponse)
                .build();
    }

    @Override
    public Page<ProductsGetListResponse> getList(Integer page, Integer size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("name")));

        Page<ProductSummary> products = productRepository.findBy(pageable);

        List<ProductsGetListResponse> productsListResponse = products.getContent().stream().map(productSummary -> {
            ProductsGetListResponse product = new ProductsGetListResponse();
            product.setId(productSummary.getId());
            product.setName(productSummary.getName());
            product.setDescription(productSummary.getDescription());
            product.setCategory(dev.mfikri.widuriestock.model.product.Category.builder()
                            .id(productSummary.getCategory().getId())
                            .name(productSummary.getCategory().getName())
                    .build());
            ProductPhoto productPhoto = productSummary.getProductPhotos();
            if (productPhoto!=null) {
                product.setImageLocation(productPhoto.getImageLocation());
            } else {
                product.setImageLocation(null);
            }

            return product;
        }).toList();


        return new PageImpl<>(productsListResponse, pageable, products.getTotalElements());
    }
}

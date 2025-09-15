package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.product.*;
import dev.mfikri.widuriestock.model.product.*;
import dev.mfikri.widuriestock.repository.*;
import dev.mfikri.widuriestock.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.util.*;

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

        {
            if (productNameIsExists) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product name is already exists.");
            }

            if (!request.getHasVariant() && (request.getPrice() == null || request.getStock() == null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price and Stock must be included when 'hasVariant' is false.");
            }

            if (request.getHasVariant() && (request.getPrice() != null || request.getStock() != null)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price and Stock must not be included when 'hasVariant' is true.");
            }


            if (!request.getHasVariant() && request.getVariants() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variants must not be included when 'hasVariant' is false.");
            }

            if (request.getHasVariant() && request.getVariants() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variant must be included when 'hasVariant' is true.");
            }
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setHasVariant(request.getHasVariant());
        product.setStock(request.getStock());
        product.setPrice(request.getPrice());
        product.setCategory(category);
        productRepository.save(product);

        List<ProductVariant> productVariantsList = new ArrayList<>();
        Map<String, List<ProductVariantAttribute>> productVariantAttributesMap = new HashMap<>();

        if (request.getHasVariant()) {
            Set<String> skuVariantProductSet = new HashSet<>();

            request.getVariants().forEach(productVariantCreateRequest -> {
                skuVariantProductSet.add(productVariantCreateRequest.getSku());

                ProductVariant productVariant = new ProductVariant();
                productVariant.setSku(productVariantCreateRequest.getSku());
                productVariant.setPrice(productVariantCreateRequest.getPrice());
                productVariant.setStock(productVariantCreateRequest.getStock());
                productVariant.setProduct(product);

                productVariantsList.add(productVariant);
            });

            // check sku duplicate
            if (skuVariantProductSet.size() != productVariantsList.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variants 'sku' must be unique in a product.");
            }

            productVariantRepository.saveAll(productVariantsList);

            int attributeSize = 0;
            for (int i = 0; i < productVariantsList.size(); i++) {
                ProductCreateRequest.ProductVariantCreateRequest productVariantCreateRequest = request.getVariants().get(i);

                if (productVariantCreateRequest.getAttributes().size() > 2) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant 'Attribute' size must not exceed than 2.");
                }

                if (i != 0 && productVariantCreateRequest.getAttributes().size() != attributeSize){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Variant 'Attribute' size must be same for each 'Variant'.");
                }
                attributeSize = productVariantCreateRequest.getAttributes().size();

                ProductVariant productVariantCurrent = productVariantsList.get(i);

                productVariantCreateRequest.getAttributes().forEach(productVariantAttributeCreateRequest -> {
                    ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
                    productVariantAttribute.setProductVariant(productVariantCurrent);
                    productVariantAttribute.setAttributeKey(productVariantAttributeCreateRequest.getAttributeKey());
                    productVariantAttribute.setAttributeValue(productVariantAttributeCreateRequest.getAttributeValue());

                    // inisiasi jike belum ada, jika sudah ada return listnya.
                    productVariantAttributesMap.computeIfAbsent(productVariantCurrent.getSku(), sku -> new ArrayList<>()).add(productVariantAttribute);
                });
            }

            productVariantAttributeRepository.saveAll(productVariantAttributesMap.values().stream().flatMap(Collection::stream).toList());
        }

        // write productVariant response
        List<ProductResponse.ProductVariant> productVariantsResponse = productVariantsList.stream().map(productVariant -> {
            ProductResponse.ProductVariant productVariantResponse = new ProductResponse.ProductVariant();
            productVariantResponse.setId(productVariant.getId());
            productVariantResponse.setSku(productVariant.getSku());
            productVariantResponse.setPrice(productVariant.getPrice());
            productVariantResponse.setStock(productVariant.getStock());

            List<ProductResponse.ProductVariantAttribute> productVariantAttributesResponse = productVariantAttributesMap
                    .get(productVariant.getSku())
                    .stream()
                    .map(productVariantAttribute -> {
                        ProductResponse.ProductVariantAttribute productVariantAttributeResponse = new ProductResponse.ProductVariantAttribute();
                        productVariantAttributeResponse.setId(productVariantAttribute.getId());
                        productVariantAttributeResponse.setAttributeKey(productVariantAttribute.getAttributeKey());
                        productVariantAttributeResponse.setAttributeValue(productVariantAttribute.getAttributeValue());
                        return productVariantAttributeResponse;
                    }).toList();

            productVariantResponse.setAttributes(productVariantAttributesResponse);

            return productVariantResponse;
        }).toList();

        List<ProductPhoto> productPhotos = new ArrayList<>();


        List<ProductResponse.ProductPhoto> productPhotosResponse = new ArrayList<>();
        if (request.getProductPhotos() != null) {
            for (int i = 0; i < request.getProductPhotos().size(); i++) {
                ProductCreateRequest.ProductPhotoCreateRequest productPhotoCreateRequest = request.getProductPhotos().get(i);

                ProductPhoto productPhoto = new ProductPhoto();
                productPhoto.setProduct(product);
                productPhoto.setId(UUID.randomUUID().toString());

                Path path = ImageUtil.uploadPhoto(productPhotoCreateRequest.getImage(), product.getName() + "-" + productPhoto.getId(), true);
                productPhoto.setImageLocation(path.toString());
                productPhotos.add(productPhoto);

                // write product photo response
                ProductResponse.ProductPhoto productPhotoResponse = new ProductResponse.ProductPhoto();
                productPhotoResponse.setId(productPhoto.getId());
                productPhotoResponse.setImageLocation(productPhoto.getImageLocation());
                productPhotosResponse.add(productPhotoResponse);
            }
            productPhotoRepository.saveAll(productPhotos);
        }


        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .hasVariant(product.getHasVariant())
                .stock(product.getStock())
                .price(product.getPrice())
                .categoryResponse(CategoryResponse
                        .builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build())
                .variants(productVariantsResponse)
                .photos(productPhotosResponse.isEmpty() ? null: productPhotosResponse)
                .build();


    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductsGetListResponse> getList(Integer page, Integer size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());

        Page<ProductSummary> productsPage = productRepository.findProductListView(pageable);

        List<ProductsGetListResponse> productsListResponse = productsPage.getContent().stream().map(productSummary -> {
            ProductsGetListResponse product = new ProductsGetListResponse();
            product.setId(productSummary.getId());
            product.setName(productSummary.getName());
            product.setDescription(productSummary.getDescription());
            product.setCategoryResponse(CategoryResponse.builder()
                            .id(productSummary.getCategoryId())
                            .name(productSummary.getCategoryName())
                    .build());
            product.setImageLocation(productSummary.getImageLocation());

            return product;
        }).toList();


        return new PageImpl<>(productsListResponse, pageable, productsPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse get(Integer productId) {
        Product product = findProductByIdOrThrows(productId);

        List<ProductResponse.ProductPhoto> productPhotos = product.getProductPhotos().stream().map(this::toProductPhotoResponse).toList();
        List<ProductResponse.ProductVariant> productVariants = product.getProductVariants().stream().map(this::toProductVariantResponse).toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .hasVariant(product.getHasVariant())
                .stock(product.getStock())
                .price(product.getPrice())
                .categoryResponse(CategoryResponse.builder()
                        .id(product.getCategory().getId())
                        .name(product.getCategory().getName())
                        .build())
                .photos(productPhotos)
                .variants(productVariants)
                .build();
    }

    @Override
    @Transactional
    public ProductResponse update(ProductUpdateRequest request) {
        validationService.validate(request);

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category is not found."));

        Product product = findProductByIdOrThrows(request.getId());

        List<ProductPhoto> productPhotos = new ArrayList<>();
        List<ProductVariant> productVariants = new ArrayList<>();
        List<ProductVariantAttribute> productVariantAttributes = new ArrayList<>();


        if (request.getHasVariant()) {
            if (request.getPrice() != null || request.getStock() != null ) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price and Stock must not be included when 'hasVariant' is true.");
            }
            if (request.getVariants() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variant must be included when 'hasVariant' is true.");
            }

            product.setCategory(category);
            product.setName(request.getName());
            product.setDescription(request.getDescription());
            product.setStock(null);
            product.setPrice(null);
            product.setHasVariant(request.getHasVariant());

            // update variant
            productVariants = request.getVariants().stream().map(productVariantUpdateRequest -> {
                ProductVariant productVariant;
                if (productVariantUpdateRequest.getId() != null) {
                    log.info(String.valueOf(productVariantUpdateRequest.getId()));
                    log.info(String.valueOf(product.getId()));
                    productVariant = productVariantRepository.findByIdAndProduct(productVariantUpdateRequest.getId(), product)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product variant is not found"));
                } else {
                    productVariant = new ProductVariant();
                    productVariant.setProduct(product);
                }

                productVariant.setSku(productVariantUpdateRequest.getSku());
                productVariant.setStock(productVariantUpdateRequest.getStock());
                productVariant.setPrice(productVariantUpdateRequest.getPrice());
                return productVariant;
            }).toList();
            productVariantRepository.saveAll(productVariants);

            // update variant attribute
            for (int i = 0; i < productVariants.size(); i++) {
                ProductVariant productVariant = productVariants.get(i);
                ProductUpdateRequest.ProductVariantUpdateRequest productVariantUpdateRequest = request.getVariants().get(i);

                List<ProductVariantAttribute> variantAttributes = productVariantUpdateRequest.getAttributes().stream().map(productVariantAttributeUpdateRequest -> {
                    ProductVariantAttribute productVariantAttribute;

                    if (productVariantAttributeUpdateRequest.getId() != null) {
                        productVariantAttribute = productVariantAttributeRepository.findByIdAndProductVariant(productVariantAttributeUpdateRequest.getId(), productVariant)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product variant attribute is not found"));
                    } else {
                        productVariantAttribute = new ProductVariantAttribute();
                        productVariantAttribute.setProductVariant(productVariant);
                    }

                    productVariantAttribute.setAttributeKey(productVariantAttributeUpdateRequest.getAttributeKey());
                    productVariantAttribute.setAttributeValue(productVariantAttributeUpdateRequest.getAttributeValue());
                    return productVariantAttribute;
                }).toList();

                productVariantAttributes.addAll(variantAttributes);
            }

            productVariantAttributeRepository.saveAll(productVariantAttributes);

        } else {
            if ((request.getPrice() == null || request.getStock() == null )) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price and Stock must be included when 'hasVariant' is false.");
            }
            if (request.getVariants() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product variants must not be included when 'hasVariant' is false.");
            }
            log.info("invoke");
            productVariantRepository.deleteAllByProduct(product);
            log.info("invoke1");

            product.setCategory(category);
            product.setName(request.getName());
            product.setDescription(request.getDescription());
            product.setStock(request.getStock());
            product.setPrice(request.getPrice());
            product.setHasVariant(request.getHasVariant());
            product.setProductVariants(null);
        }


        if (request.getProductPhotos() != null) {
            log.info("photo");
            for (int i = 0; i < request.getProductPhotos().size(); i++) {
                ProductUpdateRequest.ProductPhotoUpdateRequest productPhotoUpdateRequest = request.getProductPhotos().get(i);
                ProductPhoto photo;

                if (productPhotoUpdateRequest.getId() != null) {
                    photo = productPhotoRepository.findByIdAndProduct(productPhotoUpdateRequest.getId(), product)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product photo is not found"));
                } else {
                    photo = new ProductPhoto();
                    photo.setId(UUID.randomUUID().toString());
                }

                if (productPhotoUpdateRequest.getImage() != null) {
                    Path path = ImageUtil.uploadPhoto(productPhotoUpdateRequest.getImage(), product.getName() + "-" + photo.getId(), true);
                    photo.setImageLocation(path.toString());
                } else {
                    photo.setImageLocation(productPhotoUpdateRequest.getImageLocation());
                }
                photo.setProduct(product);
                productPhotos.add(photo);
            }
            productPhotoRepository.saveAll(productPhotos);
        }
        productRepository.save(product);


        List<ProductResponse.ProductVariant> productVariantsResponse = new ArrayList<>();
        if (!productVariants.isEmpty()) {
            productVariantsResponse.addAll(productVariants.stream().map(productVariant -> {
                List<ProductVariantAttribute> variantAttributes = productVariantAttributes.stream()
                        .filter(productVariantAttribute -> Objects.equals(productVariantAttribute.getProductVariant().getId(), productVariant.getId())).toList();
                productVariant.setProductVariantAttributes(variantAttributes);
                return toProductVariantResponse(productVariant);
            }).toList());
        }

        List<ProductResponse.ProductPhoto> productPhotosResponse = new ArrayList<>();
        if (!productPhotos.isEmpty()) {
            productPhotosResponse.addAll(productPhotos.stream().map(this::toProductPhotoResponse).toList());
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .hasVariant(product.getHasVariant())
                .stock(product.getStock())
                .price(product.getPrice())
                .categoryResponse(CategoryResponse
                        .builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build())
                .variants(productVariantsResponse)
                .photos(productPhotosResponse)
                .build();
    }

    @Override
    @Transactional
    public void delete(Integer productId) {
        Product product = findProductByIdOrThrows(productId);
        productPhotoRepository.deleteAllByProduct(product);
        productRepository.delete(product);
    }

    private Product findProductByIdOrThrows(Integer productId) {
        return productRepository.findById(productId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not found."));
    }

    private ProductResponse.ProductPhoto toProductPhotoResponse(ProductPhoto productPhoto) {
        return ProductResponse.ProductPhoto.builder()
                .id(productPhoto.getId())
                .imageLocation(productPhoto.getImageLocation())
                .build();
    }

    private ProductResponse.ProductVariant toProductVariantResponse(ProductVariant productVariant) {
        List<ProductResponse.ProductVariantAttribute> attributes = productVariant.getProductVariantAttributes()
                .stream()
                .map(productVariantAttribute -> ProductResponse.ProductVariantAttribute.builder()
                    .id(productVariantAttribute.getId())
                    .attributeKey(productVariantAttribute.getAttributeKey())
                    .attributeValue(productVariantAttribute.getAttributeValue())
                    .build())
                .toList();


        return ProductResponse.ProductVariant.builder()
                .id(productVariant.getId())
                .sku(productVariant.getSku())
                .price(productVariant.getPrice())
                .stock(productVariant.getStock())
                .attributes(attributes)
                .build();
    }
}

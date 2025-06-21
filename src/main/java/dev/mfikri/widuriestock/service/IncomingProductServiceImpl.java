package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.Supplier;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.entity.incoming_product.IncomingProduct;
import dev.mfikri.widuriestock.entity.incoming_product.IncomingProductDetail;
import dev.mfikri.widuriestock.entity.incoming_product.IncomingProductVariantDetail;
import dev.mfikri.widuriestock.entity.product.Product;
import dev.mfikri.widuriestock.entity.product.ProductVariant;
import dev.mfikri.widuriestock.model.incoming_product.*;
import dev.mfikri.widuriestock.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class IncomingProductServiceImpl implements IncomingProductService {
    private final ValidationService validationService;

    private final IncomingProductRepository incomingProductRepository;
    private final IncomingProductDetailRepository incomingProductDetailRepository;
    private final IncomingProductVariantDetailRepository incomingProductVariantDetailRepository;

    private final SupplierRepository supplierRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public IncomingProductServiceImpl(ValidationService validationService, IncomingProductRepository incomingProductRepository, IncomingProductDetailRepository incomingProductDetailRepository, IncomingProductVariantDetailRepository incomingProductVariantDetailRepository, SupplierRepository supplierRepository, UserRepository userRepository, ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
        this.validationService = validationService;
        this.incomingProductRepository = incomingProductRepository;
        this.incomingProductDetailRepository = incomingProductDetailRepository;
        this.incomingProductVariantDetailRepository = incomingProductVariantDetailRepository;
        this.supplierRepository = supplierRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Override
    @Transactional
    public IncomingProductResponse create(IncomingProductCreateRequest request) {
        validationService.validate(request);
        log.info("test invoke");
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier is not found."));

        User user = userRepository.findById(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));

        Set<Integer> productsIdList = new HashSet<>();
        Map<Integer, Set<Integer>> productVariantsIdMap = new HashMap<>();


        request.getIncomingProductDetails().forEach(incomingProductDetails -> {
            // validation hasVariant
            {
                if (incomingProductDetails.getHasVariant() &&
                        (incomingProductDetails.getPricePerUnit() != null || incomingProductDetails.getQuantity() != null)
                ) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'pricePerUnit', and 'quantity' properties must not send when 'hasVariant' is true.");
                }

                if (incomingProductDetails.getHasVariant() && incomingProductDetails.getIncomingProductVariantDetails() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'IncomingProductVariantDetails' properties must not null when 'hasVariant' is true.");
                }

                if (!incomingProductDetails.getHasVariant() &&
                        (incomingProductDetails.getPricePerUnit() == null || incomingProductDetails.getQuantity() == null)
                ) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'pricePerUnit', and 'quantity' properties must not null when 'hasVariant' is false.");
                }

                if (!incomingProductDetails.getHasVariant() && incomingProductDetails.getIncomingProductVariantDetails() != null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'IncomingProductVariantDetails' properties must not send when 'hasVariant' is false.");
                }
            }

            // collect all productsIdList
            productsIdList.add(incomingProductDetails.getProductId());

            if (incomingProductDetails.getHasVariant()) {
                // collect all productVariantsIdMap and group with it's productId
                incomingProductDetails.getIncomingProductVariantDetails().forEach(incomingProductVariantDetail -> productVariantsIdMap.computeIfAbsent(incomingProductDetails.getProductId(), variantId -> new HashSet<>()).add(incomingProductVariantDetail.getVariantId()));
            }
        });

        // check is product duplicate
        if (productsIdList.size() != request.getIncomingProductDetails().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'Product id' must not duplicate in one IncomingProduct.");
        }

        // find all products by id
        List<Product> products = productRepository.findAllById(productsIdList);
        // check if all product id is valid
        if (products.size() != productsIdList.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of 'products id' is wrong.");
        }

        // 1. create IncomingProduct
        IncomingProduct incomingProductEntity = new IncomingProduct();
        incomingProductEntity.setDateIn(request.getDateIn());
        incomingProductEntity.setSupplier(supplier);
        incomingProductEntity.setUser(user);
        incomingProductEntity.setTotalProducts(request.getTotalProducts());
        incomingProductEntity.setNote(request.getNote());
        incomingProductRepository.save(incomingProductEntity);

        List<Product> updatedProductList = new ArrayList<>();
        List<ProductVariant> updatedProductVariant = new ArrayList<>();
        List<IncomingProductDetail> incomingProductDetailListEntity = new ArrayList<>();
        Map<Integer, List<IncomingProductVariantDetail>> incomingProductVariantDetailMapEntity = new HashMap<>();

        request.getIncomingProductDetails().forEach(incomingProductDetailsRequest -> {
            // filter product by equals with request product id.
            Product product = products.stream().filter(p -> p.getId().equals(incomingProductDetailsRequest.getProductId())).findFirst().orElse(null);
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not found.");
            }

            // 2. create IncomingProductDetails
            IncomingProductDetail incomingProductDetailEntity = new IncomingProductDetail();
            incomingProductDetailEntity.setHasVariant(incomingProductDetailsRequest.getHasVariant());
            incomingProductDetailEntity.setIncomingProduct(incomingProductEntity);
            incomingProductDetailEntity.setProduct(product);

            // check if product.hasVariant properties from database is equals to incomingProductDetail.hasVariant request property
            if (incomingProductDetailsRequest.getHasVariant() != product.getHasVariant()) {
                String status = product.getHasVariant() ? "has" : "not have";
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "'Product id': " + product.getId() + " is " + status + " variant, please input valid product and valid variant."  );
            }

            if (!incomingProductDetailsRequest.getHasVariant()) {
                // set incomingProductDetail without variant
                incomingProductDetailEntity.setPricePerUnit(incomingProductDetailsRequest.getPricePerUnit());
                incomingProductDetailEntity.setQuantity(incomingProductDetailsRequest.getQuantity());
                incomingProductDetailEntity.setTotalPrice(
                        incomingProductDetailsRequest.getPricePerUnit() * incomingProductDetailsRequest.getQuantity()
                );

                // update product stock
                product.setStock(product.getStock() + incomingProductDetailsRequest.getQuantity());
                product.setPrice(incomingProductDetailsRequest.getPricePerUnit());
                updatedProductList.add(product);
            } else {
                // set incomingProductDetail with variant
                AtomicInteger totalVariantQuantity = new AtomicInteger(0);
                AtomicInteger totalVariantPrice = new AtomicInteger(0);

                // check if product variant id is unique
                if (productVariantsIdMap.get(incomingProductDetailsRequest.getProductId()).size() != incomingProductDetailsRequest.getIncomingProductVariantDetails().size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ProductVariants id must be unique in single ProductVariantDetails.");
                }

                incomingProductDetailsRequest.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailRequest -> {
                    totalVariantQuantity.getAndAdd(incomingProductVariantDetailRequest.getQuantity());
                    totalVariantPrice.getAndAdd(incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity());
                });

                incomingProductDetailEntity.setTotalVariantQuantity(totalVariantQuantity.get());
                incomingProductDetailEntity.setTotalVariantPrice(totalVariantPrice.get());
            }

            incomingProductDetailListEntity.add(incomingProductDetailEntity);
        });

        // save to db all incomingProductDetail
        incomingProductDetailRepository.saveAll(incomingProductDetailListEntity);
        // update product to db
        productRepository.saveAll(updatedProductList);



        // create incomingProductVariantDetail
        if (!productVariantsIdMap.isEmpty()) {
            for (int i = 0; i < request.getIncomingProductDetails().size(); i++) {
                IncomingProductDetail incomingProductDetailEntity = incomingProductDetailListEntity.get(i);
                IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = request.getIncomingProductDetails().get(i);

                if (incomingProductDetailEntity.getHasVariant()) {
                    Product product = products.stream().filter(p -> p.getId().equals(incomingProductDetailEntity.getProduct().getId())).findFirst().orElse(null);
                    if (product == null) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not found.");
                    }
                    List<ProductVariant> productVariantList = productVariantRepository.findAllById(productVariantsIdMap.get(incomingProductDetailsRequest.getProductId()));

                    if (productVariantList.size() != incomingProductDetailsRequest.getIncomingProductVariantDetails().size()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of product variant is not found, please check product variant id again.");
                    }

                    incomingProductDetailsRequest.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailRequest -> {
                        ProductVariant productVariant = productVariantList.stream().filter(pV -> Objects.equals(pV.getId(), incomingProductVariantDetailRequest.getVariantId())).findFirst().orElse(null);

                        if (productVariant == null) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product variant with id " + incomingProductVariantDetailRequest.getVariantId() + " is not found.");
                        }

                        if (!Objects.equals(productVariant.getProduct().getId(), product.getId())) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,"'Product Variant' " + productVariant.getId() + " is not product variant for 'Product' " + product.getId() );
                        }

                        IncomingProductVariantDetail incomingProductVariantDetailEntity = new IncomingProductVariantDetail();
                        incomingProductVariantDetailEntity.setIncomingProductDetail(incomingProductDetailEntity);
                        incomingProductVariantDetailEntity.setProductVariant(productVariant);
                        incomingProductVariantDetailEntity.setPricePerUnit(incomingProductVariantDetailRequest.getPricePerUnit());
                        incomingProductVariantDetailEntity.setQuantity(incomingProductVariantDetailRequest.getQuantity());
                        incomingProductVariantDetailEntity.setTotalPrice(
                                incomingProductVariantDetailRequest.getQuantity() * incomingProductVariantDetailRequest.getPricePerUnit()
                        );

                        // update product variant
                        productVariant.setStock(productVariant.getStock() + incomingProductVariantDetailRequest.getQuantity());
                        productVariant.setPrice(incomingProductVariantDetailRequest.getPricePerUnit());
                        updatedProductVariant.add(productVariant);

                        incomingProductVariantDetailMapEntity.computeIfAbsent(incomingProductDetailEntity.getId() , key -> new ArrayList<>()).add(incomingProductVariantDetailEntity);

                    });
                }
            }

            productVariantRepository.saveAll(updatedProductVariant);
            incomingProductVariantDetailRepository.saveAll(incomingProductVariantDetailMapEntity.values().stream().flatMap(Collection::stream).toList());
        }

        List<IncomingProductResponse.IncomingProductDetail> incomingProductDetailsResponse = incomingProductDetailListEntity.stream().map(incomingProductDetail -> {
            List<IncomingProductVariantDetail> incomingProductVariantDetailList = incomingProductVariantDetailMapEntity.get(incomingProductDetail.getId());

            List<IncomingProductResponse.IncomingProductVariantDetail> incomingProductVariantDetailsResponse = null;

            if (incomingProductVariantDetailList != null) {
                incomingProductVariantDetailsResponse  = incomingProductVariantDetailList
                    .stream()
                        .map(this::toIncomingProductVariantDetailResponse)
                        .toList();
            }

            return IncomingProductResponse.IncomingProductDetail.builder()
                    .id(incomingProductDetail.getId())
                    .product(IncomingProductProductResponse.builder()
                            .id(incomingProductDetail.getProduct().getId())
                            .name(incomingProductDetail.getProduct().getName())
                            .build())
                    .pricePerUnit(incomingProductDetail.getPricePerUnit())
                    .quantity(incomingProductDetail.getQuantity())
                    .totalPrice(incomingProductDetail.getTotalPrice())
                    .hasVariant(incomingProductDetail.getHasVariant())
                    .totalVariantQuantity(incomingProductDetail.getTotalVariantQuantity())
                    .totalVariantPrice(incomingProductDetail.getTotalVariantPrice())
                    .incomingProductVariantDetails(incomingProductVariantDetailsResponse)
                    .build();
        }).toList();


        return IncomingProductResponse.builder()
                .id(incomingProductEntity.getId())
                .dateIn(incomingProductEntity.getDateIn())
                .supplier(IncomingProductSupplierResponse.builder()
                        .id(supplier.getId())
                        .name(supplier.getSupplierName())
                        .build())
                .username(user.getUsername())
                .totalProduct(incomingProductEntity.getTotalProducts())
                .note(incomingProductEntity.getNote())
                .incomingProductDetails(incomingProductDetailsResponse)
                .build();
    }

    @Override
    public IncomingProductResponse get(Integer id) {
        // check id null
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id must be not null.");
        }

        // find incomingProductEntity
        IncomingProduct incomingProductEntity = incomingProductRepository.findById(id).orElse(null);
        if (incomingProductEntity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Incoming Product is not found.");
        }

        List<IncomingProductResponse.IncomingProductDetail> incomingProductDetailsResponse = this.toIncomingProductDetailList(incomingProductEntity.getIncomingProductDetails());

        // write response
        return IncomingProductResponse.builder()
                .id(incomingProductEntity.getId())
                .dateIn(incomingProductEntity.getDateIn())
                .supplier(IncomingProductSupplierResponse.builder()
                        .id(incomingProductEntity.getSupplier().getId())
                        .name(incomingProductEntity.getSupplier().getSupplierName())
                        .build())
                .username(incomingProductEntity.getUser().getUsername())
                .totalProduct(incomingProductEntity.getTotalProducts())
                .note(incomingProductEntity.getNote())
                .incomingProductDetails(incomingProductDetailsResponse)
                .build();
    }

    private IncomingProductResponse.IncomingProductVariantDetail toIncomingProductVariantDetailResponse (IncomingProductVariantDetail incomingProductVariantDetail) {
        return IncomingProductResponse.IncomingProductVariantDetail.builder()
                .id(incomingProductVariantDetail.getId())
                .variant(IncomingProductProductVariantResponse.builder()
                        .id(incomingProductVariantDetail.getProductVariant().getId())
                        .sku(incomingProductVariantDetail.getProductVariant().getSku())
                        .build())
                .pricePerUnit(incomingProductVariantDetail.getPricePerUnit())
                .quantity(incomingProductVariantDetail.getQuantity())
                .totalPrice(incomingProductVariantDetail.getTotalPrice())
                .build();
    }

    private List<IncomingProductResponse.IncomingProductDetail> toIncomingProductDetailList(List<IncomingProductDetail> incomingProductDetails) {
        return incomingProductDetails.stream().map(incomingProductDetail -> {
            List<IncomingProductResponse.IncomingProductVariantDetail> incomingProductVariantDetails = null;

            if (incomingProductDetail.getHasVariant()) {
                incomingProductVariantDetails = incomingProductDetail.getIncomingProductVariantDetails()
                        .stream()
                        .map(this::toIncomingProductVariantDetailResponse)
                        .toList();
            }

            return IncomingProductResponse.IncomingProductDetail.builder()
                    .id(incomingProductDetail.getId())
                    .product(IncomingProductProductResponse.builder()
                            .id(incomingProductDetail.getProduct().getId())
                            .name(incomingProductDetail.getProduct().getName())
                            .build())
                    .pricePerUnit(incomingProductDetail.getPricePerUnit())
                    .quantity(incomingProductDetail.getQuantity())
                    .totalPrice(incomingProductDetail.getTotalPrice())
                    .hasVariant(incomingProductDetail.getHasVariant())
                    .totalVariantQuantity(incomingProductDetail.getTotalVariantQuantity())
                    .totalVariantPrice(incomingProductDetail.getTotalVariantPrice())
                    .incomingProductVariantDetails(incomingProductVariantDetails)
                    .build();
        }).toList();
    }
}

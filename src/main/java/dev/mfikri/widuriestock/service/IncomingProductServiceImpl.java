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
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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
        Supplier supplier = findSupplierByIdOrThrows(request.getSupplierId());

        User user = userRepository.findById(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));

        Set<Integer> productsIdList = new HashSet<>();
        Map<Integer, Set<Integer>> productVariantsIdMap = new HashMap<>();


        request.getIncomingProductDetails().forEach(incomingProductDetails -> {
            // validation hasVariant
            incomingProductDetailsValidationHasVariant(incomingProductDetails.getHasVariant(),
                    incomingProductDetails.getPricePerUnit(),
                    incomingProductDetails.getQuantity(),
                    incomingProductDetails.getIncomingProductVariantDetails() == null,
                    incomingProductDetails.getIncomingProductVariantDetails() != null);

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of 'products' is not found, please check the productId again.");
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
            Product product = products.stream()
                    .filter(p -> p.getId().equals(incomingProductDetailsRequest.getProductId())).findFirst()
                    .orElse(null);
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not found.");
            }

            // 2. create IncomingProductDetails
            IncomingProductDetail incomingProductDetailEntity = new IncomingProductDetail();
            incomingProductDetailEntity.setHasVariant(incomingProductDetailsRequest.getHasVariant());
            incomingProductDetailEntity.setIncomingProduct(incomingProductEntity);
            incomingProductDetailEntity.setProduct(product);

            // check if product.hasVariant properties from database is equals to incomingProductDetail.hasVariant request property
            compareHasVariantIncomingProductDetailAndProduct(incomingProductDetailsRequest.getHasVariant() != product.getHasVariant(), product);

            if (!incomingProductDetailsRequest.getHasVariant()) {
                // set incomingProductDetail without variant
                incomingProductDetailEntity.setPricePerUnit(incomingProductDetailsRequest.getPricePerUnit());
                incomingProductDetailEntity.setQuantity(incomingProductDetailsRequest.getQuantity());
                incomingProductDetailEntity.setTotalPrice(
                        incomingProductDetailsRequest.getPricePerUnit() * incomingProductDetailsRequest.getQuantity()
                );

                // update product stock
                product.setStock(product.getStock() + incomingProductDetailsRequest.getQuantity());
                updatedProductList.add(product);
            } else {
                // set incomingProductDetail with variant
                AtomicInteger totalVariantQuantity = new AtomicInteger(0);
                AtomicInteger totalVariantPrice = new AtomicInteger(0);

                // check if product variant id is unique
                if (productVariantsIdMap.get(incomingProductDetailsRequest.getProductId()).size() != incomingProductDetailsRequest.getIncomingProductVariantDetails().size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ProductVariants id must not duplicate in a single ProductVariantDetails.");
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
                    Product product = products
                            .stream()
                            .filter(p -> p.getId().equals(incomingProductDetailEntity.getProduct().getId()))
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not found."));

                    List<ProductVariant> productVariantList = productVariantRepository.findAllById(productVariantsIdMap.get(incomingProductDetailsRequest.getProductId()));

                    if (productVariantList.size() != incomingProductDetailsRequest.getIncomingProductVariantDetails().size()) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of product variant is not found, please check product variant id again.");
                    }

                    incomingProductDetailsRequest.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailRequest -> {
                        ProductVariant productVariant = productVariantList.stream().filter(pV -> Objects.equals(pV.getId(), incomingProductVariantDetailRequest.getVariantId())).findFirst().orElse(null);

                        if (productVariant == null) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product variant with id " + incomingProductVariantDetailRequest.getVariantId() + " is not found. please check again");
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
                .totalProducts(incomingProductEntity.getTotalProducts())
                .note(incomingProductEntity.getNote())
                .incomingProductDetails(incomingProductDetailsResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public IncomingProductResponse get(Integer incomingProductId) {
        // check incomingProductId null
        if (incomingProductId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id must be not null.");
        }

        // find incomingProductEntity
        IncomingProduct incomingProductEntity = findIncomingProductByIdOrThrows(incomingProductId);

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
                .totalProducts(incomingProductEntity.getTotalProducts())
                .note(incomingProductEntity.getNote())
                .incomingProductDetails(incomingProductDetailsResponse)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncomingProductGetListResponse> getList(IncomingProductGetListRequest request) {
        validationService.validate(request);

        // validation startDate and endDate
        if (request.getStartDate() == null) {
            request.setStartDate(LocalDate.parse("1970-01-01"));
        }

        if (request.getEndDate() == null) {
            request.setEndDate(LocalDate.now());
        }


        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Start date " + request.getStartDate() + " must be before or equal to end date " + request.getEndDate() + ".");
        }


        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by(Sort.Order.asc("dateIn")));

        Page<IncomingProduct> incomingProductPage = incomingProductRepository.findByDateInBetween(request.getStartDate(), request.getEndDate(), pageable);

        List<IncomingProductGetListResponse> incomingProductsListResponse = incomingProductPage.getContent().stream().map(incomingProduct -> IncomingProductGetListResponse.builder()
                .id(incomingProduct.getId())
                .dateIn(incomingProduct.getDateIn())
                .supplier(IncomingProductSupplierResponse.builder()
                        .id(incomingProduct.getSupplier().getId())
                        .name(incomingProduct.getSupplier().getSupplierName())
                        .build())
                .username(incomingProduct.getUser().getUsername())
                .totalProducts(incomingProduct.getTotalProducts())
                .note(incomingProduct.getNote())
                .build()).toList();


        return new PageImpl<>(incomingProductsListResponse, pageable, incomingProductPage.getTotalElements());
    }

    @Transactional
    public IncomingProductResponse update(IncomingProductUpdateRequest request) {
        validationService.validate(request);

        // check if incoming product isPresent
        IncomingProduct incomingProduct = findIncomingProductByIdOrThrows(request.getId());

        // check if supplier is present
        Supplier supplier = findSupplierByIdOrThrows(request.getSupplierId());
        User user = userRepository.findById(request.getUsername()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));



        // update incomingProduct
        incomingProduct.setDateIn(request.getDateIn());
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);


        if (request.getTotalProducts() != request.getIncomingProductDetails().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total products 'IncomingProduct' is wrong.");
        }
        incomingProduct.setTotalProducts(request.getTotalProducts());
        incomingProduct.setNote(request.getNote());
        incomingProduct.setUpdateReason(request.getUpdateReason());
        incomingProductRepository.save(incomingProduct);

        List<Product> products = new ArrayList<>();
        List<ProductVariant> productVariants = new ArrayList<>();

        List<IncomingProductDetail> incomingProductDetails = new ArrayList<>();
        List<IncomingProductVariantDetail> incomingProductVariantDetails = new ArrayList<>();

        if (incomingProduct.getIncomingProductDetails() == null || incomingProduct.getIncomingProductDetails().size() != request.getIncomingProductDetails().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductDetails size is not same. Please check the IncomingProductDetails again.");
        }

        List<IncomingProductResponse.IncomingProductDetail> incomingProductDetailsResponse = incomingProduct.getIncomingProductDetails().stream().map(incomingProductDetail -> {

            List<IncomingProductUpdateRequest.IncomingProductDetail> incomingProductDetailUpdateRequestList = request.getIncomingProductDetails()
                    .stream()
                    .filter(iPD -> iPD.getId().equals(incomingProductDetail.getId()))
                    .toList();
            
            if (incomingProductDetailUpdateRequestList.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductDetail is not found, please check IncomingProductDetail id again.");
            } else if (incomingProductDetailUpdateRequestList.size() > 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductDetail is duplicate, please check IncomingProductDetails again.");
            }

            IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailUpdateRequest = incomingProductDetailUpdateRequestList.getFirst();


            // compare hasVariant
            if (incomingProductDetailUpdateRequest.getHasVariant() != incomingProductDetail.getHasVariant()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "IncomingProductDetails 'id': " + incomingProductDetail.getId() + " has status 'hasVariant': " + incomingProductDetail.getHasVariant().toString() + ", please input valid IncomingProductDetail hasVariant.");
            }

            // validation hasVariant
            incomingProductDetailsValidationHasVariant(incomingProductDetailUpdateRequest.getHasVariant(),
                    incomingProductDetailUpdateRequest.getPricePerUnit(),
                    incomingProductDetailUpdateRequest.getQuantity(),
                    incomingProductDetailUpdateRequest.getIncomingProductVariantDetails() == null,
                    incomingProductDetailUpdateRequest.getIncomingProductVariantDetails() != null);


            Product product = incomingProductDetail.getProduct();

            if (!incomingProductDetailUpdateRequest.getProductId().equals(product.getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product id: " + incomingProductDetailUpdateRequest.getProductId() +
                        " is wrong, please check Product id again.");
            }

            // check if product is already updated the hasVariant
            if (incomingProductDetailUpdateRequest.getHasVariant() != product.getHasVariant()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product id: " + product.getId() + " is already change hasVariant status, " +
                        "please delete and create new IncomingProductDetail for this IncomingProduct.");
            }

            List<IncomingProductResponse.IncomingProductVariantDetail> incomingProductVariantDetailsResponse = null;

            if (!incomingProductDetail.getHasVariant()) {
                // update the Product stock
                int quantityChange = incomingProductDetailUpdateRequest.getQuantity() - incomingProductDetail.getQuantity();
                // quantityChange = 15 - 10 = 5 | stockProduct = currentStock + +5
                // quantityChange = 5 - 10 = -5 | stockProduct = currentStock + -5
                product.setStock(product.getStock() + quantityChange);
                products.add(product);

                incomingProductDetail.setPricePerUnit(incomingProductDetailUpdateRequest.getPricePerUnit());
                incomingProductDetail.setQuantity(incomingProductDetailUpdateRequest.getQuantity());
                incomingProductDetail.setTotalPrice(
                        incomingProductDetailUpdateRequest.getQuantity() * incomingProductDetailUpdateRequest.getPricePerUnit()
                );
                incomingProductDetail.setHasVariant(false);
                incomingProductDetail.setTotalVariantQuantity(null);
                incomingProductDetail.setTotalVariantPrice(null);
                incomingProductDetail.setIncomingProductVariantDetails(null);
            } else {
                incomingProductDetail.setPricePerUnit(null);
                incomingProductDetail.setQuantity(null);
                incomingProductDetail.setTotalPrice(null);
                incomingProductDetail.setHasVariant(true);

                if (incomingProductDetail.getIncomingProductVariantDetails().size() != incomingProductDetailUpdateRequest.getIncomingProductVariantDetails().size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductVariantDetails size is not same. Please check the ncomingProductVariantDetails again.");
                }

                incomingProductVariantDetailsResponse = incomingProductDetail.getIncomingProductVariantDetails().stream().map(incomingProductVariantDetail -> {

                    IncomingProductUpdateRequest.IncomingProductVariantDetail incomingProductVariantDetailUpdateRequest = incomingProductDetailUpdateRequest.getIncomingProductVariantDetails()
                            .stream()
                            .filter(iPVD -> iPVD.getId().equals(incomingProductVariantDetail.getId()))
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductVariantDetail is not found, please check IncomingProductVariantDetail id again."));

                    ProductVariant productVariant = incomingProductVariantDetail.getProductVariant();

                    if (!incomingProductVariantDetailUpdateRequest.getVariantId().equals(productVariant.getId())) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductVariant id: " + incomingProductVariantDetailUpdateRequest.getVariantId() +
                                " is wrong, please check ProductVariant id again.");
                    }
                    // update the ProductVariant stock
                    int quantityChange = incomingProductVariantDetailUpdateRequest.getQuantity() - incomingProductVariantDetail.getQuantity();
                    productVariant.setStock(productVariant.getStock() + quantityChange);
                    productVariants.add(productVariant);

                    incomingProductVariantDetail.setPricePerUnit(incomingProductVariantDetailUpdateRequest.getPricePerUnit());
                    incomingProductVariantDetail.setQuantity(incomingProductVariantDetailUpdateRequest.getQuantity());
                    incomingProductVariantDetail.setTotalPrice(
                            incomingProductVariantDetailUpdateRequest.getPricePerUnit() *
                                    incomingProductVariantDetailUpdateRequest.getQuantity()
                    );

                    // set incomingProductDetailUpdateRequest.totalVariantQuantity && incomingProductDetail.totalVariantPrice
                    if (incomingProductDetailUpdateRequest.getTotalVariantQuantity() == null) {
                        incomingProductDetailUpdateRequest.setTotalVariantQuantity(incomingProductVariantDetailUpdateRequest.getQuantity());
                    } else {
                        incomingProductDetailUpdateRequest.setTotalVariantQuantity(
                                incomingProductDetailUpdateRequest.getTotalVariantQuantity() + incomingProductVariantDetailUpdateRequest.getQuantity()
                        );
                    }

                    if (incomingProductDetailUpdateRequest.getTotalVariantPrice() == null) {
                        incomingProductDetailUpdateRequest.setTotalVariantPrice(incomingProductVariantDetail.getTotalPrice());
                    } else {
                        incomingProductDetailUpdateRequest.setTotalVariantPrice(
                                incomingProductDetailUpdateRequest.getTotalVariantPrice() + incomingProductVariantDetail.getTotalPrice()
                        );
                    }

                    incomingProductVariantDetails.add(incomingProductVariantDetail);

                    // write response
                    return IncomingProductResponse.IncomingProductVariantDetail.builder()
                            .id(incomingProductVariantDetail.getId())
                            .variant(IncomingProductProductVariantResponse.builder()
                                    .id(productVariant.getId())
                                    .sku(productVariant.getSku())
                                    .build())
                            .pricePerUnit(incomingProductVariantDetail.getPricePerUnit())
                            .quantity(incomingProductVariantDetail.getQuantity())
                            .totalPrice(incomingProductVariantDetail.getTotalPrice())
                            .build();
                }).toList();

                // update incomingProductDetail totalVariantQuantity & totalVariantPrice.
                incomingProductDetail.setTotalVariantQuantity(incomingProductDetailUpdateRequest.getTotalVariantQuantity());
                incomingProductDetail.setTotalVariantPrice(incomingProductDetailUpdateRequest.getTotalVariantPrice());

            }

            incomingProductDetails.add(incomingProductDetail);

            return IncomingProductResponse.IncomingProductDetail.builder()
                    .id(incomingProductDetail.getId())
                    .product(IncomingProductProductResponse.builder()
                            .id(product.getId())
                            .name(product.getName())
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


        // update repository
        productRepository.saveAll(products);
        productVariantRepository.saveAll(productVariants);
        incomingProductDetailRepository.saveAll(incomingProductDetails);
        incomingProductVariantDetailRepository.saveAll(incomingProductVariantDetails);



        return IncomingProductResponse.builder()
                .id(incomingProduct.getId())
                .dateIn(incomingProduct.getDateIn())
                .supplier(IncomingProductSupplierResponse.builder()
                        .id(supplier.getId())
                        .name(supplier.getSupplierName())
                        .build())
                .username(user.getUsername())
                .totalProducts(incomingProduct.getTotalProducts())
                .note(incomingProduct.getNote())
                .updateReason(incomingProduct.getUpdateReason())
                .incomingProductDetails(incomingProductDetailsResponse)
                .build();
    }

//    @Override
//    @Transactional
//    public IncomingProductResponse update(IncomingProductUpdateRequest request) {
//        validationService.validate(request);
//        // check if incoming product is present
//        IncomingProduct incomingProduct = findIncomingProductByIdOrThrows(request.getId());
//        // todo:
////        incomingProduct.getIncomingProductDetails().forEach();
//
//        // check if supplier is present
//        Supplier supplier = supplierRepository.findById(request.getSupplierId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier is not found."));
//        User user = userRepository.findById(request.getUsername()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));
//        // update the incomingProduct field
//        incomingProduct.setDateIn(request.getDateIn());
//        incomingProduct.setSupplier(supplier);
//        incomingProduct.setUser(user);
//
//        if (request.getTotalProducts() != request.getIncomingProductDetails().size()) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total products 'incoming product' is wrong.");
//        }
//        incomingProduct.setTotalProducts(request.getTotalProducts());
//        incomingProduct.setNote(request.getNote());
//        incomingProduct.setUpdateReason(request.getUpdateReason());
//        incomingProductRepository.save(incomingProduct);
//
//        Set<Integer> incomingProductDetailIdSet = new HashSet<>();
//        Set<Integer> productIdSet = new HashSet<>();
//        Set<Integer> incomingProductVariantDetailsSet = new HashSet<>();
//        Set<Integer> productVariantsIdSet = new HashSet<>();
//
//        // loop over incomingProductDetails
//        request.getIncomingProductDetails().forEach(incomingProductDetailUpdateRequest -> {
//            // collect all incomingProductDetails id's
//            // check if incomingProductDetailsId and productId contain duplicate id
//            if (incomingProductDetailUpdateRequest.getId() != null && !incomingProductDetailIdSet.add(incomingProductDetailUpdateRequest.getId())) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductDetail 'id' cannot be duplicate.");
//            }
//
//            // collect all productId's
//            if (!productIdSet.add(incomingProductDetailUpdateRequest.getProductId())) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'Product id' must not duplicate in one IncomingProduct.");
//            }
//
//
//            // validation hasVariant
//
//            incomingProductDetailsValidationHasVariant(incomingProductDetailUpdateRequest.getHasVariant(),
//                    incomingProductDetailUpdateRequest.getPricePerUnit(),
//                    incomingProductDetailUpdateRequest.getQuantity(),
//                    incomingProductDetailUpdateRequest.getIncomingProductVariantDetails() == null,
//                    incomingProductDetailUpdateRequest.getIncomingProductVariantDetails() != null);
//
//            // if hasVariant true, loop over incomingProductVariantDetails
//            if (incomingProductDetailUpdateRequest.getHasVariant()) {
//                incomingProductDetailUpdateRequest.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailUpdateRequest -> {
//                    // set incomingProductDetail id to incomingProduct_VariantDetails id
//                    incomingProductVariantDetailUpdateRequest.setIncomingProductDetailId(incomingProductDetailUpdateRequest.getId());
//
//                    // collect all incomingProductVariantDetails id's
//                    if (incomingProductVariantDetailUpdateRequest.getId() != null && !incomingProductVariantDetailsSet.add(incomingProductVariantDetailUpdateRequest.getId())) {
//                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductVariantDetail id must not duplicate in a single ProductVariantDetails.");
//                    }
//
//                    // collect all variantId's and map to it's incomingProductDetails id's
//                    if (!productVariantsIdSet.add(incomingProductVariantDetailUpdateRequest.getVariantId())) {
//                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ProductVariants id must not duplicate in a single ProductVariantDetails.");
//                    }
//                });
//            }
//        });
//
//        // find to database for all incomingProductDetailsId and productId
//        // check if all incomingProductDetailsId is valid (available in database)
//        List<IncomingProductDetail> incomingProductDetailList = incomingProductDetailRepository.findAllById(incomingProductDetailIdSet);
//        if (incomingProductDetailList.size() != incomingProductDetailIdSet.size()) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of 'IncomingProductDetail' is not found, please check the IncomingProductDetail id's again.");
//        }
//
//        // check if all productId is in database
//        List<Product> productsList = productRepository.findAllById(productIdSet);
//        // check if incomingProductDetailsId is associated with incomingProductId
//        if (productsList.size() != productIdSet.size()) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of 'products' is not found, please check the productId again.");
//        }
//
//        List<IncomingProductVariantDetail> incomingProductVariantDetailList = incomingProductVariantDetailRepository.findAllById(incomingProductVariantDetailsSet);
//
//        if (incomingProductVariantDetailList.size() != incomingProductVariantDetailsSet.size()) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of IncomingProductVariantDetail is not found, please check IncomingProductVariantDetail id again.");
//        }
//
//        List<ProductVariant> productVariantList = productVariantRepository.findAllById(productVariantsIdSet);
//
//        if (productVariantList.size() != productVariantsIdSet.size()) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of product variant is not found, please check product variant id again.");
//        }
//
//
//        // loop over incomingProductDetails
//        request.getIncomingProductDetails().forEach(incomingProductDetailUpdateRequest -> {
//            IncomingProductDetail incomingProductDetail = null;
//            if (incomingProductDetailUpdateRequest.getId() != null) {
//                 incomingProductDetail = incomingProductDetailList
//                        .stream()
//                        .filter(iPD -> Objects.equals(iPD.getId(), incomingProductDetailUpdateRequest.getId()))
//                        .findFirst()
//                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductDetail is not found."));
//
//                 if (!Objects.equals(incomingProductDetail.getIncomingProduct().getId(), incomingProduct.getId())) {
//                     throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductDetail " + incomingProductDetail.getIncomingProduct().getId() +
//                             "is not IncomingProductDetail for IncomingProduct " + incomingProduct.getId() + ".");
//                 }
//            } else {
//                incomingProductDetail = new IncomingProductDetail();
//                incomingProductDetail.setIncomingProduct(incomingProduct);
//                incomingProductDetailList.add(incomingProductDetail);
//            }
//
//            Product product = productsList
//                    .stream()
//                    .filter(p -> Objects.equals(p.getId(), incomingProductDetailUpdateRequest.getProductId()))
//                    .findFirst()
//                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not found."));
//            // compare hasVariant incomingProductDetails and hastVariant product
//            compareHasVariantIncomingProductDetailAndProduct(incomingProductDetailUpdateRequest.getHasVariant() != product.getHasVariant(), product);
//            // update incomingProductDetails field
//            incomingProductDetail.setProduct(product);
//            incomingProductDetail.setHasVariant(incomingProductDetailUpdateRequest.getHasVariant());
//
//            if (!incomingProductDetailUpdateRequest.getHasVariant()) {
//                incomingProductDetail.setPricePerUnit(incomingProductDetailUpdateRequest.getPricePerUnit());
//                incomingProductDetail.setQuantity(incomingProductDetailUpdateRequest.getQuantity());
//                incomingProductDetail.setTotalPrice(incomingProductDetailUpdateRequest.getTotalPrice());
//                incomingProductDetail.setTotalVariantQuantity(null);
//                incomingProductDetail.setTotalVariantPrice(null);
//                incomingProductDetail.setIncomingProductVariantDetails(null);
//            } else {
//                incomingProductDetail.setPricePerUnit(null);
//                incomingProductDetail.setQuantity(null);
//                incomingProductDetail.setTotalPrice(null);
//                AtomicInteger totalVariantQuantity = new AtomicInteger(0);
//                AtomicInteger totalVariantPrice = new AtomicInteger(0);
//
//                IncomingProductDetail finalIncomingProductDetail = incomingProductDetail;
//                incomingProductDetailUpdateRequest.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailUpdateRequest -> {
//                    totalVariantQuantity.getAndAdd(incomingProductVariantDetailUpdateRequest.getQuantity());
//                    totalVariantPrice.getAndAdd(incomingProductVariantDetailUpdateRequest.getPricePerUnit() * incomingProductVariantDetailUpdateRequest.getQuantity());
//                    IncomingProductVariantDetail incomingProductVariantDetail = null;
//                    // update incomingProductVariantDetail
//                    if (incomingProductVariantDetailUpdateRequest.getId() != null) {
//                        incomingProductVariantDetail = incomingProductVariantDetailList
//                                .stream()
//                                .filter(iPVD -> iPVD.getId().equals(incomingProductVariantDetailUpdateRequest.getId()))
//                                .findFirst()
//                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductVariantDetail is not found."));
//
//                        if (!Objects.equals(incomingProductVariantDetail.getIncomingProductDetail().getId(), finalIncomingProductDetail.getId())){
//                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductVariantDetail " + incomingProductVariantDetail.getIncomingProductDetail().getId() +
//                                    "is not IncomingProductVariantDetail for IncomingProductDetail " + finalIncomingProductDetail.getId() + ".");
//                        }
//                    } else {
//                        incomingProductVariantDetail = new IncomingProductVariantDetail();
//
//                        ProductVariant productVariant = productVariantList
//                                .stream()
//                                .filter(pV -> Objects.equals(pV.getId(), incomingProductVariantDetailUpdateRequest.getVariantId()))
//                                .findFirst()
//                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductVariant is not found."));
//
//                        incomingProductVariantDetail.setProductVariant(productVariant);
//                        incomingProductVariantDetail.setIncomingProductDetail(finalIncomingProductDetail);
//                        incomingProductVariantDetailList.add(incomingProductVariantDetail);
//                    }
//
//                    incomingProductVariantDetail.setQuantity(incomingProductVariantDetailUpdateRequest.getQuantity());
//                    incomingProductVariantDetail.setPricePerUnit(incomingProductVariantDetailUpdateRequest.getPricePerUnit());
//                    incomingProductVariantDetail.setTotalPrice(incomingProductVariantDetailUpdateRequest.getQuantity() * incomingProductVariantDetailUpdateRequest.getPricePerUnit());
//
//                });
//                incomingProductDetail.setTotalVariantQuantity(totalVariantQuantity.get());
//                incomingProductDetail.setTotalVariantPrice(totalVariantPrice.get());
//            }
//        });
//
//        incomingProductDetailRepository.saveAll(incomingProductDetailList);
//        incomingProductVariantDetailRepository.saveAll(incomingProductVariantDetailList);
//
//        List<IncomingProductResponse.IncomingProductDetail> incomingProductDetailsResponse = incomingProductDetailList.stream().map(incomingProductDetail -> {
//            List<IncomingProductResponse.IncomingProductVariantDetail> incomingProductVariantDetailsResponse = incomingProductVariantDetailList
//                    .stream()
//                    .filter(incomingProductVariantDetail -> Objects.equals(incomingProductVariantDetail.getIncomingProductDetail().getId(), incomingProductDetail.getId()))
//                    .map(this::toIncomingProductVariantDetailResponse)
//                    .toList();
//
//
//            return IncomingProductResponse.IncomingProductDetail.builder()
//                    .id(incomingProductDetail.getId())
//                    .product(IncomingProductProductResponse.builder()
//                            .id(incomingProductDetail.getProduct().getId())
//                            .name(incomingProductDetail.getProduct().getName())
//                            .build())
//                    .pricePerUnit(incomingProductDetail.getPricePerUnit())
//                    .quantity(incomingProductDetail.getQuantity())
//                    .totalPrice(incomingProductDetail.getTotalPrice())
//                    .hasVariant(incomingProductDetail.getHasVariant())
//                    .totalVariantQuantity(incomingProductDetail.getTotalVariantQuantity())
//                    .totalVariantPrice(incomingProductDetail.getTotalVariantPrice())
//                    .incomingProductVariantDetails(incomingProductVariantDetailsResponse)
//                    .build();
//        }).toList();
//
//        return IncomingProductResponse.builder()
//                .id(incomingProduct.getId())
//                .dateIn(incomingProduct.getDateIn())
//                .supplier(IncomingProductSupplierResponse.builder()
//                        .id(supplier.getId())
//                        .name(supplier.getSupplierName())
//                        .build())
//                .username(user.getUsername())
//                .totalProducts(incomingProduct.getTotalProducts())
//                .note(incomingProduct.getNote())
//                .incomingProductDetails(incomingProductDetailsResponse)
//                .build();
//    }

    private void compareHasVariantIncomingProductDetailAndProduct (boolean compareResult, Product product) {
        if (compareResult) {
            String status = product.getHasVariant() ? "has" : "not have";
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'Product id': " + product.getId() + " is " + status + " variant, please input valid product and valid variant."  );
        }
    }

    private void incomingProductDetailsValidationHasVariant(Boolean hasVariant, Integer pricePerUnit, Integer quantity, boolean b, boolean b2) {
        if (hasVariant &&
                (pricePerUnit != null || quantity != null)
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'pricePerUnit', and 'quantity' properties must not send when 'hasVariant' is true.");
        }

        if (hasVariant && b) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'IncomingProductVariantDetails' properties must not null when 'hasVariant' is true.");
        }

        if (!hasVariant &&
                (pricePerUnit == null || quantity == null)
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'pricePerUnit', and 'quantity' properties must not null when 'hasVariant' is false.");
        }

        if (!hasVariant && b2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'IncomingProductVariantDetails' properties must not send when 'hasVariant' is false.");
        }
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

    private IncomingProduct findIncomingProductByIdOrThrows(Integer incomingProductId) {
        return incomingProductRepository.findById(incomingProductId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incoming Product is not found. Please check Incoming Product id again."));
    }
    private Supplier findSupplierByIdOrThrows(Integer supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier is not found. Please check Supplier Id again."));
    }


}

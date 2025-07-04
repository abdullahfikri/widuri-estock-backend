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
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some of ProductVariant is not found, please check ProductVariant id again.");
                    }

                    incomingProductDetailsRequest.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailRequest -> {
                        ProductVariant productVariant = productVariantList.stream().filter(pV -> Objects.equals(pV.getId(), incomingProductVariantDetailRequest.getVariantId())).findFirst().orElse(null);

                        if (productVariant == null) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductVariant with id " + incomingProductVariantDetailRequest.getVariantId() + " is not found, please check ProductVariant id again.");
                        }

                        if (!Objects.equals(productVariant.getProduct().getId(), product.getId())) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,"ProductVariant with id " + productVariant.getId() + " is not product variant for Product with id " + product.getId() + "." );
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

    @Override
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

                // set IncomingProductDetail
                setIncomingProductDetailWithoutVariant(incomingProductDetail,
                        incomingProductDetailUpdateRequest.getPricePerUnit(),
                        incomingProductDetailUpdateRequest.getQuantity());

            } else {
                incomingProductDetail.setPricePerUnit(null);
                incomingProductDetail.setQuantity(null);
                incomingProductDetail.setTotalPrice(null);
                incomingProductDetail.setHasVariant(true);

                if (incomingProductDetail.getIncomingProductVariantDetails().size() != incomingProductDetailUpdateRequest.getIncomingProductVariantDetails().size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductVariantDetails size is not same. Please check the IncomingProductVariantDetails again.");
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

    @Override
    @Transactional
    public IncomingProductDetailResponse createIncomingProductDetails(IncomingProductDetailCreateRequest request) {
        validationService.validate(request);

        // validate hasVariant
        incomingProductDetailsValidationHasVariant(request.getHasVariant(),
                request.getPricePerUnit(),
                request.getQuantity(),
                request.getIncomingProductVariantDetails() == null,
                request.getIncomingProductVariantDetails() != null
        );

        IncomingProduct incomingProduct = findIncomingProductByIdOrThrows(request.getIncomingProductId());

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not found. Please check Product id again."));

        compareHasVariantIncomingProductDetailAndProduct(request.getHasVariant() != product.getHasVariant(), product);


        Set<Integer> productVariantIdList = new HashSet<>();
        if (request.getHasVariant()) {
            // get all product variant id from request
            request.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailRequest -> {
                boolean isAddSuccess = productVariantIdList.add(incomingProductVariantDetailRequest.getVariantId());

                // check if productVariant id is duplicate
                if (!isAddSuccess) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ProductVariants id must not duplicate in a single ProductVariantDetails.");
                }
            });
        }


        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(product);
        List<IncomingProductVariantDetail> incomingProductVariantDetailList = new ArrayList<>();
        if (!request.getHasVariant()) {
            setIncomingProductDetailWithoutVariant(incomingProductDetail, request.getPricePerUnit(), request.getQuantity());

            // update product
            if (product.getStock() != null) {
                product.setStock(product.getStock() + request.getQuantity());
            } else {
                product.setStock(request.getQuantity());
            }

        } else {
            incomingProductDetail.setPricePerUnit(null);
            incomingProductDetail.setQuantity(null);
            incomingProductDetail.setTotalPrice(null);
            incomingProductDetail.setHasVariant(request.getHasVariant());

            incomingProductDetail.setTotalVariantPrice(0);
            incomingProductDetail.setTotalVariantQuantity(0);


            List<ProductVariant> productVariantList = productVariantRepository.findAllById(productVariantIdList);

            request.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailRequest -> {

                ProductVariant productVariant = productVariantList.stream()
                        .filter(pV -> pV.getId().equals(incomingProductVariantDetailRequest.getVariantId()))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductVariant with id " + incomingProductVariantDetailRequest.getVariantId() + " is not found. please check ProductVariant id again."));

                // check if productVariant is belongs to product
                if (!productVariant.getProduct().getId().equals(product.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "ProductVariant with id " + productVariant.getId() + " is not product variant for Product with id " + product.getId() + ".");
                }


                IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
                incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
                incomingProductVariantDetail.setProductVariant(productVariant);
                incomingProductVariantDetail.setPricePerUnit(incomingProductVariantDetailRequest.getPricePerUnit());
                incomingProductVariantDetail.setQuantity(incomingProductVariantDetailRequest.getQuantity());
                incomingProductVariantDetail.setTotalPrice(incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity());
                incomingProductVariantDetailList.add(incomingProductVariantDetail);

                // update ProductVariant stock
                if (productVariant.getStock() != null) {
                    productVariant.setStock(productVariant.getStock() + incomingProductVariantDetailRequest.getQuantity());
                } else {
                    productVariant.setStock(incomingProductVariantDetailRequest.getQuantity());
                }

                // increase incomingProductDetail TotalVariantPrice * TotalVariantQuantity
                incomingProductDetail.setTotalVariantPrice(incomingProductDetail.getTotalVariantPrice() + incomingProductVariantDetail.getTotalPrice());
                incomingProductDetail.setTotalVariantQuantity(incomingProductDetail.getTotalVariantQuantity() + incomingProductVariantDetail.getQuantity());
            });

            // update product variant
            productVariantRepository.saveAll(productVariantList);

        }

        // increase IncomingProduct TotalProduct
        incomingProduct.setTotalProducts(incomingProduct.getTotalProducts() + 1);

        incomingProductRepository.save(incomingProduct);
        incomingProductDetailRepository.save(incomingProductDetail);
        incomingProductVariantDetailRepository.saveAll(incomingProductVariantDetailList);
        // update product
        productRepository.save(product);

        // write incomingProductVariantDetails
        List<IncomingProductDetailResponse.IncomingProductVariantDetail> incomingProductVariantDetailsResponse = incomingProductVariantDetailList.stream()
                .map(incomingProductVariantDetail -> IncomingProductDetailResponse.IncomingProductVariantDetail.builder()
                        .id(incomingProductVariantDetail.getId())
                        .variant(IncomingProductProductVariantResponse.builder()
                            .id(incomingProductVariantDetail.getProductVariant().getId())
                            .sku(incomingProductVariantDetail.getProductVariant().getSku())
                            .build()
                        )
                        .pricePerUnit(incomingProductVariantDetail.getPricePerUnit())
                        .quantity(incomingProductVariantDetail.getQuantity())
                        .totalPrice(incomingProductVariantDetail.getTotalPrice())
                        .build()
                ).toList();

        return IncomingProductDetailResponse.builder()
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
                .incomingProductVariantDetails(!incomingProductVariantDetailsResponse.isEmpty() ? incomingProductVariantDetailsResponse : null)
                .build();
    }

    @Override
    @Transactional
    public IncomingProductVariantDetailResponse createIncomingProductVariantDetails(IncomingProductVariantDetailCreateRequest request) {
        validationService.validate(request);

        IncomingProductDetail incomingProductDetail = incomingProductDetailRepository
                .findById(request.getIncomingProductDetailId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomngProductDetail is not found, please check IncomngProductDetail id again."));

        if (!incomingProductDetail.getHasVariant()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create IncomingProductDetail, since IncomingProductDetail hasVariant is false.");
        }

        ProductVariant productVariant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductVariant is not found, please check ProductVariant id again."));

        if (incomingProductDetail.getIncomingProductVariantDetails() != null) {
            incomingProductDetail
                    .getIncomingProductVariantDetails()
                    .stream()
                    .filter(incomingProductVariantDetail -> incomingProductVariantDetail.getProductVariant().getId().equals(request.getVariantId()))
                    .findFirst()
                    .ifPresent(incomingProductVariantDetail -> {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ProductVariant is already present in the IcomingProductDetail, please check ProductVarian id again.");});
        }

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(request.getPricePerUnit());
        incomingProductVariantDetail.setQuantity(request.getQuantity());
        incomingProductVariantDetail.setTotalPrice(request.getQuantity() * request.getPricePerUnit());
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);

        // update ProductVariant Stock
        productVariant.setStock(productVariant.getStock() + incomingProductVariantDetail.getQuantity());
        productVariantRepository.save(productVariant);

        return IncomingProductVariantDetailResponse.builder()
                .id(incomingProductVariantDetail.getId())
                .variant(IncomingProductProductVariantResponse.builder()
                        .id(productVariant.getId())
                        .sku(productVariant.getSku())
                        .build())
                .pricePerUnit(incomingProductVariantDetail.getPricePerUnit())
                .quantity(incomingProductVariantDetail.getQuantity())
                .totalPrice(incomingProductVariantDetail.getTotalPrice())
                .build();
    }

    @Override
    public void deleteIncomingProduct(Integer incomingProductId) {
        if (incomingProductId.equals(null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductDetails id must not null.");
        }

        IncomingProduct incomingProduct = findIncomingProductByIdOrThrows(incomingProductId);


        List<Product> productList = new ArrayList<>();
        List<ProductVariant> productVariantList = new ArrayList<>();

        incomingProduct.getIncomingProductDetails().forEach(incomingProductDetail -> {
            if (!incomingProductDetail.getHasVariant()) {
                Product product = incomingProductDetail.getProduct();
                product.setStock(product.getStock() - incomingProductDetail.getQuantity());
                productList.add(product);
            } else {
                incomingProductDetail.getIncomingProductVariantDetails().forEach(incomingProductVariantDetail -> {
                    ProductVariant productVariant = incomingProductVariantDetail.getProductVariant();
                    productVariant.setStock(productVariant.getStock() - incomingProductVariantDetail.getQuantity());
                });
            }
        });

        productRepository.saveAll(productList);
        productVariantRepository.saveAll(productVariantList);

        incomingProductRepository.delete(incomingProduct);

    }

    @Override
    @Transactional
    public void deleteIncomingProductDetails(Integer incomingProductDetailId) {
        if (incomingProductDetailId != null) {
            IncomingProductDetail incomingProductDetail = incomingProductDetailRepository.findById(incomingProductDetailId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductDetails is not found, please check the IncomingProductDetails id again."));

            if (!incomingProductDetail.getHasVariant()) {
                Product product = incomingProductDetail.getProduct();
                product.setStock(product.getStock() - incomingProductDetail.getQuantity());
                productRepository.save(product);
            } else {
                List<ProductVariant> productVariantList = new ArrayList<>();
                incomingProductDetail.getIncomingProductVariantDetails().forEach(incomingProductVariantDetail -> {
                    ProductVariant productVariant = incomingProductVariantDetail.getProductVariant();
                    productVariant.setStock(productVariant.getStock() - incomingProductVariantDetail.getQuantity());
                });
                productVariantRepository.saveAll(productVariantList);
            }

            // update IncomingProduct
            IncomingProduct incomingProduct = incomingProductDetail.getIncomingProduct();
            incomingProduct.setTotalProducts(incomingProduct.getTotalProducts() - 1);
            incomingProductRepository.save(incomingProduct);
            incomingProductDetailRepository.delete(incomingProductDetail);

        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductDetails id must not null.");
        }
    }

    @Override
    @Transactional
    public void deleteIncomingProductVariantDetails(Integer incomingProductVariantDetailId) {
        if (incomingProductVariantDetailId != null) {
            IncomingProductVariantDetail incomingProductVariantDetail = incomingProductVariantDetailRepository.findById(incomingProductVariantDetailId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductVariantDetails is not found, please check the IncomingProductVariantDetails id again."));

            // update incomingProductDetail TotalVariantQuantity && TotalVariantPrice
            IncomingProductDetail incomingProductDetail = incomingProductVariantDetail.getIncomingProductDetail();
            incomingProductDetail.setTotalVariantQuantity(incomingProductDetail.getTotalVariantQuantity() - incomingProductVariantDetail.getQuantity());
            incomingProductDetail.setTotalVariantPrice(incomingProductDetail.getTotalVariantPrice() - incomingProductVariantDetail.getTotalPrice());
            incomingProductDetailRepository.save(incomingProductDetail);

            // update ProductVariant
            ProductVariant productVariant = incomingProductVariantDetail.getProductVariant();
            productVariant.setStock(productVariant.getStock() - incomingProductVariantDetail.getQuantity());
            productVariantRepository.save(productVariant);

            incomingProductVariantDetailRepository.delete(incomingProductVariantDetail);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductVariantDetail id must not null.");
        }
    }

    private void compareHasVariantIncomingProductDetailAndProduct (boolean compareResult, Product product) {
        if (compareResult) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product id: " + product.getId() + " hasVariant is " +  product.getHasVariant() + ", please check hasVariant again."
            );
        }
    }

    private void incomingProductDetailsValidationHasVariant(Boolean hasVariant,
                                                            Integer pricePerUnit, Integer quantity,
                                                            boolean incomingProductVariantDetailsIsNull,
                                                            boolean incomingProductVariantDetailsIsNotNull
    ) {
        if (hasVariant &&
                (pricePerUnit != null || quantity != null)
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'pricePerUnit', and 'quantity' properties must not send when 'hasVariant' is true.");
        }

        if (hasVariant && incomingProductVariantDetailsIsNull) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'IncomingProductVariantDetails' properties must not null when 'hasVariant' is true.");
        }

        if (!hasVariant &&
                (pricePerUnit == null || quantity == null)
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "incomingProductDetails 'pricePerUnit', and 'quantity' properties must not null when 'hasVariant' is false.");
        }

        if (!hasVariant && incomingProductVariantDetailsIsNotNull) {
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

    private void setIncomingProductDetailWithoutVariant (IncomingProductDetail incomingProductDetail,
                                                         Integer pricePerUnit,
                                                         Integer quantity) {
        incomingProductDetail.setPricePerUnit(pricePerUnit);
        incomingProductDetail.setQuantity(quantity);
        incomingProductDetail.setTotalPrice(pricePerUnit * quantity);
        incomingProductDetail.setHasVariant(false);
        incomingProductDetail.setTotalVariantQuantity(null);
        incomingProductDetail.setTotalVariantPrice(null);
        incomingProductDetail.setIncomingProductVariantDetails(null);
    }


    private IncomingProduct findIncomingProductByIdOrThrows(Integer incomingProductId) {
        return incomingProductRepository.findById(incomingProductId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProduct is not found. Please check IncomingProduct id again."));
    }
    private Supplier findSupplierByIdOrThrows(Integer supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier is not found. Please check Supplier Id again."));
    }


}

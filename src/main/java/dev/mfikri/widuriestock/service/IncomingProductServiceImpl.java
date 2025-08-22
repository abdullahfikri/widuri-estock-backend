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
import org.springframework.data.util.Pair;
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
        log.info("Processing request to create a new incoming product transaction.");

        validationService.validate(request);

        // 1. Fetch all required entities in bulk to avoid N+1 queries
        Supplier supplier = findSupplierByIdOrThrows(request.getSupplierId());
        User user = findUserByUsernameOrThrows(request.getUsername());

        // 2. Build the main IncomingProduct entity (still transient, not saved yet)
        IncomingProduct incomingProductEntity = buildIncomingProductEntity(request, supplier, user);

        // 3. Save the parent entity first to get an ID and make it "managed"
        log.debug("Saving main IncomingProduct entity to the database.");
        incomingProductRepository.save(incomingProductEntity);

        // 4. Process and build child entities
        Pair<List<IncomingProductDetail>, List<IncomingProductVariantDetail>> processedDetails = processAndBuildDetails(request, incomingProductEntity);

        List<IncomingProductDetail> incomingProductDetailListEntity = processedDetails.getFirst();
        List<IncomingProductVariantDetail> incomingProductVariantDetailListEntity = processedDetails.getSecond();

        // 5. Save all created child entities in bulk
        log.debug("Saving all child entities to the persistence database.");
        incomingProductDetailRepository.saveAll(incomingProductDetailListEntity);
        if (!incomingProductVariantDetailListEntity.isEmpty()) {
            incomingProductVariantDetailRepository.saveAll(incomingProductVariantDetailListEntity);
        }

        log.info("Successfully created new incoming product. incomingProductId={}", incomingProductEntity.getId());
        incomingProductEntity.setIncomingProductDetails(incomingProductDetailListEntity);
        return toIncomingProductResponse(incomingProductEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public IncomingProductResponse get(Integer incomingProductId) {
        log.info("Processing request to get an incoming product transaction. incomingProductId={}", incomingProductId);

        // find incomingProductEntity
        IncomingProduct incomingProductEntity = findIncomingProductByIdOrThrows(incomingProductId);

        log.info("Successfully get an incoming product transaction. incomingProductId={}", incomingProductId);
        return toIncomingProductResponse(incomingProductEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncomingProductGetListResponse> getList(IncomingProductGetListRequest request) {
        log.info("Processing request to get list of incoming product transactions.");

        // validation startDate and endDate
        log.debug("Validating and assign default value. startDate={}, endDate={}", request.getStartDate(), request.getEndDate());

        LocalDate activeStartDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.parse("1970-01-01");
        LocalDate activeEndDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();


        log.debug("Verifying if activeStartDate is before or equal to activeEndDate. activeStartDate={}, activeEndDate={}", activeStartDate, activeEndDate);
        if (activeStartDate.isAfter(activeEndDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Start date " + activeStartDate + " must be before or equal to end date " + activeEndDate + ".");
        }

        // validate page and size
        log.debug("Validating and assign default value. page={}, size={}", request.getPage(), request.getSize());
        int activePage = request.getPage() != null ? request.getPage() : 0;
        int activeSize = request.getSize() != null ? request.getSize() : 10;

        Pageable pageable = PageRequest.of(activePage, activeSize, Sort.by(Sort.Order.asc("dateIn")));
        Page<IncomingProduct> incomingProductPage = incomingProductRepository.findByDateInBetween(activeStartDate, activeEndDate, pageable);

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

        log.info("Successfully get list of incoming product transactions.");
        return new PageImpl<>(incomingProductsListResponse, pageable, incomingProductPage.getTotalElements());
    }

    @Override
    @Transactional
    public IncomingProductResponse update(IncomingProductUpdateRequest request) {
        log.info("Processing request to update an incoming product transaction.");

        validationService.validate(request);

        // check if incoming product, supplier, and user exists in databases.
        IncomingProduct incomingProduct = findIncomingProductByIdOrThrows(request.getId());
        Supplier supplier = findSupplierByIdOrThrows(request.getSupplierId());
        User user = findUserByUsernameOrThrows(request.getUsername());

        if (request.getTotalProducts() != request.getIncomingProductDetails().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Total products 'IncomingProduct' is wrong.");
        }

        // The main update logic is now orchestrated here, relying on Dirty Checking.
        applyUpdates(incomingProduct, request, supplier, user);

        log.info("Successfully updated an incoming product transaction. incomingProductId={}", incomingProduct.getId());
        return toIncomingProductResponse(incomingProduct);
    }

    private record UpdateRequestContext(
            Map<Integer, IncomingProductUpdateRequest.IncomingProductDetail> detailRequestMap,
            Map<Integer, IncomingProductUpdateRequest.IncomingProductVariantDetail> variantDetailRequestMap
    ) {}

    private UpdateRequestContext buildUpdateContext(List<IncomingProductDetail> existingDetails,
                                                    List<IncomingProductUpdateRequest.IncomingProductDetail> detailRequests) {
        log.debug("Receiving parameter incomingProductDetails={}, productDetailRequestList={}", existingDetails, detailRequests);

        if (existingDetails.size() != detailRequests.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductDetails size is not same. Please check the IncomingProductDetails again.");
        }


        Map<Integer, IncomingProductDetail> productDetailMap = new HashMap<>();
        Map<Integer, IncomingProductUpdateRequest.IncomingProductDetail> productDetailRequestMap = new HashMap<>();
        Map<Integer, IncomingProductUpdateRequest.IncomingProductVariantDetail> variantDetailRequestMap = new HashMap<>();

        // Pre-populate maps to easily check for existence and prevent duplicates
        for (var productDetail: existingDetails) {
            productDetailMap.put(productDetail.getId(), productDetail);
            productDetailRequestMap.put(productDetail.getId(), null);

            for (var variantDetail: productDetail.getIncomingProductVariantDetails()) {
                variantDetailRequestMap.put(variantDetail.getId(), null);
            }
        }

        for (var productDetailRequest : detailRequests) {
            if (!productDetailRequestMap.containsKey(productDetailRequest.getId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductDetail with id " + productDetailRequest.getId() + " is not found, please check IncomingProductDetail id again.");
            }

            if (productDetailRequestMap.get(productDetailRequest.getId()) != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductDetail with id " + productDetailRequest.getId() + " is duplicate, please check IncomingProductDetails again.");
            }
            productDetailRequestMap.replace(productDetailRequest.getId(), productDetailRequest);

            // --- Business Rule Validations for each detail ---
            IncomingProductDetail existingDetail = productDetailMap.get(productDetailRequest.getId());

            // validation hasVariant
            incomingProductDetailsValidationHasVariant(productDetailRequest.getHasVariant(),
                    productDetailRequest.getPricePerUnit(),
                    productDetailRequest.getQuantity(),
                    productDetailRequest.getIncomingProductVariantDetails() == null,
                    productDetailRequest.getIncomingProductVariantDetails() != null);

            // is equal, existing productDetail hasVariant and productDetailRequestList productDetail hasVariant
            if (productDetailRequest.getHasVariant() != existingDetail.getHasVariant()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot change 'hasVariant' status for IncomingProductDetail with id " + productDetailRequest.getId() + ". Please check 'hasVariant' status again.");
            }


            Product existingProduct = existingDetail.getProduct();

            if (!productDetailRequest.getProductId().equals(existingProduct.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot change 'productId' value for IncomingProductDetail with id " + productDetailRequest.getId() + ". Please check 'productId' status again.");
            }

            // check if existingProduct is already updated the hasVariant
            if (productDetailRequest.getHasVariant() != existingProduct.getHasVariant()) {

                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product id: " + productDetailRequest.getProductId() + " is already change hasVariant status, please delete IncomingProductDetail with id " + productDetailRequest.getId() + " and create new IncomingProductDetail for this IncomingProduct.");
            }

            if (existingDetail.getHasVariant()) {
                if (existingDetail.getIncomingProductVariantDetails().size() != productDetailRequest.getIncomingProductVariantDetails().size()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductVariantDetails size is not same. Please check the IncomingProductVariantDetails again.");
                }
                // mapping productVariantDetail
                for (var variantDetailRequest: productDetailRequest.getIncomingProductVariantDetails()) {
                    if (!variantDetailRequestMap.containsKey(variantDetailRequest.getId())) {
                        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductVariantDetail with id " + variantDetailRequest.getId() + " is not found, please check IncomingProductVariantDetail id again.");
                    }

                    if (variantDetailRequestMap.get(variantDetailRequest.getId()) != null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductVariantDetail with id " + variantDetailRequest.getId() + " is duplicate, please check IncomingProductVariantDetails again.");
                    }

                    variantDetailRequestMap.put(variantDetailRequest.getId(), variantDetailRequest);
                }
            }
        }

        return new UpdateRequestContext(productDetailRequestMap, variantDetailRequestMap);
    }

    private void validationIncomingProductVariantDetail(IncomingProductVariantDetail variantDetail,
                                                        IncomingProductUpdateRequest.IncomingProductVariantDetail variantDetailRequest) {
        log.debug("Validating IncomingProductVariantDetail.");
        if (!variantDetailRequest.getVariantId().equals(variantDetail.getProductVariant().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductVariant for IncomingProductVariantDetail with id: " + variantDetailRequest.getVariantId() +
                    " is wrong, please check ProductVariant id again.");
        }
    }

    private void applyUpdates(IncomingProduct incomingProduct,
                              IncomingProductUpdateRequest request,
                              Supplier supplier,
                              User user) {
        log.debug("Applying updates to entities.");

        // 1. Validate and structure the request data
        UpdateRequestContext updateContext = buildUpdateContext(
                incomingProduct.getIncomingProductDetails(),
                request.getIncomingProductDetails()
        );

        // 2. Apply changes to the parent entity
        incomingProduct.setDateIn(request.getDateIn());
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(request.getTotalProducts());
        incomingProduct.setNote(request.getNote());
        incomingProduct.setUpdateReason(request.getUpdateReason());

        // 3. Apply changes to child entities and related business logic (stock)
        applyDetailUpdates(incomingProduct.getIncomingProductDetails(), updateContext);
    }

    private void applyDetailUpdates(List<IncomingProductDetail> productDetails, UpdateRequestContext updateContext) {
        Map<Integer, IncomingProductUpdateRequest.IncomingProductDetail> productDetailRequestMap = updateContext.detailRequestMap();
        Map<Integer, IncomingProductUpdateRequest.IncomingProductVariantDetail> variantDetailRequestMap = updateContext.variantDetailRequestMap();

        for (var productDetail : productDetails) {
            IncomingProductUpdateRequest.IncomingProductDetail productDetailRequest = productDetailRequestMap.get(productDetail.getId());
            log.debug("Getting productDetailRequest from map, productDetailRequest={}", productDetailRequest);

            if (!productDetail.getHasVariant()) {
                // update the Product stock
                log.debug("Updated stock product");
                Product product = productDetail.getProduct();
                product.setStock(calcStockChange(product.getStock(), productDetailRequest.getQuantity(), productDetail.getQuantity()));

                // set IncomingProductDetail
                setIncomingProductDetailWithoutVariant(productDetail,
                        productDetailRequest.getPricePerUnit(),
                        productDetailRequest.getQuantity());

            } else {
                productDetail.setPricePerUnit(null);
                productDetail.setQuantity(null);
                productDetail.setTotalPrice(null);
                productDetail.setHasVariant(true);

                AtomicInteger totalVariantQuantity = new AtomicInteger(0);
                AtomicInteger totalVariantPrice = new AtomicInteger(0);

                for (var variantDetailEntity: productDetail.getIncomingProductVariantDetails()) {
                    IncomingProductUpdateRequest.IncomingProductVariantDetail variantDetailRequest = variantDetailRequestMap.get(variantDetailEntity.getId());

                    // update the ProductVariant stock
                    log.debug("Updated stock product variant");
                    ProductVariant productVariant = variantDetailEntity.getProductVariant();

                    int productVariantStockChange = calcStockChange(productVariant.getStock(),
                            variantDetailRequest.getQuantity(),
                            variantDetailEntity.getQuantity());

                    productVariant.setStock(productVariantStockChange);

                    validationIncomingProductVariantDetail(variantDetailEntity, variantDetailRequest);

                    // set IncomingProductVariantDetail
                    variantDetailEntity.setPricePerUnit(variantDetailRequest.getPricePerUnit());
                    variantDetailEntity.setQuantity(variantDetailRequest.getQuantity());
                    int variantDetailTotalPrice = calcTotalPrice(variantDetailRequest.getPricePerUnit(), variantDetailRequest.getQuantity());
                    variantDetailEntity.setTotalPrice(variantDetailTotalPrice);


                    totalVariantQuantity.addAndGet(variantDetailRequest.getQuantity());
                    totalVariantPrice.addAndGet(variantDetailEntity.getTotalPrice());
                }
                productDetail.setTotalVariantQuantity(totalVariantQuantity.get());
                productDetail.setTotalVariantPrice(totalVariantPrice.get());
            }
        }
    }


    @Override
    @Transactional
    public IncomingProductResponse.IncomingProductDetail addIncomingProductDetails(Integer incomingProductId, IncomingProductCreateRequest.IncomingProductDetails request) {
        log.info("Processing request to add a new IncomingProductDetail.");

        validationService.validate(request);

        IncomingProduct incomingProduct = findIncomingProductByIdOrThrows(incomingProductId);
        // increase IncomingProduct TotalProduct because we add new IncomingProductDetails
        incomingProduct.setTotalProducts(incomingProduct.getTotalProducts() + 1);

        Product product = findProductByIdOrThrows(request.getProductId());

        Set<Integer> variantIds = validateAndCollectVariantIdForAddIncomingProductDetail(request, product);

        Map<Integer, ProductVariant> variantMap = fetchProductVariants(variantIds);

        List<IncomingProductVariantDetail> allVariantDetails = new ArrayList<>();

        IncomingProductDetail incomingProductDetail = buildProductDetailEntity(request, incomingProduct, product, variantMap, allVariantDetails);

        incomingProductRepository.save(incomingProduct);
        incomingProductDetailRepository.save(incomingProductDetail);
        incomingProductVariantDetailRepository.saveAll(allVariantDetails);

        log.info("Successfully added new IncomingProductDetail. incomingProductDetailId={}", incomingProductDetail.getId());
        return toIncomingProductDetailListResponse(List.of(incomingProductDetail)).getFirst();
    }

    public Set<Integer> validateAndCollectVariantIdForAddIncomingProductDetail(IncomingProductCreateRequest.IncomingProductDetails request, Product product) {
        log.debug("Validating and collecting variantId.");

        // validate hasVariant
        incomingProductDetailsValidationHasVariant(request.getHasVariant(),
                request.getPricePerUnit(),
                request.getQuantity(),
                request.getIncomingProductVariantDetails() == null,
                request.getIncomingProductVariantDetails() != null
        );

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

        return productVariantIdList;
    }

    @Override
    @Transactional
    public IncomingProductResponse.IncomingProductVariantDetail addIncomingProductVariantDetails(Integer incomingProductDetailId, IncomingProductCreateRequest.IncomingProductVariantDetail variantDetailRequest) {
        log.info("Processing request to add a new IncomingProductVariantDetail.");

        validationService.validate(variantDetailRequest);

        IncomingProductDetail productDetail = findIncomingProductDetailByIdOrThrows(incomingProductDetailId);

        validateAddIncomingProductVariantDetail(productDetail, variantDetailRequest.getVariantId());

        ProductVariant productVariant = findProductVariantByIdOrThrows(variantDetailRequest.getVariantId());

        IncomingProductVariantDetail variantDetail = buildVariantDetailAndUpdateVariantStock(variantDetailRequest, productDetail, productVariant);

        log.debug("Set totalVariantPrice and totalVariantQuantity.");
        if (productDetail.getTotalVariantPrice() == null) {
            productDetail.setTotalVariantPrice(variantDetail.getTotalPrice());
        } else {
            productDetail.setTotalVariantPrice(productDetail.getTotalVariantPrice() + variantDetail.getTotalPrice());
        }

        if (productDetail.getTotalVariantQuantity() == null) {
            productDetail.setTotalVariantQuantity(
                    variantDetail.getQuantity());
        } else {
            productDetail.setTotalVariantQuantity(
                    productDetail.getTotalVariantQuantity() + variantDetail.getQuantity());
        }

        incomingProductVariantDetailRepository.save(variantDetail);

        log.info("Successfully added new IncomingProductVariantDetail. incomingProductVariantDetailId={}", variantDetail.getId());
        return toIncomingProductVariantDetailResponse(variantDetail);
    }

    private void validateAddIncomingProductVariantDetail(IncomingProductDetail incomingProductDetail, Integer productVariantId) {
        log.debug("Validating IncomingProductVariantDetail and ProductVariant. ");

        if (!incomingProductDetail.getHasVariant()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to create IncomingProductVariantDetail, since IncomingProductDetail hasVariant is false.");
        }

        if (incomingProductDetail.getIncomingProductVariantDetails() != null) {
            incomingProductDetail
                    .getIncomingProductVariantDetails()
                    .stream()
                    .filter(incomingProductVariantDetail -> incomingProductVariantDetail.getProductVariant().getId().equals(productVariantId))
                    .findFirst()
                    .ifPresent(incomingProductVariantDetail -> {throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ProductVariant is already present in the IncomingProductDetail, please check ProductVariant id again.");});
        }
    }

    @Override
    @Transactional
    public void deleteIncomingProduct(Integer incomingProductId) {
        log.info("Processing request to delete an incoming product transaction. incomingProductId={}", incomingProductId);
        IncomingProduct incomingProduct = findIncomingProductByIdOrThrows(incomingProductId);

        for (IncomingProductDetail productDetail: incomingProduct.getIncomingProductDetails()) {
            if (!productDetail.getHasVariant()) {
                Product product = productDetail.getProduct();
                product.setStock(product.getStock() - productDetail.getQuantity());
            } else {
                for ( IncomingProductVariantDetail variantDetail: productDetail.getIncomingProductVariantDetails()) {
                    ProductVariant productVariant = variantDetail.getProductVariant();
                    productVariant.setStock(productVariant.getStock() - variantDetail.getQuantity());
                }
            }
        }

        incomingProductRepository.delete(incomingProduct);
        log.info("Successfully deleted an incoming product transaction. incomingProductId={}", incomingProductId);
    }

    @Override
    @Transactional
    public void deleteIncomingProductDetails(Integer incomingProductDetailId) {
        log.info("Processing request to delete an incoming product detail. incomingProductDetailId={}", incomingProductDetailId);

        IncomingProductDetail incomingProductDetail = findIncomingProductDetailByIdOrThrows(incomingProductDetailId);

        log.debug("Updating product or product variant stock.");
        if (!incomingProductDetail.getHasVariant()) {
            Product product = incomingProductDetail.getProduct();
            product.setStock(product.getStock() - incomingProductDetail.getQuantity());
        } else {
            incomingProductDetail.getIncomingProductVariantDetails().forEach(incomingProductVariantDetail -> {
                ProductVariant productVariant = incomingProductVariantDetail.getProductVariant();
                productVariant.setStock(productVariant.getStock() - incomingProductVariantDetail.getQuantity());
            });
        }

        // update IncomingProduct
        IncomingProduct incomingProduct = incomingProductDetail.getIncomingProduct();
        incomingProduct.setTotalProducts(incomingProduct.getTotalProducts() - 1);

        incomingProductDetailRepository.delete(incomingProductDetail);
        log.info("Successfully deleted an incoming product detail. incomingProductDetailId={}", incomingProductDetailId);
    }

    @Override
    @Transactional
    public void deleteIncomingProductVariantDetails(Integer incomingProductVariantDetailId) {
        log.info("Processing request to delete an incoming product variant detail. incomingProductVariantDetailId={}", incomingProductVariantDetailId);

        IncomingProductVariantDetail variantDetail = findIncomingProductVariantDetailByIdOrThrows(incomingProductVariantDetailId);

        // update incomingProductDetail TotalVariantQuantity && TotalVariantPrice
        log.debug("Updating incomingProductDetail TotalVariantQuantity && TotalVariantPrice.");
        IncomingProductDetail incomingProductDetail = variantDetail.getIncomingProductDetail();
        incomingProductDetail.setTotalVariantQuantity(incomingProductDetail.getTotalVariantQuantity() - variantDetail.getQuantity());
        incomingProductDetail.setTotalVariantPrice(incomingProductDetail.getTotalVariantPrice() - variantDetail.getTotalPrice());


        // update ProductVariant
        ProductVariant productVariant = variantDetail.getProductVariant();
        productVariant.setStock(productVariant.getStock() - variantDetail.getQuantity());

        incomingProductVariantDetailRepository.delete(variantDetail);
        log.info("Successfully deleted an incoming product variant detail. incomingProductVariantDetailId={}", incomingProductVariantDetailId);
    }



    private void compareHasVariantIncomingProductDetailAndProduct (boolean compareResult, Product product) {
        log.debug("Compare has variant product detail and product. compareResult={}, product={}", compareResult, product);
        if (compareResult) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Product id: " + product.getId() + " hasVariant is " +  product.getHasVariant() + ", please check hasVariant again."
            );
        }
    }

    private void incomingProductDetailsValidationHasVariant(Boolean hasVariant,
                                                            Integer pricePerUnit, Integer quantity,
                                                            boolean incomingProductVariantDetailsIsNull,
                                                            boolean incomingProductVariantDetailsIsNotNull) {
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
        log.debug("Mapping incomingProductVariantDetail to response.");
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

    private List<IncomingProductResponse.IncomingProductDetail> toIncomingProductDetailListResponse(List<IncomingProductDetail> incomingProductDetails) {
        log.debug("Receiving parameter incomingProductDetails={}", incomingProductDetails);

        return incomingProductDetails.stream().map(incomingProductDetail -> {
            List<IncomingProductResponse.IncomingProductVariantDetail> incomingProductVariantDetails = null;

            if (incomingProductDetail.getHasVariant()) {
                incomingProductVariantDetails = incomingProductDetail.getIncomingProductVariantDetails()
                        .stream()
                        .map(this::toIncomingProductVariantDetailResponse)
                        .toList();
            }

            log.debug("Mapping incomingProductDetail to response.");
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

    private IncomingProductResponse toIncomingProductResponse (IncomingProduct incomingProduct) {
        log.debug("Mapping incomingProduct to response.");
        List<IncomingProductResponse.IncomingProductDetail> details = toIncomingProductDetailListResponse(incomingProduct.getIncomingProductDetails());

        return  IncomingProductResponse.builder()
                .id(incomingProduct.getId())
                .dateIn(incomingProduct.getDateIn())
                .supplier(IncomingProductSupplierResponse.builder()
                        .id(incomingProduct.getSupplier().getId())
                        .name(incomingProduct.getSupplier().getSupplierName())
                        .build())
                .username(incomingProduct.getUser().getUsername())
                .totalProducts(incomingProduct.getTotalProducts())
                .note(incomingProduct.getNote())
                .updateReason(incomingProduct.getUpdateReason())
                .incomingProductDetails(details)
                .build();
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
        log.debug("Finding incomingProduct. incomingProductId={}", incomingProductId);

        if (incomingProductId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id must not null.");
        }

        return incomingProductRepository.findById(incomingProductId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProduct is not found. Please check IncomingProduct id again."));
    }

    private IncomingProductDetail findIncomingProductDetailByIdOrThrows(Integer incomingProductDetailId) {
        log.debug("Finding incomingProductDetail. incomingProductDetailId={}", incomingProductDetailId);

        if (incomingProductDetailId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id must not null.");
        }
        return incomingProductDetailRepository
                .findById(incomingProductDetailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductDetail is not found, please check IncomingProductDetail id again."));
    }

    private IncomingProductVariantDetail findIncomingProductVariantDetailByIdOrThrows(Integer incomingProductVariantDetailId) {
        log.debug("Finding IncomingProductVariantDetail. incomingProductVariantDetailId={}", incomingProductVariantDetailId);

        if (incomingProductVariantDetailId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IncomingProductVariantDetail id must not null.");
        }

        return incomingProductVariantDetailRepository.findById(incomingProductVariantDetailId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "IncomingProductVariantDetail is not found, please check the IncomingProductVariantDetail id again."));
    }

    private Supplier findSupplierByIdOrThrows(Integer supplierId) {
        log.debug("Finding supplier. supplierId={}", supplierId);
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Supplier is not found. Please check Supplier Id again."));
    }

    private User findUserByUsernameOrThrows(String username) {
        log.debug("Finding user. username={}", username);
        return userRepository.findById(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User is not found."));
    }

    private Product findProductByIdOrThrows(Integer productId) {
        log.debug("Finding product. productId={}", productId);

        return productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product is not found. Please check Product id again."));
    }

    private ProductVariant findProductVariantByIdOrThrows(Integer productVariantId) {
        log.debug("Finding productVariant. productVariantId={}", productVariantId);

        return productVariantRepository.findById(productVariantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "ProductVariant is not found, please check ProductVariant id again."));
    }



    private Pair<Set<Integer>, Set<Integer>> validateAndCollectIds(IncomingProductCreateRequest request) {
        log.debug("Validating request details and collecting entity IDs.");
        Set<Integer> productIds = new HashSet<>();
        Set<Integer> variantIds = new HashSet<>();

        for (var detail : request.getIncomingProductDetails()) {
            incomingProductDetailsValidationHasVariant(detail.getHasVariant(),
                    detail.getPricePerUnit(),
                    detail.getQuantity(),
                    detail.getIncomingProductVariantDetails() == null,
                    detail.getIncomingProductVariantDetails() != null);

            if (!productIds.add(detail.getProductId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product id " + detail.getProductId() + " is duplicate.");
            }

            if (detail.getHasVariant()) {
                Set<Integer> variantsForThisProduct = new HashSet<>();
                for (var variantDetail: detail.getIncomingProductVariantDetails()) {
                    if (!variantsForThisProduct.add(variantDetail.getVariantId())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ProductVariant id " + variantDetail.getVariantId() + " is duplicate for same product.");
                    }
                    variantIds.add(variantDetail.getVariantId());
                }
            }
        }
        return Pair.of(productIds, variantIds);
    }

    private Map<Integer, Product> fetchProducts(Set<Integer> productIds) {
        log.debug("Fetching all product in bulk. count={}", productIds.size());

        List<Product> products = productRepository.findAllById(productIds);
        if (productIds.size() != products.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some products are not found. Please check product IDs again.");
        }

        Map<Integer, Product> productMap = new HashMap<>();
        products.forEach(p -> productMap.put(p.getId(), p));
        return productMap;
    }

    private Map<Integer, ProductVariant> fetchProductVariants(Set<Integer> variantsIds) {
        if (variantsIds.isEmpty()) {
            return Collections.emptyMap();
        }

        log.debug("Fetching all productVariant in bulk. count={}", variantsIds.size());
        List<ProductVariant> variants = productVariantRepository.findAllById(variantsIds);

        if (variants.size() != variantsIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some productVariants are not found. Please check productVariant IDs again.");
        }

        Map<Integer, ProductVariant> variantMap = new HashMap<>();
        variants.forEach(v -> variantMap.put(v.getId(), v));
        return  variantMap;
    }

    private Pair<List<IncomingProductDetail>, List<IncomingProductVariantDetail>> processAndBuildDetails(IncomingProductCreateRequest request, IncomingProduct incomingProductEntity) {
        log.debug("Processing and building details.");
        // 1. Validate request and collect all Ids
        Pair<Set<Integer>, Set<Integer>> allIds = validateAndCollectIds(request);
        Set<Integer> productIds = allIds.getFirst();
        Set<Integer> variantIds = allIds.getSecond();

        // 2. Fetch all required products and variants in bulk
        Map<Integer, Product> productMap = fetchProducts(productIds);
        Map<Integer, ProductVariant> variantMap = fetchProductVariants(variantIds);

        // 3. Process details, create child entities, and update stock
        List<IncomingProductDetail> incomingProductDetailListEntity = new ArrayList<>();
        List<IncomingProductVariantDetail> allVariantDetails = new ArrayList<>();

        for (IncomingProductCreateRequest.IncomingProductDetails detailRequest: request.getIncomingProductDetails()) {
            Product product = productMap.get(detailRequest.getProductId());
            compareHasVariantIncomingProductDetailAndProduct(detailRequest.getHasVariant() != product.getHasVariant(), product);

            IncomingProductDetail productDetailEntity = buildProductDetailEntity(detailRequest, incomingProductEntity, product, variantMap, allVariantDetails);

            incomingProductDetailListEntity.add(productDetailEntity);
        }
        // NOTE: We DO NOT need to save productMap or variantMap.
        // They were fetched from the database, making them "managed".
        // JPA's Dirty Checking will automatically persist any changes (like stock updates)
        // when the transaction commits.

        return Pair.of(incomingProductDetailListEntity, allVariantDetails);
    }

    private IncomingProduct buildIncomingProductEntity(IncomingProductCreateRequest request,
                                                       Supplier supplier,
                                                       User user) {
        log.debug("Building main IncomingProduct entity.");
        IncomingProduct incomingProductEntity = new IncomingProduct();
        incomingProductEntity.setDateIn(request.getDateIn());
        incomingProductEntity.setSupplier(supplier);
        incomingProductEntity.setUser(user);
        incomingProductEntity.setTotalProducts(request.getTotalProducts());
        incomingProductEntity.setNote(request.getNote());
        return incomingProductEntity;
    }



    private IncomingProductDetail buildProductDetailEntity(IncomingProductCreateRequest.IncomingProductDetails productDetailsRequest,
                                                           IncomingProduct incomingProductEntity,
                                                           Product product,
                                                           Map<Integer, ProductVariant> variantMap,
                                                           List<IncomingProductVariantDetail> allVariantDetails) {
        log.debug("Building IncomingProductDetail entity. productDetailsRequest={}", productDetailsRequest);

        IncomingProductDetail productDetailEntity = new IncomingProductDetail();
        productDetailEntity.setHasVariant(productDetailsRequest.getHasVariant());
        productDetailEntity.setIncomingProduct(incomingProductEntity);
        productDetailEntity.setProduct(product);

        if (!productDetailsRequest.getHasVariant()) {
            // Product without variants
            setIncomingProductDetailWithoutVariant(productDetailEntity, productDetailsRequest.getPricePerUnit(), productDetailsRequest.getQuantity());
            product.setStock(product.getStock() + productDetailsRequest.getQuantity());
        } else {
            // Product with variants
            AtomicInteger totalVariantQuantity = new AtomicInteger(0);
            AtomicInteger totalVariantPrice = new AtomicInteger(0);
            List<IncomingProductVariantDetail> variantDetailsForThisProduct = new ArrayList<>();

            for (IncomingProductCreateRequest.IncomingProductVariantDetail variantDetailRequest: productDetailsRequest.getIncomingProductVariantDetails()){
                ProductVariant productVariant = variantMap.get(variantDetailRequest.getVariantId());

                // Validate that the variant belongs to the correct product
                if (!productVariant.getProduct().getId().equals(product.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "ProductVariant with id " + productVariant.getId() + " is not a variant for Product with id " + product.getId() + ".");
                }


                IncomingProductVariantDetail variantDetailEntity = buildVariantDetailAndUpdateVariantStock(variantDetailRequest, productDetailEntity, productVariant);

                totalVariantQuantity.addAndGet(variantDetailRequest.getQuantity());
                totalVariantPrice.addAndGet(variantDetailEntity.getTotalPrice());

                variantDetailsForThisProduct.add(variantDetailEntity);
            }

            productDetailEntity.setTotalVariantQuantity(totalVariantQuantity.get());
            productDetailEntity.setTotalVariantPrice(totalVariantPrice.get());
            productDetailEntity.setIncomingProductVariantDetails(variantDetailsForThisProduct);
            allVariantDetails.addAll(variantDetailsForThisProduct);
        }
        return productDetailEntity;
    }

    private IncomingProductVariantDetail buildVariantDetailAndUpdateVariantStock(IncomingProductCreateRequest.IncomingProductVariantDetail variantDetailRequest,
                                                                                 IncomingProductDetail incomingProductDetail,
                                                                                 ProductVariant productVariant) {
        log.debug("Building IncomingProductVariantDetail entity. variantDetailRequest={}", variantDetailRequest);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(variantDetailRequest.getPricePerUnit());
        incomingProductVariantDetail.setQuantity(variantDetailRequest.getQuantity());
        incomingProductVariantDetail.setTotalPrice(variantDetailRequest.getQuantity() * variantDetailRequest.getPricePerUnit());

        // update productVariant stock
        productVariant.setStock(productVariant.getStock() + variantDetailRequest.getQuantity());

        return incomingProductVariantDetail;
    }

    private int calcStockChange(int currentStock, int newQuantityIn, int oldQuantityIn ) {
        log.debug("Calculating stock change. currentStock={}, newQuantityIn={}, oldQuantityIn={}", currentStock, newQuantityIn, oldQuantityIn);
        // quantityChange = 15 - 10 = 5 | stockProduct = currentStock + +5
        // quantityChange = 5 - 10 = -5 | stockProduct = currentStock + -5
        int quantityChange = newQuantityIn - oldQuantityIn;
        return currentStock + quantityChange;
    }

    private int calcTotalPrice(int price, int quantity) {
        log.debug("Calculating total price. price={}, quantity={}", price, quantity);
        return price * quantity;
    }

}

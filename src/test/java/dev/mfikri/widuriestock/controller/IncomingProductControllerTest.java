package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.Role;
import dev.mfikri.widuriestock.entity.Supplier;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.entity.incoming_product.IncomingProduct;
import dev.mfikri.widuriestock.entity.incoming_product.IncomingProductDetail;
import dev.mfikri.widuriestock.entity.incoming_product.IncomingProductVariantDetail;
import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.entity.product.Product;
import dev.mfikri.widuriestock.entity.product.ProductVariant;
import dev.mfikri.widuriestock.entity.product.ProductVariantAttribute;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.incoming_product.IncomingProductCreateRequest;
import dev.mfikri.widuriestock.model.incoming_product.IncomingProductResponse;
import dev.mfikri.widuriestock.repository.*;
import dev.mfikri.widuriestock.util.BCrypt;
import dev.mfikri.widuriestock.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class IncomingProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductVariantAttributeRepository productVariantAttributeRepository;


    @Autowired
    private IncomingProductRepository incomingProductRepository;

    @Autowired
    private IncomingProductDetailRepository incomingProductDetailRepository;

    @Autowired
    private IncomingProductVariantDetailRepository incomingProductVariantDetailRepository;

    @Autowired
    private JwtUtil jwtUtil;
    Integer jwtTtl = 300000;

    String authorizationToken = "";
    int supplierId = 0;
    int categoryId = 0;
    Product productWithoutVariant = new Product();
    Product productWithVariant = new Product();
    ProductVariant productVariant = new ProductVariant();

    @BeforeEach
    void setUp() {
        incomingProductRepository.deleteAll();
        userRepository.deleteAll();
        supplierRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        User user = new User();
        user.setUsername("admin_warehouse");
        user.setPassword("{bcrypt}" + BCrypt.hashpw("admin_warehouse_password", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.name());
        userRepository.save(user);

        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);

        Supplier supplier = new Supplier();
        supplier.setSupplierName("PT ABC");
        supplier.setPhone("62811111");
        supplier.setEmail("john@abc.com");
        supplier.setInformation("Supplier fishing tools");
        supplierRepository.save(supplier);
        supplierId = supplier.getId();

        Category category = new Category();
        category.setName("Reels");
        category.setDescription("Reels category");
        categoryRepository.save(category);
        categoryId = category.getId();

        productWithoutVariant = new Product();
        productWithoutVariant.setName("Fishing Rood 123");
        productWithoutVariant.setPrice(100500);
        productWithoutVariant.setStock(20);
        productWithoutVariant.setDescription("Description about rood");
        productWithoutVariant.setHasVariant(false);
        productWithoutVariant.setCategory(category);
        productRepository.save(productWithoutVariant);

        productWithVariant = new Product();
        productWithVariant.setName("Product Test");
        productWithVariant.setDescription("Description about product test");
        productWithVariant.setHasVariant(true);
        productWithVariant.setCategory(category);
        productRepository.save(productWithVariant);

        productVariant = new ProductVariant();
        productVariant.setProduct(productWithVariant);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);
    }

    @Test
    void createFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                post("/api/incoming-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncomingProductCreateRequest()))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void createFailedValidation() throws Exception {
        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncomingProductCreateRequest()))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createFailedSupplierNotFound() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(0);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(0)
                .pricePerUnit(100)
                .quantity(10)
                .build();
        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());


        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Supplier is not found.", response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createFailedValidationWhenHasVariantProductFalse() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(0)
//                .pricePerUnit(100)
//                .quantity(10)
                .build();
        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());


        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductDetails 'pricePerUnit', and 'quantity' properties must not null when 'hasVariant' is false.", response.getErrors());
            log.info(response.getErrors());
        });

        incomingProductDetailsRequest.setPricePerUnit(100123);
        incomingProductDetailsRequest.setQuantity(100);

        incomingProductDetailsRequest.setIncomingProductVariantDetails(
                List.of(IncomingProductCreateRequest.IncomingProductVariantDetail
                        .builder()
                        .variantId(100)
                        .pricePerUnit(1000)
                        .quantity(100)
                        .build()
                )
        );
        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductDetails 'IncomingProductVariantDetails' properties must not send when 'hasVariant' is false.", response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createFailedValidationWhenHasVariantProductTrue() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(true)
                .productId(0)
                .pricePerUnit(100)
                .quantity(10)
                .build();
        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());


        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductDetails 'pricePerUnit', and 'quantity' properties must not send when 'hasVariant' is true.", response.getErrors());
            log.info(response.getErrors());
        });

        incomingProductDetailsRequest.setPricePerUnit(null);
        incomingProductDetailsRequest.setQuantity(null);

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductDetails 'IncomingProductVariantDetails' properties must not null when 'hasVariant' is true.", response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createFailedProductIdDuplicate() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(0)
                .pricePerUnit(100)
                .quantity(10)
                .build();
        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest2 = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(0)
                .pricePerUnit(100)
                .quantity(10)
                .build();
        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest, incomingProductDetailsRequest2)));
        request.setTotalProducts(request.getIncomingProductDetails().size());


        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("'Product id' must not duplicate in one IncomingProduct.", response.getErrors());
            log.info(response.getErrors());
        });

    }

    @Test
    void createFailedProductIsNotFound() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(0)
                .pricePerUnit(100)
                .quantity(10)
                .build();
        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest2 = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(1)
                .pricePerUnit(100)
                .quantity(10)
                .build();
        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest, incomingProductDetailsRequest2)));
        request.setTotalProducts(request.getIncomingProductDetails().size());


        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Some of 'products id' is wrong.", response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createFailedProductHasVariantFromDatabaseIsNotMatchWithHasVariantRequest() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(true)
                .productId(productWithoutVariant.getId())
                .build();

        incomingProductDetailsRequest.setIncomingProductVariantDetails(
                List.of(IncomingProductCreateRequest.IncomingProductVariantDetail
                        .builder()
                        .variantId(100)
                        .pricePerUnit(1000)
                        .quantity(100)
                        .build()
                )
        );

        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("'Product id': " + productWithoutVariant.getId() + " is not have variant, please input valid product and valid variant.", response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createFailedProductVariantNotFound() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(99999)
                .pricePerUnit(120500)
                .quantity(50)
                .build();

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(true)
                .productId(productWithVariant.getId())
                .incomingProductVariantDetails(List.of(incomingProductVariantDetailRequest))
                .build();

        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Some of product variant is not found, please check product variant id again.", response.getErrors());
        });
    }

    @Test
    void createFailedProductAndProductVariantNotMatch() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test2");
        product.setDescription("Product Description Test");
        product.setHasVariant(false);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black2");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black2");
        productVariantAttributeRepository.save(productVariantAttribute);


        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant.getId())
                .pricePerUnit(120500)
                .quantity(50)
                .build();

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(true)
                .productId(productWithVariant.getId())
                .incomingProductVariantDetails(List.of(incomingProductVariantDetailRequest))
                .build();

        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isConflict()
        ).andDo(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("'Product Variant' " + productVariant.getId() + " is not product variant for 'Product' " + productWithVariant.getId(), response.getErrors());
        });
    }

    @Test
    void createSuccessForProductWithoutVariant() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(productWithoutVariant.getId())
                .pricePerUnit(100123)
                .quantity(10)
                .build();

        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(request.getSupplierId(), response.getData().getSupplier().getId());
            assertEquals("PT ABC", response.getData().getSupplier().getName());
            assertEquals("admin_warehouse", response.getData().getUsername());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProduct());

            IncomingProductResponse.IncomingProductDetail incomingProductDetail = response.getData().getIncomingProductDetails().getFirst();
            assertNotNull(incomingProductDetail);
            assertNotNull(incomingProductDetail.getId());

            Product product = productRepository.findById(request.getIncomingProductDetails().getFirst().getProductId()).orElse(null);
            assertNotNull(product);
            assertEquals(product.getId(), incomingProductDetail.getProduct().getId());
            assertEquals(product.getName(), incomingProductDetail.getProduct().getName());
            // assert updated product
            assertEquals(productWithoutVariant.getStock() + request.getIncomingProductDetails().getFirst().getQuantity(), product.getStock());
            assertEquals(request.getIncomingProductDetails().getFirst().getPricePerUnit(), product.getPrice());

            assertEquals(incomingProductDetailsRequest.getPricePerUnit() ,incomingProductDetail.getPricePerUnit());
            assertEquals(incomingProductDetailsRequest.getQuantity() ,incomingProductDetail.getQuantity());
            assertEquals(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity() ,incomingProductDetail.getTotalPrice());
            assertEquals(false ,incomingProductDetail.getHasVariant());
            assertNull(incomingProductDetail.getTotalVariantQuantity());
            assertNull(incomingProductDetail.getTotalVariantPrice());
            assertNull(incomingProductDetail.getIncomingProductVariantDetails());

        });
    }

    @Test
    void createSuccessForMultipleProductWithoutVariant() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product productWithoutVariant2 = new Product();
        productWithoutVariant2.setName("Fishing Rood 321");
        productWithoutVariant2.setPrice(100500);
        productWithoutVariant2.setStock(20);
        productWithoutVariant2.setDescription("Description about rood");
        productWithoutVariant2.setHasVariant(false);
        productWithoutVariant2.setCategory(category);
        productRepository.save(productWithoutVariant2);

        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(productWithoutVariant.getId())
                .pricePerUnit(100123)
                .quantity(10)
                .build();

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest2 = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(productWithoutVariant2.getId())
                .pricePerUnit(100123)
                .quantity(10)
                .build();

        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest, incomingProductDetailsRequest2)));
        request.setTotalProducts(request.getIncomingProductDetails().size());

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(request.getSupplierId(), response.getData().getSupplier().getId());
            assertEquals("PT ABC", response.getData().getSupplier().getName());
            assertEquals("admin_warehouse", response.getData().getUsername());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProduct());
            assertEquals(2, response.getData().getIncomingProductDetails().size());

            response.getData().getIncomingProductDetails().forEach(incomingProductDetail -> {
                assertNotNull(incomingProductDetail);
                assertNotNull(incomingProductDetail.getId());

                IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsCreateRequest = request.getIncomingProductDetails()
                        .stream()
                        .filter(iPD -> Objects.equals(iPD.getProductId(), incomingProductDetail.getProduct().getId()))
                        .findFirst()
                        .orElse(null);

                assertNotNull(incomingProductDetailsCreateRequest);

                Product product = productRepository.findById(incomingProductDetailsCreateRequest.getProductId()).orElse(null);
                assertNotNull(product);
                assertEquals(product.getId(), incomingProductDetail.getProduct().getId());
                assertEquals(product.getName(), incomingProductDetail.getProduct().getName());
                // assert updated product
                assertEquals(productWithoutVariant.getStock() + request.getIncomingProductDetails().getFirst().getQuantity(), product.getStock());
                assertEquals(request.getIncomingProductDetails().getFirst().getPricePerUnit(), product.getPrice());


                assertEquals(incomingProductDetailsCreateRequest.getPricePerUnit() ,incomingProductDetail.getPricePerUnit());
                assertEquals(incomingProductDetailsCreateRequest.getQuantity() ,incomingProductDetail.getQuantity());
                assertEquals(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity() ,incomingProductDetail.getTotalPrice());
                assertEquals(false ,incomingProductDetail.getHasVariant());
                assertNull(incomingProductDetail.getTotalVariantQuantity());
                assertNull(incomingProductDetail.getTotalVariantPrice());
                assertNull(incomingProductDetail.getIncomingProductVariantDetails());
            });
        });
    }

    @Test
    void createSuccessForProductWithVariant() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant.getId())
                .pricePerUnit(120500)
                .quantity(10)
                .build();

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(true)
                .productId(productWithVariant.getId())
                .incomingProductVariantDetails(List.of(incomingProductVariantDetailRequest))
                .build();


        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(request.getSupplierId(), response.getData().getSupplier().getId());
            assertEquals("PT ABC", response.getData().getSupplier().getName());
            assertEquals("admin_warehouse", response.getData().getUsername());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProduct());

            IncomingProductResponse.IncomingProductDetail incomingProductDetailResponse = response.getData().getIncomingProductDetails().getFirst();
            assertNotNull(incomingProductDetailResponse);
            assertNotNull(incomingProductDetailResponse.getId());

            Product product = productRepository.findById(request.getIncomingProductDetails().getFirst().getProductId()).orElse(null);
            assertNotNull(product);
            assertEquals(product.getId(), incomingProductDetailResponse.getProduct().getId());
            assertEquals(product.getName(), incomingProductDetailResponse.getProduct().getName());
            // assert updated product
            assertNull(product.getStock());
            assertNull(product.getPrice());

            assertNull(incomingProductDetailResponse.getPricePerUnit());
            assertNull(incomingProductDetailResponse.getQuantity());
            assertNull(incomingProductDetailResponse.getTotalPrice());
            assertEquals(true ,incomingProductDetailResponse.getHasVariant());

            assertEquals(incomingProductVariantDetailRequest.getQuantity(), incomingProductDetailResponse.getTotalVariantQuantity());
            assertEquals(
                    incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity(),
                    incomingProductDetailResponse.getTotalVariantPrice()
            );
            assertEquals(1, incomingProductDetailResponse.getIncomingProductVariantDetails().size());
            IncomingProductResponse.IncomingProductVariantDetail incomingProductVariantDetailResponse = incomingProductDetailResponse.getIncomingProductVariantDetails().getFirst();
            assertNotNull(incomingProductVariantDetailResponse.getId());
            assertEquals(incomingProductVariantDetailRequest.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
            assertEquals(incomingProductVariantDetailRequest.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
            assertEquals(
                    incomingProductVariantDetailRequest.getQuantity() * incomingProductVariantDetailRequest.getPricePerUnit(),
                    incomingProductVariantDetailResponse.getTotalPrice()
            );

            IncomingProductVariantDetail incomingProductVariantDetail = incomingProductVariantDetailRepository.findById(incomingProductVariantDetailResponse.getId()).orElse(null);
            assertNotNull(incomingProductVariantDetail);
            assertEquals(incomingProductVariantDetail.getId(), incomingProductVariantDetailResponse.getId());
            assertEquals(incomingProductVariantDetail.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
            assertEquals(incomingProductVariantDetail.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
            assertEquals(incomingProductVariantDetail.getTotalPrice(), incomingProductVariantDetailResponse.getTotalPrice());
        });
    }

    @Test
    void createSuccessForProductWithMultipleVariant() throws Exception {
        ProductVariant productVariant2 = new ProductVariant();
        productVariant2.setProduct(productWithVariant);
        productVariant2.setSku("product-test-black2");
        productVariant2.setPrice(100500);
        productVariant2.setStock(100);
        productVariantRepository.save(productVariant2);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant2);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant.getId())
                .pricePerUnit(120500)
                .quantity(10)
                .build();

        IncomingProductCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest2 = IncomingProductCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant2.getId())
                .pricePerUnit(120900)
                .quantity(10)
                .build();

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(true)
                .productId(productWithVariant.getId())
                .incomingProductVariantDetails(List.of(incomingProductVariantDetailRequest, incomingProductVariantDetailRequest2))
                .build();

        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest)));
        request.setTotalProducts(request.getIncomingProductDetails().size());

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(request.getSupplierId(), response.getData().getSupplier().getId());
            assertEquals("PT ABC", response.getData().getSupplier().getName());
            assertEquals("admin_warehouse", response.getData().getUsername());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProduct());

            IncomingProductResponse.IncomingProductDetail incomingProductDetailResponse = response.getData().getIncomingProductDetails().getFirst();
            assertNotNull(incomingProductDetailResponse);
            assertNotNull(incomingProductDetailResponse.getId());

            Product product = productRepository.findById(request.getIncomingProductDetails().getFirst().getProductId()).orElse(null);
            assertNotNull(product);
            assertEquals(product.getId(), incomingProductDetailResponse.getProduct().getId());
            assertEquals(product.getName(), incomingProductDetailResponse.getProduct().getName());
            // assert updated product
            assertNull(product.getStock());
            assertNull(product.getPrice());

            assertNull(incomingProductDetailResponse.getPricePerUnit());
            assertNull(incomingProductDetailResponse.getQuantity());
            assertNull(incomingProductDetailResponse.getTotalPrice());
            assertEquals(true ,incomingProductDetailResponse.getHasVariant());

            assertEquals(incomingProductVariantDetailRequest.getQuantity() + incomingProductVariantDetailRequest2.getQuantity() , incomingProductDetailResponse.getTotalVariantQuantity());
            assertEquals(
                    (incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity()) +
                            (incomingProductVariantDetailRequest2.getPricePerUnit() * incomingProductVariantDetailRequest2.getQuantity())
                    ,
                    incomingProductDetailResponse.getTotalVariantPrice()
            );
            assertEquals(2, incomingProductDetailResponse.getIncomingProductVariantDetails().size());

            incomingProductDetailResponse.getIncomingProductVariantDetails().forEach(incomingProductVariantDetailResponse -> {
                IncomingProductCreateRequest.IncomingProductVariantDetail incomingProductVariantDetail = incomingProductDetailsRequest.getIncomingProductVariantDetails()
                        .stream()
                        .filter(iPVD -> Objects.equals(iPVD.getVariantId(), incomingProductVariantDetailResponse.getVariant().getId()))
                        .findFirst()
                        .orElse(null);
                assertNotNull(incomingProductVariantDetail);

                assertNotNull(incomingProductVariantDetailResponse.getId());

                assertEquals(incomingProductVariantDetail.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
                assertEquals(incomingProductVariantDetail.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
                assertEquals(
                        incomingProductVariantDetail.getQuantity() * incomingProductVariantDetail.getPricePerUnit(),
                    incomingProductVariantDetailResponse.getTotalPrice()
                );
                IncomingProductVariantDetail incomingProductVariantDetailDB = incomingProductVariantDetailRepository.findById(incomingProductVariantDetailResponse.getId()).orElse(null);
                assertNotNull(incomingProductVariantDetailDB);
                assertEquals(incomingProductVariantDetailDB.getId(), incomingProductVariantDetailResponse.getId());
                assertEquals(incomingProductVariantDetailDB.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
                assertEquals(incomingProductVariantDetailDB.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
                assertEquals(incomingProductVariantDetailDB.getTotalPrice(), incomingProductVariantDetailResponse.getTotalPrice());
            });
        });
    }

    @Test
    void createSuccessForProductWithoutVariantAndProductWithVariant() throws Exception {
        IncomingProductCreateRequest request = new IncomingProductCreateRequest();
        request.setDateIn(LocalDate.parse("2025-10-10"));
        request.setSupplierId(supplierId);
        request.setNote("Product is well condition");

        IncomingProductCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant.getId())
                .pricePerUnit(120500)
                .quantity(10)
                .build();

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(true)
                .productId(productWithVariant.getId())
                .incomingProductVariantDetails(List.of(incomingProductVariantDetailRequest))
                .build();

        IncomingProductCreateRequest.IncomingProductDetails incomingProductDetailsRequest2 = IncomingProductCreateRequest.IncomingProductDetails.builder()
                .hasVariant(false)
                .productId(productWithoutVariant.getId())
                .pricePerUnit(100123)
                .quantity(10)
                .build();

        request.setIncomingProductDetails(new ArrayList<>(List.of(incomingProductDetailsRequest, incomingProductDetailsRequest2)));
        request.setTotalProducts(request.getIncomingProductDetails().size());

        mockMvc.perform(
                post("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(request.getSupplierId(), response.getData().getSupplier().getId());
            assertEquals("PT ABC", response.getData().getSupplier().getName());
            assertEquals("admin_warehouse", response.getData().getUsername());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProduct());

            response.getData().getIncomingProductDetails().forEach(incomingProductDetailResponse -> {
                assertNotNull(incomingProductDetailResponse);
                assertNotNull(incomingProductDetailResponse.getId());
                Product product = productRepository.findById(incomingProductDetailResponse.getProduct().getId()).orElse(null);
                assertNotNull(product);
                assertEquals(product.getId(), incomingProductDetailResponse.getProduct().getId());
                assertEquals(product.getName(), incomingProductDetailResponse.getProduct().getName());

                if (!incomingProductDetailResponse.getHasVariant()) {
                    assertEquals(productWithoutVariant.getStock() + incomingProductDetailsRequest2.getQuantity(), product.getStock());
                    assertEquals(incomingProductDetailsRequest2.getPricePerUnit(), product.getPrice());

                    assertEquals(incomingProductDetailsRequest2.getPricePerUnit() ,incomingProductDetailResponse.getPricePerUnit());
                    assertEquals(incomingProductDetailsRequest2.getQuantity() ,incomingProductDetailResponse.getQuantity());
                    assertEquals(incomingProductDetailsRequest2.getPricePerUnit() * incomingProductDetailsRequest2.getQuantity() ,incomingProductDetailResponse.getTotalPrice());
                    assertNull(incomingProductDetailResponse.getTotalVariantQuantity());
                    assertNull(incomingProductDetailResponse.getTotalVariantPrice());
                    assertNull(incomingProductDetailResponse.getIncomingProductVariantDetails());

                } else {
                    assertNull(product.getStock());
                    assertNull(product.getPrice());
                    assertNull(incomingProductDetailResponse.getPricePerUnit());
                    assertNull(incomingProductDetailResponse.getQuantity());
                    assertNull(incomingProductDetailResponse.getTotalPrice());
                    assertEquals(incomingProductVariantDetailRequest.getQuantity(), incomingProductDetailResponse.getTotalVariantQuantity());
                    assertEquals(
                            incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity(),
                            incomingProductDetailResponse.getTotalVariantPrice()
                    );
                    assertEquals(1, incomingProductDetailResponse.getIncomingProductVariantDetails().size());

                    IncomingProductResponse.IncomingProductVariantDetail incomingProductVariantDetailResponse = incomingProductDetailResponse.getIncomingProductVariantDetails().getFirst();
                    assertNotNull(incomingProductVariantDetailResponse.getId());
                    assertEquals(incomingProductVariantDetailRequest.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
                    assertEquals(incomingProductVariantDetailRequest.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
                    assertEquals(
                            incomingProductVariantDetailRequest.getQuantity() * incomingProductVariantDetailRequest.getPricePerUnit(),
                            incomingProductVariantDetailResponse.getTotalPrice()
                    );
                    IncomingProductVariantDetail incomingProductVariantDetail = incomingProductVariantDetailRepository.findById(incomingProductVariantDetailResponse.getId()).orElse(null);
                    assertNotNull(incomingProductVariantDetail);
                    assertEquals(incomingProductVariantDetail.getId(), incomingProductVariantDetailResponse.getId());
                    assertEquals(incomingProductVariantDetail.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
                    assertEquals(incomingProductVariantDetail.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
                    assertEquals(incomingProductVariantDetail.getTotalPrice(), incomingProductVariantDetailResponse.getTotalPrice());
                }
            });
        });
    }

    @Test
    void getFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                get("/api/incoming-products/123")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andExpect(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void getFailedPathIdNotNumberFormat() throws Exception {
        mockMvc.perform(
                get("/api/incoming-products/abc")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductId type data is wrong.", response.getErrors());
        });
    }

    @Test
    void getFailedIncomingProductIsNotFound() throws Exception {
        mockMvc.perform(
                get("/api/incoming-products/999999")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Incoming Product is not found.", response.getErrors());
        });
    }

    @Test
    void getSuccessForIncomingProductWithoutVariantProduct() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-22"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setPricePerUnit(40500);
        incomingProductDetail.setQuantity(100);
        incomingProductDetail.setTotalPrice(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity());
        incomingProductDetail.setHasVariant(false);
        incomingProductDetailRepository.save(incomingProductDetail);

        mockMvc.perform(
                get("/api/incoming-products/" + incomingProduct.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("Authorization", authorizationToken)
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());

            assertEquals(incomingProduct.getId(),response.getData().getId());
            assertEquals(supplier.getId(), response.getData().getSupplier().getId());
            assertEquals(supplier.getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals(incomingProduct.getTotalProducts(), response.getData().getTotalProduct());
            assertEquals(incomingProduct.getNote(), response.getData().getNote());
            assertEquals(incomingProduct.getTotalProducts(), response.getData().getIncomingProductDetails().size());
            assertEquals(incomingProductDetail.getId(), response.getData().getIncomingProductDetails().getFirst().getId());
            assertEquals(incomingProductDetail.getProduct().getId(), response.getData().getIncomingProductDetails().getFirst().getProduct().getId());
            assertEquals(incomingProductDetail.getProduct().getName(), response.getData().getIncomingProductDetails().getFirst().getProduct().getName());
            assertEquals(incomingProductDetail.getPricePerUnit(), response.getData().getIncomingProductDetails().getFirst().getPricePerUnit());
            assertEquals(incomingProductDetail.getQuantity(), response.getData().getIncomingProductDetails().getFirst().getQuantity());
            assertEquals(incomingProductDetail.getTotalPrice(), response.getData().getIncomingProductDetails().getFirst().getTotalPrice());
            assertEquals(incomingProductDetail.getHasVariant(), response.getData().getIncomingProductDetails().getFirst().getHasVariant());
            assertNull(response.getData().getIncomingProductDetails().getFirst().getTotalVariantQuantity());
            assertNull(response.getData().getIncomingProductDetails().getFirst().getTotalVariantPrice());
            assertNull(response.getData().getIncomingProductDetails().getFirst().getIncomingProductVariantDetails());
        });
    }
}
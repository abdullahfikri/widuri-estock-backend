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
import dev.mfikri.widuriestock.model.incoming_product.*;
import dev.mfikri.widuriestock.repository.*;
import dev.mfikri.widuriestock.util.BCrypt;
import dev.mfikri.widuriestock.util.JwtUtil;
import jakarta.transaction.Transactional;
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
import java.util.Optional;

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
            assertEquals("Supplier is not found. Please check Supplier Id again.", response.getErrors());
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
            assertEquals("Some of 'products' is not found, please check the productId again.", response.getErrors());
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
            assertEquals("Product id: " + productWithoutVariant.getId() + " hasVariant is false, please check hasVariant again.", response.getErrors());
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
            assertEquals("Some of ProductVariant is not found, please check ProductVariant id again.", response.getErrors());
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
            assertEquals("ProductVariant with id " + productVariant.getId() + " is not ProductVariant for Product with id " + productWithVariant.getId() + ".", response.getErrors());
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
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());

            IncomingProductResponse.IncomingProductDetail incomingProductDetail = response.getData().getIncomingProductDetails().getFirst();
            assertNotNull(incomingProductDetail);
            assertNotNull(incomingProductDetail.getId());

            Product product = productRepository.findById(request.getIncomingProductDetails().getFirst().getProductId()).orElse(null);
            assertNotNull(product);
            assertEquals(product.getId(), incomingProductDetail.getProduct().getId());
            assertEquals(product.getName(), incomingProductDetail.getProduct().getName());
            // assert updated product
            assertEquals(productWithoutVariant.getStock() + request.getIncomingProductDetails().getFirst().getQuantity(), product.getStock());

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
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());
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
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());

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
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());

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
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());

            response.getData().getIncomingProductDetails().forEach(incomingProductDetailResponse -> {
                assertNotNull(incomingProductDetailResponse);
                assertNotNull(incomingProductDetailResponse.getId());
                Product product = productRepository.findById(incomingProductDetailResponse.getProduct().getId()).orElse(null);
                assertNotNull(product);
                assertEquals(product.getId(), incomingProductDetailResponse.getProduct().getId());
                assertEquals(product.getName(), incomingProductDetailResponse.getProduct().getName());

                if (!incomingProductDetailResponse.getHasVariant()) {
                    assertEquals(productWithoutVariant.getStock() + incomingProductDetailsRequest2.getQuantity(), product.getStock());

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
            assertEquals("IncomingProduct is not found. Please check IncomingProduct id again.", response.getErrors());
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
            assertEquals(incomingProduct.getTotalProducts(), response.getData().getTotalProducts());
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

    @Test
    void getSuccessForIncomingProductWithVariantProduct() throws Exception {
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
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetail.setTotalVariantPrice(100400 * 10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setPricePerUnit(100400);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(incomingProductVariantDetail.getPricePerUnit() * incomingProductVariantDetail.getQuantity());
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);

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
            assertEquals(incomingProduct.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(incomingProduct.getNote(), response.getData().getNote());
            assertEquals(incomingProduct.getTotalProducts(), response.getData().getIncomingProductDetails().size());
            assertEquals(incomingProductDetail.getId(), response.getData().getIncomingProductDetails().getFirst().getId());
            assertEquals(incomingProductDetail.getProduct().getId(), response.getData().getIncomingProductDetails().getFirst().getProduct().getId());
            assertEquals(incomingProductDetail.getProduct().getName(), response.getData().getIncomingProductDetails().getFirst().getProduct().getName());
            assertEquals(incomingProductDetail.getPricePerUnit(), response.getData().getIncomingProductDetails().getFirst().getPricePerUnit());
            assertEquals(incomingProductDetail.getQuantity(), response.getData().getIncomingProductDetails().getFirst().getQuantity());
            assertEquals(incomingProductDetail.getTotalPrice(), response.getData().getIncomingProductDetails().getFirst().getTotalPrice());
            assertEquals(incomingProductDetail.getHasVariant(), response.getData().getIncomingProductDetails().getFirst().getHasVariant());
            assertEquals(incomingProductDetail.getTotalVariantQuantity(),response.getData().getIncomingProductDetails().getFirst().getTotalVariantQuantity());
            assertEquals(incomingProductDetail.getTotalVariantPrice(), response.getData().getIncomingProductDetails().getFirst().getTotalVariantPrice());
            assertEquals(1, response.getData().getIncomingProductDetails().getFirst().getIncomingProductVariantDetails().size());

            assertEquals(incomingProductVariantDetail.getId(), response.getData().getIncomingProductDetails().getFirst().getIncomingProductVariantDetails().getFirst().getId());
            assertEquals(productVariant.getId(), response.getData().getIncomingProductDetails().getFirst().getIncomingProductVariantDetails().getFirst().getVariant().getId());
            assertEquals(productVariant.getSku(), response.getData().getIncomingProductDetails().getFirst().getIncomingProductVariantDetails().getFirst().getVariant().getSku());
            assertEquals(incomingProductVariantDetail.getPricePerUnit(), response.getData().getIncomingProductDetails().getFirst().getIncomingProductVariantDetails().getFirst().getPricePerUnit());
            assertEquals(incomingProductVariantDetail.getQuantity(), response.getData().getIncomingProductDetails().getFirst().getIncomingProductVariantDetails().getFirst().getQuantity());
            assertEquals(incomingProductVariantDetail.getTotalPrice(), response.getData().getIncomingProductDetails().getFirst().getIncomingProductVariantDetails().getFirst().getTotalPrice());
        });
    }

    @Test
    void getSuccessForIncomingProductWithProductWithoutVariantAndProductWithVariantProduct() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-22"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(2);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetail.setTotalVariantPrice(100400 * 10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductDetail incomingProductDetail2 = new IncomingProductDetail();
        incomingProductDetail2.setIncomingProduct(incomingProduct);
        incomingProductDetail2.setProduct(productWithVariant);
        incomingProductDetail2.setPricePerUnit(40500);
        incomingProductDetail2.setQuantity(100);
        incomingProductDetail2.setTotalPrice(incomingProductDetail2.getPricePerUnit() * incomingProductDetail2.getQuantity());
        incomingProductDetail2.setHasVariant(false);
        incomingProductDetailRepository.save(incomingProductDetail2);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setPricePerUnit(100400);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(incomingProductVariantDetail.getPricePerUnit() * incomingProductVariantDetail.getQuantity());
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);

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
            assertEquals(incomingProduct.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(incomingProduct.getNote(), response.getData().getNote());
            assertEquals(incomingProduct.getTotalProducts(), response.getData().getIncomingProductDetails().size());

            response.getData().getIncomingProductDetails().forEach(incomingProductDetailResponse -> {
                assertNotNull(incomingProductDetailResponse);
                assertNotNull(incomingProductDetailResponse.getId());
                Product product = productRepository.findById(incomingProductDetailResponse.getProduct().getId()).orElse(null);
                assertNotNull(product);
                assertEquals(product.getId(), incomingProductDetailResponse.getProduct().getId());
                assertEquals(product.getName(), incomingProductDetailResponse.getProduct().getName());

                if (!incomingProductDetailResponse.getHasVariant()) {
                    assertEquals(incomingProductDetail2.getPricePerUnit(), incomingProductDetailResponse.getPricePerUnit());
                    assertEquals(incomingProductDetail2.getQuantity(), incomingProductDetailResponse.getQuantity());
                    assertEquals(incomingProductDetail2.getPricePerUnit() * incomingProductDetail2.getQuantity(), incomingProductDetailResponse.getTotalPrice());
                    assertNull(incomingProductDetailResponse.getTotalVariantQuantity());
                    assertNull(incomingProductDetailResponse.getTotalVariantPrice());
                    assertNull(incomingProductDetailResponse.getIncomingProductVariantDetails());

                } else {
                    assertNull(incomingProductDetailResponse.getPricePerUnit());
                    assertNull(incomingProductDetailResponse.getQuantity());
                    assertNull(incomingProductDetailResponse.getTotalPrice());
                    assertEquals(incomingProductVariantDetail.getQuantity(), incomingProductDetailResponse.getTotalVariantQuantity());
                    assertEquals(
                            incomingProductVariantDetail.getPricePerUnit() * incomingProductVariantDetail.getQuantity(),
                            incomingProductDetailResponse.getTotalVariantPrice()
                    );
                    assertEquals(1, incomingProductDetailResponse.getIncomingProductVariantDetails().size());

                    IncomingProductResponse.IncomingProductVariantDetail incomingProductVariantDetailResponse = incomingProductDetailResponse.getIncomingProductVariantDetails().getFirst();
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
                }
            });
        });
    }

    @Test
    void getListFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                get("/api/incoming-products")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void getListFailedPageAndSizeParamIsNotNumber() throws Exception {
        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "abc")
                        .param("size", "bca")
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("page type data is wrong.", response.getErrors());
        });

        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "1")
                        .param("size", "bca")
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("size type data is wrong.", response.getErrors());
        });
    }

    @Test
    void getListFailedStartDateAndEndDateParamIsNotDateFormat() throws Exception {
        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("start_date", "123")
                        .param("end_date", "123")
                        .param("page", "1")
                        .param("size", "10")
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("start_date type data is wrong.", response.getErrors());
        });

        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("start_date", "2025-10-30")
                        .param("end_date", "123")
                        .param("page", "1")
                        .param("size", "10")
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("end_date type data is wrong.", response.getErrors());
        });
    }

    @Test
    void getListFailedStartDateIsOverlapToEndDate() throws Exception {
        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("start_date", "2025-01-10")
                        .param("end_date", "2025-01-01")
                        .param("page", "1")
                        .param("size", "10")
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Start date 2025-01-10 must be before or equal to end date 2025-01-01.", response.getErrors());
        });
    }

    @Test
    void getListSuccessWithoutDateRange() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        List<IncomingProduct> incomingProductList = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            IncomingProduct incomingProduct = new IncomingProduct();
            incomingProduct.setDateIn(LocalDate.parse("2025-05-01").plusDays(i));
            incomingProduct.setSupplier(supplier);
            incomingProduct.setUser(user);
            incomingProduct.setTotalProducts(1);
            incomingProduct.setNote("Product is well condition");
            incomingProductList.add(incomingProduct);
        }

        incomingProductRepository.saveAll(incomingProductList);

        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "10")
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(3, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());

            IncomingProductGetListResponse incomingProduct = response.getData().getFirst();
            assertNotNull(incomingProduct.getId());
            assertEquals(LocalDate.parse("2025-05-01"), incomingProduct.getDateIn());
            assertEquals(supplier.getId(), incomingProduct.getSupplier().getId());
            assertEquals(supplier.getSupplierName(), incomingProduct.getSupplier().getName());
            assertEquals(user.getUsername(), incomingProduct.getUsername());
            assertEquals("Product is well condition", incomingProduct.getNote());
            assertEquals(1, incomingProduct.getTotalProducts());
        });
    }

    @Test
    void getListSuccessWithJustStartDate() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        List<IncomingProduct> incomingProductList = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            IncomingProduct incomingProduct = new IncomingProduct();
            incomingProduct.setDateIn(LocalDate.parse("2025-05-01").plusDays(i));
            incomingProduct.setSupplier(supplier);
            incomingProduct.setUser(user);
            incomingProduct.setTotalProducts(1);
            incomingProduct.setNote("Product is well condition");
            incomingProductList.add(incomingProduct);
        }

        incomingProductRepository.saveAll(incomingProductList);

        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("start_date", "2025-05-11")
                        .param("page", "0")
                        .param("size", "10")
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(2, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());

            IncomingProductGetListResponse incomingProduct = response.getData().getFirst();
            assertNotNull(incomingProduct.getId());
            assertEquals(LocalDate.parse("2025-05-11"), incomingProduct.getDateIn());
            assertEquals(supplier.getId(), incomingProduct.getSupplier().getId());
            assertEquals(supplier.getSupplierName(), incomingProduct.getSupplier().getName());
            assertEquals(user.getUsername(), incomingProduct.getUsername());
            assertEquals("Product is well condition", incomingProduct.getNote());
            assertEquals(1, incomingProduct.getTotalProducts());
        });
    }

    @Test
    void getListSuccessWithJustEndDate() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        List<IncomingProduct> incomingProductList = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            IncomingProduct incomingProduct = new IncomingProduct();
            incomingProduct.setDateIn(LocalDate.parse("2025-05-01").plusDays(i));
            incomingProduct.setSupplier(supplier);
            incomingProduct.setUser(user);
            incomingProduct.setTotalProducts(1);
            incomingProduct.setNote("Product is well condition");
            incomingProductList.add(incomingProduct);
        }

        incomingProductRepository.saveAll(incomingProductList);

        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("end_date", "2025-05-11")
                        .param("page", "0")
                        .param("size", "10")
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(2, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());

            IncomingProductGetListResponse incomingProduct = response.getData().getFirst();
            assertNotNull(incomingProduct.getId());
            assertEquals(LocalDate.parse("2025-05-01"), incomingProduct.getDateIn());
            assertEquals(supplier.getId(), incomingProduct.getSupplier().getId());
            assertEquals(supplier.getSupplierName(), incomingProduct.getSupplier().getName());
            assertEquals(user.getUsername(), incomingProduct.getUsername());
            assertEquals("Product is well condition", incomingProduct.getNote());
            assertEquals(1, incomingProduct.getTotalProducts());
        });
    }

    @Test
    void getListSuccessWithStartDateAndEndDate() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        List<IncomingProduct> incomingProductList = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            IncomingProduct incomingProduct = new IncomingProduct();
            incomingProduct.setDateIn(LocalDate.parse("2025-05-01").plusDays(i));
            incomingProduct.setSupplier(supplier);
            incomingProduct.setUser(user);
            incomingProduct.setTotalProducts(1);
            incomingProduct.setNote("Product is well condition");
            incomingProductList.add(incomingProduct);
        }

        incomingProductRepository.saveAll(incomingProductList);

        mockMvc.perform(
                get("/api/incoming-products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("start_date", "2025-05-05")
                        .param("end_date", "2025-05-10")
                        .param("page", "0")
                        .param("size", "10")
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<List<IncomingProductGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());
            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(1, response.getPaging().getTotalPage());
            assertEquals(10, response.getPaging().getSizePerPage());

            IncomingProductGetListResponse incomingProduct = response.getData().getFirst();
            assertNotNull(incomingProduct.getId());
            assertEquals(LocalDate.parse("2025-05-05"), incomingProduct.getDateIn());
            assertEquals(supplier.getId(), incomingProduct.getSupplier().getId());
            assertEquals(supplier.getSupplierName(), incomingProduct.getSupplier().getName());
            assertEquals(user.getUsername(), incomingProduct.getUsername());
            assertEquals("Product is well condition", incomingProduct.getNote());
            assertEquals(1, incomingProduct.getTotalProducts());
        });
    }

    @Test
    void updateFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                put("/api/incoming-products/123")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void updateFailedIncomingProductIdIsNotNumber() throws Exception {
        mockMvc.perform(
                put("/api/incoming-products/abc")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncomingProductUpdateRequest()))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductId type data is wrong.", response.getErrors());
        });
    }

    @Test
    void updateFailedValidation() throws Exception {
        mockMvc.perform(
                put("/api/incoming-products/123")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncomingProductUpdateRequest()))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void updateFailedIncomingProductIsNotFound() throws Exception {

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(123);
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(120500);
        incomingProductDetailRequest.setQuantity(50);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/999999")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProduct is not found. Please check IncomingProduct id again.", response.getErrors());
        });
    }

    @Test
    void updateFailedSupplierIsNotFound() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");

        incomingProductRepository.save(incomingProduct);


        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(9999999);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(123);
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(120500);
        incomingProductDetailRequest.setQuantity(50);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Supplier is not found. Please check Supplier Id again.", response.getErrors());
        });
    }

    @Test
    void updateFailedIncomingProductTotalProductIsWrong() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);


        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(2);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(123);
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(120500);
        incomingProductDetailRequest.setQuantity(50);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Total products 'IncomingProduct' is wrong.", response.getErrors());
        });

    }

    @Test
    void updateFailedIncomingProductDetailsSizeIsNotSameAsIncomingProductDetailsRequest() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);


        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(123);
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(120500);
        incomingProductDetailRequest.setQuantity(50);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductDetails size is not same. Please check the IncomingProductDetails again.", response.getErrors());
        });
    }

    @Test
    void updateFailedIncomingProductDetailsIdIsNotFound() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setPricePerUnit(120500);
        incomingProductDetail.setQuantity(50);
        incomingProductDetail.setTotalPrice(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity());
        incomingProductDetail.setHasVariant(false);
        incomingProductDetail.setTotalVariantPrice(null);
        incomingProductDetail.setTotalVariantQuantity(null);
        incomingProductDetail.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(999999);
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(120500);
        incomingProductDetailRequest.setQuantity(50);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductDetail is not found, please check IncomingProductDetail id again.", response.getErrors());
        });

    }

    @Test
    void updateFailedIncomingProductDetailsIdDuplicate() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(2);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setPricePerUnit(120500);
        incomingProductDetail.setQuantity(50);
        incomingProductDetail.setTotalPrice(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity());
        incomingProductDetail.setHasVariant(false);
        incomingProductDetail.setTotalVariantPrice(null);
        incomingProductDetail.setTotalVariantQuantity(null);
        incomingProductDetail.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductDetail incomingProductDetail2 = new IncomingProductDetail();
        incomingProductDetail2.setIncomingProduct(incomingProduct);
        incomingProductDetail2.setProduct(productWithoutVariant);
        incomingProductDetail2.setPricePerUnit(120500);
        incomingProductDetail2.setQuantity(50);
        incomingProductDetail2.setTotalPrice(incomingProductDetail2.getPricePerUnit() * incomingProductDetail2.getQuantity());
        incomingProductDetail2.setHasVariant(false);
        incomingProductDetail2.setTotalVariantPrice(null);
        incomingProductDetail2.setTotalVariantQuantity(null);
        incomingProductDetail2.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail2);

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(2);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(120500);
        incomingProductDetailRequest.setQuantity(50);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest2 = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest2.setId(incomingProductDetail.getId());
        incomingProductDetailRequest2.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest2.setPricePerUnit(120500);
        incomingProductDetailRequest2.setQuantity(50);
        incomingProductDetailRequest2.setHasVariant(false);
        incomingProductDetailRequest2.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest, incomingProductDetailRequest2));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductDetail is duplicate, please check IncomingProductDetails again.", response.getErrors());
        });
    }

    @Test
    void updateFailedIncomingProductDetailsHasVariantIsNotSameToIncomingProductDetailsUpdateRequest() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setPricePerUnit(120500);
        incomingProductDetail.setQuantity(50);
        incomingProductDetail.setTotalPrice(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity());
        incomingProductDetail.setHasVariant(false);
        incomingProductDetail.setTotalVariantPrice(null);
        incomingProductDetail.setTotalVariantQuantity(null);
        incomingProductDetail.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(null);
        incomingProductDetailRequest.setQuantity(null);
        incomingProductDetailRequest.setHasVariant(true);

        IncomingProductUpdateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductUpdateRequest.IncomingProductVariantDetail.builder()
                .id(incomingProductDetail.getId())
                .variantId(productVariant.getId())
                .pricePerUnit(50500)
                .quantity(100)
                .build();

        incomingProductDetailRequest.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));
        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductDetails 'id': " + incomingProductDetail.getId() + " has status 'hasVariant': false, please input valid IncomingProductDetail hasVariant.", response.getErrors());
        });
    }

    @Test
    void updateFailedProductIdIncomingProductDetailsIsWrong() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setPricePerUnit(120500);
        incomingProductDetail.setQuantity(50);
        incomingProductDetail.setTotalPrice(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity());
        incomingProductDetail.setHasVariant(false);
        incomingProductDetail.setTotalVariantPrice(null);
        incomingProductDetail.setTotalVariantQuantity(null);
        incomingProductDetail.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(999999);
        incomingProductDetailRequest.setPricePerUnit(120500);
        incomingProductDetailRequest.setQuantity(50);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Product id: 999999 is wrong, please check Product id again.", response.getErrors());
        });
    }

    @Test
    void updateFailedProductHasVariantIsAlreadyChange() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setPricePerUnit(120500);
        incomingProductDetail.setQuantity(50);
        incomingProductDetail.setTotalPrice(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity());
        incomingProductDetail.setHasVariant(false);
        incomingProductDetail.setTotalVariantPrice(null);
        incomingProductDetail.setTotalVariantQuantity(null);
        incomingProductDetail.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(120500);
        incomingProductDetailRequest.setQuantity(50);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);
        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        productWithoutVariant.setHasVariant(true);
        productRepository.save(productWithoutVariant);

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Product id: " + productWithoutVariant.getId() + " is already change hasVariant status, please delete and create new IncomingProductDetail for this IncomingProduct.", response.getErrors());
        });
    }

    @Test
    void updateFailedIncomingProductVariantDetailIdDuplicateInOneIncomingProductDetails() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(2);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setPricePerUnit(null);
        incomingProductDetail.setQuantity(null);
        incomingProductDetail.setTotalPrice(null);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantPrice(1000000);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(100000);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(1000000);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);



        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(null);
        incomingProductDetailRequest.setQuantity(null);
        incomingProductDetailRequest.setHasVariant(true);

        IncomingProductUpdateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductUpdateRequest.IncomingProductVariantDetail.builder()
                .id(incomingProductDetail.getId())
                .variantId(productVariant.getId())
                .pricePerUnit(50500)
                .quantity(100)
                .build();

        IncomingProductUpdateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest2 = IncomingProductUpdateRequest.IncomingProductVariantDetail.builder()
                .id(incomingProductDetail.getId())
                .variantId(productVariant.getId())
                .pricePerUnit(90000)
                .quantity(90)
                .build();


        incomingProductDetailRequest.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest, incomingProductVariantDetailRequest2));

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductVariantDetails size is not same. Please check the IncomingProductVariantDetails again.", response.getErrors());
        });
    }

    @Test
    void updateFailedIncomingProductVariantDetailIsNotFound() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setPricePerUnit(null);
        incomingProductDetail.setQuantity(null);
        incomingProductDetail.setTotalPrice(null);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantPrice(1000000);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(100000);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(1000000);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);



        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(null);
        incomingProductDetailRequest.setQuantity(null);
        incomingProductDetailRequest.setHasVariant(true);

        IncomingProductUpdateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductUpdateRequest.IncomingProductVariantDetail.builder()
                .id(999999)
                .variantId(productVariant.getId())
                .pricePerUnit(50500)
                .quantity(100)
                .build();


        incomingProductDetailRequest.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductVariantDetail is not found, please check IncomingProductVariantDetail id again.", response.getErrors());
        });
    }

    @Test
    void updateFailedProductVariantIsNotFoundInOneIncomingProductVariantDetail() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setPricePerUnit(null);
        incomingProductDetail.setQuantity(null);
        incomingProductDetail.setTotalPrice(null);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantPrice(1000000);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(100000);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(1000000);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);



        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(null);
        incomingProductDetailRequest.setQuantity(null);
        incomingProductDetailRequest.setHasVariant(true);

        IncomingProductUpdateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductUpdateRequest.IncomingProductVariantDetail.builder()
                .id(incomingProductVariantDetail.getId())
                .variantId(999999)
                .pricePerUnit(50500)
                .quantity(100)
                .build();


        incomingProductDetailRequest.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));
        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("ProductVariant id: " + 999999 +" is wrong, please check ProductVariant id again.", response.getErrors());
        });
    }

    @Test
    void updateSuccessIncomingProductDetailsWithoutIncomingProductVariantDetails() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setPricePerUnit(120500);
        incomingProductDetail.setQuantity(50);
        incomingProductDetail.setTotalPrice(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity());
        incomingProductDetail.setHasVariant(false);
        incomingProductDetail.setTotalVariantPrice(null);
        incomingProductDetail.setTotalVariantQuantity(null);
        incomingProductDetail.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(100000);
        incomingProductDetailRequest.setQuantity(100);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(incomingProduct.getId(), response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(supplier.getId(), response.getData().getSupplier().getId());
            assertEquals(supplier.getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getUpdateReason(), response.getData().getUpdateReason());
            assertNotNull(response.getData().getIncomingProductDetails());
            assertEquals(1,response.getData().getIncomingProductDetails().size());

            IncomingProductResponse.IncomingProductDetail incomingProductDetailResponse = response.getData().getIncomingProductDetails().getFirst();

            assertNotNull(incomingProductDetailResponse);
            assertEquals(incomingProductDetailRequest.getId(), incomingProductDetailResponse.getId());
            assertEquals(productWithoutVariant.getId(), incomingProductDetailResponse.getProduct().getId());
            assertEquals(productWithoutVariant.getName(), incomingProductDetailResponse.getProduct().getName());
            assertEquals(incomingProductDetailRequest.getPricePerUnit(), incomingProductDetailResponse.getPricePerUnit());
            assertEquals(incomingProductDetailRequest.getQuantity(), incomingProductDetailResponse.getQuantity());
            assertEquals(incomingProductDetailRequest.getPricePerUnit() * incomingProductDetailRequest.getQuantity(), incomingProductDetailResponse.getTotalPrice());
            assertEquals(incomingProductDetailRequest.getHasVariant(), incomingProductDetailResponse.getHasVariant());
            assertNull(incomingProductDetailResponse.getTotalVariantPrice());
            assertNull(incomingProductDetailResponse.getTotalVariantQuantity());
            assertNull(incomingProductDetailResponse.getIncomingProductVariantDetails());

            IncomingProduct incomingProductDB = incomingProductRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(incomingProductDB);
            assertEquals(incomingProductDB.getId(), response.getData().getId());
            assertEquals(incomingProductDB.getDateIn(), response.getData().getDateIn());
            assertEquals(incomingProductDB.getSupplier().getId(), response.getData().getSupplier().getId());
            assertEquals(incomingProductDB.getSupplier().getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(incomingProductDB.getUser().getUsername(), response.getData().getUsername());
            assertEquals(incomingProductDB.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(incomingProductDB.getNote(), response.getData().getNote());
            assertEquals(incomingProductDB.getUpdateReason(), response.getData().getUpdateReason());

            IncomingProductDetail incomingProductDetailDB = incomingProductDetailRepository.findById(incomingProductDetailResponse.getId()).orElse(null);
            assertNotNull(incomingProductDetailDB);
            assertEquals(incomingProductDetailDB.getId(), incomingProductDetailResponse.getId());
            assertEquals(incomingProductDetailDB.getProduct().getId(), incomingProductDetailResponse.getProduct().getId());
            assertEquals(incomingProductDetailDB.getProduct().getName(), incomingProductDetailResponse.getProduct().getName());
            assertEquals(incomingProductDetailDB.getPricePerUnit(), incomingProductDetailResponse.getPricePerUnit());
            assertEquals(incomingProductDetailDB.getQuantity(), incomingProductDetailResponse.getQuantity());
            assertEquals(incomingProductDetailDB.getTotalPrice(), incomingProductDetailResponse.getTotalPrice());
            assertEquals(incomingProductDetailDB.getHasVariant(), incomingProductDetailResponse.getHasVariant());
            assertNull(incomingProductDetailDB.getTotalVariantPrice());
            assertNull(incomingProductDetailDB.getTotalVariantQuantity());
            assertEquals(0, incomingProductDetailDB.getIncomingProductVariantDetails().size());

            // check updated quantity of product
            int quantityChange = incomingProductDetailRequest.getQuantity() - incomingProductDetail.getQuantity();

            Product product = productRepository.findById(productWithoutVariant.getId()).orElse(null);
            assertNotNull(product);
            assertNotEquals(productWithoutVariant.getStock(), product.getStock());
            assertEquals(productWithoutVariant.getStock() + quantityChange, product.getStock());

            log.info(String.valueOf(quantityChange));
            log.info(String.valueOf(productWithoutVariant.getStock()));
            log.info(String.valueOf(product.getStock()));
        });
    }

    @Test
    void updateSuccessMultipleIncomingProductDetailsWithoutIncomingProductVariantDetails() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product productWithoutVariant2 = new Product();
        productWithoutVariant2.setName("Fishing Rood V3");
        productWithoutVariant2.setPrice(30200);
        productWithoutVariant2.setStock(10);
        productWithoutVariant2.setDescription("Description about rood");
        productWithoutVariant2.setHasVariant(false);
        productWithoutVariant2.setCategory(category);
        productRepository.save(productWithoutVariant2);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(2);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setPricePerUnit(120500);
        incomingProductDetail.setQuantity(50);
        incomingProductDetail.setTotalPrice(incomingProductDetail.getPricePerUnit() * incomingProductDetail.getQuantity());
        incomingProductDetail.setHasVariant(false);
        incomingProductDetail.setTotalVariantPrice(null);
        incomingProductDetail.setTotalVariantQuantity(null);
        incomingProductDetail.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductDetail incomingProductDetail2 = new IncomingProductDetail();
        incomingProductDetail2.setIncomingProduct(incomingProduct);
        incomingProductDetail2.setProduct(productWithoutVariant2);
        incomingProductDetail2.setPricePerUnit(78000);
        incomingProductDetail2.setQuantity(20);
        incomingProductDetail2.setTotalPrice(incomingProductDetail2.getPricePerUnit() * incomingProductDetail2.getQuantity());
        incomingProductDetail2.setHasVariant(false);
        incomingProductDetail2.setTotalVariantPrice(null);
        incomingProductDetail2.setTotalVariantQuantity(null);
        incomingProductDetail2.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail2);

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(2);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(100000);
        incomingProductDetailRequest.setQuantity(100);
        incomingProductDetailRequest.setHasVariant(false);
        incomingProductDetailRequest.setIncomingProductVariantDetails(null);

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest2 = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest2.setId(incomingProductDetail2.getId());
        incomingProductDetailRequest2.setProductId(productWithoutVariant2.getId());
        incomingProductDetailRequest2.setPricePerUnit(100000);
        incomingProductDetailRequest2.setQuantity(100);
        incomingProductDetailRequest2.setHasVariant(false);
        incomingProductDetailRequest2.setIncomingProductVariantDetails(null);

        request.setIncomingProductDetails(List.of(incomingProductDetailRequest ,incomingProductDetailRequest2));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(incomingProduct.getId(), response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(supplier.getId(), response.getData().getSupplier().getId());
            assertEquals(supplier.getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getUpdateReason(), response.getData().getUpdateReason());
            assertNotNull(response.getData().getIncomingProductDetails());
            assertEquals(2, response.getData().getIncomingProductDetails().size());

            IncomingProduct incomingProductDB = incomingProductRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(incomingProductDB);
            assertEquals(incomingProductDB.getId(), response.getData().getId());
            assertEquals(incomingProductDB.getDateIn(), response.getData().getDateIn());
            assertEquals(incomingProductDB.getSupplier().getId(), response.getData().getSupplier().getId());
            assertEquals(incomingProductDB.getSupplier().getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(incomingProductDB.getUser().getUsername(), response.getData().getUsername());
            assertEquals(incomingProductDB.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(incomingProductDB.getNote(), response.getData().getNote());
            assertEquals(incomingProductDB.getUpdateReason(), response.getData().getUpdateReason());


            List<IncomingProductResponse.IncomingProductDetail> incomingProductDetailsResponse = response.getData().getIncomingProductDetails();
            assertNotNull(incomingProductDetailsResponse);

            List<Product> productList = List.of(productWithoutVariant, productWithoutVariant2);
            List<IncomingProductDetail> productDetailsBeforeUpdated = List.of(incomingProductDetail, incomingProductDetail2);

            incomingProductDetailsResponse.forEach(incomingProductDetailResponse -> {
                assertNotNull(incomingProductDetailResponse);

                IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailUpdateRequest = request.getIncomingProductDetails()
                        .stream()
                        .filter(iPD -> iPD.getId().equals(incomingProductDetailResponse.getId()))
                        .findFirst()
                        .orElse(null);

                Product product = productList.stream()
                        .filter(p -> p.getId().equals(incomingProductDetailUpdateRequest.getProductId()))
                        .findFirst()
                        .orElse(null);

                assertEquals(incomingProductDetailUpdateRequest.getId(), incomingProductDetailResponse.getId());
                assertEquals(incomingProductDetailUpdateRequest.getId(), incomingProductDetailResponse.getId());

                assertEquals(product.getId(), incomingProductDetailResponse.getProduct().getId());
                assertEquals(product.getName(), incomingProductDetailResponse.getProduct().getName());
                assertEquals(incomingProductDetailUpdateRequest.getPricePerUnit(), incomingProductDetailResponse.getPricePerUnit());

                assertEquals(incomingProductDetailUpdateRequest.getQuantity(), incomingProductDetailResponse.getQuantity());
                assertEquals(incomingProductDetailUpdateRequest.getPricePerUnit() * incomingProductDetailRequest2.getQuantity(), incomingProductDetailResponse.getTotalPrice());
                assertEquals(incomingProductDetailUpdateRequest.getHasVariant(), incomingProductDetailResponse.getHasVariant());
                assertNull(incomingProductDetailResponse.getTotalVariantPrice());
                assertNull(incomingProductDetailResponse.getTotalVariantQuantity());
                assertNull(incomingProductDetailResponse.getIncomingProductVariantDetails());

                IncomingProductDetail incomingProductDetailDB = incomingProductDetailRepository.findById(incomingProductDetailResponse.getId()).orElse(null);
                assertNotNull(incomingProductDetailDB);
                assertEquals(incomingProductDetailDB.getId(), incomingProductDetailResponse.getId());
                assertEquals(incomingProductDetailDB.getProduct().getId(), incomingProductDetailResponse.getProduct().getId());
                assertEquals(incomingProductDetailDB.getProduct().getName(), incomingProductDetailResponse.getProduct().getName());
                assertEquals(incomingProductDetailDB.getPricePerUnit(), incomingProductDetailResponse.getPricePerUnit());
                assertEquals(incomingProductDetailDB.getQuantity(), incomingProductDetailResponse.getQuantity());
                assertEquals(incomingProductDetailDB.getTotalPrice(), incomingProductDetailResponse.getTotalPrice());
                assertEquals(incomingProductDetailDB.getHasVariant(), incomingProductDetailResponse.getHasVariant());
                assertNull(incomingProductDetailDB.getTotalVariantPrice());
                assertNull(incomingProductDetailDB.getTotalVariantQuantity());
                assertEquals(0, incomingProductDetailDB.getIncomingProductVariantDetails().size());

                // check updated quantity of product
                IncomingProductDetail incomingProductDetailBeforeUpdated = productDetailsBeforeUpdated.stream()
                        .filter(iPD -> iPD.getId().equals(incomingProductDetailResponse.getId()))
                        .findFirst()
                        .orElse(null);


                int quantityChange = incomingProductDetailUpdateRequest.getQuantity() - incomingProductDetailBeforeUpdated.getQuantity();

                Product productDB = productRepository.findById(product.getId()).orElse(null);
                assertNotNull(productDB);
                assertNotEquals(product.getStock(), productDB.getStock());
                assertEquals(product.getStock() + quantityChange, productDB.getStock());
            });
        });
    }

    @Test
    void updateSuccessIncomingProductDetailsWithIncomingProductVariantDetails() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(1);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setPricePerUnit(null);
        incomingProductDetail.setQuantity(null);
        incomingProductDetail.setTotalPrice(null);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantPrice(100000 * 70);
        incomingProductDetail.setTotalVariantQuantity(70);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(100000);
        incomingProductVariantDetail.setQuantity(70);
        incomingProductVariantDetail.setTotalPrice(100000 * 70);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);

        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(1);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(null);
        incomingProductDetailRequest.setQuantity(null);
        incomingProductDetailRequest.setHasVariant(true);

        IncomingProductUpdateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductUpdateRequest.IncomingProductVariantDetail.builder()
                .id(incomingProductVariantDetail.getId())
                .variantId(productVariant.getId())
                .pricePerUnit(50500)
                .quantity(50)
                .build();

        incomingProductDetailRequest.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));
        request.setIncomingProductDetails(List.of(incomingProductDetailRequest));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(incomingProduct.getId(), response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(supplier.getId(), response.getData().getSupplier().getId());
            assertEquals(supplier.getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getUpdateReason(), response.getData().getUpdateReason());
            assertNotNull(response.getData().getIncomingProductDetails());
            assertEquals(1,response.getData().getIncomingProductDetails().size());

            IncomingProduct incomingProductDB = incomingProductRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(incomingProductDB);
            assertEquals(incomingProductDB.getId(), response.getData().getId());
            assertEquals(incomingProductDB.getDateIn(), response.getData().getDateIn());
            assertEquals(incomingProductDB.getSupplier().getId(), response.getData().getSupplier().getId());
            assertEquals(incomingProductDB.getSupplier().getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(incomingProductDB.getUser().getUsername(), response.getData().getUsername());
            assertEquals(incomingProductDB.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(incomingProductDB.getNote(), response.getData().getNote());
            assertEquals(incomingProductDB.getUpdateReason(), response.getData().getUpdateReason());


            // assert incomingProductDetail
            IncomingProductResponse.IncomingProductDetail incomingProductDetailResponse = response.getData().getIncomingProductDetails().getFirst();

            assertNotNull(incomingProductDetailResponse);
            assertEquals(incomingProductDetailRequest.getId(), incomingProductDetailResponse.getId());
            assertEquals(productWithVariant.getId(), incomingProductDetailResponse.getProduct().getId());
            assertEquals(productWithVariant.getName(), incomingProductDetailResponse.getProduct().getName());
            assertNull(incomingProductDetailResponse.getPricePerUnit());
            assertNull(incomingProductDetailResponse.getQuantity());
            assertNull(incomingProductDetailResponse.getTotalPrice());
            assertEquals(true, incomingProductDetailResponse.getHasVariant());

            int totalVariantQuantity = incomingProductDetailRequest.getIncomingProductVariantDetails()
                    .stream().mapToInt(iPVD -> {
                        log.info(String.valueOf(iPVD.getQuantity()));
                        return iPVD.getQuantity();
                    }).sum();

            int totalVariantPrice = incomingProductDetailRequest.getIncomingProductVariantDetails()
                    .stream().mapToInt(iPVD -> iPVD.getPricePerUnit() * iPVD.getQuantity()).sum();

            assertEquals(totalVariantQuantity, incomingProductDetailResponse.getTotalVariantQuantity());
            assertEquals((int) totalVariantPrice ,incomingProductDetailResponse.getTotalVariantPrice());
            assertNotNull(incomingProductDetailResponse.getIncomingProductVariantDetails());
            assertEquals(1, incomingProductDetailResponse.getIncomingProductVariantDetails().size());

            IncomingProductDetail incomingProductDetailDB = incomingProductDetailRepository.findById(incomingProductDetailResponse.getId()).orElse(null);
            assertNotNull(incomingProductDetailDB);
            assertEquals(incomingProductDetailDB.getId(), incomingProductDetailResponse.getId());
            assertEquals(incomingProductDetailDB.getProduct().getId(), incomingProductDetailResponse.getProduct().getId());
            assertEquals(incomingProductDetailDB.getProduct().getName(), incomingProductDetailResponse.getProduct().getName());
            assertEquals(incomingProductDetailDB.getPricePerUnit(), incomingProductDetailResponse.getPricePerUnit());
            assertEquals(incomingProductDetailDB.getQuantity(), incomingProductDetailResponse.getQuantity());
            assertEquals(incomingProductDetailDB.getTotalPrice(), incomingProductDetailResponse.getTotalPrice());
            assertEquals(incomingProductDetailDB.getHasVariant(), incomingProductDetailResponse.getHasVariant());
            assertEquals(incomingProductDetailDB.getTotalVariantPrice(), incomingProductDetailResponse.getTotalVariantPrice());
            assertEquals(incomingProductDetailDB.getTotalVariantQuantity(), incomingProductDetailResponse.getTotalVariantQuantity());
            assertEquals(1, incomingProductDetailDB.getIncomingProductVariantDetails().size());

            // assert incomingProductVariantDetail
            IncomingProductResponse.IncomingProductVariantDetail incomingProductVariantDetailResponse = incomingProductDetailResponse.getIncomingProductVariantDetails()
                    .stream()
                    .findFirst()
                    .orElse(null);

            assertNotNull(incomingProductVariantDetailResponse);
            assertEquals(incomingProductVariantDetailRequest.getId(), incomingProductVariantDetailResponse.getId());
            assertEquals(productVariant.getId(), incomingProductVariantDetailResponse.getVariant().getId());
            assertEquals(productVariant.getSku(), incomingProductVariantDetailResponse.getVariant().getSku());
            assertEquals(incomingProductVariantDetailRequest.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
            assertEquals(incomingProductVariantDetailRequest.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
            assertEquals(
                    incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity(),
                    incomingProductVariantDetailResponse.getTotalPrice()
            );

            IncomingProductVariantDetail incomingProductVariantDetailDB = incomingProductVariantDetailRepository.findById(incomingProductVariantDetailResponse.getId()).orElse(null);
            assertNotNull(incomingProductVariantDetailDB);
            assertEquals(incomingProductVariantDetailDB.getId(), incomingProductVariantDetailResponse.getId());
            assertEquals(incomingProductVariantDetailDB.getProductVariant().getId(), incomingProductVariantDetailResponse.getVariant().getId());
            assertEquals(incomingProductVariantDetailDB.getProductVariant().getSku(), incomingProductVariantDetailResponse.getVariant().getSku());
            assertEquals(incomingProductVariantDetailDB.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
            assertEquals(incomingProductVariantDetailDB.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
            assertEquals(incomingProductVariantDetailDB.getTotalPrice(), incomingProductVariantDetailResponse.getTotalPrice());

            // check productVariant quantity change
            int quantityChange = incomingProductVariantDetailRequest.getQuantity() - incomingProductVariantDetail.getQuantity();

            ProductVariant updatedProductVariant = productVariantRepository.findById(productVariant.getId()).orElse(null);
            assertNotNull(updatedProductVariant);
            assertNotEquals(productVariant.getStock(), updatedProductVariant.getStock());
            assertEquals(productVariant.getStock() + quantityChange, updatedProductVariant.getStock());

            log.info(String.valueOf(quantityChange));
            log.info(String.valueOf(productVariant.getStock()));
            log.info(String.valueOf(updatedProductVariant.getStock()));
        });
    }

    @Test
    void updateSuccessIncomingProductDetailsWithoutAndWithIncomingProductVariantDetails() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-05-01"));
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProduct.setTotalProducts(2);
        incomingProduct.setNote("Product is well condition");
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setPricePerUnit(null);
        incomingProductDetail.setQuantity(null);
        incomingProductDetail.setTotalPrice(null);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantPrice(100000 * 70);
        incomingProductDetail.setTotalVariantQuantity(70);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(100000);
        incomingProductVariantDetail.setQuantity(70);
        incomingProductVariantDetail.setTotalPrice(100000 * 70);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);


        IncomingProductDetail incomingProductDetail2 = new IncomingProductDetail();
        incomingProductDetail2.setIncomingProduct(incomingProduct);
        incomingProductDetail2.setProduct(productWithoutVariant);
        incomingProductDetail2.setPricePerUnit(120500);
        incomingProductDetail2.setQuantity(50);
        incomingProductDetail2.setTotalPrice(incomingProductDetail2.getPricePerUnit() * incomingProductDetail2.getQuantity());
        incomingProductDetail2.setHasVariant(false);
        incomingProductDetail2.setTotalVariantPrice(null);
        incomingProductDetail2.setTotalVariantQuantity(null);
        incomingProductDetail2.setIncomingProductVariantDetails(null);
        incomingProductDetailRepository.save(incomingProductDetail2);


        // request
        IncomingProductUpdateRequest request = new IncomingProductUpdateRequest();
        request.setDateIn(LocalDate.parse("2025-06-25"));
        request.setSupplierId(supplierId);
        request.setTotalProducts(2);
        request.setNote("Product is not complete");
        request.setUpdateReason("Add uncompleted product");

        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest.setId(incomingProductDetail.getId());
        incomingProductDetailRequest.setProductId(productWithVariant.getId());
        incomingProductDetailRequest.setPricePerUnit(null);
        incomingProductDetailRequest.setQuantity(null);
        incomingProductDetailRequest.setHasVariant(true);

        IncomingProductUpdateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductUpdateRequest.IncomingProductVariantDetail.builder()
                .id(incomingProductVariantDetail.getId())
                .variantId(productVariant.getId())
                .pricePerUnit(50500)
                .quantity(50)
                .build();

        incomingProductDetailRequest.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));


        IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequest2 = new IncomingProductUpdateRequest.IncomingProductDetail();
        incomingProductDetailRequest2.setId(incomingProductDetail2.getId());
        incomingProductDetailRequest2.setProductId(productWithoutVariant.getId());
        incomingProductDetailRequest2.setPricePerUnit(70700);
        incomingProductDetailRequest2.setQuantity(45);
        incomingProductDetailRequest2.setHasVariant(false);
        incomingProductDetailRequest2.setIncomingProductVariantDetails(null);


        request.setIncomingProductDetails(List.of(incomingProductDetailRequest, incomingProductDetailRequest2));

        mockMvc.perform(
                put("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<IncomingProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(incomingProduct.getId(), response.getData().getId());
            assertEquals(request.getDateIn(), response.getData().getDateIn());
            assertEquals(supplier.getId(), response.getData().getSupplier().getId());
            assertEquals(supplier.getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(user.getUsername(), response.getData().getUsername());
            assertEquals(request.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(request.getNote(), response.getData().getNote());
            assertEquals(request.getUpdateReason(), response.getData().getUpdateReason());
            assertNotNull(response.getData().getIncomingProductDetails());
            assertEquals(2,response.getData().getIncomingProductDetails().size());

            IncomingProduct incomingProductDB = incomingProductRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(incomingProductDB);
            assertEquals(incomingProductDB.getId(), response.getData().getId());
            assertEquals(incomingProductDB.getDateIn(), response.getData().getDateIn());
            assertEquals(incomingProductDB.getSupplier().getId(), response.getData().getSupplier().getId());
            assertEquals(incomingProductDB.getSupplier().getSupplierName(), response.getData().getSupplier().getName());
            assertEquals(incomingProductDB.getUser().getUsername(), response.getData().getUsername());
            assertEquals(incomingProductDB.getTotalProducts(), response.getData().getTotalProducts());
            assertEquals(incomingProductDB.getNote(), response.getData().getNote());
            assertEquals(incomingProductDB.getUpdateReason(), response.getData().getUpdateReason());


            // assert incomingProductDetail
            response.getData().getIncomingProductDetails().forEach(incomingProductDetailResponse -> {

                IncomingProductUpdateRequest.IncomingProductDetail incomingProductDetailRequestCurrent = request.getIncomingProductDetails()
                        .stream()
                        .filter(iPD -> iPD.getId().equals(incomingProductDetailResponse.getId()))
                        .findFirst()
                        .orElse(null);

                // check incomingProductDB
                IncomingProductDetail incomingProductDetailDB = incomingProductDetailRepository.findById(incomingProductDetailResponse.getId()).orElse(null);
                assertNotNull(incomingProductDetailDB);
                assertEquals(incomingProductDetailDB.getId(), incomingProductDetailResponse.getId());
                assertEquals(incomingProductDetailDB.getProduct().getId(), incomingProductDetailResponse.getProduct().getId());
                assertEquals(incomingProductDetailDB.getProduct().getName(), incomingProductDetailResponse.getProduct().getName());
                assertEquals(incomingProductDetailDB.getPricePerUnit(), incomingProductDetailResponse.getPricePerUnit());
                assertEquals(incomingProductDetailDB.getQuantity(), incomingProductDetailResponse.getQuantity());
                assertEquals(incomingProductDetailDB.getTotalPrice(), incomingProductDetailResponse.getTotalPrice());
                assertEquals(incomingProductDetailDB.getHasVariant(), incomingProductDetailResponse.getHasVariant());
                assertEquals(incomingProductDetailDB.getTotalVariantPrice(), incomingProductDetailResponse.getTotalVariantPrice());
                assertEquals(incomingProductDetailDB.getTotalVariantQuantity(), incomingProductDetailResponse.getTotalVariantQuantity());


                if (!incomingProductDetailResponse.getHasVariant()) {
                    assertEquals(incomingProductDetailRequestCurrent.getId(), incomingProductDetailResponse.getId());
                    assertEquals(productWithoutVariant.getId(), incomingProductDetailResponse.getProduct().getId());
                    assertEquals(productWithoutVariant.getName(), incomingProductDetailResponse.getProduct().getName());
                    assertEquals(incomingProductDetailRequestCurrent.getPricePerUnit(), incomingProductDetailResponse.getPricePerUnit());
                    assertEquals(incomingProductDetailRequestCurrent.getQuantity(), incomingProductDetailResponse.getQuantity());
                    assertEquals(incomingProductDetailRequestCurrent.getPricePerUnit() * incomingProductDetailRequestCurrent.getQuantity(), incomingProductDetailResponse.getTotalPrice());
                    assertEquals(incomingProductDetailRequestCurrent.getHasVariant(), incomingProductDetailResponse.getHasVariant());
                    assertNull(incomingProductDetailResponse.getTotalVariantPrice());
                    assertNull(incomingProductDetailResponse.getTotalVariantQuantity());
                    assertNull(incomingProductDetailResponse.getIncomingProductVariantDetails());

                    // check incomingProductDB
                    assertEquals(0, incomingProductDetailDB.getIncomingProductVariantDetails().size());

                    // check updated quantity of product
                    int quantityChange = incomingProductDetailRequestCurrent.getQuantity() - incomingProductDetail2.getQuantity();

                    Product product = productRepository.findById(productWithoutVariant.getId()).orElse(null);
                    assertNotNull(product);
                    assertNotEquals(productWithoutVariant.getStock(), product.getStock());
                    assertEquals(productWithoutVariant.getStock() + quantityChange, product.getStock());

                    log.info(String.valueOf(quantityChange));
                    log.info(String.valueOf(productWithoutVariant.getStock()));
                    log.info(String.valueOf(product.getStock()));
                } else {

                    assertNotNull(incomingProductDetailResponse);
                    assertEquals(incomingProductDetailRequestCurrent.getId(), incomingProductDetailResponse.getId());
                    assertEquals(productWithVariant.getId(), incomingProductDetailResponse.getProduct().getId());
                    assertEquals(productWithVariant.getName(), incomingProductDetailResponse.getProduct().getName());
                    assertNull(incomingProductDetailResponse.getPricePerUnit());
                    assertNull(incomingProductDetailResponse.getQuantity());
                    assertNull(incomingProductDetailResponse.getTotalPrice());
                    assertEquals(true, incomingProductDetailResponse.getHasVariant());

                    int totalVariantQuantity = incomingProductDetailRequestCurrent.getIncomingProductVariantDetails()
                            .stream().mapToInt(iPVD -> {
                                log.info(String.valueOf(iPVD.getQuantity()));
                                return iPVD.getQuantity();
                            }).sum();

                    int totalVariantPrice = incomingProductDetailRequestCurrent.getIncomingProductVariantDetails()
                            .stream().mapToInt(iPVD -> iPVD.getPricePerUnit() * iPVD.getQuantity()).sum();

                    assertEquals(totalVariantQuantity, incomingProductDetailResponse.getTotalVariantQuantity());
                    assertEquals(totalVariantPrice ,incomingProductDetailResponse.getTotalVariantPrice());
                    assertNotNull(incomingProductDetailResponse.getIncomingProductVariantDetails());
                    assertEquals(1, incomingProductDetailResponse.getIncomingProductVariantDetails().size());

                    // check incomingProductDB
                    assertEquals(1, incomingProductDetailDB.getIncomingProductVariantDetails().size());

                    // assert incomingProductVariantDetail response
                    IncomingProductResponse.IncomingProductVariantDetail incomingProductVariantDetailResponse = incomingProductDetailResponse.getIncomingProductVariantDetails()
                            .stream()
                            .findFirst()
                            .orElse(null);

                    assertNotNull(incomingProductVariantDetailResponse);
                    assertEquals(incomingProductVariantDetailRequest.getId(), incomingProductVariantDetailResponse.getId());
                    assertEquals(productVariant.getId(), incomingProductVariantDetailResponse.getVariant().getId());
                    assertEquals(productVariant.getSku(), incomingProductVariantDetailResponse.getVariant().getSku());
                    assertEquals(incomingProductVariantDetailRequest.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
                    assertEquals(incomingProductVariantDetailRequest.getQuantity(), incomingProductVariantDetailResponse.getQuantity());

                    assertEquals(
                            incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity(),
                            incomingProductVariantDetailResponse.getTotalPrice()
                    );

                    IncomingProductVariantDetail incomingProductVariantDetailDB = incomingProductVariantDetailRepository.findById(incomingProductVariantDetailResponse.getId()).orElse(null);
                    assertNotNull(incomingProductVariantDetailDB);
                    assertEquals(incomingProductVariantDetailDB.getId(), incomingProductVariantDetailResponse.getId());
                    assertEquals(incomingProductVariantDetailDB.getProductVariant().getId(), incomingProductVariantDetailResponse.getVariant().getId());
                    assertEquals(incomingProductVariantDetailDB.getProductVariant().getSku(), incomingProductVariantDetailResponse.getVariant().getSku());
                    assertEquals(incomingProductVariantDetailDB.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
                    assertEquals(incomingProductVariantDetailDB.getQuantity(), incomingProductVariantDetailResponse.getQuantity());
                    assertEquals(incomingProductVariantDetailDB.getTotalPrice(), incomingProductVariantDetailResponse.getTotalPrice());

                    // check productVariant quantity change
                    int quantityChange = incomingProductVariantDetailRequest.getQuantity() - incomingProductVariantDetail.getQuantity();
                    ProductVariant updatedProductVariant = productVariantRepository.findById(incomingProductVariantDetailResponse.getVariant().getId()).orElse(null);

                    assertNotNull(updatedProductVariant);
                    assertNotEquals(productVariant.getStock(), updatedProductVariant.getStock());
                    assertEquals(productVariant.getStock() + quantityChange, updatedProductVariant.getStock());

                    log.info(String.valueOf(quantityChange));
                    log.info(String.valueOf(productVariant.getStock()));
                    log.info(String.valueOf(updatedProductVariant.getStock()));
                }
            });

        });
    }


    @Test
    void createIncomingProductDetailsFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                post("/api/incoming-products/123/incoming-product-details")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void createIncomingProductDetailsFailedIncomingProductIdIsNotNumber() throws Exception {
        mockMvc.perform(
                post("/api/incoming-products/abc/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncomingProductDetailCreateRequest()))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductId type data is wrong.", response.getErrors());
        });
    }

    @Test
    void createIncomingProductDetailsFailedValidation() throws Exception {
        mockMvc.perform(
                post("/api/incoming-products/123/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncomingProductDetailCreateRequest()))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createIncomingProductDetailsFailedIncomingProductNotFound() throws Exception {

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setProductId(productWithoutVariant.getId());
        request.setPricePerUnit(100);
        request.setQuantity(10);
        request.setHasVariant(false);
        request.setIncomingProductVariantDetails(null);


        mockMvc.perform(
                post("/api/incoming-products/99999/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProduct is not found. Please check IncomingProduct id again.", response.getErrors());
        });
    }


    @Test
    void createIncomingProductDetailsFailedProductNotFound() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setProductId(99999);
        request.setPricePerUnit(100);
        request.setQuantity(10);
        request.setHasVariant(false);
        request.setIncomingProductVariantDetails(null);

        mockMvc.perform(
                post("/api/incoming-products/" + incomingProduct.getId() + "/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Product is not found. Please check Product id again.", response.getErrors());
        });
    }

    @Test
    void createIncomingProductDetailsFailedHasVariantIsConflict() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setProductId(productWithoutVariant.getId());
        request.setHasVariant(true);


        IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductDetailCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant.getId())
                .pricePerUnit(109)
                .quantity(10)
                .build();

        request.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));

        mockMvc.perform(
                post("/api/incoming-products/" + incomingProduct.getId() + "/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Product id: " + productWithoutVariant.getId() + " hasVariant is false, please check hasVariant again.", response.getErrors());
        });
    }


    @Test
    void createIncomingProductDetailsFailedProductVariantDuplicate() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setProductId(productWithVariant.getId());
        request.setHasVariant(true);


        IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductDetailCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(123)
                .pricePerUnit(109)
                .quantity(10)
                .build();

        IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest2 = IncomingProductDetailCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(123)
                .pricePerUnit(109)
                .quantity(10)
                .build();

        request.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest, incomingProductVariantDetailRequest2));

        mockMvc.perform(
                post("/api/incoming-products/" + incomingProduct.getId() + "/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("ProductVariants id must not duplicate in a single ProductVariantDetails.", response.getErrors());
        });
    }

    @Test
    void createIncomingProductDetailsFailedProductVariantIsNotFound() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setProductId(productWithVariant.getId());
        request.setHasVariant(true);


        IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductDetailCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(123)
                .pricePerUnit(109)
                .quantity(10)
                .build();

        request.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));

        mockMvc.perform(
                post("/api/incoming-products/" + incomingProduct.getId() + "/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("ProductVariant with id " + incomingProductVariantDetailRequest.getVariantId() + " is not found. please check ProductVariant id again.", response.getErrors());
        });
    }

    @Test
    void createIncomingProductDetailsFailedProductVariantIsNotBelongsToProduct() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setCategory(category);
        product.setName("Product test 22");
        product.setDescription("product description");
        product.setHasVariant(true);
        productRepository.save(product);

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setProductId(product.getId());
        request.setHasVariant(true);


        IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductDetailCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant.getId())
                .pricePerUnit(109)
                .quantity(10)
                .build();

        request.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));

        mockMvc.perform(
                post("/api/incoming-products/" + incomingProduct.getId() + "/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isConflict()
        ).andExpect(result -> {
            WebResponse<IncomingProductGetListResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("ProductVariant with id " + productVariant.getId() + " is not product variant for Product with id " + product.getId() + ".", response.getErrors());
        });
    }

    @Test
    void createIncomingProductDetailsSuccessWithoutVariant() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setPricePerUnit(60000);
        request.setQuantity(10);
        request.setProductId(productWithoutVariant.getId());
        request.setHasVariant(false);
        request.setIncomingProductVariantDetails(null);

        mockMvc.perform(
                post("/api/incoming-products/" + incomingProduct.getId() + "/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andExpect(result -> {
            WebResponse<IncomingProductDetailResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(productWithoutVariant.getId(), response.getData().getProduct().getId());
            assertEquals(productWithoutVariant.getName(), response.getData().getProduct().getName());
            assertEquals(request.getPricePerUnit(), response.getData().getPricePerUnit());
            assertEquals(request.getQuantity(), response.getData().getQuantity());
            assertEquals(request.getPricePerUnit() * request.getQuantity(), response.getData().getTotalPrice());
            assertEquals(false, response.getData().getHasVariant());
            assertNull(response.getData().getTotalVariantPrice());
            assertNull(response.getData().getTotalVariantQuantity());
            assertNull(response.getData().getIncomingProductVariantDetails());

            // check product quantity updated

            Product productUpdated = productRepository.findById(productWithoutVariant.getId()).orElse(null);
            assertNotNull(productUpdated);

            assertEquals(productWithoutVariant.getStock() + response.getData().getQuantity(), productUpdated.getStock());

            log.info(String.valueOf(response.getData().getQuantity()));
            log.info(String.valueOf(productWithoutVariant.getStock()));
            log.info(String.valueOf(productUpdated.getStock()));
        });
    }

    @Test
    void createIncomingProductDetailsSuccessWithVariant() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setProductId(productWithVariant.getId());
        request.setHasVariant(true);


        IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductDetailCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant.getId())
                .pricePerUnit(109)
                .quantity(10)
                .build();

        request.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest));

        mockMvc.perform(
                post("/api/incoming-products/" + incomingProduct.getId() + "/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andExpect(result -> {
            WebResponse<IncomingProductDetailResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(productWithVariant.getId(), response.getData().getProduct().getId());
            assertEquals(productWithVariant.getName(), response.getData().getProduct().getName());
            assertNull(response.getData().getPricePerUnit());
            assertNull(response.getData().getQuantity());
            assertNull(response.getData().getTotalPrice());
            assertEquals(true, response.getData().getHasVariant());
            assertEquals(incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity() ,response.getData().getTotalVariantPrice());
            assertEquals(incomingProductVariantDetailRequest.getQuantity(), response.getData().getTotalVariantQuantity());
            assertNotNull(response.getData().getIncomingProductVariantDetails());
            assertEquals(1, response.getData().getIncomingProductVariantDetails().size());

            // check IncomingProductVariant
            IncomingProductDetailResponse.IncomingProductVariantDetail incomingProductVariantDetailResponse = response.getData().getIncomingProductVariantDetails().getFirst();
            assertNotNull(incomingProductVariantDetailResponse.getId());
            assertEquals(productVariant.getId(), incomingProductVariantDetailResponse.getVariant().getId());
            assertEquals(productVariant.getSku(), incomingProductVariantDetailResponse.getVariant().getSku());
            assertEquals(incomingProductVariantDetailRequest.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
            assertEquals(incomingProductVariantDetailRequest.getQuantity(), incomingProductVariantDetailResponse.getQuantity());


            ProductVariant productVariantUpdated = productVariantRepository.findById(productVariant.getId()).orElse(null);
            assertNotNull(productVariantUpdated);

            assertEquals(productVariant.getStock() + incomingProductVariantDetailRequest.getQuantity(), productVariantUpdated.getStock());

            log.info(String.valueOf(incomingProductVariantDetailResponse.getQuantity()));
            log.info(String.valueOf(productVariant.getStock()));
            log.info(String.valueOf(productVariantUpdated.getStock()));
        });
    }

    @Test
    void createIncomingProductDetailsSuccessWithMultipleVariant() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);

        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetailCreateRequest request = new IncomingProductDetailCreateRequest();
        request.setProductId(productWithVariant.getId());
        request.setHasVariant(true);


        IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest = IncomingProductDetailCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant.getId())
                .pricePerUnit(109)
                .quantity(10)
                .build();

        ProductVariant productVariant2 = new ProductVariant();
        productVariant2.setProduct(productWithVariant);
        productVariant2.setSku("product-test-black2");
        productVariant2.setPrice(101000);
        productVariant2.setStock(60);
        productVariantRepository.save(productVariant2);

        IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailRequest2 = IncomingProductDetailCreateRequest.IncomingProductVariantDetail.builder()
                .variantId(productVariant2.getId())
                .pricePerUnit(109)
                .quantity(20)
                .build();

        request.setIncomingProductVariantDetails(List.of(incomingProductVariantDetailRequest, incomingProductVariantDetailRequest2));

        mockMvc.perform(
                post("/api/incoming-products/" + incomingProduct.getId() + "/incoming-product-details")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andExpect(result -> {
            WebResponse<IncomingProductDetailResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(productWithVariant.getId(), response.getData().getProduct().getId());
            assertEquals(productWithVariant.getName(), response.getData().getProduct().getName());
            assertNull(response.getData().getPricePerUnit());
            assertNull(response.getData().getQuantity());
            assertNull(response.getData().getTotalPrice());
            assertEquals(true, response.getData().getHasVariant());
            assertEquals((incomingProductVariantDetailRequest.getPricePerUnit() * incomingProductVariantDetailRequest.getQuantity()) +
                            (incomingProductVariantDetailRequest2.getPricePerUnit() * incomingProductVariantDetailRequest2.getQuantity())
                    ,response.getData().getTotalVariantPrice());
            assertEquals(incomingProductVariantDetailRequest.getQuantity() + incomingProductVariantDetailRequest2.getQuantity(), response.getData().getTotalVariantQuantity());
            assertNotNull(response.getData().getIncomingProductVariantDetails());
            assertEquals(2, response.getData().getIncomingProductVariantDetails().size());

            // check IncomingProductVariant
            List<ProductVariant> productVariantList = List.of(productVariant, productVariant2);

            for (int i = 0; i < response.getData().getIncomingProductVariantDetails().size(); i++) {
                IncomingProductDetailCreateRequest.IncomingProductVariantDetail incomingProductVariantDetailCurrentRequest = request.getIncomingProductVariantDetails()
                        .get(i);

                IncomingProductDetailResponse.IncomingProductVariantDetail incomingProductVariantDetailResponse = response.getData().getIncomingProductVariantDetails().get(i);
                ProductVariant productVariant = productVariantList.get(i);

                assertNotNull(incomingProductVariantDetailResponse.getId());
                assertEquals(productVariant.getId(), incomingProductVariantDetailResponse.getVariant().getId());
                assertEquals(productVariant.getSku(), incomingProductVariantDetailResponse.getVariant().getSku());
                assertEquals(incomingProductVariantDetailCurrentRequest.getPricePerUnit(), incomingProductVariantDetailResponse.getPricePerUnit());
                assertEquals(incomingProductVariantDetailCurrentRequest.getQuantity(), incomingProductVariantDetailResponse.getQuantity());

                ProductVariant productVariantUpdated = productVariantRepository.findById(productVariant.getId()).orElse(null);
                assertNotNull(productVariantUpdated);

                assertEquals(productVariant.getStock() + incomingProductVariantDetailCurrentRequest .getQuantity(), productVariantUpdated.getStock());

                log.info(String.valueOf(incomingProductVariantDetailResponse.getQuantity()));
                log.info(String.valueOf(productVariant.getStock()));
                log.info(String.valueOf(productVariantUpdated.getStock()));
            }
        });
    }

    @Test
    void createIncomingProductVariantDetailsFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                post("/api/incoming-product-details/123/incoming-product-variant-detail")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isUnauthorized()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void createIncomingProductVariantDetailsFailedIncomingProductDetailsIdIsNotNumber() throws Exception {
        mockMvc.perform(
                post("/api/incoming-product-details/abc/incoming-product-variant-detail")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncomingProductDetailCreateRequest()))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductDetailId type data is wrong.", response.getErrors());
        });
    }

    @Test
    void createIncomingProductVariantDetailsFailedValidation() throws Exception {
        mockMvc.perform(
                post("/api/incoming-product-details/321/incoming-product-variant-detail")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new IncomingProductVariantDetailCreateRequest()))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            log.info(response.getErrors());
        });
    }

    @Test
    void createIncomingProductVariantDetailsFailedIncomingProductDetailIsNotFound() throws Exception {
        IncomingProductVariantDetailCreateRequest request = new IncomingProductVariantDetailCreateRequest();
        request.setVariantId(99999);
        request.setPricePerUnit(10123);
        request.setQuantity(10);

        mockMvc.perform(
                post("/api/incoming-product-details/999999/incoming-product-variant-detail")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductDetail is not found, please check IncomingProductDetail id again.", response.getErrors());
        });
    }

    @Test
    void createIncomingProductVariantDetailsFailedIncomingProductVariantHasVariantIsFalse() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(false);
        incomingProductDetailRepository.save(incomingProductDetail);


        IncomingProductVariantDetailCreateRequest request = new IncomingProductVariantDetailCreateRequest();
        request.setVariantId(999999);
        request.setPricePerUnit(10123);
        request.setQuantity(10);

        mockMvc.perform(
                post("/api/incoming-product-details/" + incomingProductDetail.getId() + "/incoming-product-variant-detail")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Failed to create IncomingProductVariantDetail, since IncomingProductDetail hasVariant is false.", response.getErrors());
        });
    }

    @Test
    void createIncomingProductVariantDetailsFailedProductVariantIsNotFound() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetailRepository.save(incomingProductDetail);


        IncomingProductVariantDetailCreateRequest request = new IncomingProductVariantDetailCreateRequest();
        request.setVariantId(99999);
        request.setPricePerUnit(10123);
        request.setQuantity(10);

        mockMvc.perform(
                post("/api/incoming-product-details/" + incomingProductDetail.getId() + "/incoming-product-variant-detail")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("ProductVariant is not found, please check ProductVariant id again.", response.getErrors());
        });
    }

    @Test
    void createIncomingProductVariantDetailsFailedProductVariantIsAlreadyPresentInIncomingProductDetail() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setTotalPrice(10020);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(10020 * 10);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);


        IncomingProductVariantDetailCreateRequest request = new IncomingProductVariantDetailCreateRequest();
        request.setVariantId(productVariant.getId());
        request.setPricePerUnit(10123);
        request.setQuantity(10);

        mockMvc.perform(
                post("/api/incoming-product-details/" + incomingProductDetail.getId() + "/incoming-product-variant-detail")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("ProductVariant is already present in the IcomingProductDetail, please check ProductVarian id again.", response.getErrors());
        });
    }


    @Test
    void createIncomingProductVariantDetailsSuccess() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetailCreateRequest request = new IncomingProductVariantDetailCreateRequest();
        request.setVariantId(productVariant.getId());
        request.setPricePerUnit(10123);
        request.setQuantity(10);

        mockMvc.perform(
                post("/api/incoming-product-details/" + incomingProductDetail.getId() + "/incoming-product-variant-detail")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andExpect(result -> {
            WebResponse<IncomingProductVariantDetailResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(productVariant.getId(), response.getData().getVariant().getId());
            assertEquals(productVariant.getSku(), response.getData().getVariant().getSku());
            assertEquals(request.getPricePerUnit(), response.getData().getPricePerUnit());
            assertEquals(request.getQuantity(), response.getData().getQuantity());
            assertEquals(request.getPricePerUnit() * request.getQuantity(), response.getData().getTotalPrice());

            ProductVariant productVariantUpdated = productVariantRepository.findById(response.getData().getVariant().getId()).orElse(null);
            assertNotNull(productVariantUpdated);

            assertEquals(productVariant.getStock() + request.getQuantity(), productVariantUpdated.getStock());
        });
    }

    @Test
    void deleteFailedIncomingProductVariantDetailIdIsNotNumber() throws Exception {
        mockMvc.perform(
                delete("/api/incoming-product-variant-details/abc")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductVariantDetailId type data is wrong.", response.getErrors());
        });
    }

    @Test
    void deleteFailedIncomingProductVariantDetailIsNotFound() throws Exception {
        mockMvc.perform(
                delete("/api/incoming-product-variant-details/123")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductVariantDetails is not found, please check the IncomingProductVariantDetails id again.", response.getErrors());
        });
    }

    @Test
    void deleteSuccessIncomingProductVariantDetail() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetail.setTotalVariantPrice(1000 * 10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(1000);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(1000 * 10);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);

        mockMvc.perform(
                delete("/api/incoming-product-variant-details/" + incomingProductVariantDetail.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            IncomingProductDetail incomingProductDetailUpdated = incomingProductDetailRepository.findById(incomingProductDetail.getId()).orElse(null);
            assertNotNull(incomingProductDetailUpdated);
            assertEquals(incomingProductDetail.getTotalVariantQuantity() - incomingProductVariantDetail.getQuantity(), incomingProductDetailUpdated.getTotalVariantQuantity());
            assertEquals(incomingProductDetail.getTotalVariantPrice() - incomingProductVariantDetail.getTotalPrice(), incomingProductDetailUpdated.getTotalVariantPrice());

            ProductVariant productVariantUpdated = productVariantRepository.findById(productVariant.getId()).orElse(null);
            assertNotNull(productVariantUpdated);
            assertEquals(productVariant.getStock() - incomingProductVariantDetail.getQuantity(), productVariantUpdated.getStock());

            IncomingProductVariantDetail deletedIncomingProductVariantDetail = incomingProductVariantDetailRepository.findById(incomingProductVariantDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductVariantDetail);

        });
    }


    @Test
    void deleteFailedIncomingProductDetailIdIsNotNumber() throws Exception {
        mockMvc.perform(
                delete("/api/incoming-product-details/abc")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductDetailId type data is wrong.", response.getErrors());
        });
    }

    @Test
    void deleteFailedIncomingProductDetailIsNotFound() throws Exception {
        mockMvc.perform(
                delete("/api/incoming-product-details/123")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProductDetails is not found, please check the IncomingProductDetails id again.", response.getErrors());


        });
    }

    @Test
    void deleteSuccessIncomingProductDetailWithoutVariant() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setPricePerUnit(1000);
        incomingProductDetail.setQuantity(10);
        incomingProductDetail.setTotalPrice(1000 * 10);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(false);
        incomingProductDetailRepository.save(incomingProductDetail);

        mockMvc.perform(
                delete("/api/incoming-product-details/" + incomingProductDetail.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            IncomingProductDetail deletedIncomingProductDetail = incomingProductDetailRepository.findById(incomingProductDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductDetail);

            Product productUpdated = productRepository.findById(productWithoutVariant.getId()).orElse(null);
            assertNotNull(productUpdated);
            assertEquals(productWithoutVariant.getStock() - incomingProductDetail.getQuantity(), productUpdated.getStock());

            IncomingProduct incomingProductUpdated = incomingProductRepository.findById(incomingProduct.getId()).orElse(null);
            assertNotNull(incomingProductUpdated);
            assertEquals(incomingProduct.getTotalProducts() - 1, incomingProductUpdated.getTotalProducts());
        });
    }

    @Test
    void deleteSuccessIncomingProductDetailWithVariant() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetail.setTotalVariantPrice(1000 * 10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(1000);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(1000 * 10);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);

        mockMvc.perform(
                delete("/api/incoming-product-details/" + incomingProductDetail.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            ProductVariant productVariantUpdated = productVariantRepository.findById(productVariant.getId()).orElse(null);
            assertNotNull(productVariantUpdated);
            assertEquals(productVariant.getStock() - incomingProductVariantDetail.getQuantity(), productVariantUpdated.getStock());

            IncomingProduct incomingProductUpdated = incomingProductRepository.findById(incomingProduct.getId()).orElse(null);
            assertNotNull(incomingProductUpdated);
            assertEquals(incomingProduct.getTotalProducts() - 1, incomingProductUpdated.getTotalProducts());

            IncomingProductDetail deletedIncomingProductDetail = incomingProductDetailRepository.findById(incomingProductDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductDetail);

            IncomingProductVariantDetail deletedIncomingProductVariantDetail = incomingProductVariantDetailRepository.findById(incomingProductVariantDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductVariantDetail);


        });
    }

    @Test
    void deleteFailedIncomingProductIdIsNotNumber() throws Exception {
        mockMvc.perform(
                delete("/api/incoming-products/abc")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isBadRequest()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("incomingProductId type data is wrong.", response.getErrors());
        });
    }

    @Test
    void deleteFailedIncomingProductIsNotFound() throws Exception {
        mockMvc.perform(
                delete("/api/incoming-products/123")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("IncomingProduct is not found. Please check IncomingProduct id again.", response.getErrors());
        });
    }

    @Test
    void deleteSuccessIncomingProductWithoutVariant() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setPricePerUnit(1000);
        incomingProductDetail.setQuantity(10);
        incomingProductDetail.setTotalPrice(1000 * 10);
        incomingProductDetail.setProduct(productWithoutVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(false);
        incomingProductDetailRepository.save(incomingProductDetail);

        mockMvc.perform(
                delete("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            IncomingProduct deletedIncomingProduct = incomingProductRepository.findById(incomingProduct.getId()).orElse(null);
            assertNull(deletedIncomingProduct);

            IncomingProductDetail deletedIncomingProductDetail = incomingProductDetailRepository.findById(incomingProductDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductDetail);


            Product productUpdated = productRepository.findById(productWithoutVariant.getId()).orElse(null);
            assertNotNull(productUpdated);
            assertEquals(productWithoutVariant.getStock() - incomingProductDetail.getQuantity(), productUpdated.getStock());

        });
    }

    @Test
    void deleteSuccessIncomingProductWithVariant() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetail.setTotalVariantPrice(1000 * 10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(1000);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(1000 * 10);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);

        mockMvc.perform(
                delete("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            ProductVariant productVariantUpdated = productVariantRepository.findById(productVariant.getId()).orElse(null);
            assertNotNull(productVariantUpdated);
            assertEquals(productVariant.getStock() - incomingProductVariantDetail.getQuantity(), productVariantUpdated.getStock());

            IncomingProduct deletedIncomingProduct = incomingProductRepository.findById(incomingProduct.getId()).orElse(null);
            assertNull(deletedIncomingProduct);

            IncomingProductDetail deletedIncomingProductDetail = incomingProductDetailRepository.findById(incomingProductDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductDetail);

            IncomingProductVariantDetail deletedIncomingProductVariantDetail = incomingProductVariantDetailRepository.findById(incomingProductVariantDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductVariantDetail);


        });
    }
    @Test
    void deleteSuccessIncomingProductWithMultipleIncomingProductDetail() throws Exception {
        Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
        assertNotNull(supplier);

        User user = userRepository.findById("admin_warehouse").orElse(null);
        assertNotNull(user);


        IncomingProduct incomingProduct = new IncomingProduct();
        incomingProduct.setDateIn(LocalDate.parse("2025-06-30"));
        incomingProduct.setTotalProducts(1);
        incomingProduct.setSupplier(supplier);
        incomingProduct.setUser(user);
        incomingProductRepository.save(incomingProduct);

        IncomingProductDetail incomingProductDetail = new IncomingProductDetail();
        incomingProductDetail.setProduct(productWithVariant);
        incomingProductDetail.setIncomingProduct(incomingProduct);
        incomingProductDetail.setHasVariant(true);
        incomingProductDetail.setTotalVariantQuantity(10);
        incomingProductDetail.setTotalVariantPrice(1000 * 10);
        incomingProductDetailRepository.save(incomingProductDetail);

        IncomingProductVariantDetail incomingProductVariantDetail = new IncomingProductVariantDetail();
        incomingProductVariantDetail.setIncomingProductDetail(incomingProductDetail);
        incomingProductVariantDetail.setProductVariant(productVariant);
        incomingProductVariantDetail.setPricePerUnit(1000);
        incomingProductVariantDetail.setQuantity(10);
        incomingProductVariantDetail.setTotalPrice(1000 * 10);
        incomingProductVariantDetailRepository.save(incomingProductVariantDetail);

        IncomingProductDetail incomingProductDetail2 = new IncomingProductDetail();
        incomingProductDetail2.setPricePerUnit(1000);
        incomingProductDetail2.setQuantity(15);
        incomingProductDetail2.setTotalPrice(1000 * 10);
        incomingProductDetail2.setProduct(productWithoutVariant);
        incomingProductDetail2.setIncomingProduct(incomingProduct);
        incomingProductDetail2.setHasVariant(false);
        incomingProductDetailRepository.save(incomingProductDetail2);


        mockMvc.perform(
                delete("/api/incoming-products/" + incomingProduct.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andExpect(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            ProductVariant productVariantUpdated = productVariantRepository.findById(productVariant.getId()).orElse(null);
            assertNotNull(productVariantUpdated);
            assertEquals(productVariant.getStock() - incomingProductVariantDetail.getQuantity(), productVariantUpdated.getStock());

            IncomingProduct deletedIncomingProduct = incomingProductRepository.findById(incomingProduct.getId()).orElse(null);
            assertNull(deletedIncomingProduct);

            IncomingProductDetail deletedIncomingProductDetail = incomingProductDetailRepository.findById(incomingProductDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductDetail);

            IncomingProductVariantDetail deletedIncomingProductVariantDetail = incomingProductVariantDetailRepository.findById(incomingProductVariantDetail.getId()).orElse(null);
            assertNull(deletedIncomingProductVariantDetail);

            // incomingProductDetail without variant
            IncomingProductDetail deletedIncomingProductDetail2 = incomingProductDetailRepository.findById(incomingProductDetail2.getId()).orElse(null);
            assertNull(deletedIncomingProductDetail2);

            Product productUpdated = productRepository.findById(productWithoutVariant.getId()).orElse(null);
            assertNotNull(productUpdated);
            assertEquals(productWithoutVariant.getStock() - incomingProductDetail2.getQuantity(), productUpdated.getStock());
        });
    }
}
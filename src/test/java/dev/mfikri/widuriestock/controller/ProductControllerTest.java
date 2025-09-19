package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.Role;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.entity.product.*;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.product.ProductCreateRequest;
import dev.mfikri.widuriestock.model.product.ProductResponse;
import dev.mfikri.widuriestock.model.product.ProductsGetListResponse;
import dev.mfikri.widuriestock.repository.*;
import dev.mfikri.widuriestock.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductVariantAttributeRepository productVariantAttributeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;
    Integer jwtTtl = 300000;

    String authorizationToken = "";
    Integer categoryId = null;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        addressRepository.deleteAll();
        userRepository.deleteAll();
        productPhotoRepository.deleteAllInBatch();
        productVariantAttributeRepository.deleteAllInBatch();
        productVariantRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Category Test");
        category.setDescription("Description Test");
        categoryRepository.save(category);
        categoryId = category.getId();

        User user = new User();
        user.setUsername("admin_warehouse");
        user.setPassword(passwordEncoder.encode("admin_warehouse_password"));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.name());
        userRepository.save(user);

        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);

        User user2 = new User();
        user2.setUsername("owner");
        user2.setPassword(passwordEncoder.encode("owner123"));
        user2.setFirstName("owner");
        user2.setPhone("+000000000");
        user2.setRole("OWNER");
        userRepository.save(user2);
    }

    @Test
    void createFailedTokenNotSend() throws Exception{
        ProductCreateRequest request = new ProductCreateRequest();

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .param("name", "Product Test")
        ).andExpect(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            log.info(response.getErrors().toString());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void createFailedValidation() throws Exception{
        ProductCreateRequest request = new ProductCreateRequest();

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .param("name", "Product Test")
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            log.info(response.getErrors().toString());
        });
    }

    @Test
    void createFailedValidationVariantError() throws Exception{

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
                        .param("variants[0].sku", "joran-pancing-white")
                        .param("variants[0].stock", "10")
                        .param("variants[0].price", "215000")
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            log.info(response.getErrors().toString());
        });
    }

    @Test
    void createFailedCategoryNotFound() throws Exception{
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", "0");
        params.add("hasVariant", "false");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Category is not found.", response.getErrors());
        });
    }

    @Test
    void createFailedProductAlreadyExists() throws Exception{
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Description ...");
        product.setCategory(category);
        product.setHasVariant(false);
        product.setStock(10);
        product.setPrice(10000);
        productRepository.save(product);


        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "false");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Product name is already exists.", response.getErrors());
        });
    }

    @Test
    void createFailedHasVariantFalseError() throws Exception{

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "false");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Price and Stock must be included when 'hasVariant' is false.", response.getErrors());
        });

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
                        .param("price", "10000")
                        .param("stock", "15")
                        .param("variants[0].sku", "joran-pancing-white")
                        .param("variants[0].stock", "10")
                        .param("variants[0].price", "215000")
                        .param("variants[0].attributes[0].attributeKey", "color")
                        .param("variants[0].attributes[0].attributeValue", "white")
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Product variants must not be included when 'hasVariant' is false.", response.getErrors());
        });
    }

    @Test
    void createFailedHasVariantTrueError() throws Exception{

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
                        .param("price", "10000")
                        .param("stock", "15")
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Price and Stock must not be included when 'hasVariant' is true.", response.getErrors());
        });

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Product variant must be included when 'hasVariant' is true.", response.getErrors());
        });
    }

    @Test
    void createFailedVariantAttributeSizeNotSameError() throws Exception{

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
                        .param("variants[0].sku", "joran-pancing-white")
                        .param("variants[0].stock", "10")
                        .param("variants[0].price", "215000")
                        .param("variants[0].attributes[0].attributeKey", "color")
                        .param("variants[0].attributes[0].attributeValue", "white")
                        .param("variants[1].sku", "joran-pancing-black")
                        .param("variants[1].stock", "10")
                        .param("variants[1].price", "215000")
                        .param("variants[1].attributes[0].attributeKey", "color")
                        .param("variants[1].attributes[0].attributeValue", "black")
                        .param("variants[1].attributes[1].attributeKey", "color")
                        .param("variants[1].attributes[1].attributeValue", "black")
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Variant 'Attribute' size must be same for each 'Variant'.", response.getErrors());
        });
    }

    @Test
    void createFailedVariantSkuDuplicateError() throws Exception{

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
                        .param("variants[0].sku", "joran-pancing-white")
                        .param("variants[0].stock", "10")
                        .param("variants[0].price", "215000")
                        .param("variants[0].attributes[0].attributeKey", "color")
                        .param("variants[0].attributes[0].attributeValue", "white")
                        .param("variants[1].sku", "joran-pancing-white")
                        .param("variants[1].stock", "10")
                        .param("variants[1].price", "215000")
                        .param("variants[1].attributes[0].attributeKey", "color")
                        .param("variants[1].attributes[0].attributeValue", "black")
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Variants 'sku' must be unique in a product.", response.getErrors());
        });
    }

    @Test
    void createSuccessProductWithNoVariant() throws Exception{

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "false");
        params.add("stock", "100");
        params.add("price", "120500");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)

        ).andExpect(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertNotNull(response.getData().getId());
            assertEquals("Product Test", response.getData().getName());
            assertEquals("Product description test", response.getData().getDescription());
            assertEquals(false, response.getData().getHasVariant());
            assertEquals(100, response.getData().getStock());
            assertEquals(120500, response.getData().getPrice());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals("Category Test", response.getData().getCategoryResponse().getName());
            assertNull(response.getData().getPhotos());
            assertEquals(0, response.getData().getVariants().size());

            Product product = productRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(product);

            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getCategory().getId(), response.getData().getCategoryResponse().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategoryResponse().getName());

            List<ProductVariant> variantList = productVariantRepository.findByProduct(product);
            assertEquals(variantList.size(), response.getData().getVariants().size());

            List<ProductPhoto> productPhotoList = productPhotoRepository.findProductPhotoByProduct(product);
            assertEquals(0,productPhotoList.size());

        });
    }

    @Test
    void createSuccessProductWithOneVariant() throws Exception {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");
        params.add("variants[0].sku", "joran-pancing-white");
        params.add("variants[0].stock", "10");
        params.add("variants[0].price", "215000");
        params.add("variants[0].attributes[0].attributeKey", "color");
        params.add("variants[0].attributes[0].attributeValue", "white");
        mockMvc.perform(
                multipart(HttpMethod.POST, "/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)

        ).andExpect(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertNotNull(response.getData().getId());
            assertEquals("Product Test", response.getData().getName());
            assertEquals("Product description test", response.getData().getDescription());
            assertEquals(true, response.getData().getHasVariant());
            assertNull(response.getData().getStock());
            assertNull(response.getData().getPrice());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals("Category Test", response.getData().getCategoryResponse().getName());
            assertNull(response.getData().getPhotos());
            assertEquals(1, response.getData().getVariants().size());


            Product product = productRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(product);

            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getCategory().getId(), response.getData().getCategoryResponse().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategoryResponse().getName());
            assertNull(response.getData().getPhotos());

            ProductResponse.ProductVariant productVariant = response.getData().getVariants().getFirst();
            List<ProductVariant> variantList = productVariantRepository.findByProduct(product);

            assertEquals(variantList.size(), response.getData().getVariants().size());
            assertEquals(variantList.getFirst().getId(), productVariant.getId());
            assertEquals(variantList.getFirst().getSku(), productVariant.getSku());
            assertEquals(variantList.getFirst().getPrice(), productVariant.getPrice());
            assertEquals(variantList.getFirst().getStock(), productVariant.getStock());
        });
    }

    @Test
    void createSuccessProductWithPhotos() throws Exception{

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "false");
        params.add("stock", "100");
        params.add("price", "120500");

        mockMvc.perform(
                multipart(HttpMethod.POST,"/api/products")
                        .file(new MockMultipartFile("productPhotos[0].image", "moon.jpg", "image/jpg", getClass().getResourceAsStream("/images/moon.jpg")))
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertNotNull(response.getData().getId());
            assertEquals("Product Test", response.getData().getName());
            assertEquals("Product description test", response.getData().getDescription());
            assertEquals(false, response.getData().getHasVariant());
            assertEquals(100, response.getData().getStock());
            assertEquals(120500, response.getData().getPrice());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals("Category Test", response.getData().getCategoryResponse().getName());
            assertEquals(1, response.getData().getPhotos().size());
            assertEquals(0, response.getData().getVariants().size());

            Product product = productRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(product);

            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getCategory().getId(), response.getData().getCategoryResponse().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategoryResponse().getName());

            List<ProductPhoto> productPhotoList = productPhotoRepository.findProductPhotoByProduct(product);
            assertEquals(productPhotoList.size(), response.getData().getPhotos().size());
            assertNotNull(productPhotoList.getFirst().getId());
            assertEquals(productPhotoList.getFirst().getImageLocation(), response.getData().getPhotos().getFirst().getImageLocation());

            List<ProductVariant> variantList = productVariantRepository.findByProduct(product);
            assertEquals(variantList.size(), response.getData().getVariants().size());
        });
    }

    @Test
    void getListFailedTokenNotSend() throws Exception{
        mockMvc.perform(
                get("/api/products")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }


    @Test
    void getListSuccessWithNotSendPageAndSize() throws Exception{
        createBulkProduct(10);

        mockMvc.perform(
                get("/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<ProductsGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());

            ProductsGetListResponse dataFirst = response.getData().getFirst();

            assertNotNull(dataFirst);
            assertNotNull(dataFirst.getId());
            assertEquals("Product 0", dataFirst.getName());
            assertEquals("Description 0", dataFirst.getDescription());
            assertEquals("upload/product/product-product-test-0.png", dataFirst.getImageLocation());
            assertNotNull(dataFirst.getCategoryResponse().getId());
            assertEquals("Category Test", dataFirst.getCategoryResponse().getName());

            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(10, response.getPaging().getSizePerPage());
            assertEquals(1, response.getPaging().getTotalPage());
        });
    }

    @Test
    void getListSuccessSendPageAndSize() throws Exception{
        createBulkProduct(10);

        mockMvc.perform(
                get("/api/products")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .param("page", "0")
                        .param("size", "5")
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<ProductsGetListResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertNotNull(response.getPaging());

            ProductsGetListResponse dataFirst = response.getData().getFirst();

            assertNotNull(dataFirst);
            assertNotNull(dataFirst.getId());
            assertEquals("Product 0", dataFirst.getName());
            assertEquals("Description 0", dataFirst.getDescription());
            assertEquals("upload/product/product-product-test-0.png", dataFirst.getImageLocation());
            assertNotNull(dataFirst.getCategoryResponse().getId());
            assertEquals("Category Test", dataFirst.getCategoryResponse().getName());

            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(5, response.getPaging().getSizePerPage());
            assertEquals(2, response.getPaging().getTotalPage());
        });
    }

    @Test
    void getFailedTokenNotSend() throws Exception{
        mockMvc.perform(
                get("/api/products/123")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void getFailedPathVariableWrongTypeData() throws Exception {
        mockMvc.perform(
                get("/api/products/abc")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Invalid number format for property 'productId'. Value 'abc' is not a valid number.", response.getErrors());
        });
    }

    @Test
    void getFailedProductNotFound() throws Exception{
        mockMvc.perform(
                get("/api/products/123")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Product is not found.", response.getErrors());
        });
    }

    @Test
    void getSuccessProductNotHasVariant() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(false);
        product.setPrice(120500);
        product.setStock(100);
        product.setCategory(category);
        productRepository.save(product);

        mockMvc.perform(
                get("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(product.getId(), response.getData().getId());
            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getCategory().getId(), response.getData().getCategoryResponse().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategoryResponse().getName());
            assertEquals(0, response.getData().getVariants().size());
            assertEquals(0, response.getData().getPhotos().size());
        });
    }

    @Test
    void getSuccessProductWithVariant() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(true);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        mockMvc.perform(
                get("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(product.getId(), response.getData().getId());
            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getCategory().getId(), response.getData().getCategoryResponse().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategoryResponse().getName());
            assertEquals(1, response.getData().getVariants().size());
            assertEquals(0, response.getData().getPhotos().size());

            ProductResponse.ProductVariant variants = response.getData().getVariants().getFirst();
            assertEquals(productVariant.getId(), variants.getId());
            assertEquals(productVariant.getSku(), variants.getSku());
            assertEquals(productVariant.getStock(), variants.getStock());
            assertEquals(productVariant.getPrice(), variants.getPrice());

            ProductResponse.ProductVariantAttribute attribute = variants.getAttributes().getFirst();
            assertEquals(productVariantAttribute.getId(), attribute.getId());
            assertEquals(productVariantAttribute.getAttributeKey(), attribute.getAttributeKey());
            assertEquals(productVariantAttribute.getAttributeValue(), attribute.getAttributeValue());
        });
    }

    @Test
    void getSuccessProductWithVariantAndPhoto() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(false);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        ProductPhoto productPhoto = new ProductPhoto();
        productPhoto.setProduct(product);
        productPhoto.setImageLocation("/location/dummy/example.png");
        productPhotoRepository.save(productPhoto);

        mockMvc.perform(
                get("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals(product.getId(), response.getData().getId());
            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getCategory().getId(), response.getData().getCategoryResponse().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategoryResponse().getName());
            assertEquals(1, response.getData().getVariants().size());
            assertEquals(1, response.getData().getPhotos().size());

            ProductResponse.ProductVariant variants = response.getData().getVariants().getFirst();
            assertEquals(productVariant.getId(), variants.getId());
            assertEquals(productVariant.getSku(), variants.getSku());
            assertEquals(productVariant.getStock(), variants.getStock());
            assertEquals(productVariant.getPrice(), variants.getPrice());

            ProductResponse.ProductVariantAttribute attribute = variants.getAttributes().getFirst();
            assertEquals(productVariantAttribute.getId(), attribute.getId());
            assertEquals(productVariantAttribute.getAttributeKey(), attribute.getAttributeKey());
            assertEquals(productVariantAttribute.getAttributeValue(), attribute.getAttributeValue());

            ProductResponse.ProductPhoto photo = response.getData().getPhotos().getFirst();
            assertEquals(productPhoto.getId(), photo.getId());
            assertEquals(productPhoto.getImageLocation(), photo.getImageLocation());
        });
    }

    @Test
    void deleteFailedTokenNotSend() throws Exception{
        mockMvc.perform(
                delete("/api/products/123")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void deleteFailedPathVariableWrongTypeData() throws Exception {
        mockMvc.perform(
                delete("/api/products/abc")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Invalid number format for property 'productId'. Value 'abc' is not a valid number.", response.getErrors());
        });
    }

    @Test
    void deleteFailedProductNotFound() throws Exception{
        mockMvc.perform(
                delete("/api/products/123")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Product is not found.", response.getErrors());
        });
    }

    @Test
    void deleteSuccess() throws Exception{
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(false);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        ProductPhoto productPhoto = new ProductPhoto();
        productPhoto.setId("PHOTO-TEST");
        productPhoto.setProduct(product);
        productPhoto.setImageLocation("/location/dummy/example.png");
        productPhotoRepository.save(productPhoto);

        mockMvc.perform(
                delete("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

            Product deletedProduct = productRepository.findById(product.getId()).orElse(null);
            assertNull(deletedProduct);

            ProductVariant deletedProductVariant = productVariantRepository.findById(productVariant.getId()).orElse(null);
            assertNull(deletedProductVariant);

            ProductVariantAttribute deletedProductVariantAttribute = productVariantAttributeRepository.findById(productVariantAttribute.getId()).orElse(null);
            assertNull(deletedProductVariantAttribute);

            ProductPhoto deletedProductPhoto = productPhotoRepository.findById(productPhoto.getId()).orElse(null);
            assertNull(deletedProductPhoto);
        });
    }

    @Test
    void updateFailedTokenNotSend() throws Exception {
        mockMvc.perform(
                put("/api/products/123")
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void updateFailedPathVariableWrongDataType() throws Exception {
        mockMvc.perform(
                put("/api/products/abc")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Invalid number format for property 'productId'. Value 'abc' is not a valid number.", response.getErrors());
        });
    }

    @Test
    void updateFailedValidation() throws Exception {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());


        mockMvc.perform(
                put("/api/products/123")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("{hasVariant=[must not be null]}", response.getErrors().toString());
            log.info(response.getErrors().toString());
        });
    }

    @Test
    void updateFailedProductNotFound() throws Exception {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "false");
        params.add("stock", "100");
        params.add("price", "120500");


        mockMvc.perform(
                put("/api/products/123")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)
        ).andExpect(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Product is not found.", response.getErrors());
            log.info(response.getErrors().toString());
        });
    }

    @Test
    void updateFailedCategoryNotFound() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(false);
        product.setPrice(120500);
        product.setStock(100);
        product.setCategory(category);
        productRepository.save(product);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", "0");
        params.add("hasVariant", "false");
        params.add("stock", "100");
        params.add("price", "120500");

        mockMvc.perform(
                put("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)
        ).andExpect(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Category is not found.", response.getErrors());
            log.info(response.getErrors().toString());
        });
    }

    @Test
    void updateFailedValidationWhenHasVariantFalse() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(false);
        product.setPrice(120500);
        product.setStock(100);
        product.setCategory(category);
        productRepository.save(product);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "false");

        mockMvc.perform(
                put("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Price and Stock must be included when 'hasVariant' is false.", response.getErrors());
            log.info(response.getErrors().toString());
        });

        mockMvc.perform(
                put("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)
                        .param("price", "10000")
                        .param("stock", "15")
                        .param("variants[0].sku", "joran-pancing-white")
                        .param("variants[0].stock", "10")
                        .param("variants[0].price", "215000")
                        .param("variants[0].attributes[0].attributeKey", "color")
                        .param("variants[0].attributes[0].attributeValue", "white")
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Product variants must not be included when 'hasVariant' is false.", response.getErrors());
            log.info(response.getErrors().toString());
        });
    }

    @Test
    void updateFailedValidationWhenHasVariantTrue() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(false);
        product.setPrice(120500);
        product.setStock(100);
        product.setCategory(category);
        productRepository.save(product);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Test");
        params.add("description", "Product description test");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");

        mockMvc.perform(
                put("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)
                        .param("stock", "100")
                        .param("price", "120500")
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Price and Stock must not be included when 'hasVariant' is true.", response.getErrors());
            log.info(response.getErrors().toString());
        });

        mockMvc.perform(
                put("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)
        ).andExpect(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getData());
            assertNotNull(response.getErrors());
            assertEquals("Product variant must be included when 'hasVariant' is true.", response.getErrors());
            log.info(response.getErrors().toString());
        });
    }


    @Test
    void updateSuccessForProductAddPhoto() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(true);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Updated");
        params.add("description", "Product description Updated");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");
        params.add("variants[0].id", productVariant.getId().toString());
        params.add("variants[0].sku", "product-test-white");
        params.add("variants[0].stock", "10");
        params.add("variants[0].price", "215000");
        params.add("variants[0].attributes[0].id", productVariantAttribute.getId().toString());
        params.add("variants[0].attributes[0].attributeKey", "color");
        params.add("variants[0].attributes[0].attributeValue", "white");


        mockMvc.perform(
                multipart(HttpMethod.PUT,"/api/products/" + product.getId())
                        .file(new MockMultipartFile("productPhotos[0].image", "tan-malaka.png", "image/png", getClass().getResourceAsStream("/images/tan-malaka.png")))
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals("Product Updated", response.getData().getName());
            assertEquals("Product description Updated", response.getData().getDescription());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals(true, response.getData().getHasVariant());
            assertEquals(null, response.getData().getPrice());
            assertEquals(null, response.getData().getStock());
            assertEquals(productVariant.getId(), response.getData().getVariants().getFirst().getId());
            assertEquals("product-test-white", response.getData().getVariants().getFirst().getSku());
            assertEquals(10, response.getData().getVariants().getFirst().getStock());
            assertEquals(215000, response.getData().getVariants().getFirst().getPrice());
            assertEquals(productVariantAttribute.getId(), response.getData().getVariants().getFirst().getAttributes().getFirst().getId());
            assertEquals("color", response.getData().getVariants().getFirst().getAttributes().getFirst().getAttributeKey());
            assertEquals("white", response.getData().getVariants().getFirst().getAttributes().getFirst().getAttributeValue());
            assertEquals(1, response.getData().getPhotos().size());
            assertNotNull( response.getData().getPhotos().getFirst().getImageLocation());
            assertNotNull( response.getData().getPhotos().getFirst().getId());

        });
    }

    @Test
    void updateSuccessForProductUpdatePhotoAndAddPhoto() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(true);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        ProductPhoto productPhoto = new ProductPhoto();
        productPhoto.setImageLocation("upload/product/product-test-image-example.png");
        productPhoto.setProduct(product);
        productPhotoRepository.save(productPhoto);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Updated");
        params.add("description", "Product description Updated");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");
        params.add("variants[0].id", productVariant.getId().toString());
        params.add("variants[0].sku", "product-test-white");
        params.add("variants[0].stock", "10");
        params.add("variants[0].price", "215000");
        params.add("variants[0].attributes[0].id", productVariantAttribute.getId().toString());
        params.add("variants[0].attributes[0].attributeKey", "color");
        params.add("variants[0].attributes[0].attributeValue", "white");
        params.add("productPhotos[0].id", productPhoto.getId());


        mockMvc.perform(
                multipart(HttpMethod.PUT,"/api/products/" + product.getId())
                        .file(new MockMultipartFile("productPhotos[0].image", "1.jpg", "image/jpg", getClass().getResourceAsStream("/images/1.jpg")))
                        .file(new MockMultipartFile("productPhotos[1].image", "moon.jpg", "image/jpg", getClass().getResourceAsStream("/images/moon.jpg")))
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals("Product Updated", response.getData().getName());
            assertEquals("Product description Updated", response.getData().getDescription());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals(true, response.getData().getHasVariant());
            assertNull(response.getData().getPrice());
            assertNull(response.getData().getStock());
            assertEquals(productVariant.getId(), response.getData().getVariants().getFirst().getId());
            assertEquals("product-test-white", response.getData().getVariants().getFirst().getSku());
            assertEquals(10, response.getData().getVariants().getFirst().getStock());
            assertEquals(215000, response.getData().getVariants().getFirst().getPrice());
            assertEquals(productVariantAttribute.getId(), response.getData().getVariants().getFirst().getAttributes().getFirst().getId());
            assertEquals("color", response.getData().getVariants().getFirst().getAttributes().getFirst().getAttributeKey());
            assertEquals("white", response.getData().getVariants().getFirst().getAttributes().getFirst().getAttributeValue());
            assertEquals(2, response.getData().getPhotos().size());
            assertEquals(productPhoto.getId(), response.getData().getPhotos().getFirst().getId());
            assertNotNull(response.getData().getPhotos().getFirst().getImageLocation());
            assertNotNull(response.getData().getPhotos().get(1).getId());
            assertNotNull(response.getData().getPhotos().get(1).getImageLocation());
        });
    }


    @Test
    void updateSuccessForProductWithoutVariant() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(false);
        product.setPrice(120500);
        product.setStock(100);
        product.setCategory(category);
        productRepository.save(product);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Updated");
        params.add("description", "Product description Updated");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "false");
        params.add("stock", "50");
        params.add("price", "150500");

        mockMvc.perform(
                put("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)

        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals(product.getId(), response.getData().getId());
            assertEquals("Product Updated", response.getData().getName());
            assertEquals("Product description Updated", response.getData().getDescription());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals(false, response.getData().getHasVariant());
            assertEquals(50, response.getData().getStock());
            assertEquals(150500, response.getData().getPrice());
            assertEquals(0, response.getData().getVariants().size());
            assertEquals(0, response.getData().getPhotos().size());

            Product productRepo = productRepository.findById(product.getId()).orElse(null);
            assertNotNull(productRepo);
            assertEquals(productRepo.getId(), response.getData().getId());
            assertEquals(productRepo.getName(), response.getData().getName());
            assertEquals(productRepo.getDescription(), response.getData().getDescription());
            assertEquals(productRepo.getHasVariant(), response.getData().getHasVariant());
            assertEquals(productRepo.getCategory().getId(), response.getData().getCategoryResponse().getId());
            assertEquals(productRepo.getStock(), response.getData().getStock());
            assertEquals(productRepo.getPrice(), response.getData().getPrice());
            List<ProductPhoto> productPhotoList = productPhotoRepository.findProductPhotoByProduct(productRepo);
            assertEquals(productPhotoList.size(), response.getData().getPhotos().size());
            List<ProductVariant> variantList = productVariantRepository.findByProduct(productRepo);
            assertEquals(variantList.size(), response.getData().getVariants().size());
        });
    }

    @Test
    void updateSuccessForProductWithVariant() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(true);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Updated");
        params.add("description", "Product description Updated");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");
        params.add("variants[0].id", productVariant.getId().toString());
        params.add("variants[0].sku", "product-test-white");
        params.add("variants[0].stock", "10");
        params.add("variants[0].price", "215000");
        params.add("variants[0].attributes[0].id", productVariantAttribute.getId().toString());
        params.add("variants[0].attributes[0].attributeKey", "color");
        params.add("variants[0].attributes[0].attributeValue", "white");

        mockMvc.perform(
                put("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)

        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals("Product Updated", response.getData().getName());
            assertEquals("Product description Updated", response.getData().getDescription());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals(true, response.getData().getHasVariant());
            assertNull(response.getData().getPrice());
            assertNull(response.getData().getStock());
            assertEquals(productVariant.getId(), response.getData().getVariants().getFirst().getId());
            assertEquals("product-test-white", response.getData().getVariants().getFirst().getSku());
            assertEquals(10, response.getData().getVariants().getFirst().getStock());
            assertEquals(215000, response.getData().getVariants().getFirst().getPrice());
            assertEquals(productVariantAttribute.getId(), response.getData().getVariants().getFirst().getAttributes().getFirst().getId());
            assertEquals("color", response.getData().getVariants().getFirst().getAttributes().getFirst().getAttributeKey());
            assertEquals("white", response.getData().getVariants().getFirst().getAttributes().getFirst().getAttributeValue());
            assertEquals(0, response.getData().getPhotos().size());
        });
    }

    @Test
    void updateSuccessForAddProductVariant() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(true);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Updated");
        params.add("description", "Product description Updated");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");
        params.add("variants[0].id", productVariant.getId().toString());
        params.add("variants[0].sku",productVariant.getSku());
        params.add("variants[0].stock", String.valueOf(productVariant.getStock()));
        params.add("variants[0].price", String.valueOf(productVariant.getPrice()));
        params.add("variants[0].attributes[0].id", productVariantAttribute.getId().toString());
        params.add("variants[0].attributes[0].attributeKey", productVariantAttribute.getAttributeKey());
        params.add("variants[0].attributes[0].attributeValue", productVariantAttribute.getAttributeValue());
        // add new variant
        params.add("variants[1].sku", "product-test-blue");
        params.add("variants[1].stock", "20");
        params.add("variants[1].price", "215000");
        params.add("variants[1].attributes[0].attributeKey", "color");
        params.add("variants[1].attributes[0].attributeValue", "blue");


        mockMvc.perform(
                multipart(HttpMethod.PUT,"/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals("Product Updated", response.getData().getName());
            assertEquals("Product description Updated", response.getData().getDescription());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals(true, response.getData().getHasVariant());
            assertNull(response.getData().getPrice());
            assertNull(response.getData().getStock());
            assertEquals(2, response.getData().getVariants().size());

            ProductResponse.ProductVariant productVariant1 = response.getData().getVariants().get(0);
            ProductResponse.ProductVariant productVariant2 = response.getData().getVariants().get(1);
            assertNotNull(productVariant1);
            assertNotNull(productVariant2);

            assertEquals(productVariant.getId() ,productVariant1.getId());
            assertEquals(productVariant.getSku() ,productVariant1.getSku());
            assertEquals(productVariant.getStock() ,productVariant1.getStock());
            assertEquals(productVariant.getPrice() ,productVariant1.getPrice());
            assertEquals(productVariantAttribute.getId() ,productVariant1.getAttributes().getFirst().getId());
            assertEquals(productVariantAttribute.getAttributeKey() ,productVariant1.getAttributes().getFirst().getAttributeKey());
            assertEquals(productVariantAttribute.getAttributeValue() ,productVariant1.getAttributes().getFirst().getAttributeValue());

            assertNotNull(productVariant2.getId());
            log.info(String.valueOf(productVariant2.getId()));
            assertEquals("product-test-blue" ,productVariant2.getSku());
            assertEquals(20 ,productVariant2.getStock());
            assertEquals(215000 ,productVariant2.getPrice());
            assertNotNull(productVariant2.getAttributes().getFirst().getId());
            log.info(String.valueOf(productVariant2.getAttributes().getFirst().getId()));
            assertEquals("color" ,productVariant2.getAttributes().getFirst().getAttributeKey());
            assertEquals("blue" ,productVariant2.getAttributes().getFirst().getAttributeValue());

        });
    }

    @Test
    void updateSuccessForAddProductVariantAttribute() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(true);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Updated");
        params.add("description", "Product description Updated");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "true");
        params.add("variants[0].id", productVariant.getId().toString());
        params.add("variants[0].sku", "product-test-black-large");
        params.add("variants[0].stock", String.valueOf(productVariant.getStock()));
        params.add("variants[0].price", String.valueOf(productVariant.getPrice()));
        params.add("variants[0].attributes[0].id", productVariantAttribute.getId().toString());
        params.add("variants[0].attributes[0].attributeKey", productVariantAttribute.getAttributeKey());
        params.add("variants[0].attributes[0].attributeValue", productVariantAttribute.getAttributeValue());
        // Add new attribute
        params.add("variants[0].attributes[1].attributeKey", "size");
        params.add("variants[0].attributes[1].attributeValue", "Large");

        mockMvc.perform(
                put("/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .accept(MediaType.APPLICATION_JSON)
                        .params(params)

        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals("Product Updated", response.getData().getName());
            assertEquals("Product description Updated", response.getData().getDescription());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals(true, response.getData().getHasVariant());
            assertNull(response.getData().getPrice());
            assertNull(response.getData().getStock());
            assertEquals(productVariant.getId(), response.getData().getVariants().getFirst().getId());
            assertEquals("product-test-black-large", response.getData().getVariants().getFirst().getSku());
            assertEquals(productVariant.getStock(), response.getData().getVariants().getFirst().getStock());
            assertEquals(productVariant.getPrice(), response.getData().getVariants().getFirst().getPrice());
            assertEquals(productVariantAttribute.getId(), response.getData().getVariants().getFirst().getAttributes().getFirst().getId());
            assertEquals(productVariantAttribute.getAttributeKey(), response.getData().getVariants().getFirst().getAttributes().getFirst().getAttributeKey());
            assertEquals(productVariantAttribute.getAttributeValue(), response.getData().getVariants().getFirst().getAttributes().getFirst().getAttributeValue());
            assertNotNull(response.getData().getVariants().getFirst().getAttributes().get(1).getAttributeKey());
            assertEquals("size", response.getData().getVariants().getFirst().getAttributes().get(1).getAttributeKey());
            assertEquals("Large", response.getData().getVariants().getFirst().getAttributes().get(1).getAttributeValue());
            assertEquals(0, response.getData().getPhotos().size());
        });
    }

    @Test
    void updateSuccessForRemoveProductVariant() throws Exception {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);

        Product product = new Product();
        product.setName("Product Test");
        product.setDescription("Product Description Test");
        product.setHasVariant(true);
        product.setCategory(category);
        productRepository.save(product);

        ProductVariant productVariant = new ProductVariant();
        productVariant.setProduct(product);
        productVariant.setSku("product-test-black");
        productVariant.setPrice(100500);
        productVariant.setStock(100);
        productVariantRepository.save(productVariant);

        ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
        productVariantAttribute.setProductVariant(productVariant);
        productVariantAttribute.setAttributeKey("color");
        productVariantAttribute.setAttributeValue("black");
        productVariantAttributeRepository.save(productVariantAttribute);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "Product Updated");
        params.add("description", "Product description Updated");
        params.add("categoryId", categoryId.toString());
        params.add("hasVariant", "false");
        params.add("price", "150000");
        params.add("stock", "120");


        mockMvc.perform(
                multipart(HttpMethod.PUT,"/api/products/" + product.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE)
                        .params(params)
        ).andExpect(
                status().isOk()
        ).andDo(result -> {
            WebResponse<ProductResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });
            assertNull(response.getErrors());
            assertNotNull(response.getData());

            assertEquals("Product Updated", response.getData().getName());
            assertEquals("Product description Updated", response.getData().getDescription());
            assertEquals(categoryId, response.getData().getCategoryResponse().getId());
            assertEquals(false, response.getData().getHasVariant());
            assertEquals(150000, response.getData().getPrice());
            assertEquals(120, response.getData().getStock());
            assertEquals(0, response.getData().getVariants().size());

            ProductVariant productVariantRepo = productVariantRepository.findById(productVariant.getId()).orElse(null);
            assertNull(productVariantRepo);
            ProductVariantAttribute productVariantAttributeRepo = productVariantAttributeRepository.findById(productVariantAttribute.getId()).orElse(null);
            assertNull(productVariantAttributeRepo);

        });
    }

    private void createBulkProduct(int size) {
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);
        List<Product> productList = new ArrayList<>();
        List<ProductVariant> variantList = new ArrayList<>();
        List<ProductVariantAttribute> variantAttributeList = new ArrayList<>();
        List<ProductPhoto> productPhotoList = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setDescription("Description " + i);
            product.setPrice(100500);
            product.setStock(100 + i);
            product.setHasVariant(false);
            product.setCategory(category);


            Random random = new Random();
            // random 1-10 then if its odds, we add product with variant
            if (i != 0 && (random.nextInt(10) + 1) % 2 == 1) {
                product.setHasVariant(true);

                ProductVariant productVariant = new ProductVariant();
                productVariant.setProduct(product);
                productVariant.setSku("product-test-black");
                productVariant.setPrice(100500);
                productVariant.setStock(100);
                variantList.add(productVariant);

                ProductVariantAttribute productVariantAttribute = new ProductVariantAttribute();
                productVariantAttribute.setProductVariant(productVariant);
                productVariantAttribute.setAttributeKey("color");
                productVariantAttribute.setAttributeValue("black");
                variantAttributeList.add(productVariantAttribute);

                ProductPhoto photo = new ProductPhoto();
                photo.setId("PHOTO-TEST");
                photo.setImageLocation("upload/product/product-product-test-0.png");
                photo.setProduct(product);

                productPhotoList.add(photo);
            }

            productList.add(product);
        }

        ProductPhoto photo = new ProductPhoto();
        photo.setImageLocation("upload/product/product-product-test-0.png");
        photo.setProduct(productList.getFirst());
        productPhotoList.add(photo);

        productRepository.saveAll(productList);
        productPhotoRepository.saveAll(productPhotoList);
        productVariantRepository.saveAll(variantList);
        productVariantAttributeRepository.saveAll(variantAttributeList);
    }

//    @Test
//    void creat10kProduct() {
//        createBulkProduct(10_000);
//    }
}
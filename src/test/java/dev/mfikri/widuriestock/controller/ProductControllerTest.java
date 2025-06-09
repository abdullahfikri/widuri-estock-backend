package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.Role;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.entity.product.Product;
import dev.mfikri.widuriestock.entity.product.ProductPhoto;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.product.ProductCreateRequest;
import dev.mfikri.widuriestock.model.product.ProductResponse;
import dev.mfikri.widuriestock.model.product.ProductsGetListResponse;
import dev.mfikri.widuriestock.repository.*;
import dev.mfikri.widuriestock.util.BCrypt;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.MockMvcBuilder.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

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
    private CategoryRepository categoryRepository;

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
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = new Category();
        category.setName("Category Test");
        category.setDescription("Description Test");
        categoryRepository.save(category);
        categoryId = category.getId();

        User user = new User();
        user.setUsername("admin_warehouse");
        user.setPassword("{bcrypt}" + BCrypt.hashpw("admin_warehouse_password", BCrypt.gensalt()));
        user.setFirstName("John Doe");
        user.setPhone("+6283213121");
        user.setRole(Role.ADMIN_WAREHOUSE.name());
        userRepository.save(user);

        authorizationToken = "Bearer " + jwtUtil.generate(user.getUsername(), jwtTtl);
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
            log.info(response.getErrors());
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
            log.info(response.getErrors());
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
            assertEquals("variants[0].attributes: must not be null", response.getErrors());
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
            assertEquals("'Attribute' size must be same for each 'Variant'.", response.getErrors());
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
            assertEquals(categoryId, response.getData().getCategory().getId());
            assertEquals("Category Test", response.getData().getCategory().getName());
            assertEquals(0, response.getData().getPhotos().size());
            assertEquals(0, response.getData().getVariants().size());

            Product product = productRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(product);

            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getCategory().getId(), response.getData().getCategory().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategory().getName());
            assertEquals(product.getProductPhotos().size(), response.getData().getPhotos().size());
            assertEquals(product.getProductVariants().size(), response.getData().getVariants().size());

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
            assertEquals(null, response.getData().getStock());
            assertEquals(null, response.getData().getPrice());
            assertEquals(categoryId, response.getData().getCategory().getId());
            assertEquals("Category Test", response.getData().getCategory().getName());
            assertEquals(0, response.getData().getPhotos().size());
            assertEquals(1, response.getData().getVariants().size());


            Product product = productRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(product);

            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getCategory().getId(), response.getData().getCategory().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategory().getName());
            assertEquals(product.getProductPhotos().size(), response.getData().getPhotos().size());
            assertEquals(product.getProductVariants().size(), response.getData().getVariants().size());

            ProductResponse.ProductVariant productVariant = response.getData().getVariants().getFirst();
            assertEquals(product.getProductVariants().getFirst().getId(), productVariant.getId());
            assertEquals(product.getProductVariants().getFirst().getSku(), productVariant.getSku());
            assertEquals(product.getProductVariants().getFirst().getPrice(), productVariant.getPrice());
            assertEquals(product.getProductVariants().getFirst().getStock(), productVariant.getStock());
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
                        .file(new MockMultipartFile("productPhotos[0].image", "tan-malaka.png", "image/png", getClass().getResourceAsStream("/images/tan-malaka.png")))
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
            assertEquals(categoryId, response.getData().getCategory().getId());
            assertEquals("Category Test", response.getData().getCategory().getName());
            assertEquals(1, response.getData().getPhotos().size());
            assertEquals(0, response.getData().getVariants().size());

            Product product = productRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(product);

            assertEquals(product.getName(), response.getData().getName());
            assertEquals(product.getDescription(), response.getData().getDescription());
            assertEquals(product.getHasVariant(), response.getData().getHasVariant());
            assertEquals(product.getStock(), response.getData().getStock());
            assertEquals(product.getPrice(), response.getData().getPrice());
            assertEquals(product.getCategory().getId(), response.getData().getCategory().getId());
            assertEquals(product.getCategory().getName(), response.getData().getCategory().getName());
            assertEquals(product.getProductPhotos().size(), response.getData().getPhotos().size());
            assertNotNull(product.getProductPhotos().getFirst().getId());
            assertEquals(product.getProductPhotos().getFirst().getImageLocation(), response.getData().getPhotos().getFirst().getImageLocation());

            assertEquals(product.getProductVariants().size(), response.getData().getVariants().size());
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
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);
        List<Product> productsSave = new ArrayList<>();
        Product product0 = new Product();
        product0.setName("Product 0");
        product0.setDescription("Description 0");
        product0.setPrice(100500);
        product0.setStock(100);
        product0.setHasVariant(false);
        product0.setCategory(category);
        productRepository.save(product0);

        ProductPhoto photo = new ProductPhoto();
        photo.setImageLocation("upload/product/product-product-test-0.png");
        photo.setProduct(product0);
        productPhotoRepository.save(photo);

        for (int i = 1; i < 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setDescription("Description " + i);
            product.setPrice(100500);
            product.setStock(100 + i);
            product.setHasVariant(false);
            product.setCategory(category);
            productsSave.add(product);
        }
        productRepository.saveAll(productsSave);

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
            assertNotNull(dataFirst.getCategory().getId());
            assertEquals("Category Test", dataFirst.getCategory().getName());

            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(10, response.getPaging().getSizePerPage());
            assertEquals(1, response.getPaging().getTotalPage());
        });
    }

    @Test
    void getListSuccessSendPageAndSize() throws Exception{
        Category category = categoryRepository.findById(categoryId).orElse(null);
        assertNotNull(category);
        List<Product> productsSave = new ArrayList<>();
        Product product0 = new Product();
        product0.setName("Product 0");
        product0.setDescription("Description 0");
        product0.setPrice(100500);
        product0.setStock(100);
        product0.setHasVariant(false);
        product0.setCategory(category);
        productRepository.save(product0);

        ProductPhoto photo = new ProductPhoto();
        photo.setImageLocation("upload/product/product-product-test-0.png");
        photo.setProduct(product0);
        productPhotoRepository.save(photo);

        for (int i = 1; i < 10; i++) {
            Product product = new Product();
            product.setName("Product " + i);
            product.setDescription("Description " + i);
            product.setPrice(100500);
            product.setStock(100 + i);
            product.setHasVariant(false);
            product.setCategory(category);
            productsSave.add(product);
        }
        productRepository.saveAll(productsSave);

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
            assertNotNull(dataFirst.getCategory().getId());
            assertEquals("Category Test", dataFirst.getCategory().getName());

            assertEquals(0, response.getPaging().getCurrentPage());
            assertEquals(5, response.getPaging().getSizePerPage());
            assertEquals(2, response.getPaging().getTotalPage());
        });
    }
}
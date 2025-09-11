package dev.mfikri.widuriestock.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.mfikri.widuriestock.entity.product.Category;
import dev.mfikri.widuriestock.entity.Role;
import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.product.CategoryCreateRequest;
import dev.mfikri.widuriestock.model.product.CategoryResponse;
import dev.mfikri.widuriestock.model.product.CategoryUpdateRequest;
import dev.mfikri.widuriestock.repository.*;
import dev.mfikri.widuriestock.util.BCrypt;
import dev.mfikri.widuriestock.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CategoryControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductPhotoRepository productPhotoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;
    Integer jwtTtl = 300000;

    String authorizationToken = "";

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        productPhotoRepository.deleteAll();
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

        User user2 = new User();
        user2.setUsername("owner");
        user2.setPassword("{bcrypt}" + BCrypt.hashpw("owner123", BCrypt.gensalt()));
        user2.setFirstName("owner");
        user2.setPhone("+000000000");
        user2.setRole("OWNER");
        userRepository.save(user2);
    }

    @Test
    void createFailedTokenNotSend() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();

        mockMvc.perform(
                post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
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
        CategoryCreateRequest request = new CategoryCreateRequest();

        mockMvc.perform(
                post("/api/categories")
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
        });
    }

    @Test
    void createFailedNameAlreadyExists() throws Exception {
        Category category = new Category();
        category.setName("Category 1");
        category.setDescription("Description for category 1");
        categoryRepository.save(category);


        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName(category.getName());
        request.setDescription("Another description");

        mockMvc.perform(
                post("/api/categories")
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
            assertEquals("Category name is already exists.", response.getErrors());
        });
    }



    @Test
    void createSuccess() throws Exception {

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Category 1");
        request.setDescription("Another description");

        mockMvc.perform(
                post("/api/categories")
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(request.getName(), response.getData().getName());
            assertEquals(request.getDescription(), response.getData().getDescription());

            Category category = categoryRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(category);

            assertEquals(response.getData().getId(), category.getId());
            assertEquals(response.getData().getName(), category.getName());
            assertEquals(response.getData().getDescription(), category.getDescription());
        });
    }

    @Test
    void getListFailedTokenNotSend() throws Exception {

        mockMvc.perform(
                get("/api/categories")
                        .accept(MediaType.APPLICATION_JSON)
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
    void getListSuccess() throws Exception {

        for (int i = 0; i < 5; i++) {
            Category category = new Category();
            category.setName("Category " + i);
            category.setDescription("Description " + i);
            categoryRepository.save(category);
        }

        mockMvc.perform(
                get("/api/categories")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<CategoryResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(5, response.getData().size());
            CategoryResponse category1 = response.getData().getFirst();
            assertNotNull(category1.getId());
            assertEquals("Category 0", category1.getName());
            assertEquals("Description 0", category1.getDescription());

        });
    }

    @Test
    void getFailedIdTypeWrong() throws Exception {
        mockMvc.perform(
                get("/api/categories/abc" )
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Invalid number format for property 'categoryId'. Value 'abc' is not a valid number.", response.getErrors());
        });
    }

    @Test
    void getFailedNotFound() throws Exception {
        mockMvc.perform(
                get("/api/categories/123" )
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Category is not found.", response.getErrors());
        });
    }

    @Test
    void getSuccess() throws Exception {

        Category category = new Category();
        category.setName("Category 1");
        category.setDescription("Another description");
        categoryRepository.save(category);

        mockMvc.perform(
                get("/api/categories/" + category.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(category.getId(), response.getData().getId());
            assertEquals(category.getName(), response.getData().getName());
            assertEquals(category.getDescription(), response.getData().getDescription());

        });
    }

    @Test
    void updateFailedTokenNotSend() throws Exception {
        CategoryUpdateRequest request = new CategoryUpdateRequest();

        mockMvc.perform(
                put("/api/categories/123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isUnauthorized()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Authentication failed", response.getErrors());
        });
    }

    @Test
    void updateFailedValidation() throws Exception {
        Category category = new Category();
        category.setName("Category 1");
        category.setDescription("Another description");
        categoryRepository.save(category);

        CategoryCreateRequest request = new CategoryCreateRequest();

        mockMvc.perform(
                put("/api/categories/" + category.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
        });
    }

    @Test
    void updateFailedNotFound() throws Exception {
        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Category updated");
        request.setDescription("Description updated");

        mockMvc.perform(
                put("/api/categories/123" )
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Category is not found.", response.getErrors());
        });
    }

    @Test
    void updateSuccess() throws Exception {

        Category category = new Category();
        category.setName("Category 1");
        category.setDescription("Another description");
        categoryRepository.save(category);

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Category updated");
        request.setDescription("Description updated");

        mockMvc.perform(
                put("/api/categories/" + category.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(category.getId(), response.getData().getId());
            assertEquals(request.getName(), response.getData().getName());
            assertEquals(request.getDescription(), response.getData().getDescription());

            Category categoryDB = categoryRepository.findById(category.getId()).orElse(null);
            assertNotNull(categoryDB);
            assertEquals(category.getId(), categoryDB.getId());
            assertEquals(request.getName(), categoryDB.getName());
            assertEquals(request.getDescription(), categoryDB.getDescription());

        });
    }


    @Test
    void deleteFailedIdTypeWrong() throws Exception {
        mockMvc.perform(
                delete("/api/categories/abc" )
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isBadRequest()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Invalid number format for property 'categoryId'. Value 'abc' is not a valid number.", response.getErrors());
        });
    }

    @Test
    void deleteFailedNotFound() throws Exception {
        mockMvc.perform(
                delete("/api/categories/123" )
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isNotFound()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Category is not found.", response.getErrors());
        });
    }

    @Test
    void deleteSuccess() throws Exception {

        Category category = new Category();
        category.setName("Category 1");
        category.setDescription("Another description");
        categoryRepository.save(category);

        mockMvc.perform(
                delete("/api/categories/" + category.getId())
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());

        });
    }

    // authorization
    @Test
    void testOwner() throws Exception {
        //
        User userOwner = userRepository.findById("owner").orElse(null);
        assertNotNull(userOwner);

        String authorizationTokenOwner = "Bearer " + jwtUtil.generate(userOwner.getUsername(), jwtTtl);


        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Category 1");
        request.setDescription("Another description");

        // create
        mockMvc.perform(
                post("/api/categories")
                        .header("Authorization", authorizationTokenOwner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
        ).andExpectAll(
                status().isCreated()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertNotNull(response.getData().getId());
            assertEquals(request.getName(), response.getData().getName());
            assertEquals(request.getDescription(), response.getData().getDescription());

            Category category = categoryRepository.findById(response.getData().getId()).orElse(null);
            assertNotNull(category);

            assertEquals(response.getData().getId(), category.getId());
            assertEquals(response.getData().getName(), category.getName());
            assertEquals(response.getData().getDescription(), category.getDescription());
        });

        // get list
        mockMvc.perform(
                get("/api/categories")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<CategoryResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            CategoryResponse category1 = response.getData().getFirst();
            assertNotNull(category1.getId());
            assertEquals(request.getName(), category1.getName());
            assertEquals(request.getDescription(), category1.getDescription());

        });

        Category category = categoryRepository.findFirstByName(request.getName()).orElse(null);
        assertNotNull(category);

        // get
        mockMvc.perform(
                get("/api/categories/" + category.getId())
                        .header("Authorization", authorizationTokenOwner)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(category.getId(), response.getData().getId());
            assertEquals(category.getName(), response.getData().getName());
            assertEquals(category.getDescription(), response.getData().getDescription());
        });

        // update

        CategoryCreateRequest requestUpdate = new CategoryCreateRequest();
        requestUpdate.setName("Category updated");
        requestUpdate.setDescription("Description updated");

        mockMvc.perform(
                put("/api/categories/" + category.getId())
                        .header("Authorization", authorizationToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestUpdate))
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(category.getId(), response.getData().getId());
            assertEquals(requestUpdate.getName(), response.getData().getName());
            assertEquals(requestUpdate.getDescription(), response.getData().getDescription());

            Category categoryDB = categoryRepository.findById(category.getId()).orElse(null);
            assertNotNull(categoryDB);
            assertEquals(category.getId(), categoryDB.getId());
            assertEquals(requestUpdate.getName(), categoryDB.getName());
            assertEquals(requestUpdate.getDescription(), categoryDB.getDescription());

        });


        // delete
        mockMvc.perform(
                delete("/api/categories/" + category.getId())
                        .header("Authorization", authorizationTokenOwner)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals("OK", response.getData());
        });
    }

    @Test
    void testAdminSeller() throws Exception {
        User userSeller = new User();
        userSeller.setUsername("seller");
        userSeller.setPassword("{bcrypt}" + BCrypt.hashpw("seller_password", BCrypt.gensalt()));
        userSeller.setFirstName("John Seller");
        userSeller.setPhone("+6283213121");
        userSeller.setRole(Role.ADMIN_SELLER.name());
        userRepository.save(userSeller);

        String authorizationTokenSeller = "Bearer " + jwtUtil.generate(userSeller.getUsername(), jwtTtl);

        CategoryCreateRequest request = new CategoryCreateRequest();
        request.setName("Category 1");
        request.setDescription("Another description");

        // create
        mockMvc.perform(
                post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("Authorization", authorizationTokenSeller)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        Category category = new Category();
        category.setName("Category 1");
        category.setDescription("Another description");
        categoryRepository.save(category);

        // get list
        mockMvc.perform(
                get("/api/categories")
                        .header("Authorization", authorizationToken)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<List<CategoryResponse>> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(1, response.getData().size());
            CategoryResponse category1 = response.getData().getFirst();
            assertNotNull(category1.getId());
            assertEquals(category.getName(), category1.getName());
            assertEquals(category.getDescription(), category1.getDescription());

        });

        // get
        mockMvc.perform(
                get("/api/categories/" + category.getId())
                        .header("Authorization", authorizationTokenSeller)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isOk()
        ).andDo(result -> {
            WebResponse<CategoryResponse> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getErrors());
            assertNull(response.getPaging());
            assertNotNull(response.getData());
            assertEquals(category.getId(), response.getData().getId());
            assertEquals(category.getName(), response.getData().getName());
            assertEquals(category.getDescription(), response.getData().getDescription());
        });

        CategoryCreateRequest requestUpdate = new CategoryCreateRequest();
        requestUpdate.setName("Category updated");
        requestUpdate.setDescription("Description updated");

        mockMvc.perform(
                put("/api/categories/" + category.getId())
                        .header("Authorization", authorizationTokenSeller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestUpdate))
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });

        mockMvc.perform(
                delete("/api/categories/123")
                        .header("Authorization", authorizationTokenSeller)
                        .accept(MediaType.APPLICATION_JSON)
        ).andExpectAll(
                status().isForbidden()
        ).andDo(result -> {
            WebResponse<String> response = objectMapper.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {
            });

            assertNull(response.getData());
            assertNull(response.getPaging());
            assertNotNull(response.getErrors());
            assertEquals("Forbidden Access", response.getErrors());
        });
    }
}
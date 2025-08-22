package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.PagingResponse;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.user.*;
import dev.mfikri.widuriestock.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(path = "",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public WebResponse<UserResponse> create (@ModelAttribute UserCreateRequest request) {
        log.info("Receiving request to create user request");
        UserResponse response = userService.create(request);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<UserSearchResponse>> search (@RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                         @RequestParam(value = "size", required = false, defaultValue = "10") Integer size,
                                                         @ModelAttribute UserSearchFilterRequest filterRequest
    ) {
        log.info("Receiving request to searching user");

        filterRequest.setPage(page);
        filterRequest.setSize(size);

        Page<UserSearchResponse> responsePage = userService.searchUser(filterRequest);

        return WebResponse.<List<UserSearchResponse>>builder()
                .data(responsePage.getContent())
                .paging(PagingResponse.builder()
                        .currentPage(responsePage.getNumber())
                        .totalPage(responsePage.getTotalPages())
                        .sizePerPage(responsePage.getSize())
                        .build())
                .build();
    }

    @GetMapping(path = "/{username}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> get (@PathVariable String username) {
        log.info("Receiving request to get a user. username={}", username);


        UserResponse response = userService.get(username);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }

    @PatchMapping(path = "/{username}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> update (@ModelAttribute UserUpdateRequest request, @PathVariable String username) {
        log.info("Receiving request to update a user. username={}", username);

        request.setUsername(username);

        UserResponse response = userService.update(request, false);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }


    @GetMapping(path = "/current",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> getCurrent (HttpServletRequest httpServletRequest) {
        log.info("Receiving request to get current user");

        Principal userPrincipal = httpServletRequest.getUserPrincipal();
        UserResponse response = userService.get(userPrincipal.getName());

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }

    @PatchMapping(path = "/current",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> updateCurrent (@ModelAttribute UserUpdateRequest request, HttpServletRequest httpServletRequest) {
        log.info("Receiving request to update current user");

        Principal userPrincipal = httpServletRequest.getUserPrincipal();
        request.setUsername(userPrincipal.getName());

        UserResponse response = userService.update(request, true);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }



}

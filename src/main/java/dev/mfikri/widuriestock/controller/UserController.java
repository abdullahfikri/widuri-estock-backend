package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.user.*;
import dev.mfikri.widuriestock.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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
        UserResponse response = userService.create(request);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<List<UserSearchResponse>> search (
                                                         @RequestParam(value = "username", required = false) String username,
                                                         @RequestParam(value = "name", required = false) String name,
                                                         @RequestParam(value = "phone", required = false) String phone,
                                                         @RequestParam(value = "email", required = false) String email,
                                                         @RequestParam(value = "role", required = false) String role,
                                                         @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
                                                         @RequestParam(value = "size", required = false, defaultValue = "10") Integer size
    ) {

        UserSearchFilterRequest filterRequest = UserSearchFilterRequest.builder()
                .username(username)
                .name(name)
                .phone(phone)
                .email(email)
                .role(role)
                .page(page)
                .size(size)
                .build();

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
        // todo: get user from user details
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
        // todo: get user from user details
        Principal userPrincipal = httpServletRequest.getUserPrincipal();
        request.setUsername(userPrincipal.getName());

        UserResponse response = userService.update(request, true);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }



}

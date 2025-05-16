package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.WebResponse;
import dev.mfikri.widuriestock.model.user.UserCreateRequest;
import dev.mfikri.widuriestock.model.user.UserResponse;
import dev.mfikri.widuriestock.model.user.UserUpdateRequest;
import dev.mfikri.widuriestock.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
    public WebResponse<UserResponse> create (@ModelAttribute UserCreateRequest request, User user) {
        UserResponse response = userService.create(request);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }

    @GetMapping(path = "/{username}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> get (User user, @PathVariable String username) {
        UserResponse response = userService.get(username);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }

    @PatchMapping(path = "/{username}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public WebResponse<UserResponse> update (@ModelAttribute UserUpdateRequest request, User user, @PathVariable String username) {
        request.setUsername(username);

        UserResponse response = userService.update(request);

        return WebResponse.<UserResponse>builder()
                .data(response)
                .build();
    }

}

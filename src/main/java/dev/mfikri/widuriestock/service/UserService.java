package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.model.user.*;
import org.springframework.data.domain.Page;


public interface UserService {

    UserResponse create(UserCreateRequest request);

    UserResponse get(String username);

    UserResponse update(UserUpdateRequest request, boolean current);

    Page<UserSearchResponse> searchUser(UserSearchFilterRequest filterRequest);
}

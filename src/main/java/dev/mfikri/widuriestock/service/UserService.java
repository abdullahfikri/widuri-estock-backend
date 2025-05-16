package dev.mfikri.widuriestock.service;

import dev.mfikri.widuriestock.entity.User;
import dev.mfikri.widuriestock.model.user.UserCreateRequest;
import dev.mfikri.widuriestock.model.user.UserResponse;
import dev.mfikri.widuriestock.model.user.UserUpdateRequest;

public interface UserService {

    UserResponse create (UserCreateRequest request);

    UserResponse get (String username);

    UserResponse update (UserUpdateRequest request);
}

package dev.mfikri.widuriestock.model.user;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserSearchFilterRequest {

    private String username;

    // find eather on firstname or lastname
    private String name;
    private String phone;
    private String email;
    private String role;

    @NotNull
    private Integer page;

    @NotNull
    private Integer size;
}

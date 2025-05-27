package dev.mfikri.widuriestock.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthTokenResponse {
    private String token;
    private Date createdAt;
    private Date expiredAt;
}

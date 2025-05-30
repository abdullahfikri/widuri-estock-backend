package dev.mfikri.widuriestock.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "refresh_token")
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "user_agent") // device like Ubuntu - Chrome, Mac - Safari
    private String userAgent;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "username")
    private User user;
}

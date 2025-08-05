package dev.mfikri.widuriestock.config;

import dev.mfikri.widuriestock.entity.Role;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

@Configuration
public class AuthorizationConfig {
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui/**",
            "/*.yaml",
            "/docs",
            "/swagger-ui/**",
            "/v3/api-docs/swagger-config"
    };

    @Bean
    public Customizer<AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry> authorizeHttpRequestsCustomizer() {
        return authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers(SWAGGER_WHITELIST).permitAll()
                .requestMatchers("/api/auth/refresh-token").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users").hasRole("OWNER")
                .requestMatchers(HttpMethod.GET, "/api/users").hasRole("OWNER")
                .requestMatchers(HttpMethod.GET, "/api/users/current").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/users/current").authenticated()
                // TODO: address current request controller
                .requestMatchers(HttpMethod.POST, "/api/users/current/addresses").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/current/addresses").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/users/current/addresses/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/users/current/addresses/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/users/current/addresses/**").authenticated()

                // get/update any users
                .requestMatchers(HttpMethod.PATCH, "/api/users/**").hasRole("OWNER")
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasRole("OWNER")

                // categories crud api
                .requestMatchers(HttpMethod.POST, "/api/categories").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.GET, "/api/categories").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/categories/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/categories/*").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.DELETE, "/api/categories/*").hasRole(Role.ADMIN_WAREHOUSE.name())

                // products crud api
                .requestMatchers(HttpMethod.POST, "/api/products").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.GET, "/api/products").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/products/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/products/*").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.PUT, "/api/products/*").hasRole(Role.ADMIN_WAREHOUSE.name())

                // suppliers crud api
                .requestMatchers(HttpMethod.POST, "/api/suppliers").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.GET, "/api/suppliers").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/suppliers/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/suppliers/*").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.DELETE, "/api/suppliers/*").hasRole(Role.ADMIN_WAREHOUSE.name())

                // incoming-product api
                .requestMatchers(HttpMethod.POST, "/api/incoming-products").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.GET, "/api/incoming-products").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/incoming-products/*").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/incoming-products/*").hasRole(Role.ADMIN_WAREHOUSE.name())

                // incoming-product-detail api
                .requestMatchers(HttpMethod.POST, "/api/incoming-products/*/incoming-product-details").hasRole(Role.ADMIN_WAREHOUSE.name())

                // incoming-product-variant-detail api
                .requestMatchers(HttpMethod.POST, "/api/incoming-product-details/*/incoming-product-variant-detail").hasRole(Role.ADMIN_WAREHOUSE.name())

                .requestMatchers(HttpMethod.DELETE, "/api/incoming-products/*").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.DELETE, "/api/incoming-product-details/*").hasRole(Role.ADMIN_WAREHOUSE.name())
                .requestMatchers(HttpMethod.DELETE, "/api/incoming-product-variant-details/*").hasRole(Role.ADMIN_WAREHOUSE.name())
                .anyRequest().denyAll();
    }
}

package dev.mfikri.widuriestock.config;

import dev.mfikri.widuriestock.entrypoint.JwtAuthenticationEntryPoint;
import dev.mfikri.widuriestock.filter.JwtAuthenticationFilter;
import dev.mfikri.widuriestock.util.JwtUtil;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.DispatcherType;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AnonymousConfigurer;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.crypto.SecretKey;

@Configuration
@EnableWebSecurity(debug = true)
public class SecurityConfig {
    private final SecurityConfigProperties securityConfigProperties;
    private final UserDetailsService userDetailsService;

    private final JwtAuthenticationEntryPoint entryPoint;

    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(SecurityConfigProperties securityConfigProperties, UserDetailsService userDetailsService, JwtAuthenticationEntryPoint entryPoint, CustomAccessDeniedHandler customAccessDeniedHandler) {
        this.securityConfigProperties = securityConfigProperties;
        this.userDetailsService = userDetailsService;
        this.entryPoint = entryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(configurer -> configurer
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests((authorize) -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
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

                        .anyRequest().denyAll()
                )
                .exceptionHandling(handler-> handler.authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .authenticationProvider(authenticationProvider())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(LogoutConfigurer::disable)
                .anonymous(AnonymousConfigurer::disable)
                .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class);


        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
       return configuration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationEventPublisher authenticationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
    }


    @Bean
    public JwtUtil jwtUtil() {
        final SecretKey secretKey = Keys.hmacShaKeyFor(securityConfigProperties.getSecretKey().getBytes());
        return new JwtUtil(secretKey);
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("OWNER").implies("ADMIN_SELLER")
                .role("OWNER").implies("ADMIN_WAREHOUSE")
                .build();
    }
}

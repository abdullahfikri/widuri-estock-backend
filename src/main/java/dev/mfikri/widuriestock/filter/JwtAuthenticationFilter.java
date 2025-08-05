package dev.mfikri.widuriestock.filter;

import dev.mfikri.widuriestock.entrypoint.JwtAuthenticationEntryPoint;
import dev.mfikri.widuriestock.exception.JwtAuthenticationException;
import dev.mfikri.widuriestock.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
    private static final String USER_ID_KEY = "username";

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;


    private final String AUTH_HEADER = "Authorization";
    private final String AUTH_TYPE = "Bearer";

    public JwtAuthenticationFilter(UserDetailsService userDetailsService, JwtUtil jwtUtil, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        final String token = extractAuthorizationHeader(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String tokenUsername = jwtUtil.extractUsername(token);
            if (tokenUsername != null && securityContextHolderStrategy.getContext().getAuthentication() == null) {
                UserDetails userDetails;
                userDetails = userDetailsService.loadUserByUsername(tokenUsername);
                if (!jwtUtil.isTokenValid(token, userDetails.getUsername())) {
                    throw new UsernameNotFoundException("Failed to authenticate with access token");
                }

                final SecurityContext securityContext = this.securityContextHolderStrategy.createEmptyContext();
                final UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                securityContext.setAuthentication(authenticationToken);
                this.securityContextHolderStrategy.setContext(securityContext);

                MDC.put(USER_ID_KEY, userDetails.getUsername());
            }
        }
        catch (ExpiredJwtException e) {
            jwtAuthenticationEntryPoint.commence(request, response, new JwtAuthenticationException("Access Token Expired", e.getCause()));
            return;
        }
        catch (UsernameNotFoundException | JwtException e) {

            jwtAuthenticationEntryPoint.commence(request, response, new JwtAuthenticationException("Invalid Access Token", e.getCause()));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractAuthorizationHeader(HttpServletRequest request) {
        final String headerValue = request.getHeader(AUTH_HEADER);

        if (headerValue == null || !headerValue.startsWith(AUTH_TYPE)) {
            return null;
        }

        return headerValue.substring(AUTH_TYPE.length()).trim();
    }
}

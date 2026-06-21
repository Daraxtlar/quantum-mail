package com.daraxtlar.quantummail.config;

import com.daraxtlar.quantummail.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT authentication filter responsible for extracting and validating JSON Web Tokens
 * from incoming HTTP requests.
 *
 * <p>The filter checks the Authorization header for a Bearer token. If no header
 * is present, it also supports token retrieval from the request query parameter
 * named {@code token}. When a valid token is found, the corresponding user
 * authentication is stored in the Spring Security context.</p>
 *
 * <p>Invalid or expired tokens do not interrupt request processing. The request
 * continues without an authenticated security context.</p>
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    /**
     * Creates a new JWT filter instance.
     *
     * @param jwtService service used to validate JWT tokens and extract user information
     */
    public JwtFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Processes each incoming request and attempts to authenticate the user
     * based on the provided JWT token.
     *
     * @param request     current HTTP request
     * @param response    current HTTP response
     * @param filterChain filter chain used to continue request processing
     * @throws ServletException if a servlet-related error occurs
     * @throws IOException      if an input/output error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isEmpty()) {
                token = queryToken;
            }
        }

        if (token != null) {
            try {
                String username = jwtService.getUsernameFromToken(token);

                if (username != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(
                                            new SimpleGrantedAuthority("ROLE_USER")));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception e) {
                System.err.println("Bad JWT token or expired: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}

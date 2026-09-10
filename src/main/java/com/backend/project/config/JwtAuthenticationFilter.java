package com.backend.project.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom Filter executed ONCE per incoming HTTP request.
 * 
 * INTERVIEW WALKTHROUGH:
 * 1. Checks for "Authorization: Bearer <token>" in the request header.
 * 2. If absent or does not start with "Bearer ", passes the request down the filter chain.
 * 3. If present, extracts the token and parses the username using JwtService.
 * 4. Loads the user from database and validates the token signature & expiry.
 * 5. If valid, builds a UsernamePasswordAuthenticationToken and stores it in
 *    SecurityContextHolder.
 * 6. Now Spring Security knows who the user is, and protected endpoints can be accessed!
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Step 1: Check Authorization header
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract Token (skipping "Bearer " which is 7 characters)
        jwt = authHeader.substring(7);

        try {
            username = jwtService.extractUsername(jwt);

            // Step 3: If username found and not already authenticated in this thread
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Step 4: Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null, // credentials not needed once authenticated
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Step 5: Save authentication to Spring Security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Token is invalid/expired; context remains unauthenticated
            logger.error("Cannot set user authentication: " + e.getMessage());
        }

        // Step 6: Continue along the filter chain
        filterChain.doFilter(request, response);
    }
}

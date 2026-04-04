package com.ecommerce.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Get the Authorization header
        final String authHeader = request.getHeader("Authorization");

        // 2. If no token or doesn't start with "Bearer ", skip this filter
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extract the token (remove "Bearer " prefix)
        final String jwt = authHeader.substring(7);

        // 4. Extract email from token
        final String userEmail = jwtUtil.extractUsername(jwt);

        // 5. If email found and user not already authenticated
        if (userEmail != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            // 6. Load user from database
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(userEmail);

            // 7. Validate token
            if (jwtUtil.isTokenValid(jwt, userDetails)) {

                // 8. Create authentication token
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request));

                // 9. Set authentication in SecurityContext
                SecurityContextHolder.getContext()
                        .setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
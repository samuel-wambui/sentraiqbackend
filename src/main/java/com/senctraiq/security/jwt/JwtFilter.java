package com.senctraiq.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Log logger = LogFactory.getLog(JwtFilter.class);

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        String token = null;

        // 1) Only proceed if we have a Bearer token
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7).trim();
        }

        // If no token, just continue as anonymous
        if (token == null || token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip RS256 tokens (OIDC tokens from authorization server) — only process HS256 app tokens
        try {
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
                if (headerJson.contains("RS256") || headerJson.contains("RS384") || headerJson.contains("RS512")) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        } catch (Exception ignored) {}

        String username;
        try {
            // 2) Attempt to extract the username (this will throw if token invalid/expired)
            username = jwtService.extractUserName(token);
        } catch (JwtException e) {
            logger.warn("JWT parsing failed: " + e.getMessage(), e);

            // ✅ Do NOT block request — let Spring Security handle it
            filterChain.doFilter(request, response);
            return;
        }


        // 4) If not yet authenticated in this context, validate & set auth
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Load full details (roles, etc.) from your UserDetailsService
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Standard user authentication flow
            if (jwtService.validateToken(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }



















        // 6) Finally, continue the chain exactly once
        filterChain.doFilter(request, response);
    }
}

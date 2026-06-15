
package com.senctraiq.notifications;


import com.senctraiq.security.DetailsService;
import com.senctraiq.security.jwt.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

@Component  // ADD THIS
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final DetailsService detailsService;

    public JwtHandshakeInterceptor(JwtService jwtService, DetailsService detailsService) {
        this.jwtService = jwtService;
        this.detailsService = detailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        // try Authorization header first
        List<String> authHeaders = request.getHeaders().get("Authorization");
        String token = null;
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearer = authHeaders.get(0);
            if (bearer.startsWith("Bearer ")) token = bearer.substring(7);
        }

        // fallback to query param ?token=...
        if (token == null && request.getURI().getQuery() != null) {
            String[] params = request.getURI().getQuery().split("&");
            for (String p : params) {
                if (p.startsWith("token=")) {
                    token = p.substring("token=".length());
                    break;
                }
            }
        }

        if (token != null) {
            try {
                String username = jwtService.extractUserName(token);
                var userDetails = detailsService.loadUserByUsername(username);
                if (jwtService.validateToken(token, userDetails)) {
                    // store username so handshake handler can set a Principal
                    attributes.put("ws-username", username);
                }
            } catch (Exception ignored) {
            }
        }

        return true; // allow handshake to proceed (we attach username if found)
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               org.springframework.web.socket.WebSocketHandler wsHandler, Exception exception) {
    }
}
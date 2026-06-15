package com.senctraiq.notifications;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component  // ADD THIS
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(org.springframework.http.server.ServerHttpRequest request,
                                      org.springframework.web.socket.WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        Object username = attributes.get("ws-username");
        if (username instanceof String s) {
            return new StompPrincipal(s);
        }

        // fallback to default
        return super.determineUser(request, wsHandler, attributes);
    }
}
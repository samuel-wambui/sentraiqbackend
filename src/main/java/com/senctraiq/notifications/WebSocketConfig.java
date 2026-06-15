package com.senctraiq.notifications;

import com.senctraiq.security.DetailsService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final CustomHandshakeHandler customHandshakeHandler;
    private final DetailsService detailsService;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor,
                           CustomHandshakeHandler customHandshakeHandler,
                           DetailsService detailsService) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.customHandshakeHandler = customHandshakeHandler;
        this.detailsService = detailsService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-notifications")
                .setAllowedOrigins("http://localhost:3000", "http://localhost:8080")
                .addInterceptors((HandshakeInterceptor) jwtHandshakeInterceptor)
                .setHandshakeHandler((HandshakeHandler) customHandshakeHandler)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.setApplicationDestinationPrefixes("/app");
        config.enableSimpleBroker("/topic", "/queue");
        config.setUserDestinationPrefix("/user");
    }

    // Optional subscription authorization: prevent unauthorised subscriptions to role topics
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (accessor.getCommand() != null && accessor.getCommand().equals(org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE)) {
                    String destination = accessor.getDestination();
                    if (destination != null && destination.startsWith("/topic/notifications/role.")) {
                        // role name is after last dot
                        String[] parts = destination.split("\\.");
                        String roleName = parts[parts.length - 1]; // e.g. ADMIN
                        Authentication auth = (Authentication) accessor.getUser();
                        if (auth == null) {
                            return null; // reject subscribe
                        }
                        boolean hasRole = auth.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .anyMatch(a -> a.equals(roleName) || a.equals("ROLE_" + roleName));
                        if (!hasRole) {
                            return null; // reject subscribe
                        }
                    }
                }
                return message;
            }
        });
    }
}


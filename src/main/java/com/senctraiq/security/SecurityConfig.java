package com.senctraiq.security;


import com.senctraiq.security.jwt.JwtFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final DetailsService detailsService;
    private final CustomUserDetailsPasswordService userDetailsPasswordService;
    private final OidcUserInfoMapper oidcUserInfoMapper;


    public SecurityConfig(JwtFilter jwtFilter,
                          DetailsService detailsService,
                          CustomUserDetailsPasswordService userDetailsPasswordService,
                          OidcUserInfoMapper oidcUserInfoMapper) {
        this.jwtFilter = jwtFilter;
        this.detailsService = detailsService;
        this.userDetailsPasswordService = userDetailsPasswordService;
        this.oidcUserInfoMapper = oidcUserInfoMapper;
    }

    @Bean
    @Order(0)
    public SecurityFilterChain ssoLoginSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/login", "/login/**")
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'self' http://localhost:8080 http://localhost:3000"))
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                        .requireExplicitSave(false)
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    // Order(1): OIDC Authorization Server — session-based, NOT stateless
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(oidc -> oidc
                        .userInfoEndpoint(userInfo -> userInfo
                                .userInfoMapper(context -> oidcUserInfoMapper.loadUserInfo(
                                        context.getAuthorization().getPrincipalName()
                                ))
                        )
                )
                .clientAuthentication(clientAuth -> clientAuth
                        .authenticationProviders(providers -> providers.forEach(provider -> {
                            if (provider instanceof org.springframework.security.oauth2.server.authorization.authentication.ClientSecretAuthenticationProvider p) {
                                p.setPasswordEncoder(org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder());
                            }
                        }))
                );

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.disable())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("frame-ancestors 'self' http://localhost:8080 http://localhost:3000"))
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(new HttpSessionSecurityContextRepository())
                        .requireExplicitSave(false)
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                // /userinfo must accept Bearer token, not redirect to /login
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // Order(2): REST API — stateless JWT
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(detailsService);
        provider.setPasswordEncoder(new BCryptPasswordEncoder(12));
        provider.setUserDetailsPasswordService(userDetailsPasswordService);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/roles/create", "/api/roles/update/**", "/api/roles/delete/**")
                        .hasAnyAuthority("SUPERUSER")
                        .requestMatchers("/api/oauth2/clients", "/api/oauth2/clients/**")
                        .hasAnyAuthority("SUPERUSER")
                        .requestMatchers(HttpMethod.POST, "/api/users/change-password")
                        .authenticated()
                        .requestMatchers("/api/users/**", "/api/roles/getAllRoles", "/api/roles/assignRole", "/api/roles/removeRole")
                        .hasAnyAuthority("ADMIN", "SUPERUSER")
                        .requestMatchers(
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html",
                                "/api/auth/**",
                                "/api/shifts/**", "/api/shift-schedule/**", "/api/shift-groups/**", "/api/incidents/**", "/api/countries/**",
                                "/verification/**", "/user",
                                "/login", "/login/**",
                                "api/oauth2/**", "/.well-known/**", "/userinfo", "/api/escalations/**",
                                "/api/resolved-escalation/workflow/**",
                                "/api/leaves/**", "/ws-notifications/**","/api/users/notifications/**",
                                "/api/conversations/**", "/oauth2/authorize","/api/v1/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(provider)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "X-Requested-With"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

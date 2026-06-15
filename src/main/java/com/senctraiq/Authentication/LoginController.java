package com.senctraiq.Authentication;

import com.senctraiq.security.jwt.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class LoginController {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @GetMapping("/login")
    public void login(
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "redirect", required = false) String redirect,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("[SSO /login] Request from origin: {} | IP: {}", request.getHeader("Origin"), request.getRemoteAddr());
        log.info("[SSO /login] Token present: {} | Redirect: {}", token != null, redirect);

        // Log incoming cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            String cookieNames = Arrays.stream(cookies).map(Cookie::getName).collect(Collectors.joining(", "));
            log.info("[SSO /login] Incoming cookies: {}", cookieNames);
        } else {
            log.info("[SSO /login] No incoming cookies");
        }

        if (token != null && !token.isBlank()) {
            try {
                String username = jwtService.extractUserName(token);
                log.info("[SSO /login] JWT username: {}", username);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (username != null && jwtService.validateToken(token, userDetails)) {
                    log.info("[SSO /login] JWT valid ✓ | Authorities: {}", userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                    SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                    securityContext.setAuthentication(auth);
                    SecurityContextHolder.setContext(securityContext);
                    HttpSession session = request.getSession(true);
                    new HttpSessionSecurityContextRepository().saveContext(securityContext, request, response);

                    log.info("[SSO /login] Spring session created — JSESSIONID: {}", session.getId());
                    log.info("[SSO /login] Session max inactive: {}s", session.getMaxInactiveInterval());
                    log.info("[SSO /login] Redirecting to: {}", redirect);
                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

                    response.sendRedirect(redirect != null && !redirect.isBlank() ? redirect : "/oauth2/authorize");
                    return;
                } else {
                    log.warn("[SSO /login] JWT validation FAILED for user: {}", username);
                }
            } catch (Exception e) {
                log.error("[SSO /login] Error processing JWT: {}", e.getMessage());
            }
        }

        log.warn("[SSO /login] No valid token — returning session required page");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("""
                <html>
                <body style="font-family:sans-serif;display:flex;align-items:center;justify-content:center;height:100vh;margin:0;background:#f5f5f5">
                <div style="text-align:center;padding:40px;background:white;border-radius:8px;box-shadow:0 2px 8px rgba(0,0,0,0.1)">
                  <h2 style="color:#b71c1c">Session Required</h2>
                  <p>Please log in to the TMS application first, then return to the Dashboards page.</p>
                  <a href="http://localhost:8080/auth" style="color:#b71c1c">Go to Login</a>
                </div>
                </body>
                </html>
                """);
    }
}

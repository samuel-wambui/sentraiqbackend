package com.senctraiq.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final long ACCESS_TOKEN_TTL_MS = 5 * 60 * 1000L;
    private static final long REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000L;

    @Autowired
    JwtBlacklistService jwtBlacklistService;

    @Value("${app.jwt.secret}")
    private String secretKey;

    public SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, List<String> authorities, List<String> role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", authorities);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS)) // 5 min
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(String username, List<String> authorities) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS)) // 7 days
                .signWith(getKey())
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        return !isTokenExpired(token);
    }

    public String extractUserName(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<String> authorities = claims.get("authorities", List.class);
        return authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUserName(token);
        boolean isValidUsername = username.equals(userDetails.getUsername());
        boolean isExpired = isTokenExpired(token);
        boolean isBlacklisted = jwtBlacklistService.isTokenBlacklisted(token);
        return isValidUsername && !isExpired && !isBlacklisted;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String generateClientToken(String clientId, String clientType, List<String> scopes) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("client", true);
        claims.put("clientType", clientType);
        claims.put("scopes", scopes);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(clientId)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour
                .signWith(getKey())
                .compact();
    }

    public String extractClientType(String token) {
        return extractClaim(token, claims -> claims.get("clientType", String.class));
    }
}

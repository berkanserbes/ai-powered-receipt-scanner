package com.berkan.receiptscanner.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import com.berkan.receiptscanner.exception.InvalidTokenException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Service for JWT token generation and validation.
 * Handles access token creation, parsing, and validation with proper error handling.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration-minutes}")
    private long accessTokenExpirationMinutes;

    private SecretKey signingKey;

    /**
     * Initialize signing key once at startup to avoid repeated decoding.
     * This improves performance by caching the decoded key.
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Get access token expiration time in minutes.
     * 
     * @return expiration time in minutes
     */
    public long getAccessTokenExpirationInMinutes() {
        return accessTokenExpirationMinutes;
    }

    /**
     * Generate JWT access token for authenticated user.
     * Includes all user roles in the token claims as a JSON array.
     * 
     * @param userDetails authenticated user details
     * @return JWT token string
     * @throws InvalidTokenException if user has no authorities
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        
        // Extract all roles (not just the first one)
        if (userDetails.getAuthorities() == null || userDetails.getAuthorities().isEmpty()) {
            throw new InvalidTokenException("User must have at least one role");
        }
        
        // Store roles as JSON array (industry standard)
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        
        extraClaims.put("roles", roles);
        
        return buildToken(extraClaims, userDetails, accessTokenExpirationMinutes * 60_000L);
    }

    /**
     * Extract username from JWT token.
     * 
     * @param token JWT token
     * @return username
     * @throws InvalidTokenException if token is invalid
     */
    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            throw new InvalidTokenException("Invalid token", e);
        }
    }

    /**
     * Validate JWT token and extract claims in a single parse operation.
     * This method is optimized to parse the token only once.
     * 
     * @param token JWT token
     * @param userDetails user details to validate against
     * @return Claims if token is valid, null otherwise
     */
    public Claims validateAndExtractClaims(String token, UserDetails userDetails) {
        try {
            Claims claims = extractAllClaims(token);
            String username = claims.getSubject();
            Date expiration = claims.getExpiration();
            
            boolean isValid = username.equals(userDetails.getUsername())
                && expiration.after(new Date())
                && userDetails.isEnabled()
                && userDetails.isAccountNonLocked()
                && userDetails.isAccountNonExpired()
                && userDetails.isCredentialsNonExpired();
            
            return isValid ? claims : null;
            
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract specific claim from token.
     * 
     * @param token JWT token
     * @param claimsResolver function to extract claim
     * @return claim value
     * @throws InvalidTokenException if token parsing fails
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Build JWT token with claims and expiration.
     * 
     * @param extraClaims additional claims to include
     * @param userDetails user details
     * @param expirationMs expiration time in milliseconds
     * @return JWT token string
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expirationMs) {
        long now = System.currentTimeMillis();
        
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extract all claims from token with proper exception handling.
     * 
     * @param token JWT token
     * @return claims
     * @throws InvalidTokenException if token is invalid, expired, or malformed
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                    
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("Token has expired", e);
            
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("Malformed token", e);
            
        } catch (SecurityException e) {
            throw new InvalidTokenException("Invalid token signature", e);
            
        } catch (IllegalArgumentException e) {
            throw new InvalidTokenException("Token cannot be null or empty", e);
            
        } catch (RuntimeException e) {
            throw new InvalidTokenException("Failed to parse token", e);
        }
    }
}

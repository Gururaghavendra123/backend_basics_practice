package com.backend.project.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for:
 * 1. Generating JWT tokens after successful authentication.
 * 2. Extracting username (subject) and claims from existing tokens.
 * 3. Validating token integrity (signature check) and expiration date.
 *
 * INTERVIEW KNOWLEDGE:
 * - Uses HMAC-SHA256 (symmetric key encryption: same secret key to sign and verify).
 * - JJWT library version 0.12.x modern API.
 */
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKeyHex;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Extracts username (Subject) from JWT token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generates a token for the authenticated user with no extra custom claims.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a token with custom claims (e.g. roles, email).
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername()) // "sub" claim
                .issuedAt(new Date(System.currentTimeMillis())) // "iat" claim
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // "exp" claim
                .signWith(getSigningKey(), Jwts.SIG.HS256) // HMAC-SHA256 signature
                .compact();
    }

    /**
     * Validates if token belongs to this user and is not expired.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies the JWT signature using the secret signing key.
     * Throws an exception if signature is tampered or expired.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyHex);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

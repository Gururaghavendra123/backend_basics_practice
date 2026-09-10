# Backend & JWT Interview Cheat Sheet (Max 200 Lines)

## 1. System Architecture & Flowchart

```
[Client (Postman/React)]
    │
    │ 1. POST /api/auth/login { "username", "password" }
    ▼
[AuthController] ──► Authenticates with BCrypt in PostgreSQL
    │
    │ 2. JwtService.generateToken() returns: "eyJhbGciOi..."
    ▼
[Client] (Saves token; sends in next requests)
    │
    │ 3. GET/POST/PATCH /api/tasks
    │    Header: Authorization: Bearer eyJhbGciOi...
    ▼
[JwtAuthenticationFilter] (OncePerRequestFilter)
    │  - Extracts Bearer token
    │  - JwtService.isTokenValid() (Checks HMAC-SHA256 signature + expiry)
    │  - Sets: SecurityContextHolder.getContext().setAuthentication(user)
    ▼
[SecurityConfig] (Passes if route is authorized; Session = STATELESS)
    ▼
[TaskController] (Receives @AuthenticationPrincipal User user)
    ▼
[TaskRepository / PostgreSQL] (Executes SQL query via Spring Data JPA)
```

---

## 2. Core Code Snippets (The 4 Files That Matter)

### Snippet 1: Generate & Validate Token (`JwtService.java`)
```java
// 1. Create Token (HMAC-SHA256)
public String generateToken(UserDetails user) {
    return Jwts.builder()
        .subject(user.getUsername())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 86400000)) // 24h
        .signWith(getSigningKey(), Jwts.SIG.HS256)
        .compact();
}

// 2. Validate Token (Checks signature & expiry automatically)
public boolean isTokenValid(String token, UserDetails user) {
    return extractUsername(token).equals(user.getUsername()) && !isTokenExpired(token);
}
```

### Snippet 2: Catch Every Request (`JwtAuthenticationFilter.java`)
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
        String authHeader = req.getHeader("Authorization");
        
        // 1. Check for Bearer token
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(req, res); return;
        }
        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);

        // 2. Validate and set SecurityContext
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails user = userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(jwt, user)) {
                var authToken = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(req, res);
    }
}
```

### Snippet 3: Stateless Security (`SecurityConfig.java`)
```java
@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable()) // Disabled: No cookies used, immune to CSRF
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // No HTTP Session
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll() // Public
                .anyRequest().authenticated()               // Protected
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### Snippet 4: PUT vs PATCH (`TaskController.java`)
```java
// PUT = Full Replace (Idempotent: missing fields are erased/overwritten)
@PutMapping("/{id}")
public ResponseEntity<?> putTask(@PathVariable Long id, @RequestBody TaskRequest req) {
    Task t = repo.findById(id).orElseThrow();
    t.setTitle(req.getTitle());
    t.setDescription(req.getDescription());
    t.setCompleted(req.isCompleted());
    return ResponseEntity.ok(repo.save(t));
}

// PATCH = Partial Update (Only updates fields that are NOT null)
@PatchMapping("/{id}")
public ResponseEntity<?> patchTask(@PathVariable Long id, @RequestBody TaskPatchRequest req) {
    Task t = repo.findById(id).orElseThrow();
    if (req.getTitle() != null)       t.setTitle(req.getTitle());
    if (req.getDescription() != null) t.setDescription(req.getDescription());
    if (req.getCompleted() != null)   t.setCompleted(req.getCompleted());
    return ResponseEntity.ok(repo.save(t));
}
```

---

## 3. High-Yield Interview Q&A (Say These Exactly)

### Q1: Why JWT over HTTP Sessions?
> **Answer:** "For **scalability**. Sessions require server memory or sticky sessions across servers. JWT is **stateless** — the server stores zero session state and only validates the cryptographic signature. Any server instance can process the request."

### Q2: What is the difference between PUT and PATCH?
> **Answer:** "PUT replaces the entire resource; all fields must be sent or missing ones are overwritten. PATCH is a partial update where only the supplied fields are modified. PUT is strictly idempotent; PATCH is for delta changes."

### Q3: Is JWT payload encrypted?
> **Answer:** "No! A JWT is **signed, not encrypted**. The payload is just Base64Url-encoded JSON (`Header.Payload.Signature`). Anyone can decode it. Never store passwords in a JWT. The signature guarantees integrity, not privacy."

### Q4: How do you handle logout with stateless JWT?
> **Answer:** "Client-side: delete the token. For strict server-side invalidation: maintain a **Redis token blacklist** with TTL matching token expiry, or use short-lived access tokens (15m) with revocable database refresh tokens."

### Q5: 401 Unauthorized vs 403 Forbidden?
> **Answer:** "401 means unauthenticated (token missing, invalid, or expired — 'who are you?'). 403 means authenticated, but lacking permissions/roles to access the resource ('access denied')."

### Q6: How does PostgreSQL integrate?
> **Answer:** "Spring Data JPA with Hibernate as ORM and HikariCP for connection pooling. Mapped entity to `@Table(name = \"users\")` because `user` is a reserved keyword in PostgreSQL."

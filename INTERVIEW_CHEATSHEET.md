# Backend & JWT Interview Cheat Sheet

## 1. Request Flowchart

```
[Client (Postman/Browser)]
       │
       │ 1. POST /api/auth/login { "username", "password" }
       ▼
[AuthController] ──► Checks BCrypt password against PostgreSQL
       │
       │ 2. JwtService generates signed token: "eyJhbGci..."
       ▼
[Client] (Stores token; attaches to all future requests)
       │
       │ 3. GET / POST / PUT / PATCH / DELETE /api/tasks
       │    Header -> Authorization: Bearer eyJhbGci...
       ▼
[JwtAuthenticationFilter] (Intercepts request BEFORE controllers)
       │  • Extracts token string
       │  • Checks HMAC-SHA256 signature & expiration
       │  • Loads user and sets SecurityContextHolder
       ▼
[SecurityConfig] (Stateless check: verifies user has access)
       │
       ▼
[TaskController] (Runs business logic with @AuthenticationPrincipal)
       │
       ▼
[PostgreSQL DB] (Spring Data JPA executes SQL on 'users'/'tasks')
```

---

## 2. Step-by-Step Working Explanation (In Plain English)

1. **User Authentication (Login)**:
   The client sends username and password. Spring's `AuthenticationManager` hashes the incoming password using BCrypt and compares it to the hash in PostgreSQL. If valid, `JwtService` creates a token containing the username (`sub`), issue date (`iat`), and expiry (`exp`), signed with a 256-bit secret key.

2. **Client Stores & Sends Token**:
   The client saves the token (in memory, cookie, or storage). On every subsequent request to a protected route, the client adds the HTTP header: `Authorization: Bearer <token>`.

3. **The Filter Interception**:
   Before the request reaches any Controller, our custom `JwtAuthenticationFilter` intercepts it. It strips the word `"Bearer "`, decodes the token, checks that the signature matches our secret key, and verifies it isn't expired. It then tells Spring Security: *"This request is authenticated as user X"*.

4. **Controller & Database Execution**:
   Because `SecurityConfig` is configured as **STATELESS**, the server never creates an `HttpSession`. The controller receives the authenticated user directly and queries PostgreSQL using Spring Data JPA.

---

## 3. Conceptual Questions & Answers (No Code)

### Q1: Why did you use JWT instead of standard HTTP sessions?
> **Answer:** "For **scalability**. Traditional sessions require server memory or shared session storage (like Redis). When scaling horizontally across multiple server instances, stateful sessions need sticky sessions or synchronization. JWT is **stateless** — the server stores zero session data and only verifies the cryptographic signature on each request."

### Q2: Is the JWT payload encrypted? Can I put passwords in it?
> **Answer:** "No! JWT is **signed, NOT encrypted**. The payload is plain Base64Url-encoded JSON. Anyone can decode and read it on jwt.io. Never put sensitive data like passwords in a JWT. The signature only guarantees **integrity** (that no one tampered with the payload)."

### Q3: What is the difference between 401 Unauthorized and 403 Forbidden?
> **Answer:** "401 means **Unauthenticated** ('I don't know who you are' — missing, invalid, or expired token). 403 means **Forbidden** ('I know who you are, but you don't have permission/role to access this resource')."

### Q4: If JWT is stateless, how do you handle Logout or Token Revocation?
> **Answer:** "Standard logout is handled on the client by deleting the token. For immediate server-side revocation, we use a **Redis blacklist** with a TTL matching the token's remaining lifespan, or use short-lived access tokens (15m) paired with revocable database refresh tokens."

---

## 4. Technical Questions & Answers (With Small Code Snippets)

### Q5: What is the difference between PUT and PATCH? (The Interview Question!)
> **Answer:** "PUT is a **full replacement**; all fields must be sent, or omitted fields get erased/set to null. PATCH is a **partial update**; only the fields sent in the request body are modified."

```java
// PUT: Overwrites every field entirely (Idempotent)
task.setTitle(req.getTitle());
task.setDescription(req.getDescription());
task.setCompleted(req.isCompleted());

// PATCH: Only updates non-null fields (Partial Update)
if (req.getTitle() != null)       task.setTitle(req.getTitle());
if (req.getDescription() != null) task.setDescription(req.getDescription());
if (req.getCompleted() != null)   task.setCompleted(req.getCompleted());
```

### Q6: How does Spring Security intercept and validate the token?
> **Answer:** "We use a filter extending `OncePerRequestFilter`. It reads the `Authorization` header, extracts the token, checks the signature, and sets the `SecurityContext`."

```java
String authHeader = request.getHeader("Authorization");
if (authHeader != null && authHeader.startsWith("Bearer ")) {
    String token = authHeader.substring(7);
    String username = jwtService.extractUsername(token);
    // If signature & expiry are valid, set security context:
    SecurityContextHolder.getContext().setAuthentication(authToken);
}
chain.doFilter(request, response);
```

### Q7: How do you configure stateless session management in Spring Security?
> **Answer:** "In `SecurityFilterChain`, we set the session creation policy to `STATELESS` and disable CSRF because stateless APIs using Bearer headers are not vulnerable to CSRF."

```java
http.csrf(csrf -> csrf.disable())
    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**").permitAll()
        .anyRequest().authenticated()
    )
    .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
```

### Q8: How did you configure PostgreSQL with Spring Boot?
> **Answer:** "Using `spring-boot-starter-data-jpa` with HikariCP for connection pooling. In PostgreSQL, `user` is a reserved SQL keyword, so we explicitly map our entity to `@Table(name = "users")` to avoid syntax errors."

```java
@Entity
@Table(name = "users") // 'user' is a reserved keyword in PostgreSQL!
public class User implements UserDetails { ... }
```

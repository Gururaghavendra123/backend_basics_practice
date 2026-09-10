package com.backend.project.config;

import com.backend.project.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 6 Configuration.
 * 
 * CRITICAL INTERVIEW CONCEPTS:
 * 1. Why disable CSRF? 
 *    CSRF attacks rely on browsers automatically attaching session cookies. 
 *    Since JWT is sent manually in the "Authorization" header and we are STATELESS, CSRF is not vulnerable.
 * 
 * 2. Why SessionCreationPolicy.STATELESS?
 *    Tells Spring Security NEVER to create an HttpSession. Every request must supply its own JWT token.
 * 
 * 3. Why addFilterBefore?
 *    Places our JwtAuthenticationFilter BEFORE the default UsernamePasswordAuthenticationFilter
 *    so the token is validated and context populated before any controller is reached.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserRepository userRepository;

    public SecurityConfig(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                // 1. Disable CSRF for stateless REST APIs
                .csrf(AbstractHttpConfigurer::disable)

                // 2. Define endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public routes (anyone can register, login, or view the test UI)
                        .requestMatchers("/api/auth/**", "/", "/index.html", "/*.css", "/*.js", "/favicon.ico").permitAll()
                        // All other endpoints require a valid JWT
                        .anyRequest().authenticated()
                )

                // 3. Stateless session management (No session stored on server)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 4. Set our custom AuthenticationProvider
                .authenticationProvider(authenticationProvider())

                // 5. Insert JWT filter before Spring's username/password filter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt is a salted hash function designed for securely storing passwords
        return new BCryptPasswordEncoder();
    }
}

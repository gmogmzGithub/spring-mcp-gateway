package com.indeed.mcp.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security Configuration for MCP Gateway
 * 
 * This class demonstrates production-ready security patterns for an MCP Gateway.
 * It showcases different authentication and authorization strategies for various
 * types of routes, from public endpoints to role-based access control.
 * 
 * Key Security Concepts Demonstrated:
 * - Route-based security (different paths, different security requirements)
 * - HTTP Basic Authentication (simple but effective for APIs)
 * - Role-based access control (USER vs ADMIN roles)
 * - Password encryption using BCrypt
 * - CSRF protection disabled (appropriate for API gateways)
 * 
 * Production Considerations:
 * - Replace in-memory users with database or LDAP integration
 * - OAuth2 support TO BE PROVIDED in future updates
 *
 * OAuth2 Readiness:
 * This configuration is designed to be extended with OAuth2 support.
 *
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges -> exchanges
                        // ========================================
                        // PUBLIC ROUTES (No Authentication Required)
                        // ========================================
                        
                        /**
                         * Test and demonstration routes - open for easy testing
                         * These routes help developers understand gateway functionality
                         * without dealing with authentication complexity initially
                         */
                        .pathMatchers("/test/**").permitAll()           // Basic routing test
                        .pathMatchers("/headers/**").permitAll()        // Header manipulation demo
                        .pathMatchers("/oldapi/**").permitAll()         // Path rewriting demo

                        // ========================================
                        // AUTHENTICATED ROUTES (Valid User Required)
                        // ========================================
                        
                        /**
                         * Protected demonstration routes
                         * These show how to secure routes while allowing any authenticated user
                         * Perfect for internal tools and APIs that need basic protection
                         */
                        .pathMatchers("/params/**").authenticated()      // Parameter manipulation demo
                        .pathMatchers("/demo/**").authenticated()        // Combined filters demo
                        .pathMatchers("/mcp/**").authenticated()         // MCP simulation route

                        // ========================================
                        // MCP SERVER ROUTES (Critical - Must Be Protected)
                        // ========================================
                        
                        /**
                         * Production MCP server routes - ALWAYS require authentication
                         * These routes provide access to actual MCP servers and should
                         * never be left unprotected in production environments
                         * 
                         * Security rationale:
                         * - MCP servers may access sensitive data (files, databases, APIs)
                         * - AI tools should be authenticated to prevent abuse
                         * - Gateway provides centralized authentication point
                         */
                        .pathMatchers("/calculator/**").authenticated()  // Calculator MCP server
                        
                        /**
                         * Template for securing additional MCP servers:
                         * .pathMatchers("/your-mcp-server/**").authenticated()
                         * 
                         * For higher security requirements, use role-based access:
                         * .pathMatchers("/sensitive-mcp/**").hasRole("ADMIN")
                         */

                        // ========================================
                        // ADMIN ROUTES (Role-Based Access Control)
                        // ========================================
                        
                        /**
                         * Administrative routes requiring elevated privileges
                         * Only users with ADMIN role can access these endpoints
                         * Useful for gateway management, monitoring, and configuration
                         */
                        .pathMatchers("/admin/**").hasRole("ADMIN")      // Admin-only access
                        
                        /**
                         * Additional admin routes can be added:
                         * .pathMatchers("/management/**").hasRole("ADMIN")
                         * .pathMatchers("/metrics/**").hasRole("ADMIN")
                         * .pathMatchers("/config/**").hasRole("ADMIN")
                         */

                        // ========================================
                        // DEFAULT SECURITY POLICY
                        // ========================================
                        
                        /**
                         * Fail-safe: All other routes require authentication
                         * This ensures that any new routes added to the gateway
                         * are secure by default (security-first approach)
                         */
                        .anyExchange().authenticated()
                )
                
                /**
                 * Authentication Method Configuration
                 * 
                 * HTTP Basic Authentication is enabled here for simplicity and wide compatibility.
                 * It works well for:
                 * - API clients (curl, Postman, etc.)
                 * - Server-to-server communication
                 * - Development and testing
                 * 
                 * For production, consider:
                 * - OAuth2 with external providers (.oauth2Login())
                 * - JWT token authentication (.bearer())
                 * - Custom authentication filters
                 */
                .httpBasic(httpBasic -> {})
                
                /**
                 * CSRF Protection Configuration
                 * 
                 * CSRF is disabled for API gateways because:
                 * - APIs are typically stateless
                 * - CSRF protection is primarily for browser-based applications
                 * - API clients don't use cookies for authentication
                 * - Basic Auth and Bearer tokens are not vulnerable to CSRF
                 * 
                 * If you enable cookie-based authentication, re-enable CSRF:
                 * .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                 */
                .csrf(csrf -> csrf.disable())
                
                .build();
    }

    /**
     * User Details Service - In-Memory User Store
     * ONLY FOR NON-PRODUCTION USE
     * 
     * This method defines users and their credentials for authentication.
     * It's implemented as an in-memory store for simplicity and learning purposes.
     * 
     * Users Created:
     * 1. mcpuser (password: password123) - Regular user with USER role
     * 2. mcpadmin (password: admin123) - Admin user with USER and ADMIN roles
     * 
     * Production Alternatives:
     * - LdapUserDetailsManager: Integrate with LDAP/Active Directory
     * - OAuth2UserService: Use external identity providers (Google, GitHub, etc.)
     *
     * Testing the users:
     * - curl -u mcpuser:password123 http://localhost:8080/calculator/sse
     * - curl -u mcpadmin:admin123 http://localhost:8080/admin/anything
     * 
     * @return MapReactiveUserDetailsService containing configured users
     */
    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        
        /**
         * Regular User Configuration
         * Role: USER
         * Access: Can access authenticated routes but not admin routes
         * Use case: AI clients, regular API consumers, MCP server access
         */
        UserDetails user = User.builder()
                .username("mcpuser")                                 // Username for authentication
                .password(passwordEncoder().encode("password123"))   // BCrypt-hashed password
                .roles("USER")                                       // Assigned role(s)
                .build();

        /**
         * Administrator User Configuration  
         * Roles: USER, ADMIN
         * Access: Can access all routes including admin-only endpoints
         * Use case: Gateway administration, monitoring, configuration
         */
        UserDetails admin = User.builder()
                .username("mcpadmin")                               // Admin username
                .password(passwordEncoder().encode("admin123"))     // BCrypt-hashed password  
                .roles("USER", "ADMIN")                             // Multiple roles
                .build();

        /**
         * Additional users can be added here:
         * 
         * UserDetails apiClient = User.builder()
         *         .username("api-client")
         *         .password(passwordEncoder().encode("secure-password"))
         *         .roles("USER", "API_CLIENT")
         *         .build();
         * 
         * UserDetails monitor = User.builder()
         *         .username("monitoring")
         *         .password(passwordEncoder().encode("monitor-pass"))
         *         .roles("USER", "MONITOR")
         *         .build();
         */

        return new MapReactiveUserDetailsService(user, admin);
    }

    // For development/testing only (faster but less secure):
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
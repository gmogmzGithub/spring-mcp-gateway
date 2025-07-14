package com.indeed.mcp.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

/**
 * Gateway Routing Configuration for MCP Gateway
 * 
 * This class contains all route definitions for the MCP Gateway, following Spring Boot's
 * configuration best practices by separating routing concerns from the main application class.
 *
 * Adding New MCP Servers:
 * 1. Add a new route method or extend customRouteLocator()
 * 2. Configure appropriate filters and security
 * 3. Update SecurityConfig.java for authentication requirements
 * 4. Test the integration thoroughly
 */
@Configuration
public class GatewayRoutingConfig {

    /**
     * Route Categories:
     * - Demonstration routes: For learning and testing gateway functionality
     * - MCP-specific routes: For actual MCP server integration
     * - Admin routes: For management and monitoring
     *
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                
                // ========================================
                // DEMONSTRATION ROUTES (for learning)
                // ========================================
                
                /**
                 * Route 1: Basic Path Routing
                 * Purpose: Demonstrates simple path-based routing
                 * URL: http://localhost:8080/test/anything → http://httpbin.org/anything
                 * stripPrefix(1) removes the first path segment (/test)
                 */
                .route("test-route", r -> r
                        .path("/test/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://httpbin.org"))

                // ========================================
                // MCP-SPECIFIC ROUTES
                // ========================================

                /**
                 * Route 2: MCP Protocol Simulation
                 * Purpose: Simulates routing to an MCP server with proper headers
                 * This route demonstrates the headers typically needed for MCP communication:
                 * - Content-Type: application/json (MCP uses JSON-RPC 2.0)
                 * - X-MCP-Version: Protocol version identification
                 * - X-MCP-Gateway: Identifies requests coming through gateway
                 * 
                 * To test: curl -u mcpuser:password123 http://localhost:8080/mcp/anything
                 */
                .route("mcp-simulation", r -> r
                        .path("/mcp/**")
                        .filters(f -> f
                                .stripPrefix(1)
                                .addRequestHeader("Content-Type", "application/json")
                                .addRequestHeader("X-MCP-Version", "1.0")
                                .addRequestHeader("X-MCP-Gateway", "true"))
                        .uri("http://httpbin.org"))

                /**
                 * Route 3: Calculator MCP Server (PRODUCTION EXAMPLE)
                 * Purpose: Routes to a real MCP server running locally
                 * 
                 * This is the main example of connecting to an actual MCP server.
                 * The calculator MCP server should be running on localhost:8050
                 * 
                 * MCP Protocol Details:
                 * - Uses Server-Sent Events (SSE) for streaming communication
                 * - Requires authentication (configured in SecurityConfig)
                 * 
                 * To start a calculator MCP server (Python example):
                 * 1. Install FastMCP: pip install fastmcp
                 * 2. Create calculator server with /sse endpoint
                 * 3. Start on port 8050
                 * 
                 * To test this route:
                 * curl -u mcpuser:password123 http://localhost:8080/calculator/sse
                 * 
                 * Headers added:
                 * - X-MCP-Gateway: Identifies requests from this gateway
                 * - X-MCP-Client: Identifies the client type
                 * 
                 * IMPORTANT: Change localhost:8050 to your MCP server's actual address
                 */
                .route("calculator-mcp", r -> r
                        .path("/calculator/**")
                        .filters(f -> f
                                .stripPrefix(1)                                  // Remove /calculator prefix
                                .addRequestHeader("X-MCP-Gateway", "true")       // Gateway identification
                                .addRequestHeader("X-MCP-Client", "Gateway"))    // Client type identification
                        .uri("http://localhost:8050"))                       // MCP server URL

                // ========================================
                // ADD YOUR MCP SERVERS HERE
                // ========================================
                
                /**
                 * Template for adding your own MCP server:
                 * 
                 * .route("your-mcp-server", r -> r
                 *         .path("/your-server/**")                     // Choose your path
                 *         .filters(f -> f
                 *                 .stripPrefix(1)                      // Remove path prefix
                 *                 .addRequestHeader("X-MCP-Gateway", "true")
                 *                 .addRequestHeader("Authorization", "Bearer " + token)) // Add auth if needed
                 *         .uri("http://your-mcp-server:port"))         // Your server URL
                 * 
                 * Don't forget to:
                 * 1. Add security configuration in SecurityConfig.java
                 * 2. Test with: curl -u mcpuser:password123 http://localhost:8080/your-server/endpoint
                 * 3. Update documentation
                 */

                .build();
    }

    /**
     * Additional Route Configuration Methods
     * 
     * For complex applications, you can break down routes into separate methods:
     * 
     * @Bean
     * public RouteLocator publicRoutes(RouteLocatorBuilder builder) {
     *     return builder.routes()
     *         // Only public routes here
     *         .build();
     * }
     * 
     * @Bean
     * public RouteLocator mcpServerRoutes(RouteLocatorBuilder builder) {
     *     return builder.routes()
     *         // Only MCP server routes here
     *         .build();
     * }
     * 
     * Spring will automatically combine all RouteLocator beans into a single routing table.
     */
}

/**
 * CONNECTING YOUR MCP SERVER - Developer Guide
 * ============================================
 * 
 * 1. PREPARE YOUR MCP SERVER
 *    - Ensure your MCP server implements the Model Context Protocol
 *    - Common endpoints: /sse for Server-Sent Events, /websocket for WebSocket
 *    - Server should handle JSON-RPC 2.0 messages
 * 
 * 2. ADD A ROUTE (in this class)
 *    - Add a new route in the customRouteLocator() method
 *    - Choose a unique route ID and path pattern
 *    - Configure appropriate filters (headers, authentication)
 *    - Set the URI to your MCP server's address
 * 
 * 3. CONFIGURE SECURITY (in SecurityConfig.java)
 *    - Add your route path to the security configuration
 *    - Choose authentication level: permitAll(), authenticated(), or hasRole()
 * 
 * 4. TEST YOUR INTEGRATION
 *    - Start your MCP server
 *    - Start this gateway (mvn spring-boot:run)
 *    - Test with curl: curl -u mcpuser:password123 http://localhost:8080/your-path/endpoint
 * 
 * 5. COMMON MCP HEADERS TO ADD
 *    - Content-Type: application/json
 *    - X-MCP-Version: 1.0
 *    - Authorization: Bearer <token> (for OAuth)
 *    - X-MCP-Gateway: true (for identification)
 * 
 * EXAMPLE MCP SERVERS TO TRY:
 * - Calculator server (FastMCP Python)
 * - File system server (MCP TypeScript)
 * - Database query server (custom implementation)
 * - Weather API server (external service wrapper)
 * 
 * ADVANCED ROUTING PATTERNS:
 * - Weighted routing: Route traffic percentage to different servers
 * - Circuit breaker: Fail fast when backend is down
 * - Rate limiting: Protect backend from overload
 * - Load balancing: Distribute load across multiple instances
 * - A/B testing: Route based on user characteristics
 */
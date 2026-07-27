package com.apigateway;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Component
public class JwtFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private RequestLogService requestLogService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        System.out.println("JwtFilter ran for: " + request.getRequestURI());
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtUtil.extractUsername(token);

            if (userRepository.findByUsername(username).isEmpty()) {
                throw new RuntimeException("User not found");
            }

            String internalCallHeader = request.getHeader("X-Internal-Call");
            boolean isInternalCall = "true".equals(internalCallHeader);

            if (!isInternalCall && !rateLimiterService.isAllowed(username)) {
                RequestLog log = new RequestLog();
                log.setUsername(username);
                log.setTargetUrl(request.getRequestURI());
                log.setHttpMethod(request.getMethod());
                log.setTimestamp(LocalDateTime.now());
                log.setResponseStatus(429);
                log.setIsAnomaly(true);
                log.setAnomalyReason("Rate limit exceeded");
                log.setClientIp(request.getRemoteAddr());
                requestLogService.saveLog(log);

                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
                return;
            }

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());

            SecurityContextHolder.getContext().setAuthentication(authToken);

            filterChain.doFilter(request, response);

            RequestLog log = new RequestLog();
            log.setUsername(username);
            log.setTargetUrl(request.getRequestURI());
            log.setHttpMethod(request.getMethod());
            log.setTimestamp(LocalDateTime.now());
            log.setResponseStatus(response.getStatus());
            log.setIsAnomaly(false);
            log.setClientIp(request.getRemoteAddr());

            requestLogService.saveLog(log);

        } catch (ExpiredJwtException e) {

            String username = e.getClaims().getSubject();

            RequestLog log = new RequestLog();
            log.setUsername(username);
            log.setTargetUrl(request.getRequestURI());
            log.setHttpMethod(request.getMethod());
            log.setTimestamp(LocalDateTime.now());
            log.setResponseStatus(401);
            log.setIsAnomaly(true);
            log.setAnomalyReason("Token expired");
            log.setClientIp(request.getRemoteAddr());

            requestLogService.saveLog(log);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Token expired\"}");

        } catch (Exception e) {

            RequestLog log = new RequestLog();
            log.setUsername("unknown");
            log.setTargetUrl(request.getRequestURI());
            log.setHttpMethod(request.getMethod());
            log.setTimestamp(LocalDateTime.now());
            log.setResponseStatus(401);
            log.setIsAnomaly(true);
            log.setAnomalyReason("Invalid token");
            log.setClientIp(request.getRemoteAddr());

            requestLogService.saveLog(log);

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid token\"}");
        }
    }
}
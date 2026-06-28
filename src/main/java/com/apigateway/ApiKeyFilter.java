package com.apigateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY = "myapigatewaykey123";
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private final Map<String, Integer> requestCounts = new ConcurrentHashMap<>();
    private final Map<String, Long> requestTimes = new ConcurrentHashMap<>();
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestApiKey = request.getHeader("X-API-KEY");

        if (requestApiKey == null || !requestApiKey.equals(API_KEY)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or missing API Key");
            return;
        }

        long currentTime = System.currentTimeMillis();

        if (!requestTimes.containsKey(requestApiKey)) {
            requestTimes.put(requestApiKey, currentTime);
            requestCounts.put(requestApiKey, 0);
        } else {
            long timeElapsed = currentTime - requestTimes.get(requestApiKey);
            if (timeElapsed > 60000) {
                requestTimes.put(requestApiKey, currentTime);
                requestCounts.put(requestApiKey, 0);
            }
        }

        int count = requestCounts.get(requestApiKey);
        if (count > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            response.getWriter().write("Rate limit exceeded. Try after 1 minute.");
            return;
        }

        requestCounts.put(requestApiKey, count + 1);
        filterChain.doFilter(request, response);
    }
}
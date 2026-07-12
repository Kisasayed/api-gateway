package com.apigateway;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@RestController
public class GatewayController {

    @Autowired
    private GatewayService gatewayService;

    @Value("${server.port:8080}")
    private String serverPort;

    private final Map<String, String> routeMap = new HashMap<>();

    @PostConstruct
    public void init() {
        routeMap.put("users", "http://localhost:" + serverPort + "/mock/users");
        routeMap.put("products", "http://localhost:" + serverPort + "/mock/products");
    }

    @GetMapping("/api/{route}")
    public ResponseEntity<String> forwardRequest(@PathVariable String route,
                                                 HttpServletRequest request) {

        String targetUrl = routeMap.get(route);

        if (targetUrl == null) {
            return ResponseEntity.badRequest().body("Error: Unknown route '" + route + "'");
        }

        String token = request.getHeader("Authorization");

        return gatewayService.forwardRequest(targetUrl, "GET", token);
    }
}
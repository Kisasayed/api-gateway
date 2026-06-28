package com.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;

@Service
public class GatewayService {

    @Autowired
    RequestLogRepository repository;

    private RestTemplate restTemplate = new RestTemplate();

    public String forwardRequest(String targetUrl, String httpMethod) {

        LocalDateTime start = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        String response = "";
        int statusCode = 200;

        try {
            ResponseEntity<String> result = restTemplate.getForEntity(targetUrl, String.class);
            response = result.getBody();
            statusCode = result.getStatusCode().value();
        } catch (Exception e) {
            response = "Error: " + e.getMessage();
            statusCode = 500;
        }

        long responseTime = System.currentTimeMillis() - startTime;

        RequestLog log = new RequestLog();
        log.setTargetUrl(targetUrl);
        log.setHttpMethod(httpMethod);
        log.setTimestamp(start);
        log.setResponseStatus(statusCode);
        log.setResponseTimeMs(responseTime);
        if (statusCode == 401 || statusCode == 429) {
            log.setIsAnomaly(true);
            log.setAnomalyReason(statusCode == 401 ? "Unauthorized access" : "Rate limit exceeded");
        } else {
            log.setIsAnomaly(false);
            log.setAnomalyReason("");
        }
        log.setClientIp("unknown");

        repository.save(log);

        return response;
    }

    public java.util.List<RequestLog> getAllLogs() {
        return repository.findAll();
    }
}

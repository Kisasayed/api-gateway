package com.apigateway;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;

@Service
public class GatewayService {

    @Autowired
    RequestLogRepository repository;

    private RestTemplate restTemplate = new RestTemplate();
    public ResponseEntity<String> forwardRequest(String targetUrl, String httpMethod, String token) {

        LocalDateTime start = LocalDateTime.now();
        long startTime = System.currentTimeMillis();

        String response = "";
        int statusCode = 200;

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.set("Authorization", token);
        }
        headers.set("X-Internal-Call", "true");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> result = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
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

        return ResponseEntity.status(statusCode).body(response);
    }

    public java.util.List<RequestLog> getAllLogs() { // reqlog objects in list , one per db row
        return repository.findAll(); // will return this to the dashboard controller
    }
}

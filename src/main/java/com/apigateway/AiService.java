package com.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Autowired
    private RequestLogRepository requestLogRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public String analyzeTraffic() {
        List<RequestLog> logs = requestLogRepository.findTop10ByOrderByTimestampDesc();
        String prompt = buildPrompt(logs);
        return callGemini(prompt);
    }

    private String buildPrompt(List<RequestLog> logs) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a security analyst for an API Gateway. ")
                .append("Analyze these recent request logs and detect anomalies:\n\n");

        for (int i = 0; i < logs.size(); i++) {
            RequestLog log = logs.get(i);
            sb.append("Log ").append(i + 1).append(": ")
                    .append("IP=").append(log.getClientIp()).append(", ")
                    .append("URL=").append(log.getTargetUrl()).append(", ")
                    .append("Status=").append(log.getResponseStatus()).append("\n");
        }

        sb.append("\nFind suspicious patterns. Tell me:\n")
                .append("1. Is there any anomaly?\n")
                .append("2. Which logs look suspicious and why?\n")
                .append("3. What kind of attack could this be?\n")
                .append("Keep your answer short and clear.");

        return sb.toString();
    }

    private String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map body = response.getBody();
            List candidates = (List) body.get("candidates");
            Map firstCandidate = (Map) candidates.get(0);
            Map content = (Map) firstCandidate.get("content");
            List parts = (List) content.get("parts");
            Map firstPart = (Map) parts.get(0);

            return (String) firstPart.get("text");

        } catch (Exception e) {
            return "AI analysis failed: " + e.getMessage();
        }
    }
}
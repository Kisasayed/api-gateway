package com.apigateway;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String targetUrl;
    private String httpMethod;
    private LocalDateTime timestamp;
    private Integer responseStatus;
    private Long responseTimeMs;
    private Boolean isAnomaly;
    private String anomalyReason;
    private String clientIp;
}
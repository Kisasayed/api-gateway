package com.apigateway;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RequestLogRepository requestLogRepository;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        System.out.println("LOGIN API HIT");

        User user = userRepository.findByUsername(loginRequest.getUsername()).orElse(null);

        if (user == null) {
            logLoginAttempt(loginRequest.getUsername(), false, "Invalid username");
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            logLoginAttempt(loginRequest.getUsername(), false, "Incorrect password");
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        String accessToken = jwtUtil.generateToken(user.getUsername());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        logLoginAttempt(user.getUsername(), true, "");

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken.getToken());

        return ResponseEntity.ok(response);
    }
    private void logLoginAttempt(String username, boolean success, String reason) {
        RequestLog log = new RequestLog();
        log.setUsername(username);
        log.setTargetUrl("/auth/login");
        log.setHttpMethod("POST");
        log.setTimestamp(LocalDateTime.now());
        log.setResponseStatus(success ? 200 : 401);
        log.setIsAnomaly(!success);
        log.setAnomalyReason(reason);
        log.setClientIp("unknown");
        requestLogRepository.save(log);
    }

    @GetMapping("/test")
    public String test() {
        System.out.println("TEST HIT");
        return "OK";
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        RefreshToken oldToken = refreshTokenRepository.findByToken(request.getRefreshToken()).orElse(null);

        if (oldToken == null) {
            logRefreshAttempt("unknown", false, "Invalid refresh token");
            return ResponseEntity.status(401).body("Invalid refresh token");
        }

        if (oldToken.isRevoked() || oldToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logRefreshAttempt(oldToken.getUser().getUsername(), false, "Refresh token expired or revoked");
            return ResponseEntity.status(401).body("Refresh token expired or revoked");
        }

        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        String newAccessToken = jwtUtil.generateToken(oldToken.getUser().getUsername());

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setToken(UUID.randomUUID().toString());
        newRefreshToken.setUser(oldToken.getUser());
        newRefreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        newRefreshToken.setRevoked(false);
        refreshTokenRepository.save(newRefreshToken);

        logRefreshAttempt(oldToken.getUser().getUsername(), true, "");

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("refreshToken", newRefreshToken.getToken());

        return ResponseEntity.ok(response);
    }

    private void logRefreshAttempt(String username, boolean success, String reason) {
        RequestLog log = new RequestLog();
        log.setUsername(username);
        log.setTargetUrl("/auth/refresh");
        log.setHttpMethod("POST");
        log.setTimestamp(LocalDateTime.now());
        log.setResponseStatus(success ? 200 : 401);
        log.setIsAnomaly(!success);
        log.setAnomalyReason(reason);
        log.setClientIp("unknown");
        requestLogRepository.save(log);
    }
}
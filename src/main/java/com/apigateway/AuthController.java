package com.apigateway;
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
    private JwtUtil jwtUtil;

    @Autowired
    private RequestLogService requestLogService;

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            logRegisterAttempt(request.getUsername(), false, "Username already taken");
            return ResponseEntity.badRequest().body("Username already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        logRegisterAttempt(user.getUsername(), true, "");

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

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
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        logLoginAttempt(user.getUsername(), true, "");

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);

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
        requestLogService.saveLog(log);
    }

    private void logRegisterAttempt(String username, boolean success, String reason) {
        RequestLog log = new RequestLog();
        log.setUsername(username);
        log.setTargetUrl("/auth/register");
        log.setHttpMethod("POST");
        log.setTimestamp(LocalDateTime.now());
        log.setResponseStatus(success ? 200 : 400);
        log.setIsAnomaly(!success);
        log.setAnomalyReason(reason);
        log.setClientIp("unknown");
        requestLogService.saveLog(log);
    }

    @GetMapping("/test")
    public String test() {
        System.out.println("TEST HIT");
        return "OK";
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest request) {
        return authService.refreshAccessToken(request.getRefreshToken());
    }
}
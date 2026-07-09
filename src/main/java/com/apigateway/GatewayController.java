package com.apigateway;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {
    @Autowired
    private GatewayService gatewayService;



    @GetMapping("/gateway")
    public String forwardRequest(@RequestParam(required = false) String url,
                                 HttpServletRequest request) {
        if (url == null || url.isEmpty()) {
            return "Error: Please provide a target URL using ?url= parameter";
        }

        String token = request.getHeader("Authorization");

        return gatewayService.forwardRequest(url, "GET", token);
    }
}

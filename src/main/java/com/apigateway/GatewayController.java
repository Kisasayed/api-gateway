package com.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GatewayController {
    @Autowired
    private GatewayService gatewayService;

    @GetMapping("/gateway")
    public String forwardRequest(@RequestParam String url) {
        return gatewayService.forwardRequest(url, "GET");
    }
}

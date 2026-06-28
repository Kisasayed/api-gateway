package com.apigateway;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockServiceController {
    @GetMapping("/mock/users")
    public String getUsers() {
        return "[{\"id\":1,\"name\":\"Alice\"},{\"id\":2,\"name\":\"Bob\"}]";
    }

    @GetMapping("/mock/products")
    public String getProducts() {
        return "[{\"id\":1,\"name\":\"Laptop\"},{\"id\":2,\"name\":\"Phone\"}]";
    }
}
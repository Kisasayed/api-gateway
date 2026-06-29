package com.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private RequestLogRepository requestLogRepository;

    @Autowired
    private AiService aiService;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        List<RequestLog> logs = requestLogRepository.findTop10ByOrderByTimestampDesc();
        model.addAttribute("logs", logs);
        return "dashboard";
    }

    @PostMapping("/dashboard/analyze")
    public String analyzeTraffic(Model model) {
        List<RequestLog> logs = requestLogRepository.findTop10ByOrderByTimestampDesc();
        model.addAttribute("logs", logs);
        String analysis = aiService.analyzeTraffic();
        model.addAttribute("analysis", analysis);
        return "dashboard";
    }
}
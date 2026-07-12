package com.apigateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private RequestLogRepository requestLogRepository;

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<RequestLog> todaysLogs = requestLogRepository.findByTimestampBetween(startOfDay, endOfDay);

        List<RequestLog> successfulLogs = new ArrayList<>();
        List<RequestLog> unsuccessfulLogs = new ArrayList<>();

        for (RequestLog log : todaysLogs) {
            int status = log.getResponseStatus();
            if (status >= 200 && status < 300) {
                successfulLogs.add(log);
            } else {
                unsuccessfulLogs.add(log);
            }
        }

        model.addAttribute("totalRequests", todaysLogs.size());
        model.addAttribute("successfulLogs", successfulLogs);
        model.addAttribute("unsuccessfulLogs", unsuccessfulLogs);

        return "dashboard";
    }
}
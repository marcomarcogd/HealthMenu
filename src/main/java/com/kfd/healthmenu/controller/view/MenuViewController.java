package com.kfd.healthmenu.controller.view;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MenuViewController {

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @GetMapping("/view/menu/{id}")
    public String view(@PathVariable Long id) {
        return "redirect:" + buildFrontendUrl("/view/menu/" + id);
    }

    @GetMapping("/share/menu/{token}")
    public String share(@PathVariable String token) {
        return "redirect:" + buildFrontendUrl("/share/menu/" + token);
    }

    private String buildFrontendUrl(String path) {
        String baseUrl = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }
}

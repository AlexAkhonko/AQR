package org.aqr.controller.ui;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        String login = auth.getName();
        model.addAttribute("login", login);
        return "dashboard";
    }
}


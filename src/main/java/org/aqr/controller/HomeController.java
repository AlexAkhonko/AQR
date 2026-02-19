package org.aqr.controller;

import org.aqr.entity.User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;

@Controller
public class HomeController {

//    @GetMapping("/")
//    public String home(Model model) {
//        model.addAttribute("message", "Hello, Thymeleaf!");
//        User user = new User();
//        user.setLogin("john");
//        model.addAttribute("user", user);
//        model.addAttribute("items", Arrays.asList("Item1", "Item2", "Item3"));
//        return "home"; // Ищет home.html в templates/
//    }
}

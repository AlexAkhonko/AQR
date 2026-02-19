package org.aqr.controller.ui;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.aqr.entity.User;
import org.aqr.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UserPageController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());  // ✅ Обязательно!
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult,
                           Model model) {  // ✅ Добавь Model!
        if (bindingResult.hasErrors()) {
            model.addAttribute("form", form);  // ✅ Верни form обратно!
            return "register";
        }

        // Проверка повтор пароля
        if (!form.getPassword().equals(form.getPassword2())) {
            bindingResult.rejectValue("password2", "error.passwords.mismatch", "Пароли не совпадают");
            model.addAttribute("form", form);
            return "register";
        }

        try {
            User user = new User();
            user.setLogin(form.getLogin());
            user.setPassword(passwordEncoder.encode(form.getPassword()));
            userService.save(user);  // encode внутри
            return "redirect:/login?registered";
        } catch (Exception e) {
            model.addAttribute("error", "Логин уже занят");
            model.addAttribute("form", form);
            return "register";
        }
    }

//    // Профиль (будущее)
//    @GetMapping("/profile")
//    public String profile(Authentication auth, Model model) {
//        String login = auth.getName();
//        User user = userService.findByLogin(login);
//        model.addAttribute("user", user);
//        return "profile";
//    }

    @Data  // lombok
    public static class RegisterForm {  // ✅ Внутри контроллера
        @NotBlank(message = "Логин обязателен")
        @Size(min = 3, max = 20)
        private String login;
        @NotBlank(message = "Пароль обязателен")
        @Size(min = 6)
        private String password;
        @NotBlank(message = "Повторите пароль")
        private String password2;
    }
}

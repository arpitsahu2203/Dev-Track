package com.devtracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String redirectToHome() {
        return "redirect:/devtracker/home";
    }

    @GetMapping({"/devtracker", "/devtracker/", "/devtracker/home"})
    public String home(Model model) {
        model.addAttribute("pageTitle", "Home");
        return "home";
    }

    @GetMapping("/devtracker/about")
    public String about(Model model) {
        model.addAttribute("pageTitle", "About");
        return "about";
    }

    @GetMapping("/devtracker/services")
    public String services(Model model) { model.addAttribute("pageTitle", "Features"); return "services"; }

    @GetMapping("/devtracker/contact")
    public String contact(Model model) { model.addAttribute("pageTitle", "Contact"); return "contact"; }
}

package com.bustrans.fleettrack.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class FrontendController {

    @GetMapping("/")
    public RedirectView home() {
        return new RedirectView("/login.html");
    }
}

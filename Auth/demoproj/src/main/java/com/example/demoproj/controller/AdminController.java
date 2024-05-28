package com.example.demoproj.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    @PostMapping("/create")
    public String adminPost() {
        return "This is an admin endpoint";
    }
}

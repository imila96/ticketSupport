package com.example.demoproj.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ticket")
public class TaskController {
    @PostMapping("/create")
    public String TaskPost() {
        return "Ticket created";
    }
}

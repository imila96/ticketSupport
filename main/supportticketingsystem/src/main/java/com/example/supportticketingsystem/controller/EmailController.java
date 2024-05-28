package com.example.supportticketingsystem.controller;

import com.example.supportticketingsystem.service.email.ModEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class EmailController {

    @Autowired
    private ModEmailService emailService;

    @GetMapping("/fetchAndSaveEmails/{minutes}")
    public void fetchAndSaveEmails(@PathVariable int minutes) {
        emailService.saveEmailsFromExternalService(minutes);
    }
}
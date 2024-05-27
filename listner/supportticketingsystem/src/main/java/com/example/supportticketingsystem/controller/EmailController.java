package com.example.supportticketingsystem.controller;

import com.example.supportticketingsystem.dto.request.EmailDTO;
import com.example.supportticketingsystem.service.email.MailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EmailController {

    @Autowired
    private MailService mailService;

    @GetMapping("/emails/{minutes}")
    public List<EmailDTO> getEmails(@PathVariable int minutes) {
        try {
            return mailService.fetchEmails(minutes);
        } catch (Exception e) {
            // Handle exception appropriately, maybe return an error response
            return null;
        }
    }
}

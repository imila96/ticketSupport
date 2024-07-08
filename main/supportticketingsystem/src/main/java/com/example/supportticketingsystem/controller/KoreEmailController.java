package com.example.supportticketingsystem.controller;


import com.example.supportticketingsystem.dto.collection.EmailEntity;
import com.example.supportticketingsystem.dto.response.KoreEmailDTO;
import com.example.supportticketingsystem.service.ticket.KoreEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/emails")
public class KoreEmailController {

    @Autowired
    private KoreEmailService emailService;


    @GetMapping
    public ResponseEntity<List<KoreEmailDTO>> getAllEmailAddresses() {
        List<KoreEmailDTO> emailAddresses = emailService.findAllWithEmailAndSeverities();
        return ResponseEntity.ok(emailAddresses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailEntity> getEmailById(@PathVariable Long id) {
        Optional<EmailEntity> emailEntity = emailService.findById(id);
        return emailEntity.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public EmailEntity createEmail(@RequestBody EmailEntity emailEntity) {
        return emailService.save(emailEntity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EmailEntity> updateEmail(@PathVariable Long id, @RequestBody EmailEntity emailDetails) {
        Optional<EmailEntity> optionalEmailEntity = emailService.findById(id);

        if (optionalEmailEntity.isPresent()) {
            EmailEntity emailEntity = optionalEmailEntity.get();
            emailEntity.setEmail(emailDetails.getEmail());
            emailEntity.setSeverities(emailDetails.getSeverities());
            return ResponseEntity.ok(emailService.save(emailEntity));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmail(@PathVariable Long id) {
        if (emailService.findById(id).isPresent()) {
            emailService.deleteById(id);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
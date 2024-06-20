package com.example.supportticketingsystem.service.ticket;


import com.example.supportticketingsystem.dto.collection.EmailEntity;
import com.example.supportticketingsystem.repository.KoreEmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KoreEmailService {

    @Autowired
    private KoreEmailRepository emailRepository;

    public List<EmailEntity> findAll() {
        return emailRepository.findAll();
    }

    public Optional<EmailEntity> findById(Long id) {
        return emailRepository.findById(id);
    }

    public EmailEntity save(EmailEntity emailEntity) {
        return emailRepository.save(emailEntity);
    }

    public void deleteById(Long id) {
        emailRepository.deleteById(id);
    }
}

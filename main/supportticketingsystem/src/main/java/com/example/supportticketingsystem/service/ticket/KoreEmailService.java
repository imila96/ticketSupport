package com.example.supportticketingsystem.service.ticket;


import com.example.supportticketingsystem.dto.collection.EmailEntity;
import com.example.supportticketingsystem.dto.response.KoreEmailDTO;
import com.example.supportticketingsystem.repository.KoreEmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class KoreEmailService {

    @Autowired
    private KoreEmailRepository emailRepository;

    public List<EmailEntity> findAll() {
        return emailRepository.findAll();
    }


    public List<KoreEmailDTO> findAllWithEmailAndSeverities() {
        List<EmailEntity> emailEntities = emailRepository.findAll();
        return emailEntities.stream()
                .map(emailEntity -> new KoreEmailDTO(emailEntity.getEmail(), emailEntity.getSeverities()))
                .collect(Collectors.toList());
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
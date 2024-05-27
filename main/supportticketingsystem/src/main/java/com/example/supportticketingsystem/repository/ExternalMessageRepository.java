package com.example.supportticketingsystem.repository;


import com.example.supportticketingsystem.dto.collection.ExternalMessage;
import com.example.supportticketingsystem.dto.collection.MessageAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalMessageRepository extends JpaRepository<ExternalMessage, Long> {
    ExternalMessage findByTicketId(Long ticketId);
}

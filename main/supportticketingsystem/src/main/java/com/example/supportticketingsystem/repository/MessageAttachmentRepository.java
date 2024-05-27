package com.example.supportticketingsystem.repository;

import com.example.supportticketingsystem.dto.collection.MessageAttachment;
import com.example.supportticketingsystem.dto.collection.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {


    @Query("SELECT a FROM MessageAttachment a WHERE a.message.ticket.id = ?1")
    List<MessageAttachment> findByTicketId(Long ticketId);


    @Query("SELECT a FROM MessageAttachment a WHERE a.message.id = ?1")
    List<MessageAttachment> findByMessageId(Long messageId);
}

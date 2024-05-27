package com.example.supportticketingsystem.repository;

import com.example.supportticketingsystem.dto.collection.Message;
import com.example.supportticketingsystem.dto.collection.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByTicketId(Long ticketId);

    boolean existsByUniqueId(String uniqueId);
}

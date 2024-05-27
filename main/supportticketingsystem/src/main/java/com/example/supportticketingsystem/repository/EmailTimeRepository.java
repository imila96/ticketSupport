
package com.example.supportticketingsystem.repository;

import com.example.supportticketingsystem.dto.collection.EmailTime;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTimeRepository extends JpaRepository<EmailTime, Long> {


    EmailTime findByTicketId(Long ticketId);
}
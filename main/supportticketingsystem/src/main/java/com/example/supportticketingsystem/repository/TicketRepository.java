package com.example.supportticketingsystem.repository;

import com.example.supportticketingsystem.dto.collection.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

}
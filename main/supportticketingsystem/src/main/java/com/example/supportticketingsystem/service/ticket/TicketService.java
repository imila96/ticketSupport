package com.example.supportticketingsystem.service.ticket;

import com.example.supportticketingsystem.dto.collection.Ticket;
import com.example.supportticketingsystem.dto.request.ReopenTicketRequest;
import com.example.supportticketingsystem.dto.request.TicketRequest;
import com.example.supportticketingsystem.dto.response.TicketResponse;
import com.example.supportticketingsystem.enums.MessageType;
import com.example.supportticketingsystem.enums.Severity;
import jakarta.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;


public interface TicketService {


    TicketResponse createTicket(TicketRequest request, List<MultipartFile> attachments) throws MessagingException, IOException;

    byte[] downloadFile(Long fileId);




    void closeTicket(Long ticketId, String sentBy) throws MessagingException;

    int getMaxAttemptsByTicketId(Long ticketId);

    void reopenTicket(ReopenTicketRequest request) throws MessagingException;

    List<Ticket> getAllTickets();

    Optional<Ticket> getTicketById(Long ticketId);

    // In TicketService interface
    public List<Ticket> getTicketsBySeverity(Severity severity);

}


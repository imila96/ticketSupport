package com.example.supportticketingsystem.service.message;

import com.example.supportticketingsystem.dto.request.MessageRequest;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public interface MessageService {

    String createMessage(MessageRequest request, Long ticketId) throws IOException, MessagingException;

    String createExternalMessage(MessageRequest request, Long ticketId) throws IOException, MessagingException;
}

package com.example.supportticketingsystem.service.message;

import com.example.supportticketingsystem.dto.request.MessageRequest;
import com.example.supportticketingsystem.dto.response.MessageResponse;
import jakarta.mail.MessagingException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public interface MessageService {

    String createMessage(MessageRequest request, Long ticketId) throws IOException, MessagingException;

    String createExternalMessage(MessageRequest request, Long ticketId) throws IOException, MessagingException;

    List<MessageResponse> getMessagesByTicketId(Long ticketId);
}

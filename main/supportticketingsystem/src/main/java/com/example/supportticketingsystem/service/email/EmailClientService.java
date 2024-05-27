package com.example.supportticketingsystem.service.email;

import com.example.supportticketingsystem.dto.collection.ExternalMessage;
import com.example.supportticketingsystem.dto.request.EmailDTO;
import com.example.supportticketingsystem.dto.request.MessageRequest;
import com.example.supportticketingsystem.repository.ExternalMessageRepository;
import com.example.supportticketingsystem.service.message.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailClientService {

    @Autowired
    private RestTemplate restTemplate; // Assuming RestTemplate is configured in your project
    @Autowired
    private ExternalMessageRepository externalMessageRepository;

    @Autowired
    private final MessageService messageService;

    private static final Pattern TICKET_ID_PATTERN = Pattern.compile("Support Ticket -(\\d+)");

    public EmailClientService(RestTemplate restTemplate, ExternalMessageRepository externalMessageRepository, MessageService messageService) {
        this.restTemplate = restTemplate;
        this.externalMessageRepository = externalMessageRepository;
        this.messageService = messageService;
    }


    public List<EmailDTO> getEmailsFrom8086() throws MessagingException, IOException {
        String url = "http://localhost:8086/emails";

        // Make HTTP GET request to the endpoint and retrieve the response
        ResponseEntity<List<Map<String, Object>>> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}); // Specify the type

        List<EmailDTO> emails = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (Map<String, Object> emailMap : responseEntity.getBody()) {
            // Convert LinkedHashMap to EmailDTO using ObjectMapper
            EmailDTO emailDTO = objectMapper.convertValue(emailMap, EmailDTO.class);
            emails.add(emailDTO);
        }
        for (EmailDTO email : emails) {
            Long ticketId = extractTicketId(email.getSubject());
            if (ticketId != null) {
                ExternalMessage existingMessage = externalMessageRepository.findByTicketId(ticketId);
                if (existingMessage == null || email.getReceivedDate().after(existingMessage.getTime())) {
                    // Convert java.util.Date to java.sql.Timestamp
                    Timestamp receivedDate = new Timestamp(email.getReceivedDate().getTime());
                    createExternalMessage(receivedDate, ticketId);
                }
            }
        }
        return emails;
    }

    private Long extractTicketId(String subject) {
        Matcher matcher = TICKET_ID_PATTERN.matcher(subject);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }

    private void createExternalMessage(Timestamp time, Long ticketId) throws MessagingException, IOException {
        ExternalMessage externalMessage = new ExternalMessage();
        externalMessage.setTime(time);
        externalMessage.setTicketId(ticketId);
        externalMessageRepository.save(externalMessage);

        List<EmailDTO> emails = restTemplate.getForObject("http://localhost:8086/emails", List.class);
        for (EmailDTO email : emails) {
            Long extractedTicketId = extractTicketId(email.getSubject());
            if (extractedTicketId != null && extractedTicketId.equals(ticketId)) {
                MessageRequest request = new MessageRequest();
                // Assuming you have appropriate methods to set these properties in MessageRequest
                request.setAttachments(null);
                request.setContent(email.getBody());
                request.setSender(request.getSender());
                request.setCcEmailAddresses(null);
                request.setSentBy(email.getTo());

                messageService.createExternalMessage(request, ticketId);
            }
        }
    }

}


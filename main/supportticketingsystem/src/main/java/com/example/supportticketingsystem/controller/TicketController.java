package com.example.supportticketingsystem.controller;

import com.example.supportticketingsystem.dto.collection.Ticket;
import com.example.supportticketingsystem.dto.response.MessageResponse;
import com.example.supportticketingsystem.dto.response.TRes;
import com.example.supportticketingsystem.enums.*;
import com.example.supportticketingsystem.dto.collection.MessageAttachment;
import com.example.supportticketingsystem.dto.exception.TicketNotFoundException;
import com.example.supportticketingsystem.dto.request.CloseTicketRequest;
import com.example.supportticketingsystem.dto.request.MessageRequest;
import com.example.supportticketingsystem.dto.request.ReopenTicketRequest;
import com.example.supportticketingsystem.dto.request.TicketRequest;
import com.example.supportticketingsystem.dto.response.GenericResponse;
import com.example.supportticketingsystem.dto.response.TicketResponse;
import com.example.supportticketingsystem.repository.MessageAttachmentRepository;
import com.example.supportticketingsystem.service.message.MessageService;
import com.example.supportticketingsystem.service.ticket.DurationService;
import com.example.supportticketingsystem.service.ticket.TicketService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    private final TicketService ticketService;


    private final DurationService durationService;

    private final MessageService messageService;

    private final MessageAttachmentRepository attachmentRepository;

    @Autowired
    public TicketController(TicketService ticketService, DurationService durationService, MessageService messageService, MessageAttachmentRepository attachmentRepository) {
        this.ticketService = ticketService;
        this.durationService = durationService;
        this.messageService = messageService;
        this.attachmentRepository = attachmentRepository;
    }


        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<TicketResponse> createTicket(
                @RequestParam("emailAddress") String emailAddress,
                @RequestParam("ccEmailAddresses") List<String> ccEmailAddresses,
                @RequestParam("supportRequestType") SupportRequestType supportRequestType,
                @RequestParam("subject") String subject,
                @RequestParam("description") String description,
                @RequestParam("severity") Severity severity,
                @RequestParam("product") Product product,
                @RequestParam("installationType") InstallationType installationType,
                @RequestParam("affectedEnvironment") Environment affectedEnvironment,
                @RequestParam("platformVersion") String platformVersion,
                @RequestParam("attachments") List<MultipartFile> attachments
        ) {
            try {
                TicketRequest request = TicketRequest.builder()
                        .emailAddress(emailAddress)
                        .ccEmailAddresses(ccEmailAddresses)
                        .supportRequestType(supportRequestType)
                        .subject(subject)
                        .description(description)
                        .severity(severity)
                        .product(product)
                        .installationType(installationType)
                        .affectedEnvironment(affectedEnvironment)
                        .platformVersion(platformVersion)
                        .attachments(attachments.stream().map(MultipartFile::getOriginalFilename).collect(Collectors.toList()))
                        .build();

                TicketResponse createdTicket = ticketService.createTicket(request, attachments);
                return new ResponseEntity<>(createdTicket, HttpStatus.CREATED);
            } catch (MessagingException | IOException e) {
                // Handle the exception here (e.g., log the error, return an error response)
                System.out.println("Error creating ticket: " + e.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }


    @PostMapping("/{ticketId}/CreateMessage")
    public ResponseEntity<?> createMessage(@PathVariable Long ticketId,
                                           @RequestParam("sender") MessageType sender,
                                           @RequestParam("content") String content,
                                           @RequestParam("attachments") List<MultipartFile> attachments,
                                           @RequestParam("sentBy") String sentBy,
                                           @RequestParam("ccEmailAddresses") List<String> ccEmailAddresses) {
        try {
            MessageRequest messageRequest = MessageRequest.builder()
                    .sender(sender)
                    .content(content)
                    .attachments(attachments)
                    .sentBy(sentBy)
                    .ccEmailAddresses(ccEmailAddresses)
                    .build();

            messageService.createMessage(messageRequest, ticketId);
            return ResponseEntity.ok().build();
        } catch (TicketNotFoundException | IOException e) {
            return ResponseEntity.notFound().build();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    @GetMapping("getFileById/{fileId}")
    public ResponseEntity<?> downloadFile(@PathVariable Long fileId) {
        byte[] fileData = ticketService.downloadFile(fileId);
        Optional<MessageAttachment> atc = attachmentRepository.findById(fileId);
        MessageAttachment attachment = atc.get();
        if (fileData != null) {
            String fileExtension = attachment.getFileExtension().toLowerCase(); // Convert extension to lowercase for uniformity
            String mimeType;

            // Set MIME type based on file extension
            if (fileExtension.equals("pdf")) {
                mimeType = "application/pdf";
            } else if (fileExtension.equals("jpg") || fileExtension.equals("jpeg")) {
                mimeType = "image/jpeg";
            } else if (fileExtension.equals("png")) {
                mimeType = "image/png";
            } else if (fileExtension.equals("doc") || fileExtension.equals("docx")) {
                mimeType = "application/msword";
            } else if (fileExtension.equals("txt")) {
                mimeType = "text/plain";
            } else if (fileExtension.equals("xml")) {
                mimeType = "application/xml";
            } else {
                // For unknown file types, set a generic MIME type
                mimeType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(fileData);
        } else {
            return ResponseEntity.notFound().build();
        }
    }



    @GetMapping("/ticket/{ticketId}")
    public List<String> getAttachmentLinksByTicketId(@PathVariable Long ticketId) {
        List<MessageAttachment> attachments = attachmentRepository.findByTicketId(ticketId);
        List<String> attachmentLinks = new ArrayList<>();

        for (MessageAttachment attachment : attachments) {
            String attachmentLink = "http://localhost:8085/api/attachments/file/" + attachment.getId();
            attachmentLinks.add(attachmentLink);
        }

        return attachmentLinks;
    }


    @GetMapping("/getTime/{ticketId}/open-duration")
    public ResponseEntity<String> getOpenDuration(@PathVariable Long ticketId) {

        LocalDateTime endTime= ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime();
        String openDuration = durationService.calculateOpenDuration(ticketId, endTime);
        return ResponseEntity.ok("Open duration for ticket " + ticketId + " is " + openDuration);
    }



    @PutMapping("/{ticketId}/closeTicket")
    public ResponseEntity<?> closeTicket(@PathVariable Long ticketId, @RequestBody CloseTicketRequest closeTicketRequest) {
        try {
            String sentBy = closeTicketRequest.getSentBy();
            ticketService.closeTicket(ticketId, sentBy);
            return ResponseEntity.ok().build();
        } catch (TicketNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }


    @PostMapping("/reopen")
    public ResponseEntity<GenericResponse> reopenTicket(@RequestBody ReopenTicketRequest request) {
        try {
            ticketService.reopenTicket(request);
            return ResponseEntity.ok(new GenericResponse("Ticket reopened successfully"));
        } catch (MessagingException e) {
            // Handle the exception here (e.g., log the error, return an error response)
            // You can customize this based on your needs
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new GenericResponse("Failed to reopen ticket"));
        }
    }

    @GetMapping("/{ticketId}/open-duration/{attempt}")
    public ResponseEntity<String> getOpenDurationByAttempt(@PathVariable Long ticketId, @PathVariable int attempt) {
        LocalDateTime endTime = ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime();
        String openDuration = durationService.calculateOpenDuration(ticketId, attempt, endTime);
        return ResponseEntity.ok("Open duration for ticket " + ticketId + " at attempt " + attempt + " is " + openDuration);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<TRes>> getAllTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ticketResponses);
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TRes> getTicketById(@PathVariable Long ticketId) {
        Optional<Ticket> ticket = ticketService.getTicketById(ticketId);
        return ticket.map(value -> ResponseEntity.ok(convertToResponse(value)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private TRes convertToResponse(Ticket ticket) {
        return TRes.builder()
                .id(ticket.getId())
                .createdAt(ticket.getCreatedAt())
                .emailAddress(ticket.getEmailAddress())
                .ccEmailAddresses(ticket.getCcEmailAddresses())
                .supportRequestType(ticket.getSupportRequestType())
                .subject(ticket.getSubject())
                .description(ticket.getDescription())
                .severity(ticket.getSeverity())
                .product(ticket.getProduct())
                .installationType(ticket.getInstallationType())
                .affectedEnvironment(ticket.getAffectedEnvironment())
                .platformVersion(ticket.getPlatformVersion())
                .clientStatus(ticket.getClientStatus())
                .vendorStatus(ticket.getVendorStatus())
                .reopenReason(ticket.getReopenReason())
                .build();
    }

    @GetMapping("/getAllMessagesByTicketId/{ticketId}")
    public ResponseEntity<List<MessageResponse>> getMessagesByTicketId(@PathVariable Long ticketId) {
        List<MessageResponse> messages = messageService.getMessagesByTicketId(ticketId);
        return ResponseEntity.ok(messages);
    }

}
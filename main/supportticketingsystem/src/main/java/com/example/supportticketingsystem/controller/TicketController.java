package com.example.supportticketingsystem.controller;

import com.example.supportticketingsystem.dto.collection.OurUsers;
import com.example.supportticketingsystem.dto.collection.Product;
import com.example.supportticketingsystem.dto.collection.Ticket;
import com.example.supportticketingsystem.dto.request.*;
import com.example.supportticketingsystem.dto.response.MessageResponse;
import com.example.supportticketingsystem.dto.response.TRes;
import com.example.supportticketingsystem.enums.*;
import com.example.supportticketingsystem.dto.collection.MessageAttachment;
import com.example.supportticketingsystem.dto.exception.TicketNotFoundException;
import com.example.supportticketingsystem.dto.response.GenericResponse;
import com.example.supportticketingsystem.dto.response.TicketResponse;
import com.example.supportticketingsystem.repository.MessageAttachmentRepository;
import com.example.supportticketingsystem.repository.ProductRepository;
import com.example.supportticketingsystem.repository.TicketRepository;
import com.example.supportticketingsystem.repository.UsersRepo;
import com.example.supportticketingsystem.service.message.MessageService;
import com.example.supportticketingsystem.service.ticket.DurationService;
import com.example.supportticketingsystem.service.ticket.TicketService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tickets")
@CrossOrigin(origins = "*")
public class TicketController {

    private final TicketService ticketService;

    private final UsersRepo usersRepo;

    private final TicketRepository ticketRepository;

    private final DurationService durationService;

    private final MessageService messageService;

    private final MessageAttachmentRepository attachmentRepository;

    private final ProductRepository productRepository;

    @Autowired
    public TicketController(TicketService ticketService, UsersRepo usersRepo, TicketRepository ticketRepository, DurationService durationService, MessageService messageService, MessageAttachmentRepository attachmentRepository, ProductRepository productRepository) {
        this.ticketService = ticketService;
        this.usersRepo = usersRepo;
        this.ticketRepository = ticketRepository;
        this.durationService = durationService;
        this.messageService = messageService;
        this.attachmentRepository = attachmentRepository;
        this.productRepository = productRepository;
    }


        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<TicketResponse> createTicket(
                @RequestParam("emailAddress") String emailAddress,
                @RequestParam("ccEmailAddresses") List<String> ccEmailAddresses,
                @RequestParam("supportRequestType") SupportRequestType supportRequestType,
                @RequestParam("subject") String subject,
                @RequestParam("description") String description,
                @RequestParam("severity") Severity severity,
                @RequestParam("product") String product,
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


    @PostMapping("/general/{ticketId}/CreateMessage")
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


    @GetMapping("/general/getFileById/{fileId}")
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



    @GetMapping("/ticket/general/{ticketId}")
    public List<String> getAttachmentLinksByTicketId(@PathVariable Long ticketId) {
        List<MessageAttachment> attachments = attachmentRepository.findByTicketId(ticketId);
        List<String> attachmentLinks = new ArrayList<>();

        for (MessageAttachment attachment : attachments) {
            String attachmentLink = "http://localhost:8085/api/attachments/file/" + attachment.getId();
            attachmentLinks.add(attachmentLink);
        }

        return attachmentLinks;
    }


    @GetMapping("/general/getTime/{ticketId}/open-duration")
    public ResponseEntity<String> getOpenDuration(@PathVariable Long ticketId) {

        LocalDateTime endTime= ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime();
        String openDuration = durationService.calculateOpenDuration(ticketId, endTime);
        return ResponseEntity.ok("Open duration for ticket " + ticketId + " is " + openDuration);
    }



    @PutMapping("/general/{ticketId}/closeTicket")
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


    @PostMapping("/general/reopen")
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

    @GetMapping("/general/{ticketId}/open-duration/{attempt}")
    public ResponseEntity<Map<String, String>> getOpenDurationByAttempt(@PathVariable Long ticketId, @PathVariable int attempt) {
        LocalDateTime endTime = ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime();
        Map<String, String> openDuration = durationService.calculateOpenDuration(ticketId, attempt, endTime);
        return ResponseEntity.ok(openDuration);
    }


    @GetMapping("/general/getAll")
    public ResponseEntity<List<TRes>> getAllTickets() {
        List<Ticket> tickets = ticketService.getAllTickets();
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ticketResponses);
    }

    @GetMapping("/getAllKore")
    public ResponseEntity<List<TRes>> getAllTicketsByProduct(Principal principal) {
        String email = principal.getName();
        Optional<OurUsers> userOpt = usersRepo.findByEmail(email);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        OurUsers user = userOpt.get();
        Set<String> productGroups = user.getProductGroup(); // Assuming `productGroup` is a Set<String> in `OurUsers`

        if (productGroups == null || productGroups.isEmpty()) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }

        List<Ticket> allTickets = new ArrayList<>();
        for (String productGroupName : productGroups) {
            List<Ticket> tickets = ticketService.getTicketsByProduct(productGroupName);
            allTickets.addAll(tickets);
        }

        List<TRes> ticketResponses = allTickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ticketResponses);
    }


    @GetMapping("/general/{ticketId}")
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

    @GetMapping("/general/getAllMessagesByTicketId/{ticketId}")
    public ResponseEntity<List<MessageResponse>> getMessagesByTicketId(@PathVariable Long ticketId) {
        List<MessageResponse> messages = messageService.getMessagesByTicketId(ticketId);
        return ResponseEntity.ok(messages);
    }


    @GetMapping("/general/{ticketId}/clientTime/{attempt}")
    public ResponseEntity<Map<String, String>> getDurationBetweenAwaitingAndClosed(
            @PathVariable Long ticketId, @PathVariable int attempt) {
        LocalDateTime endTime = ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime();
        Map<String, String> openDuration = durationService.calculateClientOpenDuration(ticketId, attempt, endTime);
        return ResponseEntity.ok(openDuration);
    }

    @GetMapping("/general/max-attempts/{ticketId}")
    public int getMaxAttempts(@PathVariable Long ticketId) {
        return ticketService.getMaxAttemptsByTicketId(ticketId);
    }

    //http://localhost:8085/tickets/max-attempts/1 use this get method


    //sevirity based ticket filtering
    @GetMapping("/severity/{severity}")
    @PreAuthorize("hasAnyAuthority('LEVEL-1', 'LEVEL-2', 'LEVEL-3', 'LEVEL-4', 'ADMIN')")
    public ResponseEntity<List<TRes>> getTicketsBySeverity(@PathVariable Severity severity, Principal principal) {

        List<Ticket> tickets = ticketService.getTicketsBySeverity(severity);
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        // Filter tickets based on the user's role
        List<TRes> filteredTickets = ticketResponses.stream()
                .filter(ticket -> canUserAccessTicket(ticket, principal))
                .collect(Collectors.toList());

        return ResponseEntity.ok(filteredTickets);
    }

    private boolean canUserAccessTicket(TRes ticket, Principal principal) {
        Collection<? extends GrantedAuthority> authorities = ((Authentication) principal).getAuthorities();
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            switch (role) {
                case "LEVEL-1":
                    if (ticket.getSeverity() == Severity.SEVERITY_1) {
                        return true;
                    }
                    break;
                case "LEVEL-2":
                    if (ticket.getSeverity() == Severity.SEVERITY_1 || ticket.getSeverity() == Severity.SEVERITY_2) {
                        return true;
                    }
                    break;
                case "LEVEL-3":
                    if (ticket.getSeverity() == Severity.SEVERITY_1 || ticket.getSeverity() == Severity.SEVERITY_2 ||
                            ticket.getSeverity() == Severity.SEVERITY_3) {
                        return true;
                    }
                    break;
                case "LEVEL-4":
                    if (ticket.getSeverity() == Severity.SEVERITY_1 || ticket.getSeverity() == Severity.SEVERITY_2 ||
                            ticket.getSeverity() == Severity.SEVERITY_3 || ticket.getSeverity() == Severity.SEVERITY_4) {
                        return true;
                    }
                    break;
                case "ADMIN":
                    // Admin has access to all severity levels
                    if (ticket.getSeverity() == Severity.SEVERITY_1 || ticket.getSeverity() == Severity.SEVERITY_2 ||
                            ticket.getSeverity() == Severity.SEVERITY_3 || ticket.getSeverity() == Severity.SEVERITY_4) {
                        return true;
                    }
                    break;
                default:
                    // Do nothing, continue checking other roles
                    break;
            }
        }
        return false; // If no matching role is found
    }


    @GetMapping("/user/getAllTicketsByUser")
    public ResponseEntity<List<TRes>> getTicketsForUser(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        List<Ticket> tickets = ticketService.getTicketsByEmailAddress(email);
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ticketResponses);
    }

    @GetMapping("/by-cc-email")
    public ResponseEntity<List<TRes>> getTicketsByCcEmail(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();

        List<Ticket> tickets = ticketService.getTicketsByCcEmail(email);
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ticketResponses);
    }


    @GetMapping("/waiting-times")
    public ResponseEntity<List<Map<String, String>>> getWaitingTimes(
            @RequestParam("startMonth") String startMonth,
            @RequestParam("endMonth") String endMonth) {
        List<Map<String, String>> waitingTimes = durationService.getTicketsWithinDateRange(startMonth, endMonth);
        return ResponseEntity.ok(waitingTimes);
    }

    @GetMapping("/severity-count")
    public ResponseEntity<List<Map<String, Object>>> getSeverityCount(
            @RequestParam String startMonth,
            @RequestParam String endMonth) {
        Integer start = Integer.parseInt(startMonth.replace("-", ""));
        Integer end = Integer.parseInt(endMonth.replace("-", ""));
        List<SeverityCountDTO> severityCounts = ticketRepository.getSeverityCount(start, end);

        Map<String, Map<String, Object>> groupedByMonthYear = new HashMap<>();

        for (SeverityCountDTO dto : severityCounts) {
            String monthYearKey = String.format("%s %d", Month.of(dto.getMonth()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH), dto.getYear());

            groupedByMonthYear.computeIfAbsent(monthYearKey, k -> {
                Map<String, Object> map = new HashMap<>();
                map.put("year", monthYearKey);
                map.put("severity1", 0L); // Ensure these are Long types
                map.put("severity2", 0L); // Ensure these are Long types
                map.put("severity3", 0L); // Ensure these are Long types
                map.put("severity4", 0L); // Ensure these are Long types
                return map;
            });

            Map<String, Object> monthYearData = groupedByMonthYear.get(monthYearKey);
            String severityKey = String.format("severity%d", Integer.parseInt(dto.getSeverity().substring(dto.getSeverity().length() - 1)));
            monthYearData.put(severityKey, (Long) monthYearData.get(severityKey) + dto.getCount());
        }

        List<Map<String, Object>> result = groupedByMonthYear.values().stream().collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/general/searchByTicketId/{ticketId}")
    public ResponseEntity<List<TRes>> getTicketsByTicketIdContaining(@PathVariable String ticketId) {
        List<Ticket> tickets = ticketService.getTicketsByTicketIdContaining(ticketId);
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ticketResponses);
    }



    @GetMapping("/searchTicketsByUser/{ticketId}")
    public ResponseEntity<List<TRes>> getTicketsByUserAndTicketIdContaining(Authentication authentication, @PathVariable String ticketId) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        List<Ticket> tickets = ticketService.getTicketsByUserAndTicketIdContaining(email, ticketId);
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ticketResponses);
    }

    @GetMapping("/general/searchByTicketSubject/{subject}")
    public ResponseEntity<List<TRes>> getTicketsByTicketSubjectContaining(@PathVariable String subject) {
        List<Ticket> tickets = ticketService.getTicketsByTicketSubjectContaining(subject);
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ticketResponses);
    }

    @GetMapping("/searchTicketsByUserSubject/{subject}")
    public ResponseEntity<List<TRes>> getTicketsByUserAndTicketSubjectContaining(Authentication authentication, @PathVariable String subject) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername();
        List<Ticket> tickets = ticketService.getTicketsByUserAndTicketSubjectContaining(email, subject);
        List<TRes> ticketResponses = tickets.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ticketResponses);
    }

    @PutMapping("/general/{ticketId}/solve")
    public ResponseEntity<String> markTicketAsSolved(@PathVariable Long ticketId) {
        try {
            durationService.markTicketAsSolved(ticketId);
            return ResponseEntity.ok("Tickets with ID " + ticketId + " marked as solved.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
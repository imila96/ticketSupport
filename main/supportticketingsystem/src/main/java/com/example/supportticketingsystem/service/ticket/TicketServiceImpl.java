package com.example.supportticketingsystem.service.ticket;

import com.example.supportticketingsystem.dto.collection.*;
import com.example.supportticketingsystem.dto.exception.TicketNotFoundException;
import com.example.supportticketingsystem.dto.request.ReopenTicketRequest;
import com.example.supportticketingsystem.dto.request.TicketRequest;
import com.example.supportticketingsystem.dto.response.TicketResponse;
import com.example.supportticketingsystem.enums.MessageType;
import com.example.supportticketingsystem.enums.Severity;
import com.example.supportticketingsystem.enums.Status;
import com.example.supportticketingsystem.repository.*;
import com.example.supportticketingsystem.service.email.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TicketServiceImpl implements TicketService {



    private final TicketRepository ticketRepository;

    private final DurationTimeRepository durationTimeRepository;

    private final MessageAttachmentRepository messageAttachmentRepository;

    private final MessageRepository messageRepository;
    private final EmailService emailService;

    private final EmailTimeRepository emailTimeRepository;

    @Value("${email.support.recipient}")
    private String emailRecipient;

    @Autowired
    public TicketServiceImpl(TicketRepository ticketRepository, DurationTimeRepository durationTimeRepository, MessageAttachmentRepository messageAttachmentRepository, MessageRepository messageRepository, EmailService emailService, EmailTimeRepository emailTimeRepository) {

        this.ticketRepository = ticketRepository;
        this.durationTimeRepository = durationTimeRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.messageRepository = messageRepository;
        this.emailService = emailService;
        this.emailTimeRepository = emailTimeRepository;
    }

    @Override
    public TicketResponse createTicket(TicketRequest request, List<MultipartFile> attachments) throws MessagingException, IOException {

        List<File> files = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();
        List<String> fileExtensions = new ArrayList<>();
        List<byte[]> fileBytesList = new ArrayList<>();

        boolean hasValidAttachment = false; // Flag to check if there is at least one valid attachment

        if (attachments != null) {
            for (MultipartFile attachment : attachments) {
                String fileName = attachment.getOriginalFilename();
                String contentType = attachment.getContentType();
                long size = attachment.getSize();

                // Validate attachment
                if (fileName == null || fileName.isEmpty() || "empty.json".equals(fileName) || contentType == null || size == 0) {
                    System.out.println("Invalid attachment found and skipped: " + fileName);
                    continue; // Skip this invalid attachment
                }

                hasValidAttachment = true;
                fileNames.add(fileName);
                fileExtensions.add(fileName.substring(fileName.lastIndexOf(".") + 1));
                fileBytesList.add(attachment.getBytes());
            }
        }

        // 1. Save the Ticket first
        Ticket ticket = Ticket.builder()
                .emailAddress(request.getEmailAddress())
                .ccEmailAddresses(request.getCcEmailAddresses())
                .supportRequestType(request.getSupportRequestType())
                .subject(request.getSubject())
                .description(request.getDescription())
                .severity(request.getSeverity())
                .product(request.getProduct())
                .installationType(request.getInstallationType())
                .affectedEnvironment(request.getAffectedEnvironment())
                .platformVersion(request.getPlatformVersion())
                .clientStatus(Status.OPEN)
                .vendorStatus(Status.AWAITING_REPLY)
                .build();

        ticket = ticketRepository.save(ticket); // Persist the Ticket

        DurationTime durationTime = DurationTime.builder()
                .ticketId(ticket.getId())
                .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                .status("AWAITING")
                .attempts(1)
                .build();

        durationTimeRepository.save(durationTime);

        ticket.setSubject(request.getSubject() + " : Support Ticket -" + ticket.getId());
        ticket = ticketRepository.save(ticket);

        String initialMessageId = generateUniqueMessageId(ticket.getId());
        ticket.setInitialMessageId(initialMessageId);
        ticket = ticketRepository.save(ticket);

        EmailTime emailTime = EmailTime.builder()
                .ticketId(ticket.getId())
                .createdAt(null)
                .build();

        emailTimeRepository.save(emailTime);

        // 2. Create Message with the saved Ticket object
        Message initialMessageBuilder = Message.builder()
                .ticket(ticket) // Set the saved ticket object here
                .sender(MessageType.CLIENT)
                .ccEmailAddresses(request.getCcEmailAddresses())
                .content(request.getDescription())
                .createdAt(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                .sentBy(request.getEmailAddress())
                .build();

        Message initialMessage = messageRepository.save(initialMessageBuilder);
        String uniqueId = ticket.getId() + "_" + initialMessage.getId();
        initialMessage.setUniqueId(uniqueId);

        List<Message> conversation = ticket.getConversation() == null ? new ArrayList<>() : ticket.getConversation();
        conversation.add(initialMessage); // Add initial message to conversation

        List<MessageAttachment> attachmentsList = new ArrayList<>();
        if (fileBytesList.size() > 0) {
            for (int i = 0; i < fileBytesList.size(); i++) {
                MessageAttachment attachment = MessageAttachment.builder()
                        .name(fileNames.get(i))
                        .fileExtension(fileExtensions.get(i))
                        .fileBytes(fileBytesList.get(i))
                        .message(initialMessage) // Set the saved message object here
                        .build();
                attachmentsList.add(attachment);
            }
            initialMessage.setAttachments(attachmentsList);
        }

        ticket.setConversation(conversation); // Set the updated conversation list

        messageRepository.save(initialMessage);

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("Sent by: ").append(request.getEmailAddress()).append("\n\n");
        bodyBuilder.append("Unique Id : ").append(initialMessage.getUniqueId()).append("\n\n");
        bodyBuilder.append("Support Request Type: ").append(request.getSupportRequestType()).append("\n");
        bodyBuilder.append("Description: ").append(request.getDescription()).append("\n");
        bodyBuilder.append("Severity: ").append(request.getSeverity()).append("\n");
        bodyBuilder.append("Product: ").append(request.getProduct()).append("\n");
        bodyBuilder.append("Installation Type: ").append(request.getInstallationType()).append("\n");
        bodyBuilder.append("Affected Environment: ").append(request.getAffectedEnvironment()).append("\n");
        bodyBuilder.append("Platform Version: ").append(request.getPlatformVersion()).append("\n");

        String emailBody = bodyBuilder.toString();

        List<String> ccEmails = new ArrayList<>();
        if (request.getCcEmailAddresses() != null) {
            // Remove extra characters from each email address
            ccEmails = request.getCcEmailAddresses().stream()
                    .map(email -> email.replaceAll("[\\[\\]\"]", ""))
                    .collect(Collectors.toList());
        }

        ccEmails.add(request.getEmailAddress()); // Add primary recipient to CC list

        // Check for single invalid attachment scenario
        if (attachments.size() == 1 && !hasValidAttachment) {
            emailService.sendEmail(
                    request.getEmailAddress(),
                    ticket.getSubject(),
                    emailBody,
                    ccEmails,
                    initialMessageId
            );
        } else if (!attachments.isEmpty() && hasValidAttachment) {
            emailService.sendMailWithAttachment(
                    request.getEmailAddress(),
                    ticket.getSubject(),
                    emailBody,
                    ccEmails,
                    fileNames,
                    fileBytesList,
                    initialMessageId
            );
        } else {
            emailService.sendEmail(
                    request.getEmailAddress(),
                    ticket.getSubject(),
                    emailBody,
                    ccEmails,
                    initialMessageId
            );
        }

        ticket = ticketRepository.save(ticket);

        return TicketResponse.builder()
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
                .build();
    }



    @Override
        public byte[] downloadFile(Long fileId) {
            Optional<MessageAttachment> dbFileData = messageAttachmentRepository.findById(fileId);
            return dbFileData.map(MessageAttachment::getFileBytes).orElse(null);
        }


    public String generateUniqueMessageId(long ticketId) {
        // Generate a unique UUID
        UUID uuid = UUID.randomUUID();

        // Get current timestamp
        Instant timestamp = Instant.now();

        // Combine ticket ID, UUID, and timestamp to form a unique Message-ID
        String messageId = String.format("<%d-%s-%d@example.com>", ticketId, uuid.toString(), timestamp.toEpochMilli());

        return messageId;
    }


    @Override
    public void closeTicket(Long ticketId, String sentBy) throws MessagingException {
        // Retrieve the ticket from the database
        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);
        if (optionalTicket.isPresent()) {
            Ticket ticket = optionalTicket.get();

            // Update the status for both client and vendor
            ticket.setClientStatus(Status.CLOSED);
            ticket.setVendorStatus(Status.CLOSED);
            ticketRepository.save(ticket); // Save the updated ticket

            // Send email notification to inform about the status change
            String initialMessageId = ticket.getInitialMessageId();
            String userEmail = "Unknown";
            if (sentBy.equals(ticket.getEmailAddress())) {
                // If sent by client
                userEmail = ticket.getEmailAddress();
            } else {
                // If sent by vendor
                userEmail = "Vendor";
            }
            String body = userEmail + " closed the ticket : "+ticket.getId();

            List<String> ccEmails = new ArrayList<>();
            if (ticket.getCcEmailAddresses() != null) {
                // Remove extra characters from each email address
                ccEmails = ticket.getCcEmailAddresses().stream()
                        .map(email -> email.replaceAll("[\\[\\]\"]", ""))
                        .collect(Collectors.toList());
            }
            ccEmails.add(ticket.getEmailAddress());
            // Save the message
            Message messageBuilder = Message.builder()
                    .ticket(ticket)
                    .sender(sentBy.equals(ticket.getEmailAddress()) ? MessageType.CLIENT : MessageType.VENDOR)
                    .content(body)
                    .createdAt(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                    .sentBy(sentBy)
                    .ccEmailAddresses(ccEmails)
                    .build();

            Message message = messageRepository.save(messageBuilder);
            String uniqueId = ticketId + "_" + message.getId();
            message.setUniqueId(uniqueId);
            messageRepository.save(message);

            StringBuilder bodyBuilder = new StringBuilder();

            bodyBuilder.append("Unique Id : ").append(message.getUniqueId()).append("\n\n");
            bodyBuilder.append("Content: ").append(body).append("\n");


            String emailBody = bodyBuilder.toString();



            emailService.sendEmail(
                    emailRecipient,
                    ticket.getSubject(),
                    emailBody,
                    ccEmails,
                    initialMessageId
            );


            List<DurationTime> closedDurationTimes = durationTimeRepository.findByTicketIdAndStatus(ticketId, "CLOSED");
            int attempts = closedDurationTimes.size() + 1; // Increment for the current attempt

            DurationTime durationTime= DurationTime.builder()
                    .ticketId(ticket.getId())
                    .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                    .status("CLOSED")
                    .attempts(attempts)
                    .build();

            durationTimeRepository.save(durationTime);

        } else {
            throw new TicketNotFoundException("Ticket with ID " + ticketId + " not found");
        }
    }

    @Override
    public int getMaxAttemptsByTicketId(Long ticketId) {
        return durationTimeRepository.findByTicketIdOrderByTimeAsc(ticketId)
                .stream()
                .mapToInt(durationTime -> durationTime.getAttempts())
                .max()
                .orElse(0);
    }


    @Override
    public void reopenTicket(ReopenTicketRequest request) throws MessagingException {
        Long ticketId = request.getTicketId();
        String sentBy = request.getSentBy();
        String reason = request.getReason();



        // Retrieve the ticket from the database
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        String initialMessageId = ticket.getInitialMessageId();

        // Update ticket status
        ticket.setClientStatus(Status.OPEN);
        ticket.setVendorStatus(Status.AWAITING_REPLY);

        // Save the updated ticket
        ticketRepository.save(ticket);

        String body = "Ticket reopened by " + sentBy + " for reason: " + reason;


        // Save the message
        Message message = Message.builder()
                .ticket(ticket)
                .sender(MessageType.CLIENT)
                .content(body)
                .createdAt(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                .sentBy(sentBy)
                .build();

        Message messageSaved =messageRepository.save(message);


        String uniqueId = ticketId + "_" + messageSaved.getId();


        messageSaved.setUniqueId(uniqueId);
        message = messageRepository.save(messageSaved);

        StringBuilder bodyBuilder = new StringBuilder();

        bodyBuilder.append("Unique Id : ").append(message.getUniqueId()).append("\n\n");
        bodyBuilder.append("Content: ").append(body).append("\n");


        String emailBody = bodyBuilder.toString();

        List<String> ccEmails = new ArrayList<>();
        if (ticket.getCcEmailAddresses() != null) {
            // Remove extra characters from each email address
            ccEmails = ticket.getCcEmailAddresses().stream()
                    .map(email -> email.replaceAll("[\\[\\]\"]", ""))
                    .collect(Collectors.toList());
        }
        ccEmails.add(ticket.getEmailAddress());

        emailService.sendEmail(
                emailRecipient,
                ticket.getSubject(),
                emailBody,
                ccEmails,
                initialMessageId
        );

        List<DurationTime> closedDurationTimes = durationTimeRepository.findByTicketIdAndStatus(ticketId, "CLOSED");
        int attempts = closedDurationTimes.size() + 1; // Increment for the current attempt

        DurationTime durationTime= DurationTime.builder()
                .ticketId(ticket.getId())
                .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                .status("AWAITING")
                .attempts(attempts)
                .build();

        durationTimeRepository.save(durationTime);
    }

    @Override
    public List<Ticket> getAllTickets() {
        return ticketRepository.findAllByOrderByIdDesc();
    }

    @Override
    public Optional<Ticket> getTicketById(Long ticketId) {
        return ticketRepository.findById(ticketId);
    }


    // In TicketServiceImpl class
    @Override
    public List<Ticket> getTicketsBySeverity(Severity severity) {
        return ticketRepository.findBySeverity(severity);
    }

    @Override
    public List<Ticket> getTicketsByProduct(String product) {
        return ticketRepository.findByProduct(product);
    }

    @Override
    public List<Ticket> getTicketsByEmailAddress(String emailAddress) {
        return ticketRepository.findByEmailAddress(emailAddress);
    }
    @Override
    public List<Ticket> getTicketsByCcEmail(String ccEmail) {
        return ticketRepository.findByCcEmailAddressesContaining(ccEmail);
    }

    @Override
    public List<Ticket> getTicketsByTicketIdContaining(String ticketId) {
        return ticketRepository.findByTicketIdContaining(ticketId);
    }

    @Override
    public List<Ticket> getTicketsByUserAndTicketIdContaining(String emailAddress, String ticketId) {
        return ticketRepository.findByEmailAddressAndTicketIdContaining(emailAddress, ticketId);
    }
}

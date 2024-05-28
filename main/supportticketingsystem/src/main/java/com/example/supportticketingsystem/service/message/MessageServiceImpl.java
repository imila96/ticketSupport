package com.example.supportticketingsystem.service.message;

import com.example.supportticketingsystem.dto.collection.DurationTime;
import com.example.supportticketingsystem.dto.collection.Message;
import com.example.supportticketingsystem.dto.collection.MessageAttachment;
import com.example.supportticketingsystem.dto.collection.Ticket;
import com.example.supportticketingsystem.dto.exception.TicketNotFoundException;
import com.example.supportticketingsystem.dto.request.MessageRequest;
import com.example.supportticketingsystem.dto.response.MessageResponse;
import com.example.supportticketingsystem.enums.MessageType;
import com.example.supportticketingsystem.enums.Status;
import com.example.supportticketingsystem.repository.DurationTimeRepository;
import com.example.supportticketingsystem.repository.MessageAttachmentRepository;
import com.example.supportticketingsystem.repository.MessageRepository;
import com.example.supportticketingsystem.repository.TicketRepository;
import com.example.supportticketingsystem.service.attachment.AttachmentService;
import com.example.supportticketingsystem.service.email.EmailService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class MessageServiceImpl implements MessageService{


    private TicketRepository ticketRepository;
    private MessageRepository messageRepository;
    private MessageAttachmentRepository messageAttachmentRepository;

    private final AttachmentService attachmentService;
    private final DurationTimeRepository durationTimeRepository;

    private static final Logger logger = LoggerFactory.getLogger(MessageServiceImpl.class);

    @Value("${email.support.recipient}")
    private String emailRecipient;
    private final EmailService emailService;


    @Autowired
    public MessageServiceImpl(TicketRepository ticketRepository, MessageRepository messageRepository, MessageAttachmentRepository messageAttachmentRepository, AttachmentService attachmentService, DurationTimeRepository durationTimeRepository, EmailService emailService) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.attachmentService = attachmentService;
        this.durationTimeRepository = durationTimeRepository;
        this.emailService = emailService;
    }

    @Override
    public String createMessage(MessageRequest request, Long ticketId) throws IOException, MessagingException {
        List<MultipartFile> attachments = request.getAttachments();

        List<MessageAttachment> messageAttachments = new ArrayList<>();
        for (MultipartFile attachment : attachments) {
            String fileName = attachment.getOriginalFilename();
            String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
            byte[] fileBytes = attachment.getBytes();

            MessageAttachment messageAttachment = MessageAttachment.builder()
                    .name(fileName)
                    .fileExtension(fileExtension)
                    .fileBytes(fileBytes)
                    .build();

            messageAttachments.add(messageAttachment);
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));

        String initialMessageId = ticket.getInitialMessageId();

        if (request.getSender() == MessageType.CLIENT) {
            ticket.setClientStatus(Status.OPEN);
            ticket.setVendorStatus(Status.AWAITING_REPLY);
        } else if (request.getSender() == MessageType.VENDOR) {
            ticket.setClientStatus(Status.AWAITING_REPLY);
            ticket.setVendorStatus(Status.OPEN);
        }

        Message messageBuilder = Message.builder()
                .ticket(ticket)
                .sender(request.getSender())
                .content(request.getContent())
                .createdAt(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                .sentBy(request.getSentBy())
                .ccEmailAddresses(request.getCcEmailAddresses())
                .attachments(messageAttachments) // Attachments added here
                .build();

        messageBuilder.setAttachments(messageAttachments);
        Message message = messageRepository.save(messageBuilder);

        logger.info("message id: {}",message.getId());

        String uniqueId = ticketId + "_" + message.getId();

        message.setUniqueId(uniqueId);
        message = messageRepository.save(message);

        StringBuilder bodyBuilder = new StringBuilder();
        bodyBuilder.append("Sent by: ").append(request.getSentBy()).append("\n\n");
        bodyBuilder.append("Unique Id : ").append(message.getUniqueId()).append("\n\n");
        bodyBuilder.append("Content: ").append(request.getContent()).append("\n");
        bodyBuilder.append("Ticket Id: ").append(ticketId).append("\n");

        String emailBody = bodyBuilder.toString();

        List<String> ccEmails = new ArrayList<>();
        if (request.getCcEmailAddresses() != null) {
            // Remove extra characters from each email address
            ccEmails = request.getCcEmailAddresses().stream()
                    .map(email -> email.replaceAll("[\\[\\]\"]", ""))
                    .collect(Collectors.toList());
        }




        if (!attachments.isEmpty()) {
            List<String> attachmentNames = new ArrayList<>();
            List<byte[]> attachmentBytesList = new ArrayList<>();
            for (MultipartFile attachment : attachments) {
                attachmentNames.add(attachment.getOriginalFilename());
                attachmentBytesList.add(attachment.getBytes());
            }

            if (request.getSender() == MessageType.CLIENT) {
                emailService.sendMailWithAttachment(
                        emailRecipient,
                        ticket.getSubject(),
                        emailBody,
                        ccEmails,
                        attachmentNames,
                        attachmentBytesList,
                        initialMessageId
                );
            } else if (request.getSender() == MessageType.VENDOR) {
                emailService.sendMailWithAttachment(
                        ticket.getEmailAddress(),
                        ticket.getSubject(),
                        emailBody,
                        ccEmails,
                        attachmentNames,
                        attachmentBytesList,
                        initialMessageId
                );
            }
        } else {
            if (request.getSender() == MessageType.CLIENT) {
                emailService.sendEmail(
                        emailRecipient,
                        ticket.getSubject(),
                        emailBody,
                        ccEmails,
                        initialMessageId
                );
            } else if (request.getSender() == MessageType.VENDOR) {
                emailService.sendEmail(
                        ticket.getEmailAddress(),
                        ticket.getSubject(),
                        emailBody,
                        ccEmails,
                        initialMessageId
                );
            }
        }


        List<Message> conversation = ticket.getConversation();
        conversation.add(message);
        ticket.setConversation(conversation);

        Ticket savedTicket = ticketRepository.save(ticket);

        List<DurationTime> closedDurationTimes = durationTimeRepository.findByTicketIdAndStatus(ticketId, "CLOSED");
        int attempts = closedDurationTimes.size() + 1; // Increment for the current attempt

        if (savedTicket.getVendorStatus() == Status.AWAITING_REPLY) {
            DurationTime durationTime = DurationTime.builder()
                    .ticketId(savedTicket.getId())
                    .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                    .status("AWAITING")
                    .attempts(attempts)
                    .build();

            durationTimeRepository.save(durationTime);
        } else if (savedTicket.getVendorStatus() == Status.OPEN) {
            DurationTime durationTime = DurationTime.builder()
                    .ticketId(savedTicket.getId())
                    .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                    .status("OPEN")
                    .attempts(attempts)
                    .build();

            durationTimeRepository.save(durationTime);
        }

        ticketRepository.save(savedTicket);
        return "Message Created";
    }


    @Override
    public String createExternalMessage(MessageRequest request, Long ticketId) throws IOException, MessagingException {



        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id: " + ticketId));


            ticket.setClientStatus(Status.OPEN);
            ticket.setVendorStatus(Status.AWAITING_REPLY);


        Message message = Message.builder()
                .ticket(ticket)
                .sender(request.getSender())
                .content(request.getContent())
                .createdAt(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                .sentBy(request.getSentBy())
                .ccEmailAddresses(request.getCcEmailAddresses())
                .build();


        messageRepository.save(message);

        List<Message> conversation = ticket.getConversation();
        conversation.add(message);
        ticket.setConversation(conversation);

        ticketRepository.save(ticket);

        return "Message Created";
    }

    public List<MessageResponse> getMessagesByTicketId(Long ticketId) {
        List<Message> messages = messageRepository.findByTicketId(ticketId);

        return messages.stream()
                .map(message -> toMessageResponse(message, ticketId))
                .collect(Collectors.toList());
    }

    private MessageResponse toMessageResponse(Message message, Long ticketId) {
        return MessageResponse.builder()
                .id(message.getId())
                .sentBy(message.getSentBy())
                .sender(message.getSender())
                .ccEmailAddresses(message.getCcEmailAddresses())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .uniqueId(message.getUniqueId())
                .attachments(attachmentService.getAttachmentLinksByMessageId(message.getId())) // Assuming this method exists
                .build();
    }

}

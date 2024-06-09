package com.example.supportticketingsystem.service.email;

import com.example.supportticketingsystem.dto.collection.*;
import com.example.supportticketingsystem.dto.request.EmailAttachmentDTO;
import com.example.supportticketingsystem.dto.request.ModEmailDTO;
import com.example.supportticketingsystem.enums.MessageType;
import com.example.supportticketingsystem.enums.Status;
import com.example.supportticketingsystem.repository.DurationTimeRepository;
import com.example.supportticketingsystem.repository.EmailTimeRepository;
import com.example.supportticketingsystem.repository.MessageRepository;
import com.example.supportticketingsystem.repository.TicketRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ModEmailService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private EmailTimeRepository emailTimeRepository;

    @Autowired
    private DurationTimeRepository durationTimeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private static final Logger logger = LoggerFactory.getLogger(ModEmailService.class);

    @Autowired
    private RestTemplate restTemplate;




    @Scheduled(fixedRate = 60000)
    @Transactional
    public void saveEmailsFromExternalService() {
        System.out.println("attept started");
        int minutes=1800;
        String url = "http://localhost:8086/emails/" + minutes;
        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                url, HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        List<Map<String, Object>> emailMaps = response.getBody();

        for (Map<String, Object> emailMap : emailMaps) {
            ModEmailDTO email = convertMapToEmail(emailMap);

            Long ticketId = extractTicketIdFromSubject(email.getSubject());
            Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);

            Message message = convertEmailToMessage(email);

            logger.info("unique id of this: {}", message.getUniqueId());
            logger.info("is this unique: {}", !isUniqueIdExists(message.getUniqueId()));

            List<DurationTime> closedDurationTimes = durationTimeRepository.findByTicketIdAndStatus(ticketId, "CLOSED");
            int attempts = closedDurationTimes.size() + 1; // Increment for the current attempt

            if (!isUniqueIdExists(message.getUniqueId()) && !message.getUniqueId().equals("Unknown")) {
                if (optionalTicket.isPresent()) {
                    logger.info("system messages");

                    Ticket ticket = optionalTicket.get();

                    LocalDateTime emailReceivedDateTime = convertToDateTime((Date) email.getReceivedDate());

                    LocalDateTime latestReceivedDate = getLatestReceivedDateForTicket(ticketId);

                    logger.info("Latest received date for ticket {}: {}", ticketId, latestReceivedDate != null ? latestReceivedDate : "No emails found for this ticket");
                    logger.info("Received date of current email: {}", emailReceivedDateTime);

                    if (latestReceivedDate == null || emailReceivedDateTime.isAfter(latestReceivedDate)) {
                        message.setTicket(ticket);
                        if (message.getSender() == MessageType.VENDOR) {
                            ticket.setClientStatus(Status.AWAITING_REPLY);
                            ticket.setVendorStatus(Status.OPEN);

                            DurationTime durationTime = DurationTime.builder()
                                    .ticketId(ticket.getId())
                                    .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                                    .status("OPEN")
                                    .attempts(attempts)
                                    .build();

                            durationTimeRepository.save(durationTime);

                        } else if (message.getSender() == MessageType.CLIENT) {
                            ticket.setVendorStatus(Status.AWAITING_REPLY);
                            ticket.setClientStatus(Status.OPEN);

                            DurationTime durationTime = DurationTime.builder()
                                    .ticketId(ticket.getId())
                                    .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                                    .attempts(attempts)
                                    .status("AWAITING")
                                    .build();

                            durationTimeRepository.save(durationTime);
                        }

                        ticket.getConversation().add(message);
                        ticketRepository.save(ticket);
                    }

                } else {
                    logger.error("Ticket with ID {} not found", ticketId);
                }
            } else if (message.getUniqueId().equals("Unknown")) {

                if (optionalTicket.isPresent()) {
                    logger.info("external email messages");

                    Ticket ticket = optionalTicket.get();
                    LocalDateTime emailReceivedDateTime = convertToDateTime((Date) email.getReceivedDate());

                    LocalDateTime latestReceivedDate = getLatestReceivedDateForMessageTicket(ticketId);

                    logger.info("Latest received date for ticket {}: {}", ticketId, latestReceivedDate != null ? latestReceivedDate : "No emails found for this ticket");
                    logger.info("Received date of current email: {}", emailReceivedDateTime);

                    if (latestReceivedDate == null || emailReceivedDateTime.isAfter(latestReceivedDate)) {
                        logger.info("latest received date-in db-external mail: {}", latestReceivedDate);
                        logger.info("latest received date-in email-external mail: {}", emailReceivedDateTime);

                        logger.info("ticket id: {}", ticketId);

                        EmailTime emailTime = emailTimeRepository.findByTicketId(ticketId);
                        emailTime.setTicketId(ticketId);
                        emailTime.setCreatedAt(emailReceivedDateTime);

                        emailTimeRepository.save(emailTime);

                        message.setTicket(ticket);
                        if (message.getSender() == MessageType.VENDOR) {
                            ticket.setClientStatus(Status.AWAITING_REPLY);
                            ticket.setVendorStatus(Status.OPEN);

                            DurationTime durationTime = DurationTime.builder()
                                    .ticketId(ticket.getId())
                                    .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                                    .status("OPEN")
                                    .attempts(attempts)
                                    .build();
                            durationTimeRepository.save(durationTime);

                        } else if (message.getSender() == MessageType.CLIENT) {
                            ticket.setVendorStatus(Status.AWAITING_REPLY);
                            ticket.setClientStatus(Status.OPEN);

                            DurationTime durationTime = DurationTime.builder()
                                    .ticketId(ticket.getId())
                                    .time(ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime())
                                    .status("AWAITING")
                                    .attempts(attempts)
                                    .build();
                            durationTimeRepository.save(durationTime);
                        }

                        ticket.getConversation().add(message);
                        ticketRepository.save(ticket);
                    }

                } else {
                    logger.error("Ticket with ID {} not found", ticketId);
                }
            }
        }
        System.out.println("attept completed");
    }

    private LocalDateTime getLatestReceivedDateForTicket(Long ticketId) {
        List<Message> existingMessages = messageRepository.findByTicketId(ticketId);
        if (!existingMessages.isEmpty()) {
            LocalDateTime latestReceivedDate = null;
            for (Message message : existingMessages) {
                if (latestReceivedDate == null || message.getCreatedAt().isAfter(latestReceivedDate)) {
                    latestReceivedDate = message.getCreatedAt();
                }
            }
            return latestReceivedDate;
        }
        return null;
    }

    private ModEmailDTO convertMapToEmail(Map<String, Object> emailMap) {
        ModEmailDTO email = new ModEmailDTO();
        email.setSubject((String) emailMap.get("subject"));
        email.setFrom((String) emailMap.get("from"));
        email.setTo((String) emailMap.get("to"));
        email.setCc((String) emailMap.get("cc"));

        String content = (String) emailMap.get("body");
        String originalBody = extractOriginalBody(content);
        email.setBody(originalBody);

        String receivedDateStr = (String) emailMap.get("receivedDate");
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
            java.util.Date parsedDate = dateFormat.parse(receivedDateStr);
            email.setReceivedDate(new java.sql.Date(parsedDate.getTime()));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        email.setTicketId((Long) emailMap.get("ticketId"));

        List<EmailAttachmentDTO> attachments = convertAttachmentMapsToEmailAttachments((List<Map<String, Object>>) emailMap.get("emailAttachmentDTOList"));
        email.setEmailAttachmentDTOList(attachments);

        return email;
    }

    private String extractOriginalBody(String content) {
        String TIMESTAMP_PATTERN = "On\\s+(Mon|Tue|Wed|Thu|Fri|Sat|Sun),\\s+\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\s+\\d{4}\\s+at\\s+\\d{1,2}:\\d{2}";
        Pattern pattern = Pattern.compile(TIMESTAMP_PATTERN);
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return content.substring(0, matcher.start()).trim();
        } else {
            return content.trim();
        }
    }

    private List<EmailAttachmentDTO> convertAttachmentMapsToEmailAttachments(List<Map<String, Object>> attachmentMaps) {
        List<EmailAttachmentDTO> attachments = new ArrayList<>();
        if (attachmentMaps != null) {
            for (Map<String, Object> attachmentMap : attachmentMaps) {
                if (attachmentMap != null) {
                    EmailAttachmentDTO attachment = new EmailAttachmentDTO();
                    attachment.setName((String) attachmentMap.get("name"));
                    attachment.setFileExtension((String) attachmentMap.get("fileExtension"));

                    Object fileBytesObj = attachmentMap.get("fileBytes");
                    if (fileBytesObj instanceof String) {
                        String fileBytesStr = (String) fileBytesObj;
                        attachment.setFileBytes(fileBytesStr.getBytes(StandardCharsets.UTF_8));
                    } else if (fileBytesObj instanceof byte[]) {
                        attachment.setFileBytes((byte[]) fileBytesObj);
                    } else {
                        attachment = null;
                    }

                    attachments.add(attachment);
                }
            }
        }
        return attachments;
    }

    private Message convertEmailToMessage(ModEmailDTO email) {
        Message message = new Message();
        message.setSentBy(email.getFrom());

        String body = extractOriginalBody(email.getBody());

        int sentByIndex = body.indexOf("Sent by:");

        if (sentByIndex != -1) {
            if ((email.getFrom()).equals("imilamaheshan30@gmail.com")) {
                message.setSender(MessageType.VENDOR);
            } else {
                message.setSender(MessageType.CLIENT);
            }
        } else {
            String sender = message.getSentBy();
            String convertedSender = extractEmailAddress(sender);

            if (convertedSender.equals("imilamaheshan30@gmail.com")) {
                message.setSender(MessageType.VENDOR);
            } else {
                message.setSender(MessageType.CLIENT);
            }
        }

        int uniqueIdIndex = body.indexOf("Unique Id : ");
        String uniqueId = null;
        if (uniqueIdIndex != -1) {
            int uniqueIdEndIndex = body.indexOf("\n", uniqueIdIndex);
            if (uniqueIdEndIndex != -1) {
                uniqueId = body.substring(uniqueIdIndex + "Unique Id : ".length(), uniqueIdEndIndex).trim();
            }
        }
        String uniqueIdString = uniqueId != null ? uniqueId : "Unknown";

        message.setUniqueId(uniqueIdString);
        message.setCcEmailAddresses(Collections.singletonList(email.getCc()));
        message.setContent(email.getBody());

        message.setCreatedAt(convertToDateTime((Date) email.getReceivedDate()));

        Long ticketId = extractTicketIdFromSubject(email.getSubject());

        List<MessageAttachment> attachments = convertEmailAttachments(email.getEmailAttachmentDTOList());
        message.setAttachments(attachments);

        for (MessageAttachment attachment : attachments) {
            attachment.setMessage(message);
        }
        return message;
    }

    private LocalDateTime convertToDateTime(Date date) {
        LocalDateTime localDateTime = new java.sql.Timestamp(date.getTime()).toLocalDateTime();
        ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(ZoneId.of("America/Chicago"));
        return zonedDateTime.toLocalDateTime();
    }

    private List<MessageAttachment> convertEmailAttachments(List<EmailAttachmentDTO> emailAttachments) {
        List<MessageAttachment> attachments = new ArrayList<>();
        if (emailAttachments != null) {
            for (EmailAttachmentDTO emailAttachment : emailAttachments) {
                MessageAttachment attachment = new MessageAttachment();
                attachment.setName(emailAttachment.getName());
                attachment.setFileExtension(emailAttachment.getFileExtension());
                attachment.setFileBytes(emailAttachment.getFileBytes());
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    private Long extractTicketIdFromSubject(String subject) {
        String[] parts = subject.split("Support Ticket -");
        if (parts.length == 2) {
            try {
                return Long.parseLong(parts[1].trim());
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return null;
        }
    }

    public boolean isUniqueIdExists(String uniqueId) {
        return messageRepository.existsByUniqueId(uniqueId);
    }

    public LocalDateTime getLatestReceivedDateForMessageTicket(Long ticketId) {
        EmailTime emailTime = emailTimeRepository.findByTicketId(ticketId);
        return emailTime.getCreatedAt();
    }

    private String extractEmailAddress(String from) {
        int startIndex = from.indexOf("<");
        int endIndex = from.indexOf(">");
        if (startIndex != -1 && endIndex != -1) {
            return from.substring(startIndex + 1, endIndex);
        }
        return from;
    }
}


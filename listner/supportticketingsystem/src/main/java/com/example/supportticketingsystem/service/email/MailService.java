package com.example.supportticketingsystem.service.email;

import com.example.supportticketingsystem.dto.request.EmailAttachmentDTO;
import com.example.supportticketingsystem.dto.request.EmailDTO;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MailService {

    @Value("${spring.mail.username}")
    private String email;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${mail.search.subject.prefix}")
    private String subjectPrefix;

    public List<EmailDTO> fetchEmails(int fetchIntervalMinutes) throws Exception {
        List<EmailDTO> emails = new ArrayList<>();

        // Set properties for accessing Gmail
        Properties properties = new Properties();
        properties.setProperty("mail.store.protocol", "imaps");
        properties.setProperty("mail.imaps.host", "imap.gmail.com");
        properties.setProperty("mail.imaps.port", "993");

        // Get the session object
        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });

        // Connect to the IMAP server
        Store store = session.getStore("imaps");
        store.connect();

        // Get the Inbox folder
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);

        // Get current time in Sri Lanka time zone
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Colombo"));
        long currentTimeInSL = cal.getTimeInMillis();

        // Calculate time period for emails (fetchIntervalMinutes minutes ago)
        long timeOfMailPeriod = currentTimeInSL - (1000 * 60 * fetchIntervalMinutes);
        SearchTerm dateTerm = new ReceivedDateTerm(ComparisonTerm.GT, new Date(timeOfMailPeriod));

        // SearchTerm for subjects that contain "Support Ticket -"
        Pattern subjectPattern = Pattern.compile(".*Support Ticket -\\d+.*");
        SearchTerm subjectTerm = new SubjectTerm("Support Ticket -");

        // Combined search term
        SearchTerm searchTerm = new AndTerm(dateTerm, subjectTerm);

        // Fetch matching emails
        Message[] messages = inbox.search(searchTerm);

        // Logging the number of messages fetched
        System.out.println("Number of messages fetched: " + messages.length);

        // Iterate through messages and filter by regex
        for (Message message : messages) {
            try {
                if (message.getSubject() != null) {
                    Matcher matcher = subjectPattern.matcher(message.getSubject());
                    if (matcher.matches()) {
                        System.out.println("Message subject matched: " + message.getSubject());
                        EmailDTO emailDTO = new EmailDTO();
                        emailDTO.setSubject(message.getSubject());
                        emailDTO.setFrom(Arrays.toString(message.getFrom()));
                        emailDTO.setTo(Arrays.toString(message.getRecipients(Message.RecipientType.TO)));
                        emailDTO.setCc(Arrays.toString(message.getRecipients(Message.RecipientType.CC)));
                        emailDTO.setBody(getTextFromMessage(message));

                        Long ticketId = extractTicketIdFromSubject(message.getSubject());
                        emailDTO.setTicketid(ticketId);

                        // Convert received date to Sri Lanka time zone
                        Date receivedDate = message.getReceivedDate();
                        cal.setTime(receivedDate);
                        cal.setTimeZone(TimeZone.getTimeZone("Asia/Colombo"));
                        receivedDate = cal.getTime();
                        emailDTO.setReceivedDate(receivedDate);

                        // Check for attachments only if the message content is multipart
                        if (message.isMimeType("multipart/*")) {
                            List<EmailAttachmentDTO> attachments = fetchAttachments(message);
                            emailDTO.setEmailAttachmentDTOList(attachments);
                        }

                        emails.add(emailDTO);
                    } else {
                        System.out.println("Message subject did not match: " + message.getSubject());
                    }
                } else {
                    System.out.println("Message subject is null.");
                }
            } catch (Exception e) {
                System.out.println("Error processing message: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Close connections
        inbox.close(false);
        store.close();

        return emails;
    }


    private Long extractTicketIdFromSubject(String subject) {
        // Assuming the subject format is consistent: "xxxx : Support Ticket -xx"
        // Split the subject by "Support Ticket -" and extract the second part
        String[] parts = subject.split("Support Ticket -");
        if (parts.length == 2) {
            try {
                return Long.parseLong(parts[1].trim());
            } catch (NumberFormatException e) {
                // Log or handle parsing error
                System.out.println("Error parsing ticket ID from subject: " + e.getMessage());
                return null;
            }
        } else {
            // Handle invalid subject format
            System.out.println("Invalid subject format: " + subject);
            return null;
        }
    }

    private String getTextFromMessage(Message message) throws Exception {
        String result = "";
        if (message.isMimeType("text/plain")) {
            result = message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        int count = mimeMultipart.getCount();
        if (count == 0)
            throw new MessagingException("Multipart with no body parts not supported.");
        boolean multipartAlt = new ContentType(mimeMultipart.getContentType()).match("multipart/alternative");
        if (multipartAlt)
            // alternatives appear in an order of increasing faithfulness to the original content
            return getTextFromBodyPart(mimeMultipart.getBodyPart(count - 1));
        String result = "";
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            result += getTextFromBodyPart(bodyPart);
        }
        return result;
    }

    private String getTextFromBodyPart(BodyPart bodyPart) throws Exception {
        String result = "";
        if (bodyPart.isMimeType("text/plain")) {
            result = (String) bodyPart.getContent();
        } else if (bodyPart.isMimeType("text/html")) {
            String html = (String) bodyPart.getContent();
            result = Jsoup.parse(html).text();
        } else if (bodyPart.getContent() instanceof MimeMultipart) {
            result = getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
        }
        return result;
    }

    private List<EmailAttachmentDTO> fetchAttachments(Message message) throws Exception {
        List<EmailAttachmentDTO> attachments = new ArrayList<>();

        if (message.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                        !StringUtils.isEmpty(bodyPart.getFileName())) {
                    // Fetch attachment details
                    EmailAttachmentDTO attachmentDTO = new EmailAttachmentDTO();
                    attachmentDTO.setName(bodyPart.getFileName());
                    attachmentDTO.setFileExtension(FilenameUtils.getExtension(bodyPart.getFileName()));
                    attachmentDTO.setFileBytes(IOUtils.toByteArray(bodyPart.getInputStream()));

                    attachments.add(attachmentDTO);
                }
            }
        } else {
            // If the content is not multipart, there are no attachments
            System.out.println("Message content is not multipart. No attachments found.");
        }

        return attachments;
    }

}

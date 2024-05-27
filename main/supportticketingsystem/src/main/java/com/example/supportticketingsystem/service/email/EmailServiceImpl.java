package com.example.supportticketingsystem.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    public EmailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body, List<String> cc, String inReplyTo) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper;
        try {
            helper = new MimeMessageHelper(message, true); // Enable multipart
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);

            if (cc != null && !cc.isEmpty()) {
                for (String ccAddress : cc) {
                    helper.addCc(new InternetAddress(ccAddress));
                }
            }

            if (inReplyTo != null && !inReplyTo.isEmpty()) {
                message.setHeader("In-Reply-To", inReplyTo);
            }

            javaMailSender.send(message);
        } catch (MessagingException e) {
            // Handle exception
            e.printStackTrace();
        }
    }

    @Override
    public void sendMailWithAttachment(String to, String subject, String body, List<String> cc, List<String> attachmentNames, List<byte[]> attachmentBytesList, String inReplyTo) throws MessagingException {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true); // Enable multipart

        try {
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);

            if (cc != null && !cc.isEmpty()) {
                for (String ccAddress : cc) {
                    helper.addCc(new InternetAddress(ccAddress));
                }
            }

            // Add each attachment from the list
            for (int i = 0; i < attachmentNames.size(); i++) {
                byte[] attachmentBytes = attachmentBytesList.get(i);
                String attachmentName = attachmentNames.get(i);
                helper.addAttachment(attachmentName, new ByteArrayResource(attachmentBytes));
            }

            if (inReplyTo != null && !inReplyTo.isEmpty()) {
                message.setHeader("In-Reply-To", inReplyTo);
            }

            javaMailSender.send(message);
        } catch (MessagingException e) {
            // Handle exception
            e.printStackTrace();
        }
    }

}

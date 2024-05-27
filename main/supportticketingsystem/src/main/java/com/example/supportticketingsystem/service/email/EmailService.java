package com.example.supportticketingsystem.service.email;

import jakarta.mail.MessagingException;

import java.util.List;

public interface EmailService {
    void sendEmail(String to, String subject, String body, List<String> cc,String inReplyTo);


    void sendMailWithAttachment(String to, String subject, String body, List<String> cc, List<String> attachmentNames, List<byte[]> attachmentBytesList, String inReplyTo) throws MessagingException;
}



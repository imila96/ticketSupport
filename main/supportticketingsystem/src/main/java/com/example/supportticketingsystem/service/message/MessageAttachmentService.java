package com.example.supportticketingsystem.service.message;

import com.example.supportticketingsystem.dto.collection.MessageAttachment;
import com.example.supportticketingsystem.repository.MessageAttachmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageAttachmentService {

    private final MessageAttachmentRepository messageAttachmentRepository;

    public MessageAttachmentService(MessageAttachmentRepository messageAttachmentRepository) {
        this.messageAttachmentRepository = messageAttachmentRepository;
    }

    public List<String> getDownloadLinksByTicketId(@PathVariable Long ticketId) {
        List<MessageAttachment> attachments = messageAttachmentRepository.findByTicketId(ticketId);
        return attachments.stream()
                .map(attachment -> "/tickets/getFileById/" + attachment.getId()) // Modify URL path as needed
                .collect(Collectors.toList());
    }

    public MessageAttachment getAttachmentById(Long attachmentId) {
        return messageAttachmentRepository.findById(attachmentId).orElse(null);
    }

}
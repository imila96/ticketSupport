package com.example.supportticketingsystem.service.attachment;

import com.example.supportticketingsystem.dto.collection.MessageAttachment;
import com.example.supportticketingsystem.repository.MessageAttachmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AttachmentService {

    private final MessageAttachmentRepository attachmentRepository;

    @Autowired
    public AttachmentService(MessageAttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    public List<String> getAttachmentLinksByTicketId(Long ticketId) {
        List<MessageAttachment> attachments = attachmentRepository.findByTicketId(ticketId);
        List<String> attachmentLinks = new ArrayList<>();
        for (MessageAttachment attachment : attachments) {
            String downloadLink;
            if ("Unknown".equals(attachment.getMessage().getUniqueId())) {
                downloadLink = "http://localhost:8085/api/attachments/file/" + attachment.getId();
            } else {
                downloadLink = "http://localhost:8085/tickets/getFileById/" + attachment.getId();
            }
            attachmentLinks.add(downloadLink);
        }
        return attachmentLinks;
    }
}

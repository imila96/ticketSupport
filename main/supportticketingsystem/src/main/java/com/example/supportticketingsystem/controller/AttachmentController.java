package com.example.supportticketingsystem.controller;

import com.example.supportticketingsystem.service.attachment.AttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = "*")
public class AttachmentController {

    private final AttachmentService attachmentService;

    @Autowired
    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @GetMapping("/getAllAttachmentById/{ticketId}")
    public List<String> getAllAttachmentsByTicketId(@PathVariable Long ticketId) {
        return attachmentService.getAttachmentLinksByTicketId(ticketId);
    }
}

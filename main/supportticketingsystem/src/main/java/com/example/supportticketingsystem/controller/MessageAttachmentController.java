package com.example.supportticketingsystem.controller;

import com.example.supportticketingsystem.dto.collection.MessageAttachment;
import com.example.supportticketingsystem.service.message.MessageAttachmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;


import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/attachments")
@CrossOrigin(origins = "*")
public class MessageAttachmentController {

    private final MessageAttachmentService messageAttachmentService;

    @Autowired
    public MessageAttachmentController(MessageAttachmentService messageAttachmentService) {
        this.messageAttachmentService = messageAttachmentService;
    }

    @GetMapping("/{ticketId}/download-links")
    public ResponseEntity<List<String>> getDownloadLinksByTicketId(@PathVariable Long ticketId) {
        List<String> downloadLinks = messageAttachmentService.getDownloadLinksByTicketId(ticketId);
        return ResponseEntity.ok(downloadLinks);
    }


    @GetMapping("/file/{attachmentId}")
    public ResponseEntity<Resource> getAttachmentFileById(@PathVariable Long attachmentId) {
        MessageAttachment attachment = messageAttachmentService.getAttachmentById(attachmentId);
        if (attachment != null) {
            byte[] fileContent = Base64.getDecoder().decode(attachment.getFileBytes()); // Decode Base64 string
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getName() + "\"");

            // Inside your method
            String fileExtension = attachment.getFileExtension();

// Map file extensions to media types
            MediaType mediaType;
            switch (fileExtension.toLowerCase()) {
                case "pdf":
                    mediaType = MediaType.APPLICATION_PDF;
                    break;
                case "jpg":
                case "jpeg":
                    mediaType = MediaType.IMAGE_JPEG;
                    break;
                case "png":
                    mediaType = MediaType.IMAGE_PNG;
                    break;
                case "docx":
                    mediaType = MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    break;
                case "txt":
                    mediaType = MediaType.TEXT_PLAIN;
                    break;
                case "xml":
                    mediaType = MediaType.APPLICATION_XML;
                    break;
                // Add more cases for other file types as needed
                default:
                    // Set a default content type if the file extension is not recognized
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    break;
            }

            headers.setContentType(mediaType);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileContent.length)
                    .body(resource);
        } else {
            return ResponseEntity.notFound().build();
        }
    }


}
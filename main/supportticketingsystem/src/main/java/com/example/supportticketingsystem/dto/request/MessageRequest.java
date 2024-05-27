package com.example.supportticketingsystem.dto.request;

import com.example.supportticketingsystem.enums.MessageType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageRequest {

    @Enumerated(EnumType.STRING)
    private MessageType sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    // Change attachments to MultipartFile
    private List<MultipartFile> attachments;

    private String sentBy;

    private List<String> ccEmailAddresses;
}

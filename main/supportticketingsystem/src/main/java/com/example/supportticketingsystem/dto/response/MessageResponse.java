package com.example.supportticketingsystem.dto.response;

import com.example.supportticketingsystem.enums.MessageType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MessageResponse {

    private Long id;
    private String sentBy;
    private MessageType sender;
    private List<String> ccEmailAddresses;
    private String content;
    private LocalDateTime createdAt;
    private String uniqueId;
    private List<String> attachments;
}

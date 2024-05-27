package com.example.supportticketingsystem.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class ReopenTicketRequest {
    private Long ticketId;
    private String sentBy;
    private String reason;
    private List<String> ccEmailAddresses;
}

package com.example.supportticketingsystem.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TicketCloseResponse {

    private Long ticketId;
    private String closeReason;
    private String closedBy;
}

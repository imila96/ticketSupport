package com.example.supportticketingsystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModEmailDTO {
    private String subject;
    private String from;
    private String to;
    private String cc;
    private String body;
    private Date receivedDate;
    private Long ticketId;

    private List<EmailAttachmentDTO> emailAttachmentDTOList;

}

package com.example.supportticketingsystem.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailDTO {
    private String subject;
    private String from;
    private String to;
    private String cc;
    private String body;
    private Date receivedDate;

}


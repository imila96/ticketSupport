package com.example.supportticketingsystem.dto.request;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import com.example.supportticketingsystem.enums.SupportRequestType;
import com.example.supportticketingsystem.enums.Severity;
import com.example.supportticketingsystem.enums.InstallationType;
import com.example.supportticketingsystem.enums.Environment;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketRequest {



    private Long id;


    private String emailAddress;

    private List<String> ccEmailAddresses;

    @Enumerated(EnumType.STRING)
    private SupportRequestType supportRequestType;


    private String subject;


    private String description;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String product;

    @Enumerated(EnumType.STRING)
    private InstallationType installationType;

    @Enumerated(EnumType.STRING)
    private Environment affectedEnvironment;

    private String platformVersion;
    private List<String> attachments;



}
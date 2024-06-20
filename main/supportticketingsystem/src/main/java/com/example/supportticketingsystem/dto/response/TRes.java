package com.example.supportticketingsystem.dto.response;

import com.example.supportticketingsystem.enums.*;
import lombok.Builder;
import lombok.Data;

import java.time.*;
import java.util.List;

@Data
@Builder
public class TRes {

    private Long id;
    private LocalDateTime createdAt;
    private String emailAddress;
    private List<String> ccEmailAddresses;
    private SupportRequestType supportRequestType;
    private String subject;
    private String description;
    private Severity severity;
    private String product;
    private InstallationType installationType;
    private Environment affectedEnvironment;
    private String platformVersion;
    private Status clientStatus;
    private Status vendorStatus;
    private String reopenReason;
}

package com.example.supportticketingsystem.dto.response;

import com.example.supportticketingsystem.enums.*;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketResponse {

    @NotNull
    private Long id;

    @NotNull
    private LocalDateTime createdAt;

    @NotNull
    private String emailAddress;

    @NotNull
    private List<String> ccEmailAddresses;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SupportRequestType supportRequestType;

    @NotNull
    private String subject;

    @NotNull
    private String description;


    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @NotNull
    private String product;

    @NotNull
    @Enumerated(EnumType.STRING)
    private InstallationType installationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Environment affectedEnvironment;

    @NotNull
    private String platformVersion;


}
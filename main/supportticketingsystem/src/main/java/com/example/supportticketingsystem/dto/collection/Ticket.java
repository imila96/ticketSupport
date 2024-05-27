package com.example.supportticketingsystem.dto.collection;

import com.example.supportticketingsystem.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ticket")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Use appropriate ID generation strategy
    private Long id;

    @NotNull
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    @NotNull
    private String emailAddress;


    private List<String> ccEmailAddresses;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SupportRequestType supportRequestType;

    @NotNull
    private String subject;

    @NotBlank
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Product product;

    @NotNull
    @Enumerated(EnumType.STRING)
    private InstallationType installationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Environment affectedEnvironment;

    @NotNull
    private String platformVersion;



    @Enumerated(EnumType.STRING)
    private Status clientStatus = Status.OPEN;


    @Enumerated(EnumType.STRING)
    private Status vendorStatus = Status.AWAITING_REPLY;

    private String initialMessageId;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> conversation;

    // Add a field to store the reason for reopening the ticket
    private String reopenReason;


}

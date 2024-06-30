package com.example.supportticketingsystem.dto.collection;

import com.example.supportticketingsystem.enums.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Builder.Default
    private LocalDateTime createdAt = ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime();

    @NotNull
    @Email(message = "must be a valid email address")
    private String emailAddress;


    private List<String> ccEmailAddresses;

    @NotNull(message = "Support request type is required")
    @Enumerated(EnumType.STRING)
    private SupportRequestType supportRequestType;

    @NotNull(message = "Subject is required")
    @NotBlank(message = "Subject cannot be blank")
    private String subject;

    @NotBlank(message = "Description cannot be blank")
    private String description;

    @NotNull(message = "Severity is required")
    @Enumerated(EnumType.STRING)
    private Severity severity;

    @NotNull(message = "Product is required")
    private String product;

    @NotNull(message = "Installation type is required")
    @Enumerated(EnumType.STRING)
    private InstallationType installationType;

    @NotNull(message = "Affected environment is required")
    @Enumerated(EnumType.STRING)
    private Environment affectedEnvironment;

    @NotNull(message = "Platform version is required")
    private String platformVersion;

    @Enumerated(EnumType.STRING)
    private Status clientStatus = Status.OPEN;

    @Enumerated(EnumType.STRING)
    private Status vendorStatus = Status.AWAITING_REPLY;

    private String initialMessageId;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Message> conversation;

    private String reopenReason;

    private String closeReason;
}

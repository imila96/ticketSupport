    package com.example.supportticketingsystem.dto.collection;

    import com.example.supportticketingsystem.enums.MessageType;
    import jakarta.persistence.*;
    import jakarta.validation.constraints.NotNull;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;

    import java.time.LocalDateTime;
    import java.util.List;

    @Entity
    @Table(name = "message")
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public class Message {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "ticket_id")
        private Ticket ticket;


        @NotNull
        private String sentBy;

        @Enumerated(EnumType.STRING)
        private MessageType sender;

        private List<String> ccEmailAddresses;

        @Column(columnDefinition = "TEXT")
        private String content;

        private LocalDateTime createdAt;

        private String uniqueId;


        @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        private List<MessageAttachment> attachments;
    }
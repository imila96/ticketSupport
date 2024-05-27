package com.example.supportticketingsystem.dto.collection;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "messageattachment")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    private String name;

    private String fileExtension;

    @Lob
    @Column(length = 10485760) // Example: Set the length to 10 MB (in bytes)
    private byte[] fileBytes;
}


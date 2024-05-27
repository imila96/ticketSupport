package com.example.supportticketingsystem.dto.collection;


import com.example.supportticketingsystem.enums.MessageType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "emailtime")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;

    private LocalDateTime createdAt;


}
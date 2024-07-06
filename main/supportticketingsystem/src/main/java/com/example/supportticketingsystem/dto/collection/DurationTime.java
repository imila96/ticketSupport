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
@Table(name = "durationtime")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DurationTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;

    private String status;

    private LocalDateTime time;

    private int attempts;

    private boolean solvedStatus;

    private boolean slaBreach;

    private boolean delayedReply;

    private String severity;

    private String solvedBy;


}

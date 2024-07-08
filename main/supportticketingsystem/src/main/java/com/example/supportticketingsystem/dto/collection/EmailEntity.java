package com.example.supportticketingsystem.dto.collection;

import com.example.supportticketingsystem.enums.Severity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "email")})
public class EmailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "email_severities", joinColumns = @JoinColumn(name = "email_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private List<Severity> severities;
}
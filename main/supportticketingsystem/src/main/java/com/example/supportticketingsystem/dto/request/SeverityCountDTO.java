package com.example.supportticketingsystem.dto.request;

import com.example.supportticketingsystem.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeverityCountDTO {
    private String severity;
    private long count;
}
package com.example.supportticketingsystem.dto.request;

import lombok.Data;

@Data
public class SeverityCountDTO {
    private String severity;
    private long count;
    private int month;
    private int year;

    public SeverityCountDTO(String severity, long count, int month, int year) {
        this.severity = severity;
        this.count = count;
        this.month = month;
        this.year = year;
    }


}
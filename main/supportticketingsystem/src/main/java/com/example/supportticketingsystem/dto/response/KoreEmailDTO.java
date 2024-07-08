package com.example.supportticketingsystem.dto.response;


import com.example.supportticketingsystem.enums.Severity;
import lombok.Data;

import java.util.List;

@Data
public class KoreEmailDTO {

    private String email;
    private List<Severity> severities;

    public KoreEmailDTO(String email, List<Severity> severities) {
        this.email = email;
        this.severities = severities;
    }
}

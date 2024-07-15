package com.example.supportticketingsystem.dto.response;


import com.example.supportticketingsystem.enums.Severity;
import lombok.Data;

import java.util.List;

@Data
public class KoreEmailDTO {

    private long id;
    private String email;
    private List<Severity> severities;

    public KoreEmailDTO(Long id, String email, List<Severity> severities) {
        this.id=id;
        this.email = email;
        this.severities = severities;
    }
}

package com.example.supportticketingsystem.dto.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailAttachmentDTO {




    private String name;

    private String fileExtension;


    private byte[] fileBytes;

}

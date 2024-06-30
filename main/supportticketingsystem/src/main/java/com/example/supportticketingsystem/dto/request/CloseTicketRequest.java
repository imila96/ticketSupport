package com.example.supportticketingsystem.dto.request;

public class CloseTicketRequest {
    private String sentBy;

    private String closeTicketRequest;

    public String getCloseTicketRequest() {
        return closeTicketRequest;
    }

    public void setCloseTicketRequest(String closeTicketRequest) {
        this.closeTicketRequest = closeTicketRequest;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }
}

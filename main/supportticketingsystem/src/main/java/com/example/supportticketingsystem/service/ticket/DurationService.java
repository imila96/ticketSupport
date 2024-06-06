package com.example.supportticketingsystem.service.ticket;


import com.example.supportticketingsystem.dto.collection.DurationTime;
import com.example.supportticketingsystem.repository.DurationTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DurationService {

    @Autowired
    private DurationTimeRepository durationTimeRepository;

    public String calculateOpenDuration(Long ticketId, LocalDateTime endTime) {
        List<DurationTime> tickets = durationTimeRepository.findByTicketIdOrderByTimeAsc(ticketId);
        LocalDateTime lastOpenTime = null;
        Duration totalOpenDuration = Duration.ZERO;

        for (DurationTime ticket : tickets) {
            if (ticket.getStatus().equalsIgnoreCase("AWAITING")) {
                if (lastOpenTime == null) {
                    lastOpenTime = ticket.getTime();
                }
            } else if (lastOpenTime != null) {
                totalOpenDuration = totalOpenDuration.plus(Duration.between(lastOpenTime, ticket.getTime()));
                lastOpenTime = null;
            }
        }

        // Check if the last ticket status is still open
        if (lastOpenTime != null) {
            totalOpenDuration = totalOpenDuration.plus(Duration.between(lastOpenTime, endTime));
        }

        long days = totalOpenDuration.toDays();
        totalOpenDuration = totalOpenDuration.minusDays(days);
        long hours = totalOpenDuration.toHours();
        totalOpenDuration = totalOpenDuration.minusHours(hours);
        long minutes = totalOpenDuration.toMinutes();
        totalOpenDuration = totalOpenDuration.minusMinutes(minutes);
        long seconds = totalOpenDuration.getSeconds();

        return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }


    public Map<String, String> calculateOpenDuration(Long ticketId, int attempt, LocalDateTime endTime) {
        List<DurationTime> tickets = durationTimeRepository.findByTicketIdAndAttemptsOrderByTimeAsc(ticketId, attempt);
        LocalDateTime lastOpenTime = null;
        Duration totalOpenDuration = Duration.ZERO;

        for (DurationTime ticket : tickets) {
            if (ticket.getStatus().equalsIgnoreCase("AWAITING")) {
                if (lastOpenTime == null) {
                    lastOpenTime = ticket.getTime();
                }
            } else if (lastOpenTime != null) {
                totalOpenDuration = totalOpenDuration.plus(Duration.between(lastOpenTime, ticket.getTime()));
                lastOpenTime = null;
            }
        }

        // Check if the last ticket status is still open
        if (lastOpenTime != null) {
            totalOpenDuration = totalOpenDuration.plus(Duration.between(lastOpenTime, endTime));
        }

        long days = totalOpenDuration.toDays();
        totalOpenDuration = totalOpenDuration.minusDays(days);
        long hours = totalOpenDuration.toHours();
        totalOpenDuration = totalOpenDuration.minusHours(hours);
        long minutes = totalOpenDuration.toMinutes();
        totalOpenDuration = totalOpenDuration.minusMinutes(minutes);
        long seconds = totalOpenDuration.getSeconds();

        String durationString = String.format("%d days %d hours %d minutes %d seconds", days, hours, minutes, seconds);

        Map<String, String> result = new HashMap<>();
        result.put("duration", durationString);

        return result;
    }

    public Map<String, String> calculateClientOpenDuration(Long ticketId, int attempt, LocalDateTime endTime) {
        List<DurationTime> tickets = durationTimeRepository.findByTicketIdAndAttemptsOrderByTimeAsc(ticketId, attempt);
        LocalDateTime lastOpenTime = null;
        Duration totalOpenDuration = Duration.ZERO;

        for (DurationTime ticket : tickets) {
            if (ticket.getStatus().equalsIgnoreCase("OPEN")) {
                if (lastOpenTime == null) {
                    lastOpenTime = ticket.getTime();
                }
            } else if (lastOpenTime != null) {
                totalOpenDuration = totalOpenDuration.plus(Duration.between(lastOpenTime, ticket.getTime()));
                lastOpenTime = null;
            }
        }

        // Check if the last ticket status is still open
        if (lastOpenTime != null) {
            totalOpenDuration = totalOpenDuration.plus(Duration.between(lastOpenTime, endTime));
        }

        long days = totalOpenDuration.toDays();
        totalOpenDuration = totalOpenDuration.minusDays(days);
        long hours = totalOpenDuration.toHours();
        totalOpenDuration = totalOpenDuration.minusHours(hours);
        long minutes = totalOpenDuration.toMinutes();
        totalOpenDuration = totalOpenDuration.minusMinutes(minutes);
        long seconds = totalOpenDuration.getSeconds();

        String durationString = String.format("%d days %d hours %d minutes %d seconds", days, hours, minutes, seconds);

        Map<String, String> result = new HashMap<>();
        result.put("duration", durationString);

        return result;
    }



}

package com.example.supportticketingsystem.service.ticket;


import com.example.supportticketingsystem.dto.collection.DurationTime;
import com.example.supportticketingsystem.repository.DurationTimeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    public List<Map<String, String>> getTicketsWithinDateRange(String startMonth, String endMonth) {
        LocalDate startDate = YearMonth.parse(startMonth, DateTimeFormatter.ofPattern("yyyy-MM")).atDay(1);
        LocalDate endDate = YearMonth.parse(endMonth, DateTimeFormatter.ofPattern("yyyy-MM")).atEndOfMonth();

        List<DurationTime> allTickets = durationTimeRepository.findAll();

        Map<Long, LocalDateTime> earliestDates = allTickets.stream()
                .collect(Collectors.groupingBy(DurationTime::getTicketId,
                        Collectors.collectingAndThen(
                                Collectors.minBy(Comparator.comparing(DurationTime::getTime)),
                                optional -> optional.get().getTime())));

        List<DurationTime> filteredTickets = allTickets.stream()
                .filter(ticket -> {
                    LocalDateTime earliestDate = earliestDates.get(ticket.getTicketId());
                    return !earliestDate.toLocalDate().isBefore(startDate) && !earliestDate.toLocalDate().isAfter(endDate);
                })
                .collect(Collectors.toList());

        return calculateWaitingTimes(filteredTickets);
    }

    private List<Map<String, String>> calculateWaitingTimes(List<DurationTime> tickets) {
        Map<String, String> waitingTimes = new HashMap<>();

        for (DurationTime ticket : tickets) {
            String ticketId = String.valueOf(ticket.getTicketId());
            String attempt = String.valueOf(ticket.getAttempts());

            // Calculate client waiting time
            Map<String, String> clientOpenDuration = calculateClientOpenDuration(ticket.getTicketId(), ticket.getAttempts(), getEndTime());
            String clientWaitingTimeKey = ticketId + "-clientwaitingTime-" + attempt;
            waitingTimes.put(clientWaitingTimeKey, clientOpenDuration.get("duration"));

            // Calculate vendor waiting time
            Map<String, String> openDuration = calculateOpenDuration(ticket.getTicketId(), ticket.getAttempts(), getEndTime());
            String vendorWaitingTimeKey = ticketId + "-vendorwaitingTime-" + attempt;
            waitingTimes.put(vendorWaitingTimeKey, openDuration.get("duration"));
        }

        return aggregateWaitingTimes(waitingTimes);
    }

    private LocalDateTime getEndTime() {
        return ZonedDateTime.now(ZoneId.of("America/Chicago")).toLocalDateTime();
    }

    private List<Map<String, String>> aggregateWaitingTimes(Map<String, String> waitingTimes) {
        Map<String, Duration> clientDurations = new HashMap<>();
        Map<String, Duration> vendorDurations = new HashMap<>();

        for (Map.Entry<String, String> entry : waitingTimes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            Duration duration = parseDuration(value);

            String[] parts = key.split("-");
            String ticketId = parts[0];
            String type = parts[1];

            if (type.equals("clientwaitingTime")) {
                clientDurations.put(ticketId, clientDurations.getOrDefault(ticketId, Duration.ZERO).plus(duration));
            } else if (type.equals("vendorwaitingTime")) {
                vendorDurations.put(ticketId, vendorDurations.getOrDefault(ticketId, Duration.ZERO).plus(duration));
            }
        }

        List<Map<String, String>> result = new ArrayList<>();
        for (String ticketId : clientDurations.keySet()) {
            Map<String, String> entry = new HashMap<>();
            entry.put("ticketId", ticketId);
            entry.put("clientWaitingTime", formatDurationAsDays(clientDurations.get(ticketId)));
            entry.put("vendorWaitingTime", formatDurationAsDays(vendorDurations.getOrDefault(ticketId, Duration.ZERO)));
            result.add(entry);
        }

        return result;
    }

    private Duration parseDuration(String durationString) {
        String[] parts = durationString.split(" ");
        long days = Long.parseLong(parts[0]);
        long hours = Long.parseLong(parts[2]);
        long minutes = Long.parseLong(parts[4]);
        long seconds = Long.parseLong(parts[6]);
        return Duration.ofDays(days).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
    }

    private String formatDurationAsDays(Duration duration) {
        double days = duration.toDays() + duration.toHoursPart() / 24.0 + duration.toMinutesPart() / 1440.0 + duration.toSecondsPart() / 86400.0;
        return String.format("%.2f", days);
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();
        duration = duration.minusMinutes(minutes);
        long seconds = duration.getSeconds();

        return String.format("%d days %d hours %d minutes %d seconds", days, hours, minutes, seconds);
    }

}


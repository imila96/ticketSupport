package com.example.supportticketingsystem.service.ticket;


import com.example.supportticketingsystem.dto.collection.DurationTime;
import com.example.supportticketingsystem.dto.collection.Ticket;
import com.example.supportticketingsystem.enums.Status;
import com.example.supportticketingsystem.repository.DurationTimeRepository;
import com.example.supportticketingsystem.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;


import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DurationService {

    @Autowired
    private DurationTimeRepository durationTimeRepository;

    @Autowired
    private TicketRepository ticketRepository;

    public DurationService() {
    }

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

    public void markTicketAsSolved(Long ticketId, String email) {
        Logger logger = Logger.getLogger(DurationService.class.getName());

        List<DurationTime> ticketsToUpdate = durationTimeRepository.findByTicketId(ticketId);

        for (DurationTime ticket : ticketsToUpdate) {
            ticket.setSolvedStatus(true);
            ticket.setSolvedBy(email); // Assuming you have a field to store who solved the ticket
        }

        Optional<Ticket> optionalTicket = ticketRepository.findById(ticketId);
        if (optionalTicket.isPresent()) {
            Ticket ticket = optionalTicket.get();

            // Logging the values before saving
            logger.info("Marking ticket as solved. Ticket ID: " + ticketId + ", Email: " + email);

            ticket.setSolvedBy(email);
            ticket.setClientStatus(Status.SOLVED);
            ticket.setVendorStatus(Status.SOLVED);

            // Logging the values being saved
            logger.info("Client Status: " + ticket.getClientStatus() + ", Vendor Status: " + ticket.getVendorStatus());

            ticketRepository.save(ticket);
            durationTimeRepository.saveAll(ticketsToUpdate);
        } else {
            throw new IllegalArgumentException("Ticket not found");
        }
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000) // 15 minutes in milliseconds
    public void checkAndUpdateDelayedReplies() {
        System.out.println("time breach started");
        // Query DurationTime entities where severity is SEVERITY_1
        List<DurationTime> durationTimes = durationTimeRepository.findBySeverity("SEVERITY_1");

        for (DurationTime durationTime : durationTimes) {
            // Check if delayedReply is not already set
            if (!durationTime.isDelayedReply()) {
                // Get all related times for the same ticket and attempt
                List<DurationTime> relatedTimes = durationTimeRepository.findByTicketIdAndAttemptsOrderByTimeAsc(
                        durationTime.getTicketId(), durationTime.getAttempts());

                LocalDateTime ticketCreationTime = durationTime.getTime();
                LocalDateTime firstOpenTime = null;
                boolean foundFirstOpen = false;

                for (DurationTime relatedTime : relatedTimes) {
                    if (relatedTime.getStatus().equalsIgnoreCase("OPEN")) {
                        if (!foundFirstOpen) {
                            firstOpenTime = relatedTime.getTime();
                            foundFirstOpen = true;
                        }
                    }
                }

                boolean shouldUpdate = false;
                if (foundFirstOpen && firstOpenTime != null) {
                    // Check if 30 minutes have passed since the ticket creation time
                    if (Duration.between(ticketCreationTime, firstOpenTime).toMinutes() > 30) {
                        shouldUpdate = true;
                    }
                } else {
                    LocalDateTime currentTimeInChicago = LocalDateTime.now(ZoneId.of("America/Chicago"));

                    // Check if more than 30 minutes have passed since ticket creation
                    if (Duration.between(ticketCreationTime, currentTimeInChicago).toMinutes() > 30) {
                        shouldUpdate = true;
                    }
                }

                if (shouldUpdate) {
                    for (DurationTime relatedTime : relatedTimes) {
                        relatedTime.setDelayedReply(true);
                    }
                    durationTimeRepository.saveAll(relatedTimes);
                }
            }
        }
        System.out.println("time breach end");
    }

    @Scheduled(fixedDelay = 15 * 60 * 1000) // 15 minutes in milliseconds
    public void checkAndUpdateSLA() {
        System.out.println("time breach started");
        // Query DurationTime entities where severity is SEVERITY_1
        List<DurationTime> durationTimes = durationTimeRepository.findBySeverity("SEVERITY_1");

        for (DurationTime durationTime : durationTimes) {
            // Check if delayedReply is not already set
            if (!durationTime.isSlaBreach()) {
                // Get all related times for the same ticket and attempt
                List<DurationTime> relatedTimes = durationTimeRepository.findByTicketIdAndAttemptsOrderByTimeAsc(
                        durationTime.getTicketId(), durationTime.getAttempts());

                LocalDateTime ticketCreationTime = durationTime.getTime();
                LocalDateTime firstOpenTime = null;
                boolean foundFirstOpen = false;

                for (DurationTime relatedTime : relatedTimes) {
                    if (relatedTime.getStatus().equalsIgnoreCase("RESOLVED")) {
                        System.out.println("sla breach found:");

                        if (!foundFirstOpen) {
                            firstOpenTime = relatedTime.getTime();
                            foundFirstOpen = true;
                            System.out.println("sla breach found 2:"+ firstOpenTime);
                        }
                    }
                }

                boolean shouldUpdate = false;
                if (foundFirstOpen && firstOpenTime != null) {
                    System.out.println("ticket time:"+ ticketCreationTime);
                    System.out.println("sla breach :"+Duration.between(ticketCreationTime, firstOpenTime).toMinutes());
                    if (Duration.between(ticketCreationTime, firstOpenTime).toMinutes() > 360) {
                        shouldUpdate = true;
                    }
                } else {
                    LocalDateTime currentTimeInChicago = LocalDateTime.now(ZoneId.of("America/Chicago"));

                    // Check if more than 360 minutes have passed since ticket creation
                    if (Duration.between(ticketCreationTime, currentTimeInChicago).toMinutes() > 360) {
                        shouldUpdate = true;
                    }
                }

                if (shouldUpdate) {
                    for (DurationTime relatedTime : relatedTimes) {
                        relatedTime.setSlaBreach(true);
                    }
                    durationTimeRepository.saveAll(relatedTimes);
                }
            }
        }
        System.out.println("time breach end");
    }


}


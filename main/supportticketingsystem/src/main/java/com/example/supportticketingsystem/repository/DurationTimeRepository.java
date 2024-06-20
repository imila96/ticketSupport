package com.example.supportticketingsystem.repository;


import com.example.supportticketingsystem.dto.collection.DurationTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DurationTimeRepository extends JpaRepository<DurationTime, Long> {
    List<DurationTime> findByTicketIdOrderByTimeAsc(Long ticketId);

    List<DurationTime> findByTicketId(Long ticketId);

    List<DurationTime> findByTicketIdAndStatus(Long ticketId, String status);

    List<DurationTime> findByTicketIdAndAttemptsOrderByTimeAsc(Long ticketId, int attempts);

}
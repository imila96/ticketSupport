package com.example.supportticketingsystem.repository;

        import com.example.supportticketingsystem.dto.collection.Ticket;
        import com.example.supportticketingsystem.dto.request.SeverityCountDTO;

        import com.example.supportticketingsystem.enums.Severity;
        import org.springframework.data.jpa.repository.JpaRepository;
        import org.springframework.data.jpa.repository.Query;
        import org.springframework.data.repository.query.Param;
        import org.springframework.stereotype.Repository;

        import java.time.YearMonth;
        import java.util.List;
        import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findAllByOrderByIdDesc();

    List<Ticket> findBySeverity(Severity severity);

    List<Ticket> findByProduct(String product);

    List<Ticket> findByEmailAddress(String emailAddress);


    @Query("SELECT new com.example.supportticketingsystem.dto.request.SeverityCountDTO(CAST(t.severity AS string), COUNT(t.id), FUNCTION('MONTH', t.createdAt), FUNCTION('YEAR', t.createdAt)) " +
            "FROM Ticket t " +
            "WHERE FUNCTION('YEAR', t.createdAt) * 100 + FUNCTION('MONTH', t.createdAt) BETWEEN :startMonth AND :endMonth " +
            "GROUP BY t.severity, FUNCTION('YEAR', t.createdAt), FUNCTION('MONTH', t.createdAt)")
    List<SeverityCountDTO> getSeverityCount(@Param("startMonth") Integer startMonth, @Param("endMonth") Integer endMonth);

    @Query("SELECT t FROM Ticket t WHERE CONCAT(',', t.ccEmailAddresses, ',') LIKE %:emailAddress%")
    List<Ticket> findByCcEmailAddressesContaining(@Param("emailAddress") String emailAddress);


    @Query("SELECT t FROM Ticket t WHERE str(t.id) LIKE %:ticketId%")
    List<Ticket> findByTicketIdContaining(@Param("ticketId") String ticketId);

    @Query("SELECT t FROM Ticket t WHERE str(t.subject) LIKE %:subject%")
    List<Ticket> findByTicketSubjectContaining(@Param("subject") String subject);


    @Query("SELECT t FROM Ticket t WHERE t.emailAddress = :emailAddress AND str(t.id) LIKE %:ticketId%")
    List<Ticket> findByEmailAddressAndTicketIdContaining(@Param("emailAddress") String emailAddress, @Param("ticketId") String ticketId);


    @Query("SELECT t FROM Ticket t WHERE t.emailAddress = :emailAddress AND str(t.subject) LIKE %:subject%")
    List<Ticket> findByEmailAddressAndTicketSubjectContaining(@Param("emailAddress") String emailAddress, @Param("subject") String subject);

}
package com.example.supportticketingsystem.repository;

        import com.example.supportticketingsystem.dto.collection.Ticket;
        import com.example.supportticketingsystem.dto.request.SeverityCountDTO;

        import com.example.supportticketingsystem.enums.Severity;
        import com.example.supportticketingsystem.enums.Status;
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

    long countByClientStatus(Status clientStatus);

    long countByVendorStatus(Status vendorStatus);

    long countByEmailAddressAndClientStatus(String emailAddress, Status clientStatus);

    long countByEmailAddressAndVendorStatus(String emailAddress, Status vendorStatus);

    List<Ticket> findBySeverityOrderByIdDesc(Severity severity);

    List<Ticket> findByProductOrderByIdDesc(String product);

    List<Ticket> findByEmailAddressOrderByIdDesc(String emailAddress);



    @Query("SELECT new com.example.supportticketingsystem.dto.request.SeverityCountDTO(CAST(t.severity AS string), COUNT(t.id), FUNCTION('MONTH', t.createdAt), FUNCTION('YEAR', t.createdAt)) " +
            "FROM Ticket t " +
            "WHERE FUNCTION('YEAR', t.createdAt) * 100 + FUNCTION('MONTH', t.createdAt) BETWEEN :startMonth AND :endMonth " +
            "GROUP BY t.severity, FUNCTION('YEAR', t.createdAt), FUNCTION('MONTH', t.createdAt)")
    List<SeverityCountDTO> getSeverityCount(@Param("startMonth") Integer startMonth, @Param("endMonth") Integer endMonth);

    @Query("SELECT t FROM Ticket t WHERE CONCAT(',', t.ccEmailAddresses, ',') LIKE %:emailAddress% ORDER BY t.id DESC")
    List<Ticket> findByCcEmailAddressesContaining(@Param("emailAddress") String emailAddress);

    @Query("SELECT t FROM Ticket t WHERE str(t.id) LIKE %:ticketId% ORDER BY t.id DESC")
    List<Ticket> findByTicketIdContaining(@Param("ticketId") String ticketId);

    @Query("SELECT t FROM Ticket t WHERE str(t.subject) LIKE %:subject% ORDER BY t.id DESC")
    List<Ticket> findByTicketSubjectContaining(@Param("subject") String subject);

    @Query("SELECT t FROM Ticket t WHERE t.emailAddress = :emailAddress AND str(t.id) LIKE %:ticketId% ORDER BY t.id DESC")
    List<Ticket> findByEmailAddressAndTicketIdContaining(@Param("emailAddress") String emailAddress, @Param("ticketId") String ticketId);

    @Query("SELECT t FROM Ticket t WHERE t.emailAddress = :emailAddress AND str(t.subject) LIKE %:subject% ORDER BY t.id DESC")
    List<Ticket> findByEmailAddressAndTicketSubjectContaining(@Param("emailAddress") String emailAddress, @Param("subject") String subject);

    Optional<Ticket> findByReferenceNumberOrderByIdDesc(String referenceNumber);

}
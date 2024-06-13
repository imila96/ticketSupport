package com.example.supportticketingsystem.repository;

        import com.example.supportticketingsystem.dto.collection.Ticket;
        import com.example.supportticketingsystem.enums.Product;
        import com.example.supportticketingsystem.enums.Severity;
        import org.springframework.data.jpa.repository.JpaRepository;
        import org.springframework.stereotype.Repository;

        import java.util.List;
        import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findAllByOrderByIdDesc();

    List<Ticket> findBySeverity(Severity severity);

    List<Ticket> findByProduct(Product product);

    List<Ticket> findByEmailAddress(String emailAddress);
}
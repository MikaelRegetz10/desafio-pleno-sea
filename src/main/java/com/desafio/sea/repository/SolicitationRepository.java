package com.desafio.sea.repository;

import com.desafio.sea.domain.Solicitation;
import com.desafio.sea.domain.enums.SolicitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SolicitationRepository extends JpaRepository<Solicitation, UUID> {
    @Query("SELECT s FROM Solicitation s WHERE s.state IN :states AND (:status IS NULL OR s.status = :status)")
    List<Solicitation> findByStateInAndOptionalStatus(
            @Param("states") Collection<String> states,
            @Param("status") SolicitationStatus status
    );

    @Query("SELECT s FROM Solicitation s WHERE (:status IS NULL OR s.status = :status)")
    List<Solicitation> findByOptionalStatus(@Param("status") SolicitationStatus status);
}

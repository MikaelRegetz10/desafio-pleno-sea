package com.desafio.sea.repository;

import com.desafio.sea.domain.Solicitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SolicitationRepository extends JpaRepository<Solicitation, UUID> {
}

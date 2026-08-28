package com.desafio.sea.repository;

import com.desafio.sea.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.coverageStates")
    List<User> findAllWithCoverageStates();
}

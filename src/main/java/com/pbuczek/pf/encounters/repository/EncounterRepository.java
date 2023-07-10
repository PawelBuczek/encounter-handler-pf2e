package com.pbuczek.pf.encounters.repository;

import com.pbuczek.pf.encounters.Encounter;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Integer> {

    Optional<Encounter> findById(Integer id);

    @Transactional
    @Modifying
    @Query("DELETE FROM Encounter u WHERE u.id = ?1")
    int deleteEncounter(Integer id);

}
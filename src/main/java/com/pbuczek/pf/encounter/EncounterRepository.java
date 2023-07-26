package com.pbuczek.pf.encounter;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface EncounterRepository extends JpaRepository<Encounter, Integer> {

    Optional<Encounter> findById(Integer id);

    List<Encounter> findByUserId(Integer userId);

    @Query("SELECT COUNT(*) FROM Encounter e WHERE e.userId = ?1")
    Integer getCountOfEncountersByUserId(Integer userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Encounter e WHERE e.id = ?1")
    int deleteEncounter(Integer id);

}
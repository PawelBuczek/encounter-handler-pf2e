package com.pbuczek.pf.apikey;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Integer> {

    Optional<ApiKey> findById(Integer id);

    @Query("SELECT a.validTillDate FROM ApiKey a WHERE a.identifier = ?1")
    Optional<LocalDate> getValidTillDateByIdentifier(String identifier);

    @Query("SELECT a.userId FROM ApiKey a WHERE a.identifier = ?1")
    Optional<Integer> getUserIdByApiKeyIdentifier(String apiKey);

    List<ApiKey> findByUserId(Integer userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM ApiKey a WHERE a.id = ?1")
    int deleteApiKeyById(Integer id);

    @Transactional
    @Modifying
    @Query("DELETE FROM ApiKey a WHERE a.apiKeyValue = ?1")
    int deleteApiKeyByValue(String id);

}
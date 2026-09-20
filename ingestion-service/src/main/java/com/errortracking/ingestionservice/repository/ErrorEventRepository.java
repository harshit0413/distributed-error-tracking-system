package com.errortracking.ingestionservice.repository;

import com.errortracking.ingestionservice.entity.ErrorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorEventRepository extends JpaRepository<ErrorEvent, Long> {

    // TimescaleDB Native Query: Pichle 24 ghante ka hourly average jisme current unfinished hour ko exclude kar diya hai
    @Query(value = "SELECT COALESCE(AVG(cnt), 0) FROM (SELECT COUNT(*) as cnt FROM errors WHERE fingerprint = :fingerprint AND occurred_at >= NOW() - INTERVAL '24 hours' AND occurred_at < date_trunc('hour', NOW()) GROUP BY date_trunc('hour', occurred_at)) sub", nativeQuery = true)
    Double getHistoricalHourlyAverage(@Param("fingerprint") String fingerprint);

    // Sirf ek sample error uthane ke liye (Groq AI ko dene ke liye)
    ErrorEvent findFirstByFingerprint(String fingerprint);
}
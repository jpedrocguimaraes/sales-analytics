package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.ServerStatusSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ServerStatusRepository extends JpaRepository<ServerStatusSnapshot, Long> {

    List<ServerStatusSnapshot> findAllByTimestampBetweenOrderByTimestampAsc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.timestamp BETWEEN :start AND :end ORDER BY s.onlinePlayers DESC LIMIT 1")
    ServerStatusSnapshot findPeakSnapshot(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.timestamp BETWEEN :start AND :end ORDER BY s.onlinePlayers ASC LIMIT 1")
    ServerStatusSnapshot findFloorSnapshot(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT AVG(s.onlinePlayers) FROM ServerStatusSnapshot s WHERE s.timestamp BETWEEN :start AND :end")
    Double findAveragePlayers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.ServerStatusSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ServerStatusRepository extends JpaRepository<ServerStatusSnapshot, Long> {

    List<ServerStatusSnapshot> findAllByServerIdAndTimestampBetweenOrderByTimestampAsc(Long serverId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.server.id = :serverId AND s.timestamp BETWEEN :start AND :end ORDER BY s.onlinePlayers DESC LIMIT 1")
    Optional<ServerStatusSnapshot> findPeakSnapshot(@Param("serverId") Long serverId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT s FROM ServerStatusSnapshot s WHERE s.server.id = :serverId AND s.online = true AND s.onlinePlayers > 0 AND s.timestamp BETWEEN :start AND :end ORDER BY s.onlinePlayers ASC LIMIT 1")
    Optional<ServerStatusSnapshot> findFloorSnapshot(@Param("serverId") Long serverId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT AVG(s.onlinePlayers) FROM ServerStatusSnapshot s WHERE s.server.id = :serverId AND s.online = true AND s.timestamp BETWEEN :start AND :end")
    Double findAveragePlayers(@Param("serverId") Long serverId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Optional<ServerStatusSnapshot> findTopByServerIdOrderByTimestampDesc(Long serverId);

    long countByServerIdAndTimestampBetween(Long serverId, LocalDateTime start, LocalDateTime end);

    long countByServerIdAndOnlineTrueAndTimestampBetween(Long serverId, LocalDateTime start, LocalDateTime end);
}
package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.ExecutionLog;
import br.com.fourzerofourdev.salesanalyticsbackend.model.enums.LogType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    @Query("""
        SELECT l FROM ExecutionLog l
        WHERE l.server.id = :serverId
        AND (:type IS NULL OR l.type = :type)
    """)
    Page<ExecutionLog> findLogsByServerIdAndType(@Param("serverId") Long serverId, @Param("type") LogType type, Pageable pageable);
}
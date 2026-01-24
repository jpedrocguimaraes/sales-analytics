package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.ExecutionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {

    @Query("SELECT l FROM ExecutionLog l ORDER BY l.startTime DESC")
    Page<ExecutionLog> findAllLogs(Pageable pageable);
}
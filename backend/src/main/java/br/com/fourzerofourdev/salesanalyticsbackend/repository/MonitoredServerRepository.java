package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.MonitoredServer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitoredServerRepository extends JpaRepository<MonitoredServer, Long> {
    List<MonitoredServer> findAllByActiveTrue();
    boolean existsByName(String name);
}
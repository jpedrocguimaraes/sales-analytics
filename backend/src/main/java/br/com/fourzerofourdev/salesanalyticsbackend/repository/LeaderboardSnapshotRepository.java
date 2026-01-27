package br.com.fourzerofourdev.salesanalyticsbackend.repository;

import br.com.fourzerofourdev.salesanalyticsbackend.model.Customer;
import br.com.fourzerofourdev.salesanalyticsbackend.model.LeaderboardSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LeaderboardSnapshotRepository extends JpaRepository<LeaderboardSnapshot, Long> {

    Optional<LeaderboardSnapshot> findTopByCustomerOrderBySnapshotTimeDesc(Customer customer);

    Optional<LeaderboardSnapshot> findTopByServerIdOrderBySnapshotTimeDesc(Long serverId);
}
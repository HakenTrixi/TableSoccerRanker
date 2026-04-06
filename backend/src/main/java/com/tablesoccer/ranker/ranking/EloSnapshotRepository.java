package com.tablesoccer.ranker.ranking;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EloSnapshotRepository extends JpaRepository<EloSnapshot, UUID> {

    Optional<EloSnapshot> findByUserIdAndSnapshotDate(UUID userId, LocalDate snapshotDate);

    List<EloSnapshot> findByUserIdOrderBySnapshotDateAsc(UUID userId);

    List<EloSnapshot> findBySnapshotDate(LocalDate snapshotDate);
}

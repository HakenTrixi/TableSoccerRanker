package com.tablesoccer.ranker.match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, UUID> {

    List<MatchPlayer> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<MatchPlayer> findByMatchId(UUID matchId);

    @Query("SELECT mp FROM MatchPlayer mp JOIN FETCH mp.match m JOIN FETCH mp.user u " +
           "WHERE mp.eloAfter IS NOT NULL ORDER BY m.playedAt ASC")
    List<MatchPlayer> findAllWithEloData();

    @Query("SELECT mp FROM MatchPlayer mp JOIN FETCH mp.match m " +
           "WHERE mp.user.id = :userId AND mp.eloAfter IS NOT NULL ORDER BY m.playedAt ASC")
    List<MatchPlayer> findByUserIdWithEloData(UUID userId);

    @Query("SELECT mp FROM MatchPlayer mp JOIN FETCH mp.match m JOIN FETCH mp.user u " +
           "WHERE mp.eloAfter IS NOT NULL AND m.playedAt BETWEEN :from AND :to ORDER BY m.playedAt ASC")
    List<MatchPlayer> findWithEloDataInPeriod(Instant from, Instant to);
}

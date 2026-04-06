package com.tablesoccer.ranker.ranking;

import com.tablesoccer.ranker.user.User;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "elo_snapshots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "snapshot_date"}))
public class EloSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "elo_rating", nullable = false)
    private int eloRating;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    public EloSnapshot() {}

    public EloSnapshot(User user, int eloRating, LocalDate snapshotDate) {
        this.user = user;
        this.eloRating = eloRating;
        this.snapshotDate = snapshotDate;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public int getEloRating() { return eloRating; }
    public void setEloRating(int eloRating) { this.eloRating = eloRating; }
    public LocalDate getSnapshotDate() { return snapshotDate; }
}

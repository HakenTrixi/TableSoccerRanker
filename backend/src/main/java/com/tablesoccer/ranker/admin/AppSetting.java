package com.tablesoccer.ranker.admin;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    @Column(length = 100)
    private String key;

    @Column(nullable = false, length = 500)
    private String value;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void onSave() {
        updatedAt = Instant.now();
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public Instant getUpdatedAt() { return updatedAt; }
}

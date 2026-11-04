package io.github.hanziertoo.masterdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "change_record")
public class ChangeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(nullable = false)
    private String action;

    @Column(name = "before_json", columnDefinition = "TEXT")
    private String beforeJson;

    @Column(name = "after_json", columnDefinition = "TEXT")
    private String afterJson;

    public ChangeRecord() {
    }

    public ChangeRecord(String actor, String entityType, Long entityId, String action,
            String beforeJson, String afterJson) {
        this.actor = actor;
        this.occurredAt = Instant.now();
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.beforeJson = beforeJson;
        this.afterJson = afterJson;
    }
}

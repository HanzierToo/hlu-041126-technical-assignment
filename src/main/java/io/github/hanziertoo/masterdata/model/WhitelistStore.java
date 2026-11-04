package io.github.hanziertoo.masterdata.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "whitelist_store")
public class WhitelistStore {

    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WhitelistStore() {
    }

    public WhitelistStore(Long storeId) {
        this.storeId = storeId;
        this.createdAt = Instant.now();
    }
}

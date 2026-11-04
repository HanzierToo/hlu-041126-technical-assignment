package io.github.hanziertoo.masterdata.repository;

import io.github.hanziertoo.masterdata.model.WhitelistStore;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WhitelistRepository extends JpaRepository<WhitelistStore, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WhitelistStore w where w.storeId = :storeId")
    Optional<WhitelistStore> findForUpdate(@Param("storeId") Long storeId);
}

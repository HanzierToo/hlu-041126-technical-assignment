package io.github.hanziertoo.masterdata.repository;

import io.github.hanziertoo.masterdata.model.Branch;

import jakarta.persistence.LockModeType;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select b from Branch b
        join fetch b.province
        where b.id = :id
          and b.deleted = false
          and b.province.active = true
          and b.province.deleted = false
        """)
    Optional<Branch> findForUpdate(@Param("id") Long id);
}

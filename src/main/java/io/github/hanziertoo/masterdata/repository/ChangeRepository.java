package io.github.hanziertoo.masterdata.repository;

import io.github.hanziertoo.masterdata.model.ChangeRecord;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeRepository extends JpaRepository<ChangeRecord, Long> {
}

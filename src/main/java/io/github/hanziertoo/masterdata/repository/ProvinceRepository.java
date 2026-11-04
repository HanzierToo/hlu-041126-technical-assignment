package io.github.hanziertoo.masterdata.repository;

import io.github.hanziertoo.masterdata.model.Province;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProvinceRepository extends JpaRepository<Province, Long> {

    List<Province> findByNameContainingAndActiveTrueAndDeletedFalse(String name);
}

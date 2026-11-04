package io.github.hanziertoo.masterdata.repository;

import io.github.hanziertoo.masterdata.model.AppUser;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);
}

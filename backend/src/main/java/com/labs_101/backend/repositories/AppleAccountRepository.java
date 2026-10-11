package com.labs_101.backend.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.labs_101.backend.entities.AppleAccount;

public interface AppleAccountRepository extends JpaRepository<AppleAccount, Long> {
    Optional<AppleAccount> findByUser_IdAndAppleIdIgnoreCase(String userId, String appleId);

    Optional<AppleAccount> findByIdAndUser_Id(Long id, String userId);
}

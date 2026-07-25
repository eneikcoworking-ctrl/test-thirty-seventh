package com.eneik.generated.repository;

import com.eneik.generated.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByTelegramId(String telegramId);

    Page<Account> findByWarmUpStatus(String warmUpStatus, Pageable pageable);
}

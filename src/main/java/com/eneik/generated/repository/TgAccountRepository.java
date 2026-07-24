package com.eneik.generated.repository;

import com.eneik.generated.domain.TgAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TgAccountRepository extends JpaRepository<TgAccount, Long> {
}

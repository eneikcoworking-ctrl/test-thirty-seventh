package com.eneik.generated.leadgen.repository;

import com.eneik.generated.leadgen.model.TelegramAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramAccountRepository extends JpaRepository<TelegramAccount, String> {
}

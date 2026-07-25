package com.eneik.generated.repository;

import com.eneik.generated.model.AiState;
import com.eneik.generated.model.Dialog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DialogRepository extends JpaRepository<Dialog, Long> {
    Optional<Dialog> findByTelegramChatId(String telegramChatId);

    @Modifying
    @Query("UPDATE Dialog d SET d.aiState = :newState WHERE d.id = :id AND d.aiState = :oldState")
    int updateAiStateGuarded(@Param("id") Long id, @Param("oldState") AiState oldState, @Param("newState") AiState newState);
}

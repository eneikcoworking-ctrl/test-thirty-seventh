package com.eneik.generated.repository;

import com.eneik.generated.model.WarmUpTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarmUpTaskRepository extends JpaRepository<WarmUpTask, Long> {
    List<WarmUpTask> findByAccountId(Long accountId);
    List<WarmUpTask> findByAccountIdAndStatus(Long accountId, String status);
}

package com.eneik.generated.repository;

import com.eneik.generated.model.WarmUpCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarmUpCycleRepository extends JpaRepository<WarmUpCycle, Long> {
    List<WarmUpCycle> findByAccountIdAndStatus(Long accountId, String status);
}

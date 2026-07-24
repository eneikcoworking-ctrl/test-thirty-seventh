package com.eneik.generated.repository;

import com.eneik.generated.domain.DailyLimitTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;

public interface DailyLimitTrackerRepository extends JpaRepository<DailyLimitTracker, Long> {
    Optional<DailyLimitTracker> findByTgAccountIdAndTrackedDate(Long tgAccountId, LocalDate date);
}

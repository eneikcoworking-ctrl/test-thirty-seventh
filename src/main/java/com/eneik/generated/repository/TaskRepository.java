package com.eneik.generated.repository;

import com.eneik.generated.domain.Task;
import com.eneik.generated.domain.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TaskRepository extends JpaRepository<Task, String> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Task t SET t.status = :newStatus, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :id AND t.status = :expectedStatus")
    int updateStatusAtomically(
            @Param("id") String id,
            @Param("newStatus") TaskStatus newStatus,
            @Param("expectedStatus") TaskStatus expectedStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Task t SET t.status = :newStatus, t.rejectionCount = t.rejectionCount + 1, t.updatedAt = CURRENT_TIMESTAMP WHERE t.id = :id AND t.status = :expectedStatus")
    int rejectAndIncrementAtomically(
            @Param("id") String id,
            @Param("newStatus") TaskStatus newStatus,
            @Param("expectedStatus") TaskStatus expectedStatus
    );
}

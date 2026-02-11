package by.losik.errorfreetext.repository;

import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorrectionTaskRepository extends JpaRepository<CorrectionTask, UUID> {
    @Query("SELECT t FROM CorrectionTask t WHERE t.id = :id")
    Optional<CorrectionTask> findById(@Param("id") UUID id);

    @Transactional
    @Modifying
    @Query(value = """
    WITH next_task AS (
        SELECT id FROM correction_tasks
        WHERE status = 'NEW'
        ORDER BY created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
    )
    UPDATE correction_tasks 
    SET status = 'PROCESSING', 
        processed_at = NOW(),
        version = version + 1
    FROM next_task
    WHERE correction_tasks.id = next_task.id
    RETURNING correction_tasks.id   -- ← явно указываем таблицу!
    """, nativeQuery = true)
    List<UUID> pollAndGetId();

    default Optional<CorrectionTask> pollNextTask() {
        return pollAndGetId().stream()
                .findFirst()
                .flatMap(this::findById);
    }

    @Transactional
    @Modifying
    @Query("UPDATE CorrectionTask t " +
            "SET t.status = 'COMPLETED', " +
            "    t.correctedText = :correctedText, " +
            "    t.processedAt = :processedAt, " +
            "    t.version = t.version + 1 " +
            "WHERE t.id = :id AND t.status = 'PROCESSING'")
    int markAsCompleted(@Param("id") UUID id,
                        @Param("correctedText") String correctedText,
                        @Param("processedAt") LocalDateTime processedAt);

    @Transactional
    @Modifying
    @Query("UPDATE CorrectionTask t " +
            "SET t.status = 'FAILED', " +
            "    t.errorMessage = :errorMessage, " +
            "    t.processedAt = :processedAt, " +
            "    t.version = t.version + 1 " +
            "WHERE t.id = :id AND t.status = 'PROCESSING'")
    int markAsFailed(@Param("id") UUID id,
                     @Param("errorMessage") String errorMessage,
                     @Param("processedAt") LocalDateTime processedAt);
}
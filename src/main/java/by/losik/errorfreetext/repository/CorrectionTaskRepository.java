package by.losik.errorfreetext.repository;

import by.losik.errorfreetext.entity.CorrectionTask;
import by.losik.errorfreetext.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CorrectionTaskRepository extends JpaRepository<CorrectionTask, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM CorrectionTask t " +
            "WHERE t.id = :id")
    Optional<CorrectionTask> findById(@Param("id") UUID id);

    @Query("SELECT t FROM CorrectionTask t " +
            "WHERE t.status = :status " +
            "ORDER BY t.createdAt ASC")
    List<CorrectionTask> findByStatusOrderByCreatedAtAsc(@Param("status") TaskStatus status);

    @Transactional
    @Modifying
    @Query("UPDATE CorrectionTask t " +
            "SET t.status = :newStatus, t.processedAt = :processedAt " +
            "WHERE t.id = :id " +
            "AND t.status = :oldStatus")
    int updateStatus(@Param("id") UUID id,
                     @Param("oldStatus") TaskStatus oldStatus,
                     @Param("newStatus") TaskStatus newStatus,
                     @Param("processedAt") LocalDateTime processedAt);

    @Transactional
    @Modifying
    @Query("UPDATE CorrectionTask t " +
            "SET t.status = :processingStatus, t.processedAt = :processedAt " +
            "WHERE t.id = :id " +
            "AND t.status = :newStatus")
    int markAsProcessing(@Param("id") UUID id,
                         @Param("newStatus") TaskStatus newStatus,
                         @Param("processingStatus") TaskStatus processingStatus,
                         @Param("processedAt") LocalDateTime processedAt);

    default int markAsProcessing(UUID id, LocalDateTime processedAt) {
        return markAsProcessing(id, TaskStatus.NEW, TaskStatus.PROCESSING, processedAt);
    }

    @Transactional
    @Modifying
    @Query("UPDATE CorrectionTask t " +
            "SET t.status = :completedStatus, t.correctedText = :correctedText, t.processedAt = :processedAt " +
            "WHERE t.id = :id " +
            "AND t.status = :processingStatus")
    int markAsCompleted(@Param("id") UUID id,
                        @Param("processingStatus") TaskStatus processingStatus,
                        @Param("completedStatus") TaskStatus completedStatus,
                        @Param("correctedText") String correctedText,
                        @Param("processedAt") LocalDateTime processedAt);

    default int markAsCompleted(UUID id, String correctedText, LocalDateTime processedAt) {
        return markAsCompleted(id, TaskStatus.PROCESSING, TaskStatus.COMPLETED, correctedText, processedAt);
    }

    @Transactional
    @Modifying
    @Query("UPDATE CorrectionTask t " +
            "SET t.status = :failedStatus, t.errorMessage = :errorMessage, t.processedAt = :processedAt " +
            "WHERE t.id = :id " +
            "AND t.status IN (:validStatuses)")
    int markAsFailed(@Param("id") UUID id,
                     @Param("validStatuses") List<TaskStatus> validStatuses,
                     @Param("failedStatus") TaskStatus failedStatus,
                     @Param("errorMessage") String errorMessage,
                     @Param("processedAt") LocalDateTime processedAt);

    default int markAsFailed(UUID id, String errorMessage, LocalDateTime processedAt) {
        List<TaskStatus> validStatuses = List.of(TaskStatus.NEW, TaskStatus.PROCESSING);
        return markAsFailed(id, validStatuses, TaskStatus.FAILED, errorMessage, processedAt);
    }
}
package com.cotato.nextstation.domain.auth.repository;

import com.cotato.nextstation.domain.auth.entity.EmailVerification;
import com.cotato.nextstation.domain.auth.entity.VerificationStatus;
import com.cotato.nextstation.domain.auth.entity.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findFirstByEmailAndTypeAndStatusOrderByCreatedAtDesc(
            String email, VerificationType type, VerificationStatus status);

    @Modifying
    @Query("DELETE FROM EmailVerification e WHERE e.status IN :statuses AND e.createdAt < :threshold")
    int deleteByStatusInAndCreatedAtBefore(
            @Param("statuses") List<VerificationStatus> statuses, @Param("threshold") LocalDateTime threshold);
}
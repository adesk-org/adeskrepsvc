package com.adesk.repsvc.repository;

import com.adesk.repsvc.domain.RepOutboxEntity;
import com.adesk.repsvc.domain.RepOutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;

public interface RepOutboxRepository extends JpaRepository<RepOutboxEntity, UUID> {

    List<RepOutboxEntity> findByStatusAndAvailableAtLessThanEqual(
            RepOutboxStatus status,
            OffsetDateTime threshold,
            Pageable pageable);
}

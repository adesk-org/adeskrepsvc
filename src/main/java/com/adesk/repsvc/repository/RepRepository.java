package com.adesk.repsvc.repository;

import com.adesk.repsvc.domain.RepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface RepRepository extends JpaRepository<RepEntity, UUID>, JpaSpecificationExecutor<RepEntity> {
    Optional<RepEntity> findByIdAndTenantId(UUID id, UUID tenantId);
}

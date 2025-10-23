package com.adesk.repsvc.service;

import com.adesk.repsvc.domain.RepEntity;
import com.adesk.repsvc.domain.RepEventType;
import com.adesk.repsvc.domain.RepOutboxEntity;
import com.adesk.repsvc.domain.RepOutboxStatus;
import com.adesk.repsvc.dto.RepEventPayload;
import com.adesk.repsvc.mapper.RepMapper;
import com.adesk.repsvc.repository.RepOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class RepOutboxService {

    private final RepOutboxRepository outboxRepository;
    private final RepMapper repMapper;
    private final ObjectMapper objectMapper;

    public RepOutboxService(RepOutboxRepository outboxRepository, RepMapper repMapper, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.repMapper = repMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(RepEntity rep, RepEventType eventType) {
        RepOutboxEntity entity = new RepOutboxEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateId(rep.getId());
        entity.setTenantId(rep.getTenantId());
        entity.setEventType(eventType.name());
        entity.setStatus(RepOutboxStatus.PENDING);
        entity.setAttemptCount(0);
        entity.setPayload(serializePayload(rep, eventType));
        entity.setAvailableAt(OffsetDateTime.now());
        outboxRepository.save(entity);
    }

    private String serializePayload(RepEntity rep, RepEventType type) {
        RepEventPayload payload = new RepEventPayload(
                type.name(),
                OffsetDateTime.now(),
                repMapper.toResponse(rep)
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize outbox payload", e);
        }
    }
}

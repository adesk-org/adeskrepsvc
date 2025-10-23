package com.adesk.repsvc.mapper;

import com.adesk.repsvc.domain.RepEntity;
import com.adesk.repsvc.dto.RepCreateRequest;
import com.adesk.repsvc.dto.RepPutRequest;
import com.adesk.repsvc.dto.RepResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class RepMapper {

    public RepResponse toResponse(RepEntity entity) {
        return new RepResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getUserId(),
                entity.isActive(),
                new HashMap<>(entity.getAttributes()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public RepEntity fromCreate(UUID tenantId, RepCreateRequest request) {
        RepEntity entity = new RepEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setUserId(request.userId());
        entity.setActive(request.isActive() == null || request.isActive());
        entity.setAttributes(sanitiseAttributes(request.attributes()));
        return entity;
    }

    public void applyPut(RepEntity target, RepPutRequest request) {
        target.setUserId(request.userId());
        target.setActive(Boolean.TRUE.equals(request.isActive()));
        target.setAttributes(sanitiseAttributes(request.attributes()));
    }

    public void applyPartial(RepEntity target, Map<String, Object> patchValues) {
        if (patchValues.containsKey("userId")) {
            Object value = patchValues.get("userId");
            if (value != null) {
                if (value instanceof UUID uuid) {
                    target.setUserId(uuid);
                } else {
                    target.setUserId(UUID.fromString(value.toString()));
                }
            }
        }
        if (patchValues.containsKey("isActive")) {
            Object value = patchValues.get("isActive");
            if (value != null) {
                if (value instanceof Boolean bool) {
                    target.setActive(bool);
                } else {
                    target.setActive(Boolean.parseBoolean(value.toString()));
                }
            }
        }
        if (patchValues.containsKey("attributes") && patchValues.get("attributes") instanceof Map<?, ?> map) {
            target.setAttributes(sanitiseAttributes((Map<String, Object>) map));
        }
    }

    private Map<String, Object> sanitiseAttributes(Map<String, Object> input) {
        if (input == null) {
            return new HashMap<>();
        }
        return new HashMap<>(input);
    }
}

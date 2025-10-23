package com.adesk.repsvc.service;

import com.adesk.repsvc.domain.RepEntity;
import com.adesk.repsvc.domain.RepEventType;
import com.adesk.repsvc.dto.RepCreateRequest;
import com.adesk.repsvc.dto.RepPageResponse;
import com.adesk.repsvc.dto.RepPutRequest;
import com.adesk.repsvc.dto.RepResponse;
import com.adesk.repsvc.error.BadRequestException;
import com.adesk.repsvc.error.NotFoundException;
import com.adesk.repsvc.mapper.RepMapper;
import com.adesk.repsvc.repository.RepRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RepService {

    private final RepRepository repRepository;
    private final RepMapper repMapper;
    private final RepOutboxService outboxService;

    public RepService(RepRepository repRepository,
                      RepMapper repMapper,
                      RepOutboxService outboxService) {
        this.repRepository = repRepository;
        this.repMapper = repMapper;
        this.outboxService = outboxService;
    }

    @Transactional(readOnly = true)
    public RepPageResponse list(UUID tenantId,
                                int page,
                                int size,
                                Sort sort,
                                Boolean isActive,
                                UUID userId) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<RepEntity> specification = buildSpecification(tenantId, isActive, userId);
        Page<RepEntity> result = repRepository.findAll(specification, pageable);
        return new RepPageResponse(
                result.map(repMapper::toResponse).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    @Transactional
    public RepResponse create(UUID tenantId, RepCreateRequest request) {
        RepEntity rep = repMapper.fromCreate(tenantId, request);
        RepEntity saved = repRepository.save(rep);
        outboxService.enqueue(saved, RepEventType.REP_CREATED);
        return repMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public RepResponse get(UUID tenantId, UUID repId) {
        return repRepository.findByIdAndTenantId(repId, tenantId)
                .map(repMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("Rep '%s' not found".formatted(repId)));
    }

    @Transactional
    public RepResponse replace(UUID tenantId, UUID repId, RepPutRequest request) {
        RepEntity existing = repRepository.findByIdAndTenantId(repId, tenantId)
                .orElseThrow(() -> new NotFoundException("Rep '%s' not found".formatted(repId)));
        repMapper.applyPut(existing, request);
        RepEntity saved = repRepository.save(existing);
        outboxService.enqueue(saved, RepEventType.REP_UPDATED);
        return repMapper.toResponse(saved);
    }

    @Transactional
    public RepResponse patch(UUID tenantId, UUID repId, Map<String, Object> patch) {
        if (patch == null || patch.isEmpty()) {
            return get(tenantId, repId);
        }
        RepEntity existing = repRepository.findByIdAndTenantId(repId, tenantId)
                .orElseThrow(() -> new NotFoundException("Rep '%s' not found".formatted(repId)));

        Map<String, Object> filtered = sanitizePatch(patch);
        if (filtered.isEmpty()) {
            return repMapper.toResponse(existing);
        }

        repMapper.applyPartial(existing, filtered);
        RepEntity saved = repRepository.save(existing);
        outboxService.enqueue(saved, RepEventType.REP_UPDATED);
        return repMapper.toResponse(saved);
    }

    private Specification<RepEntity> buildSpecification(UUID tenantId, Boolean isActive, UUID userId) {
        Specification<RepEntity> spec = (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
        if (isActive != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), isActive));
        }
        if (userId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        return spec;
    }

    private Map<String, Object> sanitizePatch(Map<String, Object> patch) {
        Map<String, Object> result = new HashMap<>();
        if (patch.containsKey("userId")) {
            Object value = patch.get("userId");
            if (value == null) {
                throw new BadRequestException("userId cannot be null");
            }
            try {
                UUID userId = value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
                result.put("userId", userId);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("userId must be a valid UUID");
            }
        }
        if (patch.containsKey("isActive")) {
            Object value = patch.get("isActive");
            if (value == null) {
                throw new BadRequestException("isActive cannot be null");
            }
            if (value instanceof Boolean bool) {
                result.put("isActive", bool);
            } else {
                String token = value.toString().toLowerCase();
                if (!token.equals("true") && !token.equals("false")) {
                    throw new BadRequestException("isActive must be a boolean");
                }
                result.put("isActive", Boolean.parseBoolean(token));
            }
        }
        if (patch.containsKey("attributes")) {
            Object value = patch.get("attributes");
            if (value == null) {
                throw new BadRequestException("attributes cannot be null");
            }
            if (!(value instanceof Map<?, ?> rawMap)) {
                throw new BadRequestException("attributes must be an object");
            }
            Map<String, Object> attributes = new HashMap<>();
            rawMap.forEach((k, v) -> attributes.put(String.valueOf(k), v));
            result.put("attributes", attributes);
        }
        return result;
    }
}

package com.adesk.repsvc.controller;

import com.adesk.repsvc.dto.RepCreateRequest;
import com.adesk.repsvc.dto.RepPageResponse;
import com.adesk.repsvc.dto.RepPutRequest;
import com.adesk.repsvc.dto.RepResponse;
import com.adesk.repsvc.error.BadRequestException;
import com.adesk.repsvc.service.RepService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v0")
public class RepController {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt", "userId", "isActive");

    private final RepService repService;
    private final ObjectMapper objectMapper;

    public RepController(RepService repService, ObjectMapper objectMapper) {
        this.repService = repService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/reps")
    public RepPageResponse listReps(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sort", required = false) List<String> sortParams,
            @RequestParam(value = "isActive", required = false) Boolean isActive,
            @RequestParam(value = "userId", required = false) UUID userId) {
        Sort sort = parseSort(sortParams);
        return repService.list(tenantId, page, size, sort, isActive, userId);
    }

    @PostMapping("/reps")
    public ResponseEntity<RepResponse> createRep(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestBody @Valid RepCreateRequest body) {
        RepResponse created = repService.create(tenantId, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(location(created.id()))
                .body(created);
    }

    @GetMapping("/reps/{id}")
    public RepResponse getRep(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("id") UUID repId) {
        return repService.get(tenantId, repId);
    }

    @PutMapping("/reps/{id}")
    public RepResponse replaceRep(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("id") UUID repId,
            @RequestBody @Valid RepPutRequest body) {
        return repService.replace(tenantId, repId, body);
    }

    @PatchMapping(value = "/reps/{id}", consumes = "application/merge-patch+json")
    public RepResponse patchRep(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @PathVariable("id") UUID repId,
            @RequestBody JsonNode patch) {
        if (patch == null || !patch.isObject()) {
            throw new BadRequestException("Patch body must be a JSON object");
        }
        Map<String, Object> patchMap = objectMapper.convertValue(patch, MAP_TYPE);
        return repService.patch(tenantId, repId, patchMap);
    }

    private URI location(UUID repId) {
        return URI.create("/v0/reps/" + repId);
    }

    private Sort parseSort(List<String> sortParams) {
        if (CollectionUtils.isEmpty(sortParams)) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }
        Set<String> seenProperties = new HashSet<>();
        List<Sort.Order> orders = sortParams.stream()
                .map(String::trim)
                .filter(param -> !param.isBlank())
                .map(param -> parseSortExpression(param, seenProperties))
                .collect(Collectors.toList());

        if (orders.isEmpty()) {
            return Sort.by(Sort.Order.desc("createdAt"));
        }
        return Sort.by(orders);
    }

    private Sort.Order parseSortExpression(String expression, Set<String> seenProperties) {
        String[] parts = expression.split(",");
        String property = parts[0].trim();
        if (!ALLOWED_SORT_FIELDS.contains(property)) {
            throw new BadRequestException("Unsupported sort property: " + property);
        }
        if (!seenProperties.add(property)) {
            throw new BadRequestException("Duplicate sort property: " + property);
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            String dirToken = parts[1].trim().toLowerCase();
            if ("desc".equals(dirToken)) {
                direction = Sort.Direction.DESC;
            } else if (!"asc".equals(dirToken) && !dirToken.isBlank()) {
                throw new BadRequestException("Unsupported sort direction: " + dirToken);
            }
        }
        return new Sort.Order(direction, property);
    }
}

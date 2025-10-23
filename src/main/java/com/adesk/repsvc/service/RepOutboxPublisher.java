package com.adesk.repsvc.service;

import com.adesk.repsvc.config.AppProperties;
import com.adesk.repsvc.domain.RepOutboxEntity;
import com.adesk.repsvc.domain.RepOutboxStatus;
import com.adesk.repsvc.repository.RepOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class RepOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(RepOutboxPublisher.class);

    private final RepOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppProperties properties;

    public RepOutboxPublisher(RepOutboxRepository outboxRepository,
                              KafkaTemplate<String, String> kafkaTemplate,
                              AppProperties properties) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.outbox.poll-delay-ms:5000}")
    public void publishPending() {
        int batchSize = Math.max(1, properties.getOutbox().getBatchSize());
        List<RepOutboxEntity> batch = outboxRepository.findByStatusAndAvailableAtLessThanEqual(
                RepOutboxStatus.PENDING,
                OffsetDateTime.now(),
                PageRequest.of(0, batchSize, Sort.by(Sort.Direction.ASC, "createdAt")));

        if (batch.isEmpty()) {
            return;
        }

        for (RepOutboxEntity event : batch) {
            publish(event);
        }
    }

    private void publish(RepOutboxEntity event) {
        try {
            kafkaTemplate.send(
                            properties.getKafka().getTopic(),
                            event.getTenantId().toString(),
                            event.getPayload())
                    .get();
            event.markPublished();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            event.markPendingRetry(properties.getOutbox().getRetryDelaySeconds(), e.getMessage());
        } catch (ExecutionException e) {
            handleFailure(event, e.getCause() == null ? e : e.getCause());
        } catch (Exception e) {
            handleFailure(event, e);
        }
    }

    private void handleFailure(RepOutboxEntity event, Throwable error) {
        int attempts = event.getAttemptCount() + 1;
        int maxAttempts = properties.getOutbox().getMaxAttempts();
        String message = error.getMessage() != null ? error.getMessage() : error.getClass().getSimpleName();
        if (attempts >= maxAttempts) {
            log.error("Outbox event {} failed after {} attempts", event.getId(), attempts, error);
            event.markFailed(message, properties.getOutbox().getRetryDelaySeconds());
        } else {
            log.warn("Outbox event {} failed (attempt {}/{}). Retrying later.", event.getId(), attempts, maxAttempts, error);
            event.markPendingRetry(properties.getOutbox().getRetryDelaySeconds(), message);
        }
    }
}

package com.adesk.repsvc.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Outbox outbox = new Outbox();
    private final Kafka kafka = new Kafka();

    public Outbox getOutbox() {
        return outbox;
    }

    public Kafka getKafka() {
        return kafka;
    }

    public static class Outbox {
        /**
         * Number of records to fetch per publishing batch.
         */
        private int batchSize = 50;

        /**
         * Delay between each poll execution in milliseconds.
         */
        private long pollDelayMs = 5000L;

        /**
         * Delay before retrying a failed publish in seconds.
         */
        private long retryDelaySeconds = 30L;

        /**
         * Maximum number of attempts before the event is marked failed.
         */
        private int maxAttempts = 10;

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getPollDelayMs() {
            return pollDelayMs;
        }

        public void setPollDelayMs(long pollDelayMs) {
            this.pollDelayMs = pollDelayMs;
        }

        public long getRetryDelaySeconds() {
            return retryDelaySeconds;
        }

        public void setRetryDelaySeconds(long retryDelaySeconds) {
            this.retryDelaySeconds = retryDelaySeconds;
        }

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }

    public static class Kafka {
        /**
         * Kafka topic to publish rep events.
         */
        private String topic = "rep-events";

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }
}

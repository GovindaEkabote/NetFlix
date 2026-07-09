package com.netflix.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    /*
     * Creates the "video-upload" Kafka topic.
     *
     * Purpose:
     * --------
     * This topic receives events immediately after a video has been
     * successfully uploaded to MinIO.
     *
     * Publisher:
     * - Video Service
     *
     * Consumer:
     * - Encoding Service
     *
     * Partitions:
     * - 1 (sufficient for development; increase in production for parallel processing)
     *
     * Replication Factor:
     * - 1 (development only; use 3 or more in production for fault tolerance)
     */
    @Bean
    public NewTopic videoUploadTopic() {
        return TopicBuilder.name("video-upload")
                .partitions(3)
                .replicas(1)
                .build();
    }

    /*
     * Creates the "video-encoded" Kafka topic.
     *
     * Purpose:
     * --------
     * This topic is published after the Encoding Service finishes
     * transcoding the uploaded video (for example, generating HLS playlists
     * and multiple video resolutions).
     *
     * Publisher:
     * - Encoding Service
     *
     * Consumers:
     * - Notification Service
     * - Metadata Service
     * - Recommendation Service
     * - Any other service interested in completed video processing
     *
     * Partitions:
     * - 1 (development; increase in production if higher throughput is required)
     *
     * Replication Factor:
     * - 1 (development; use 3 or more in a production Kafka cluster)
     */
    @Bean
    public NewTopic videoEncodedTopic() {
        return TopicBuilder.name("video-encoded")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
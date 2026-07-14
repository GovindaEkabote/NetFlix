package com.netflix.service;


import com.netflix.event.VideoUploadedEvent;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final MinioClient minioClient;

    private final KafkaTemplate<String, VideoUploadedEvent> kafkaTemplate;

    @Value("${minio.bucket}")
    private String bucketName;

    public String uploadVideo(String movieId, MultipartFile file) {

        try {

            // Create bucket if it doesn't exist
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
            }

            String extension = "";

            if (file.getOriginalFilename() != null &&
                    file.getOriginalFilename().contains(".")) {

                extension = file.getOriginalFilename()
                        .substring(file.getOriginalFilename().lastIndexOf("."));
            }

            String objectName = movieId + "/"
                    + UUID.randomUUID()
                    + extension;

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(file.getContentType())
                            .build()
            );



            VideoUploadedEvent videoUploadedEvent  = new VideoUploadedEvent(
                    movieId,
                    objectName,
                    bucketName,
                    file.getOriginalFilename(),
                    file.getSize()
            );

            kafkaTemplate.send("video.uploaded", movieId, videoUploadedEvent)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Kafka send failed", ex);
                        } else {
                            log.info("Kafka message sent successfully");
                            log.info("Topic: {}", result.getRecordMetadata().topic());
                            log.info("Partition: {}", result.getRecordMetadata().partition());
                            log.info("Offset: {}", result.getRecordMetadata().offset());
                        }
                    });

            return objectName;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video", e);
        }
    }
}

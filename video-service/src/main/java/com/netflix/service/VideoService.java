package com.netflix.service;

import com.netflix.event.VideoUploadEvent;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final MinioClient minioClient;

    private final KafkaTemplate<String, VideoUploadEvent> kafkaTemplate;

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

            return objectName;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload video", e);
        }
    }
}

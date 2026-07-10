package com.netflix.service;

import com.netflix.dto.response.StreamingResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import io.minio.GetObjectArgs;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreaminService {

    private final MinioClient minioClient;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${minio.presignedUrlExpire}")
    private int presignedUrlExpire;

    private static final String STREAMING_URL_CACHE_KEY = "streaming:url:";

    public StreamingResponse getStreamingUrl(String movieId, String masterPlaylist) {

        String cacheKey = STREAMING_URL_CACHE_KEY + movieId;

        // 1. Check Redis
        String cachedUrl = redisTemplate.opsForValue().get(cacheKey);

        if (cachedUrl != null) {
            log.info("Streaming URL found in Redis for movie {}", movieId);

            return new StreamingResponse(
                    movieId,
                    cachedUrl,
                    "1080p,720p,480p,360p",
                    presignedUrlExpire
            );
        }

        try {

            // 2. Generate presigned URL
            String presignedUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(masterPlaylist)
                            .expiry(presignedUrlExpire, TimeUnit.MINUTES)
                            .build()
            );

            // 3. Cache URL
            redisTemplate.opsForValue().set(
                    cacheKey,
                    presignedUrl,
                    presignedUrlExpire,
                    TimeUnit.MINUTES
            );

            log.info("Generated new streaming URL for movie {}", movieId);

            // 4. Return response
            return new StreamingResponse(
                    movieId,
                    cachedUrl,
                    "1080p, 720p, 480p, 360p",
                    presignedUrlExpire
            );

        } catch (Exception e) {

            log.error("Failed to generate streaming URL", e);

            throw new RuntimeException("Unable to generate streaming URL", e);
        }
    }

    public void invalidateCache(String movieId) {
        String cacheKey = STREAMING_URL_CACHE_KEY + movieId;
        redisTemplate.delete(cacheKey);
    }


    public String  getSignedUrl(String movieId, String playlistPath) {
        String basePath = playlistPath.substring(0, playlistPath.lastIndexOf('/') + 1);


        String m3u8Path = null;
        try {
            m3u8Path = readFromS3(playlistPath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String signedContent  = rewriteS3u8SignedUrl(
                m3u8Path,
                basePath
        );
        return  signedContent;
    }

    private  String  rewriteS3u8SignedUrl(
            String m3u8Path,
            String basePath
    ){
        StringBuilder signedContent = new StringBuilder();
        for(String line : m3u8Path.split("\n")){
            String trimmed = line.trim();
            if(trimmed.isEmpty() || trimmed.startsWith("#")){
                signedContent.append(line).append("\n");
                continue;
            }

            String fullKey = basePath + trimmed;
            String signedUrl = generatePresignedUrl(fullKey);
            signedContent.append(signedUrl).append("\n");
        }

        return signedContent.toString();
    }

    private String readFromS3(String objectName) throws Exception {

        try (InputStream inputStream =
                     minioClient.getObject(
                             GetObjectArgs.builder()
                                     .bucket(bucketName)
                                     .object(objectName)
                                     .build())) {

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String generatePresignedUrl(String objectKey) {

        try {

            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectKey)
                            .expiry(presignedUrlExpire, TimeUnit.MINUTES)
                            .build()
            );

        } catch (Exception e) {

            throw new RuntimeException("Failed to generate signed URL", e);
        }
    }

}

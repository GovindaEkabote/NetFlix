package ai_search_service.service;


import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubtitleDownloadService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public InputStream downloadSubtitle(String subtitleKey)  throws Exception{
        log.info("Downloading subtitle from miniIO {} ", subtitleKey);

        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(subtitleKey)
                            .build()
            );

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(subtitleKey)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download subtitle from minIO {}", subtitleKey, e);
            throw new RuntimeException("Failed to download subtitle from minIO", e);
        }
    }

    public boolean subtitleExists(String subtitleKey){
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(subtitleKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            log.error("Failed to check if subtitle exists in minIO {}", subtitleKey, e);
            return false;
        }
    }
}

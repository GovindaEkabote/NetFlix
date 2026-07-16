package com.netflix.service;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    public void download(String objectName, String filePath) throws Exception {


        Path path = Paths.get(filePath);


        if(Files.exists(path)){
            Files.delete(path);
            log.info("Deleted existing file {}", filePath);
        }


        Files.createDirectories(path.getParent());


        minioClient.downloadObject(
                DownloadObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .filename(filePath)
                        .build()
        );


        log.info("Downloaded {} -> {}", objectName, filePath);
    }
    public void upload(String objectName, File file)throws Exception{

        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .filename(file.getAbsolutePath())
                        .build()
        );
    }
}

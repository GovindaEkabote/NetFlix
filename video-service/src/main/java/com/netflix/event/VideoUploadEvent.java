package com.netflix.event;


/*
*
* Event published to kafka when a video is uploaded to minio
* Encoding service consume this to start FFmpeg encoding
*
* Topic: video-upload
*
* */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadEvent {

    private String movieId;
    private String videoKey;
    private String bucketName;
    private String originalFileName;
    private Long fileSizeBytes;
}

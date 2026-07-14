package com.netflix.service;


import com.netflix.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoEventConsumer {

    private final  EncodingService encodingService;

    /*
    *  Listens to video uploaded topic
    *  Triggered when video is uploaded a raw video to MinIO
    *  Flow: video servive -> minio -> kafka (Video.uploaded)
    *                               -> this consumer
    *                               -> EncodingService -> ffmpeg -> minio
    *                               -> kafka (video.encoded)
    * */

    @KafkaListener(
            topics = "video.uploaded",
            groupId = "encoding-service-group-v2"
    )
    public void consumeVideoUploadedEvent(VideoUploadedEvent event) {
        log.info("========== EVENT RECEIVED ==========");
        System.out.println("========== EVENT RECEIVED ==========");
        System.out.println("MovieId      = " + event.getMovieId());
        System.out.println("VideoKey     = " + event.getVideoKey());
        System.out.println("Bucket       = " + event.getBucketName());
        System.out.println("OriginalName = " + event.getOriginalFileName());
        try{
            encodingService.encodeVideo(event);
        }catch (Exception e){
            log.error("Failed to encode video", e);
            System.out.println("Failed to encode video");
            throw new RuntimeException("Failed to encode video", e);
        }
    }
}

package com.netflix.content_service.service;


import com.netflix.content_service.model.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class VideoUploadedEncodedEventConsumer {

    private  final ContentService contentService;

    @KafkaListener(topics = "video.uploaded")
    public void consumerVideoUploadedEvent(
            @Payload Map<String, Object> payload
            ){
        String movieeId = payload.get("movieId").toString();
        String videoKey = payload.get("videoKey").toString();

        log.info("Video uploaded event received for movieId: {} and videoKey: {}", movieeId, videoKey);
        contentService.updateVideoKey(movieeId, videoKey);
    }


    @KafkaListener(topics = "video-encoded")
    public void consumeVideoEncodedEvent(
            @Payload Map<String, Object> payload
    ){
        String movieeId = payload.get("movieId").toString();
        String hlsUrl = payload.get("hlsUrl").toString();
        boolean success = (Boolean) payload.get("success");

        if (success){
            contentService.updateHlsUrl(movieeId, hlsUrl);
        }else{
            String errorMessage = payload.get("error").toString();
            contentService.updateVideoStatus(movieeId, VideoStatus.FAILED);
        }
    }
}

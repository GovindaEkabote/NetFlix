package com.netflix.service;


import com.netflix.event.VideoEncodedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoEncodedEvenrConsumer {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String MASTER_PLAYLIST_KEY_PREFIX = "streaming:playlist:";

    @KafkaListener(topics = "video-encoded", groupId = "streaming-service-group")
    public void consumeVideoEncodedEvent(VideoEncodedEvent event) {

        if(event.isSuccess()){
            String cacheKey = MASTER_PLAYLIST_KEY_PREFIX + event.getMovieId();
            redisTemplate.opsForValue().set(cacheKey, event.getMasterPlaylistKey());
        }else{
            log.error("Failed to encode video for movie {}", event.getMovieId());
        }
    }
}



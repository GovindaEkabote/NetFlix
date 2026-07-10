package com.netflix.controller;


import com.netflix.dto.response.StreamingResponse;
import com.netflix.service.StreaminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/streaming")
@RequiredArgsConstructor
@Slf4j
public class StreamingController {

    private final StreaminService streaminService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String MASTER_PLAYLIST_KEY_PREFIX = "master_playlist_";

    /*
    * get streaming url for a movie
    * return presigned hls master playlist url
    * */

    @GetMapping("{movieId}")
    public ResponseEntity<StreamingResponse> getStreamingUrl(@PathVariable String movieId) {
        String masterPlaylist = redisTemplate.opsForValue().get(MASTER_PLAYLIST_KEY_PREFIX + movieId);
        if (masterPlaylist == null) {
            return ResponseEntity.notFound().build();
        }
        StreamingResponse response = streaminService.getStreamingUrl(movieId, masterPlaylist);
        return ResponseEntity.ok(response);
    }

    @GetMapping("{movieId}/signed-playlist")
    public ResponseEntity<String> getSignedPlaylist(
            @PathVariable String movieId,
            @RequestParam  String path
    ){
        String signedUrl = streaminService.getSignedUrl(movieId, path);
        return ResponseEntity.ok()
                .header("Content-Type","application/x-mpegURL")
                .body(signedUrl);
    }
}

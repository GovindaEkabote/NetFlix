package com.netflix.Controller;

import com.netflix.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/video")
@Slf4j
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @PostMapping("/upload/{movieId}")
    public ResponseEntity<String> uploadVideo(
            @PathVariable String movieId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {

        log.info("Video upload request for movie: {}, size: {} MB",
                movieId,
                file.getSize() / (1024 * 1024));

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }

        String videoKey = videoService.uploadVideo(movieId, file);

        return ResponseEntity.ok(videoKey);
    }
}
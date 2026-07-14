package com.netflix.controller;

import com.netflix.event.VideoUploadedEvent;
import com.netflix.service.EncodingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    private final EncodingService encodingService;

    @GetMapping("/test")
    public String test() {

        VideoUploadedEvent event = new VideoUploadedEvent(
                "movie1",
                "test.mp4",
                "netflix-videos",
                "test.mp4",
                100L
        );

        encodingService.encodeVideo(event);

        return "OK";
    }
}

// TestController.java - ADD THIS FILE
package ai_search_service.controller;

import ai_search_service.model.MovieScene;
import ai_search_service.model.VideoMetadata;
import ai_search_service.repository.VideoMetadataRepository;
import ai_search_service.service.SceneIndexerService;
import ai_search_service.service.SubtitleParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TestController {

    private final SubtitleParserService subtitleParserService;
    private final SceneIndexerService sceneIndexerService;
    private final VideoMetadataRepository videoMetadataRepository;

    @PostMapping("/index-subtitle")
    public ResponseEntity<Map<String, Object>> indexSubtitle(
            @RequestParam("movieId") String movieId,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Manually indexing subtitle for movie: {}", movieId);

            // Check if metadata exists, if not create it
            if (!videoMetadataRepository.existsByMovieId(movieId)) {
                VideoMetadata metadata = VideoMetadata.builder()
                        .movieId(movieId)
                        .movieTitle("Movie " + movieId.substring(0, 8))
                        .status("PROCESSING")
                        .indexed(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                videoMetadataRepository.save(metadata);
                log.info("Created new video metadata for movie: {}", movieId);
            }

            // Parse subtitle
            InputStream inputStream = file.getInputStream();
            List<MovieScene> scenes = subtitleParserService.parseSubtitle(
                    inputStream,
                    movieId,
                    file.getOriginalFilename()
            );

            // Index scenes
            sceneIndexerService.indexScenes(movieId, scenes);

            response.put("success", true);
            response.put("movieId", movieId);
            response.put("scenesIndexed", scenes.size());
            response.put("message", "Successfully indexed " + scenes.size() + " scenes");

            log.info("Successfully indexed {} scenes for movie: {}", scenes.size(), movieId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to index subtitle", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/check-index/{movieId}")
    public ResponseEntity<Map<String, Object>> checkIndex(@PathVariable String movieId) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean exists = videoMetadataRepository.existsByMovieId(movieId);
            response.put("movieId", movieId);
            response.put("exists", exists);

            if (exists) {
                VideoMetadata metadata = videoMetadataRepository.findByMovieId(movieId).orElse(null);
                if (metadata != null) {
                    response.put("indexed", metadata.getIndexed());
                    response.put("totalScenes", metadata.getTotalScenes());
                    response.put("status", metadata.getStatus());
                }
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/index-sample")
    public ResponseEntity<Map<String, Object>> indexSampleData(@RequestParam String movieId) {
        Map<String, Object> response = new HashMap<>();

        try {
            log.info("Indexing sample data for movie: {}", movieId);

            String sampleSubtitle =
                    "1\n" +
                            "00:00:00,000 --> 00:00:02,000\n" +
                            "There is no creativity without love.\n\n" +
                            "2\n" +
                            "00:00:02,000 --> 00:00:05,800\n" +
                            "I have tried to water so much.\n\n" +
                            "3\n" +
                            "00:00:05,800 --> 00:00:07,400\n" +
                            "I have learnt this from movies.\n\n" +
                            "4\n" +
                            "00:00:07,400 --> 00:00:09,600\n" +
                            "No matter how many times you fail, despair.\n\n" +
                            "5\n" +
                            "00:00:09,600 --> 00:00:11,600\n" +
                            "How sad, how many times do you have?\n\n" +
                            "6\n" +
                            "00:00:12,200 --> 00:00:13,800\n" +
                            "I don't have to face the same situation.\n\n" +
                            "7\n" +
                            "00:00:13,800 --> 00:00:14,600\n" +
                            "I don't care.\n\n" +
                            "8\n" +
                            "00:00:14,600 --> 00:00:16,600\n" +
                            "I feel like this world is against you.\n\n" +
                            "9\n" +
                            "00:00:17,800 --> 00:00:20,000\n" +
                            "In the end, everything gets better.\n\n" +
                            "10\n" +
                            "00:00:24,400 --> 00:00:25,800\n" +
                            "And if it's not good?\n\n";

            // Save metadata
            if (!videoMetadataRepository.existsByMovieId(movieId)) {
                VideoMetadata metadata = VideoMetadata.builder()
                        .movieId(movieId)
                        .movieTitle("Sample Movie")
                        .status("PROCESSING")
                        .indexed(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                videoMetadataRepository.save(metadata);
            }

            InputStream inputStream = new java.io.ByteArrayInputStream(
                    sampleSubtitle.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

            List<MovieScene> scenes = subtitleParserService.parseSubtitle(
                    inputStream,
                    movieId,
                    "sample.srt"
            );

            sceneIndexerService.indexScenes(movieId, scenes);

            response.put("success", true);
            response.put("movieId", movieId);
            response.put("scenesIndexed", scenes.size());
            response.put("message", "Successfully indexed " + scenes.size() + " scenes");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to index sample data", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
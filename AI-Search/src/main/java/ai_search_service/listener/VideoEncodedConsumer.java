// VideoEncodedConsumer.java - Updated to save movie title
package ai_search_service.listener;

import ai_search_service.event.VideoEncodedEvent;
import ai_search_service.model.MovieScene;
import ai_search_service.model.VideoMetadata;
import ai_search_service.repository.VideoMetadataRepository;
import ai_search_service.service.SceneIndexerService;
import ai_search_service.service.SubtitleDownloadService;
import ai_search_service.service.SubtitleParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class VideoEncodedConsumer {

    private final SubtitleDownloadService subtitleDownloadService;
    private final SubtitleParserService subtitleParserService;
    private final SceneIndexerService sceneIndexerService;
    private final VideoMetadataRepository videoMetadataRepository;

    @KafkaListener(
            topics = "video-encoded",
            groupId = "video.encoded.group.v2",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeVideoEncoded(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            VideoEncodedEvent event = (VideoEncodedEvent) record.value();
            log.info("========== AI SEARCH: VIDEO ENCODED EVENT RECEIVED ==========");
            log.info("Movie ID: {}, Movie Title: {}, Subtitle Key: {}, Status: {}",
                    event.getMovieId(), event.getMovieTitle(), event.getSubtitleKey(), event.getEncodingStatus());

            // Check if encoding was successful
            if (!event.isSuccess() || "FAILED".equals(event.getEncodingStatus())) {
                log.error("Encoding failed for movie: {}, error: {}",
                        event.getMovieId(), event.getErrorMessage());
                ack.acknowledge();
                return;
            }

            // Process the event
            processVideoEncoded(event);

            // Acknowledge the message
            ack.acknowledge();
            log.info("Successfully processed video.encoded event for movie: {}", event.getMovieId());

        } catch (Exception e) {
            log.error("Failed to process video.encoded event", e);
            // In production, you might want to send to DLQ or retry
        }
    }

    private void processVideoEncoded(VideoEncodedEvent event) {
        try {
            // Step 1: Save video metadata (WITH MOVIE TITLE)
            log.info("STEP 1 - Saving metadata for movie: {}", event.getMovieId());
            saveVideoMetadata(event);

            // Step 2: Download subtitle from MinIO
            log.info("STEP 2 - Downloading subtitle: {}", event.getSubtitleKey());
            InputStream subtitleStream = subtitleDownloadService.downloadSubtitle(event.getSubtitleKey());

            // Step 3: Parse subtitle
            log.info("STEP 3 - Parsing subtitle");
            List<MovieScene> scenes = subtitleParserService.parseSubtitle(
                    subtitleStream,
                    event.getMovieId(),
                    event.getSubtitleKey()
            );

            log.info("Parsed {} scenes from subtitle", scenes.size());

            if (scenes.isEmpty()) {
                log.warn("No scenes parsed from subtitle for movie: {}", event.getMovieId());
                return;
            }

            // Step 4: Index scenes to MySQL and Qdrant
            log.info("STEP 4 - Indexing {} scenes to MySQL and Qdrant", scenes.size());
            sceneIndexerService.indexScenes(event.getMovieId(), scenes);

            log.info("✅ SUCCESS: Movie {} indexed with {} scenes", event.getMovieId(), scenes.size());

        } catch (Exception e) {
            log.error("Error processing video.encoded event for movie: {}", event.getMovieId(), e);
            throw new RuntimeException("Failed to process video.encoded event", e);
        }
    }

    private void saveVideoMetadata(VideoEncodedEvent event) {
        try {
            VideoMetadata metadata = VideoMetadata.builder()
                    .movieId(event.getMovieId())
                    .movieTitle(event.getMovieTitle() != null ? event.getMovieTitle() : "Unknown Movie")
                    .hlsUrl(event.getHlsUrl())
                    .subtitleKey(event.getSubtitleKey())
                    .duration(event.getDuration())
                    .status("PROCESSING")
                    .indexed(false)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            videoMetadataRepository.save(metadata);
            log.info("Saved video metadata for movie: {} with title: {}",
                    event.getMovieId(), metadata.getMovieTitle());

        } catch (Exception e) {
            log.warn("Failed to save video metadata for movie: {}", event.getMovieId(), e);
            // Try to save again with minimal data
            try {
                VideoMetadata fallbackMetadata = VideoMetadata.builder()
                        .movieId(event.getMovieId())
                        .movieTitle("Movie_" + event.getMovieId().substring(0, 8))
                        .status("PROCESSING")
                        .indexed(false)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                videoMetadataRepository.save(fallbackMetadata);
                log.info("Saved fallback metadata for movie: {}", event.getMovieId());
            } catch (Exception ex) {
                log.error("Failed to save fallback metadata", ex);
            }
        }
    }
}
package ai_search_service.service;

import ai_search_service.model.MovieScene;
import ai_search_service.model.VideoMetadata;
import ai_search_service.repository.MovieSceneRepository;
import ai_search_service.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SceneIndexerService {

    private final MovieSceneRepository movieSceneRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;

    @Transactional
    public void indexScenes(String movieId, List<MovieScene> scenes) {
        log.info("Starting to index {} scenes for movie: {}", scenes.size(), movieId);

        if (scenes.isEmpty()) {
            log.warn("No scenes to index for movie: {}", movieId);
            return;
        }

        // Delete existing scenes for this movie
        movieSceneRepository.deleteByMovieId(movieId);
        log.info("Deleted existing scenes for movie: {}", movieId);

        // Process each scene
        List<Document> documents = new ArrayList<>();
        List<MovieScene> savedScenes = new ArrayList<>();

        for (int i = 0; i < scenes.size(); i++) {
            MovieScene scene = scenes.get(i);

            try {
                // Generate embedding for the scene text
                float[] embedding = embeddingService.generateEmbedding(scene.getSceneText());

                if (embedding == null || embedding.length == 0) {
                    log.warn("Empty embedding for scene: {}", scene.getSceneText());
                    continue;
                }

                // Set timestamps
                scene.setCreatedAt(LocalDateTime.now());
                scene.setUpdatedAt(LocalDateTime.now());
                scene.setMovieId(movieId);

                // Save to MySQL first
                MovieScene savedScene = movieSceneRepository.save(scene);
                savedScenes.add(savedScene);

                // Create metadata map for Qdrant - CONVERT ALL VALUES TO SUPPORTED TYPES
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("movieId", movieId);
                metadata.put("sceneText", scene.getSceneText());

                // Convert to Double (Qdrant supports Double)
                metadata.put("startTime", scene.getStartTime().doubleValue());
                metadata.put("endTime", scene.getEndTime().doubleValue());

                // Convert to String for language
                metadata.put("language", scene.getLanguage() != null ? scene.getLanguage() : "en");

                // Convert sequence number to Integer (not Long)
                metadata.put("sequenceNumber", scene.getSequenceNumber() != null ?
                        scene.getSequenceNumber().intValue() : i + 1);

                // Convert sceneId to String (Qdrant handles String better than Long)
                metadata.put("sceneId", savedScene.getId().toString());

                // Create document with embedding
                Document document = new Document(scene.getSceneText(), metadata);
                documents.add(document);

                log.debug("Processed scene {}: '{}' at {}",
                        i + 1,
                        scene.getSceneText().substring(0, Math.min(30, scene.getSceneText().length())),
                        scene.getStartTime()
                );

            } catch (Exception e) {
                log.error("Failed to process scene: {}", scene.getSceneText(), e);
            }
        }

        // Add all documents to vector store at once
        if (!documents.isEmpty()) {
            try {
                vectorStore.add(documents);
                log.info("Successfully added {} documents to vector store", documents.size());
            } catch (Exception e) {
                log.error("Failed to add documents to vector store", e);
                throw new RuntimeException("Failed to index scenes in Qdrant", e);
            }
        }

        // Update video metadata
        try {
            VideoMetadata metadata = videoMetadataRepository.findById(movieId).orElse(null);
            if (metadata != null) {
                metadata.setTotalScenes(savedScenes.size());
                metadata.setIndexed(true);
                metadata.setStatus("COMPLETED");
                metadata.setUpdatedAt(LocalDateTime.now());
                videoMetadataRepository.save(metadata);
                log.info("Updated video metadata for movie: {}", movieId);
            }
        } catch (Exception e) {
            log.warn("Failed to update video metadata for movie: {}", movieId, e);
        }

        log.info("Successfully indexed {} scenes for movie: {}", savedScenes.size(), movieId);
    }
}
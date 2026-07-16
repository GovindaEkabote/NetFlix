package ai_search_service.service;

import ai_search_service.dto.request.SceneSearchRequest;
import ai_search_service.dto.response.SceneSearchResponse;
import ai_search_service.model.MovieScene;
import ai_search_service.repository.MovieSceneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SceneSearchService {

    private final VectorStoreService vectorStoreService;
    private final MovieSceneRepository movieSceneRepository;

    /**
     * Search similar scenes
     */
    public List<SceneSearchResponse> searchScenes(SceneSearchRequest request) {

        List<Document> documents = vectorStoreService.searchScenes(
                request.getQuery(),
                request.getMovieId(),
                request.getTopK()
        );

        return documents.stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Convert Spring AI Document to DTO
     */
    private SceneSearchResponse convertToResponse(Document document) {

        return SceneSearchResponse.builder()
                .movieId((String) document.getMetadata().get("movieId"))
                .movieTitle((String) document.getMetadata().get("movieTitle"))
                .sceneText(document.getText())
                .startTime(
                        document.getMetadata().get("startTime") == null
                                ? null
                                : Double.parseDouble(document.getMetadata().get("startTime").toString())
                )
                .endTime(
                        document.getMetadata().get("endTime") == null
                                ? null
                                : Double.parseDouble(document.getMetadata().get("endTime").toString())
                )
                .sceneNumber(
                        document.getMetadata().get("sceneNumber") == null
                                ? null
                                : Integer.parseInt(document.getMetadata().get("sceneNumber").toString())
                )
                .language((String) document.getMetadata().get("language"))
                .score(document.getScore())
                .build();
    }

    /**
     * Check if movie is already indexed
     */
    public void indexMovieScenes(String movieId, String subtitleKey) {

        if (movieSceneRepository.existsByMovieId(movieId)) {
            log.info("Movie {} already indexed.", movieId);
            return;
        }

        log.info("Indexing movie {}...", movieId);

        // Download subtitle
        // Parse subtitle
        // Save MovieScene in MySQL
        // Store scenes in VectorStore

    }
}
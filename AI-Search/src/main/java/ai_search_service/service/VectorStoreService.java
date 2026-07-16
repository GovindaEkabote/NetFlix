package ai_search_service.service;

import ai_search_service.model.MovieScene;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

    private final VectorStore vectorStore;

    /**
     * Store movie scenes in Qdrant
     */
    public void storeScenes(List<MovieScene> scenes) {

        if (scenes == null || scenes.isEmpty()) {
            log.warn("No scenes found to store.");
            return;
        }

        List<Document> documents = scenes.stream()
                .map(this::convertToDocument)
                .toList();

        vectorStore.add(documents);

        log.info("Stored {} scenes in Qdrant.", documents.size());
    }

    /**
     * Search similar scenes
     */
    public List<Document> searchScenes(String query, int topK) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * Search inside one movie only
     */
    public List<Document> searchScenes(String query,
                                       String movieId,
                                       int topK) {

        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .filterExpression("movieId == '" + movieId + "'")
                .build();

        return vectorStore.similaritySearch(request);
    }

    /**
     * Convert MovieScene into Spring AI Document
     */
    private Document convertToDocument(MovieScene scene) {

        Map<String, Object> metadata = new HashMap<>();

        metadata.put("movieId", scene.getMovieId());
        metadata.put("movieTitle", scene.getMovieTitle());
        metadata.put("startTime", scene.getStartTime());
        metadata.put("endTime", scene.getEndTime());
        metadata.put("sceneNumber", scene.getSceneNumber());
        metadata.put("language", scene.getLanguage());

        return new Document(
                scene.getSceneText(),
                metadata
        );
    }
}
// DebugController.java
package ai_search_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.document.Document;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final VectorStore vectorStore;

    @GetMapping("/qdrant/check/{movieId}")
    public Map<String, Object> checkQdrantData(@PathVariable String movieId) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Search with a very low threshold to get all documents
            SearchRequest request = SearchRequest.builder()
                    .query("")  // Empty query to get all
                    .topK(100)
                    .similarityThreshold(0.0)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(request);

            response.put("totalDocuments", documents.size());
            response.put("movieId", movieId);

            // Filter by movieId
            List<Document> filteredDocs = documents.stream()
                    .filter(doc -> {
                        Object docMovieId = doc.getMetadata().get("movieId");
                        return docMovieId != null && movieId.equals(docMovieId.toString());
                    })
                    .toList();

            response.put("documentsForMovie", filteredDocs.size());

            if (!filteredDocs.isEmpty()) {
                Document first = filteredDocs.get(0);
                response.put("sampleMetadata", first.getMetadata());
                response.put("sampleText", first.getText());
            }

            return response;

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return response;
        }
    }

    @GetMapping("/qdrant/all")
    public Map<String, Object> getAllDocuments() {
        Map<String, Object> response = new HashMap<>();

        try {
            SearchRequest request = SearchRequest.builder()
                    .query("")  // Empty query to get all
                    .topK(1000)
                    .similarityThreshold(0.0)
                    .build();

            List<Document> documents = vectorStore.similaritySearch(request);

            response.put("totalDocuments", documents.size());

            List<Map<String, Object>> docList = documents.stream()
                    .map(doc -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("text", doc.getText());
                        map.put("metadata", doc.getMetadata());
                        return map;
                    })
                    .limit(10)
                    .toList();

            response.put("sampleDocuments", docList);

            return response;

        } catch (Exception e) {
            response.put("error", e.getMessage());
            return response;
        }
    }
}
package ai_search_service.service;

import ai_search_service.dto.response.SceneSearchResponse;
import ai_search_service.model.VideoMetadata;
import ai_search_service.repository.VideoMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SceneSearchService {

    private final VectorStore vectorStore;
    private final VideoMetadataRepository videoMetadataRepository;

    // Cache for movie titles to avoid repeated database calls
    private final Map<String, String> movieTitleCache = new ConcurrentHashMap<>();

    public List<SceneSearchResponse.SceneMatch> searchScenes(
            String query,
            String movieId,
            Integer limit,
            Double threshold) {

        log.info("========== SEARCH REQUEST ==========");
        log.info("Query: '{}'", query);
        log.info("MovieId: '{}'", movieId);
        log.info("Limit: {}, Threshold: {}", limit, threshold);

        // Set defaults
        int searchLimit = limit != null ? limit : 5;
        double similarityThreshold = threshold != null ? threshold : 0.01;

        // Get all documents from Qdrant
        List<Document> allDocuments = getAllDocuments();
        log.info("Total documents in Qdrant: {}", allDocuments.size());

        if (allDocuments.isEmpty()) {
            log.warn("No documents found in Qdrant");
            return new ArrayList<>();
        }

        // Filter by movieId manually
        List<Document> filteredDocuments = new ArrayList<>();
        for (Document doc : allDocuments) {
            Map<String, Object> metadata = doc.getMetadata();
            Object docMovieId = metadata.get("movieId");

            if (movieId != null && !movieId.isEmpty()) {
                if (docMovieId != null && movieId.equals(docMovieId.toString())) {
                    filteredDocuments.add(doc);
                }
            } else {
                filteredDocuments.add(doc);
            }
        }

        log.info("Found {} documents for movieId: {}", filteredDocuments.size(), movieId);

        if (filteredDocuments.isEmpty()) {
            log.warn("No documents found for movieId: {}", movieId);
            return new ArrayList<>();
        }

        // Perform similarity search
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(searchLimit * 3)
                .similarityThreshold(similarityThreshold)
                .build();

        List<Document> searchResults;
        try {
            searchResults = vectorStore.similaritySearch(searchRequest);
            log.info("Vector search returned {} documents", searchResults != null ? searchResults.size() : 0);
        } catch (Exception e) {
            log.error("Vector search failed", e);
            return convertToMatches(filteredDocuments, searchLimit);
        }

        if (searchResults == null || searchResults.isEmpty()) {
            log.info("No results from vector search");
            return convertToMatches(filteredDocuments, searchLimit);
        }

        // Filter and convert results
        List<SceneSearchResponse.SceneMatch> matches = new ArrayList<>();
        int count = 0;

        for (Document doc : searchResults) {
            if (count >= searchLimit) {
                break;
            }

            Map<String, Object> metadata = doc.getMetadata();
            Object docMovieId = metadata.get("movieId");

            if (movieId != null && !movieId.isEmpty()) {
                if (docMovieId == null || !movieId.equals(docMovieId.toString())) {
                    continue;
                }
            }

            SceneSearchResponse.SceneMatch match = createMatchFromDocument(doc);
            if (match != null) {
                matches.add(match);
                count++;
            }
        }

        // If no matches from vector search, return filtered documents without scoring
        if (matches.isEmpty()) {
            log.info("No vector search matches, returning filtered documents");
            return convertToMatches(filteredDocuments, searchLimit);
        }

        log.info("Returning {} matched scenes", matches.size());
        return matches;
    }

    private List<Document> getAllDocuments() {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query("")
                    .topK(1000)
                    .similarityThreshold(0.0)
                    .build();

            return vectorStore.similaritySearch(request);
        } catch (Exception e) {
            log.error("Failed to get all documents", e);
            return new ArrayList<>();
        }
    }

    private List<SceneSearchResponse.SceneMatch> convertToMatches(List<Document> documents, int limit) {
        List<SceneSearchResponse.SceneMatch> matches = new ArrayList<>();
        int count = 0;

        for (Document doc : documents) {
            if (count >= limit) {
                break;
            }

            SceneSearchResponse.SceneMatch match = createMatchFromDocument(doc);
            if (match != null) {
                matches.add(match);
                count++;
            }
        }

        log.info("Converted {} documents to matches", matches.size());
        return matches;
    }

    private SceneSearchResponse.SceneMatch createMatchFromDocument(Document doc) {
        try {
            Map<String, Object> metadata = doc.getMetadata();

            String movieId = getString(metadata, "movieId");
            String sceneText = doc.getText();
            Double startTime = getDouble(metadata, "startTime");
            Double endTime = getDouble(metadata, "endTime");
            Integer sequenceNumber = getInteger(metadata, "sequenceNumber");
            Double score = getDouble(metadata, "distance");

            if (score == null || score == 0.0) {
                score = 0.85;
            }

            // Get movie title from database
            String movieTitle = getMovieTitle(movieId);

            log.debug("MovieId: {}, MovieTitle: {}", movieId, movieTitle);

            return SceneSearchResponse.SceneMatch.builder()
                    .movieId(movieId)
                    .movieTitle(movieTitle)
                    .sceneText(sceneText)
                    .startTime(startTime != null ? startTime : 0.0)
                    .endTime(endTime != null ? endTime : 0.0)
                    .sequenceNumber(sequenceNumber)
                    .formattedTimestamp(formatTimestamp(startTime))
                    .formattedEndTimestamp(formatTimestamp(endTime))
                    .score(score)
                    .duration(endTime != null && startTime != null ? endTime - startTime : 0.0)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to create match from document", e);
            return null;
        }
    }

    /**
     * Get movie title from database with caching and fallback
     */
    private String getMovieTitle(String movieId) {
        if (movieId == null) {
            return "Unknown Movie";
        }

        // Check cache first
        if (movieTitleCache.containsKey(movieId)) {
            String cachedTitle = movieTitleCache.get(movieId);
            if (cachedTitle != null) {
                return cachedTitle;
            }
        }

        try {
            // Query database
            log.debug("Fetching movie title for movieId: {} from database", movieId);

            VideoMetadata metadata = videoMetadataRepository.findById(movieId).orElse(null);

            if (metadata != null && metadata.getMovieTitle() != null) {
                String title = metadata.getMovieTitle();
                log.debug("Found movie title: {} for movieId: {}", title, movieId);

                // Cache the result
                movieTitleCache.put(movieId, title);
                return title;
            }

            // If not found, try to get from document metadata or use fallback
            String fallbackTitle = getFallbackTitle(movieId);
            movieTitleCache.put(movieId, fallbackTitle);
            return fallbackTitle;

        } catch (Exception e) {
            log.warn("Failed to get movie title for movieId: {}, error: {}", movieId, e.getMessage());

            // Return fallback on error
            String fallbackTitle = getFallbackTitle(movieId);
            movieTitleCache.put(movieId, fallbackTitle);
            return fallbackTitle;
        }
    }

    /**
     * Fallback title if database lookup fails
     */
    private String getFallbackTitle(String movieId) {
        // If movieId is a UUID, use it as fallback
        if (movieId != null && !movieId.isEmpty()) {
            // Try to extract title from movieId or use a default
            return "Movie (" + movieId.substring(0, Math.min(8, movieId.length())) + "...)";
        }
        return "Unknown Movie";
    }

    private String getString(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value == null ? null : value.toString();
    }

    private Double getDouble(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer getInteger(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatTimestamp(Double seconds) {
        if (seconds == null || seconds < 0) {
            return "00:00:00";
        }
        int hrs = (int) (seconds / 3600);
        int mins = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", hrs, mins, secs);
    }

    // Method to clear cache if needed
    public void clearCache() {
        movieTitleCache.clear();
        log.info("Movie title cache cleared");
    }
}
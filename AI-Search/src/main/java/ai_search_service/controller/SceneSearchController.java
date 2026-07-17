package ai_search_service.controller;

import ai_search_service.dto.request.SceneSearchRequest;
import ai_search_service.dto.response.SceneSearchResponse;
import ai_search_service.service.SceneSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SceneSearchController {

    private final SceneSearchService sceneSearchService;

    @PostMapping("/scenes")
    public ResponseEntity<SceneSearchResponse> searchScenes(
            @Valid @RequestBody SceneSearchRequest request) {

        log.info("========== SEARCH API CALLED ==========");
        log.info("Request: query='{}', movieId='{}', limit={}, threshold={}",
                request.getQuery(), request.getMovieId(), request.getLimit(), request.getConfidenceThreshold());

        long startTime = System.currentTimeMillis();

        List<SceneSearchResponse.SceneMatch> matches =
                sceneSearchService.searchScenes(
                        request.getQuery(),
                        request.getMovieId(),
                        request.getLimit(),
                        request.getConfidenceThreshold());

        long processingTime = System.currentTimeMillis() - startTime;

        log.info("Search completed in {}ms, found {} matches", processingTime, matches.size());

        SceneSearchResponse response = SceneSearchResponse.builder()
                .matches(matches)
                .query(request.getQuery())
                .processingTimeMs(processingTime)
                .totalMatches(matches.size())
                .build();

        return ResponseEntity.ok(response);
    }
}
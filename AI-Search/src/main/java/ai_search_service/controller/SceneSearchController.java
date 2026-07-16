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
@RequestMapping("/api/v1/scenes")
@RequiredArgsConstructor
@Slf4j
public class SceneSearchController {

    private final SceneSearchService sceneSearchService;

    @PostMapping("/search")
    public ResponseEntity<List<SceneSearchResponse>> searchScenes(
            @Valid @RequestBody SceneSearchRequest request) {

        log.info(
                "Searching scenes | movieId={} | query={} | topK={}",
                request.getMovieId(),
                request.getQuery(),
                request.getTopK()
        );

        List<SceneSearchResponse> results =
                sceneSearchService.searchScenes(request);

        log.info("Found {} matching scenes.", results.size());

        return ResponseEntity.ok(results);
    }
}
package ai_search_service.service;


import ai_search_service.model.MovieScene;
import ai_search_service.util.SubtitleUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubtitleParserService {

    private static final int MIN_TEXT_LENGTH = 3;
    private static final double MAX_SCENE_DURATION = 60.0; // seconds
    private static final double MERGE_THRESHOLD = 3.0; // seconds

    public List<MovieScene> parseSubtitle(InputStream subtitleStream, String movieId, String fileName) {
        try{
            log.info("Parsing subtitle for movie: {} from file: {}", movieId, fileName);


            //Parse using SubtitleUtil
            List<SubtitleUtil.SubtitleEntry> entries=
                    SubtitleUtil.parseSubtitle(subtitleStream, fileName);

            log.info("Parsed {} raw subtitle entries", entries.size());

            // Clean text
            entries = entries.stream()
                    .map(entry -> {
                        entry.setText(SubtitleUtil.cleanText(entry.getText()));
                        return entry;
                    })
                    .collect(Collectors.toList());

            // Filter by minimum text length
            entries = SubtitleUtil.filterByMinTextLength(entries, MIN_TEXT_LENGTH);
            log.info("After filtering by text length: {} entries", entries.size());

            // Filter by maximum duration
            entries = SubtitleUtil.filterByMaxDuration(entries, MAX_SCENE_DURATION);
            log.info("After filtering by duration: {} entries", entries.size());

            // Merge adjacent scenes with similar text
            entries = SubtitleUtil.mergeAdjacentScenes(entries, MERGE_THRESHOLD);
            log.info("After merging adjacent scenes: {} entries", entries.size());

            // Convert to MovieScene entities
            List<MovieScene> scenes = new ArrayList<>();
            for (SubtitleUtil.SubtitleEntry entry : entries) {
                MovieScene scene = MovieScene.builder()
                        .movieId(movieId)
                        .sceneText(entry.getText())
                        .startTime(entry.getStartTime())
                        .endTime(entry.getEndTime())
                        .sequenceNumber(entry.getSequenceNumber())
                        .language("en")
                        .confidenceScore(0.95) // Default confidence
                        .build();
                scenes.add(scene);
            }

            log.info("Successfully parsed {} scenes from subtitle", scenes.size());
            return scenes;

        } catch (Exception e) {
            log.error("Failed to parse subtitle for movie {} and file {}", movieId, fileName, e);
            throw new RuntimeException("Failed to parse subtitle", e);
        }
    }
}

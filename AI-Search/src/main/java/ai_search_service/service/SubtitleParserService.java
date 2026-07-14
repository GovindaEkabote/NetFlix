package ai_search_service.service;

import ai_search_service.dto.request.CreateSceneRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class SubtitleParserService {

    /**
     * Parses an SRT subtitle file into a list of CreateSceneRequest objects.
     *
     * @param subtitleFile Subtitle (.srt) file
     * @param movieId Movie ID
     * @param movieTitle Movie title
     * @param subtitleKey Object storage key (MinIO/S3)
     * @return List of parsed scenes
     */
    public List<CreateSceneRequest> parseSubtitleFile(
            File subtitleFile,
            String movieId,
            String movieTitle,
            String subtitleKey
    ) {

        List<CreateSceneRequest> scenes = new ArrayList<>();
        int sceneNumber = 1;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(subtitleFile),
                        StandardCharsets.UTF_8))) {

            String line;
            List<String> block = new ArrayList<>();

            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty()) {

                    if (!block.isEmpty()) {

                        CreateSceneRequest scene = parseBlock(
                                block,
                                movieId,
                                movieTitle,
                                subtitleKey,
                                sceneNumber++
                        );

                        if (scene != null) {
                            scenes.add(scene);
                        }

                        block.clear();
                    }

                } else {
                    block.add(line);
                }
            }

            // Process last subtitle block
            if (!block.isEmpty()) {

                CreateSceneRequest scene = parseBlock(
                        block,
                        movieId,
                        movieTitle,
                        subtitleKey,
                        sceneNumber
                );

                if (scene != null) {
                    scenes.add(scene);
                }
            }

        } catch (Exception e) {
            log.error("Failed to parse subtitle file", e);
            throw new RuntimeException("Failed to parse subtitle file", e);
        }

        log.info("Successfully parsed {} subtitle scenes.", scenes.size());

        return scenes;
    }

    /**
     * Parses a single subtitle block.
     */
    private CreateSceneRequest parseBlock(
            List<String> block,
            String movieId,
            String movieTitle,
            String subtitleKey,
            int sceneNumber
    ) {

        if (block.size() < 2) {
            return null;
        }

        try {

            // Example:
            // 1
            // 00:00:20,000 --> 00:00:23,000
            // Hello World

            String timeLine = block.get(1);
            String[] timeParts = timeLine.split("-->");

            if (timeParts.length != 2) {
                return null;
            }

            double startTime = parseTime(timeParts[0].trim());
            double endTime = parseTime(timeParts[1].trim());

            String sceneText = String.join(" ", block.subList(2, block.size()))
                    .replaceAll("\\s+", " ")
                    .trim();

            if (sceneText.isEmpty()) {
                return null;
            }

            return CreateSceneRequest.builder()
                    .movieId(movieId)
                    .movieTitle(movieTitle)
                    .sceneText(sceneText)
                    .startTime(startTime)
                    .endTime(endTime)
                    .sceneNumber(sceneNumber)
                    .language("en")
                    .subtitleObjectKey(subtitleKey)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse subtitle block: {}", block, e);
            return null;
        }
    }

    /**
     * Converts SRT timestamp into seconds.
     *
     * Example:
     * 00:01:30,500 -> 90.5
     */
    private double parseTime(String timeStr) {

        try {

            String[] parts = timeStr.replace(',', '.').split(":");

            if (parts.length != 3) {
                return 0.0;
            }

            double hours = Double.parseDouble(parts[0]);
            double minutes = Double.parseDouble(parts[1]);
            double seconds = Double.parseDouble(parts[2]);

            return (hours * 3600) + (minutes * 60) + seconds;

        } catch (Exception e) {

            log.warn("Invalid timestamp: {}", timeStr);
            return 0.0;
        }
    }
}
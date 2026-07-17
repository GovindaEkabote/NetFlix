package ai_search_service.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneSearchResponse {

    private List<SceneMatch> matches;
    private String query;
    private long processingTimeMs;
    private int totalMatches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SceneMatch{
        private String movieId;
        private String movieTitle;
        private String sceneText;
        private Double startTime;
        private Double endTime;
        private Double score;
        private String formattedTimestamp;
        private String formattedEndTimestamp;
        private Integer sequenceNumber;

        // Helper to get duration in seconds
        Double duration;
    }

}

package ai_search_service.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneSearchResponse {

    private Long sceneId;

    private String movieId;

    private String movieTitle;

    private String sceneText;

    private Double startTime;

    private Double endTime;

    private Integer sceneNumber;

    private Double similarityScore;

    private String language;
    private Double score;
}

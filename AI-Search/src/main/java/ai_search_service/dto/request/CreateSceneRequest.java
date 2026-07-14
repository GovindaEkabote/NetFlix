package ai_search_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.validation.constraints.NotNull;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSceneRequest {

    @NotBlank(message = "Movie ID is required")
    private String movieId;

    @NotBlank(message = "Movie title is required")
    private String movieTitle;

    @NotBlank(message = "Scene text is required")
    private String sceneText;

    @NotNull(message = "Start time is required")
    @Positive(message = "Start time must be positive")
    private Double startTime;

    @NotNull(message = "End time is required")
    @Positive(message = "End time must be positive")
    private Double endTime;

    @NotNull(message = "Scene number is required")
    @Positive(message = "Scene number must be positive")
    private Integer sceneNumber;

    private String language = "en";

    private String subtitleObjectKey;
}

package ai_search_service.dto.request;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class SceneSearchRequest {
    @NotBlank(message = "Query cannot be empty")
    private String query;

    @NotBlank(message = "Movie ID is required")
    private String movieId;

    @Min(value = 1, message = "Limit must be at least 1")
    private Integer limit = 10;

    private Double confidenceThreshold = 0.5;
}

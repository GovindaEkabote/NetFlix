package ai_search_service.dto.request;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SceneSearchRequest {
    @NotBlank(message = "Query cannot be empty")
    private String query;

    @NotBlank(message = "Movie ID is required")
    private String movieId;

    @Min(1)
    @Max(50)
    private Integer topK = 5;
}

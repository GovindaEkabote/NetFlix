package ai_search_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoEncodedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private String movieId;
    private String movieTitle;
    private String hlsUrl;
    private String subtitleKey;
    private String audioKey;
    private String encodingStatus;
    private Double duration;
    private LocalDateTime processedAt;
}

package ai_search_service.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_scenes")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Movie ID from your movie-service
    @Column(nullable = false)
    private String movieId;

    // Movie title
    private String movieTitle;

    // Scene text extracted from the video
    @Lob
    @Column(nullable = false)
    private String sceneText;

    // Scene start time (seconds)
    private Integer startTime;

    // Scene end time (seconds)
    private Integer endTime;

    // Subtitle index or scene number
    private Integer sceneNumber;

    // Language
    private String language;

    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;

}

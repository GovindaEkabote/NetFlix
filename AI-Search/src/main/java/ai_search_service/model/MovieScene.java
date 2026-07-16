package ai_search_service.model;


import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "movie_scenes",
        indexes = {
                @Index(name = "idx_movie_id", columnList = "movieId"),
                @Index(name = "idx_movie_start_time", columnList = "movieId, startTime")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovieScene {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String movieId;

    @Column(nullable = false, length = 1000)
    private String sceneText;

    @Column(nullable = false)
    private Double startTime; // in seconds

    @Column(nullable = false)
    private Double endTime; // in seconds

    @Column(length = 10)
    private String language;

    @Column
    private Double confidenceScore;

    @Column(name = "qdrant_point_id", length = 100)
    private String qdrantPointId;

    @Column
    private Integer sequenceNumber;

    @Column
    private String movieTitle;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}

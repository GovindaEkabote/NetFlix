package ai_search_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "video_metadata")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoMetadata {

    @Id
    @Column(length = 100)
    private String movieId;

    @Column(nullable = false, length = 255)
    private String movieTitle;

    @Column(length = 500)
    private String description;

    @Column
    private Double duration; // in seconds

    @Column(length = 255)
    private String hlsUrl;

    @Column(length = 255)
    private String subtitleKey;

    @Column(length = 50)
    private String status; // PROCESSING, COMPLETED, FAILED

    @Column
    private Integer totalScenes;

    @Column
    private Boolean indexed;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}
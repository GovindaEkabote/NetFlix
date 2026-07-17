package ai_search_service.repository;

import ai_search_service.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, String> {

    Optional<VideoMetadata> findByMovieId(String movieId);
    boolean existsByMovieId(String movieId);
}

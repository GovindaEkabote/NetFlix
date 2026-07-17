package ai_search_service.repository;

import ai_search_service.model.MovieScene;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieSceneRepository extends JpaRepository<MovieScene, Long> {

    List<MovieScene> findByMovieId(String movieId);

    List<MovieScene> findByMovieIdOrderByStartTimeAsc(String movieId);

    Optional<MovieScene> findByMovieIdAndSequenceNumber(String movieId, int sceneNumber);

    @Query("SELECT m FROM MovieScene m WHERE m.movieId = :movieId AND m.sceneText LIKE %:keyword%")
    List<MovieScene> searchByKeyword(@Param("movieId") String movieId, @Param("keyword") String keyword);

    @Query("SELECT m FROM MovieScene m WHERE m.movieId = :movieId AND m.startTime >= :startTime AND m.endTime <= :endTime")
    List<MovieScene> findByTimeRange(@Param("movieId") String movieId,
                                     @Param("startTime") double startTime,
                                     @Param("endTime") double endTime);

    @Modifying
    @Transactional
    @Query("DELETE FROM MovieScene m WHERE m.movieId = :movieId")
    void deleteByMovieId(@Param("movieId") String movieId);

    @Query("SELECT COUNT(m) FROM MovieScene m WHERE m.movieId = :movieId")
    long countByMovieId(@Param("movieId") String movieId);

    @Query("SELECT m FROM MovieScene m WHERE m.movieId = :movieId ORDER BY m.startTime LIMIT :limit")
    List<MovieScene> findTopScenes(@Param("movieId") String movieId, @Param("limit") int limit);
}

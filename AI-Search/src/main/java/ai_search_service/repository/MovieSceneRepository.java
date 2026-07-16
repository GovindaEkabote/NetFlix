package ai_search_service.repository;

import ai_search_service.model.MovieScene;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MovieSceneRepository extends JpaRepository<MovieScene, Long> {

    List<MovieScene> findByMovieId(String movieId);

    // Find scenes by movie ID ordered by scene number
    List<MovieScene> findByMovieIdOrderBySceneNumber(String movieId);


    // Find a specific scene by movie ID and scene number
    Optional<MovieScene> findByMovieIdAndSceneNumber(String movieId, Integer sceneNumber);

    boolean existsByMovieId(String movieId);

    long countByMovieId(String movieId);

    // find seanc by language
    List<MovieScene> findByMovieIdAndLanguage(String movieId, String language);

    // Delete All seances for a movie (for re-indexing)
    void deleteAllByMovieId(String movieId);

    // Get distinct movie IDs (for analytics)
    @Query("""
        SELECT DISTINCT m.movieId
        FROM MovieScene m
        ORDER BY m.movieId
    """)
    List<String> findDistinctMovieIds();


    //  Get movie title by movie ID (assumes all scenes for a movie have same title)
    @Query("""
        SELECT m.movieTitle
        FROM MovieScene m
        WHERE m.movieId=:movieId
    """)
    List<String> findMovieTitles(@Param("movieId") String movieId);

    // Find latest scenes (most recently indexed)
    List<MovieScene> findTop10ByOrderByCreatedAtDesc();

    // Count scenes by movie (for dashboard)
    @Query("SELECT m.movieId, COUNT(m) as sceneCount FROM MovieScene m GROUP BY m.movieId")
    List<Object[]> countScenesByMovie();

    // Search scenes by text (SQL LIKE - not for production, use Qdrant instead)
    @Query(value = """
SELECT *
FROM movie_scenes
WHERE movie_id = :movieId
AND scene_text LIKE CONCAT('%', :keyword, '%')
""", nativeQuery = true)
    List<MovieScene> searchByText(
            @Param("movieId") String movieId,
            @Param("keyword") String keyword);

    // Find scenes with high similarity scores (stored for caching)
    // Note: Actual similarity search is done in Qdrant, this is just for metadata
    @Query("SELECT m FROM MovieScene m WHERE m.movieId = :movieId ORDER BY m.sceneNumber")
    List<MovieScene> findAllScenesForMovieOrdered(@Param("movieId") String movieId);
}

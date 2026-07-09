package com.netflix.content_service.controller;


import com.netflix.content_service.dto.request.MovieRequest;
import com.netflix.content_service.dto.response.MovieResponse;
import com.netflix.content_service.model.Genre;
import com.netflix.content_service.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.swing.text.AbstractDocument;
import java.util.List;

@RestController
@RequestMapping("/api/v1/content")
@Slf4j
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;

    @PostMapping
    public ResponseEntity<MovieResponse> addMovies(
            @Valid @RequestBody MovieRequest request
            ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contentService.addMovie(request));
    }

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(contentService.getAllMovies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable String id) {
        return ResponseEntity.ok(contentService.getMovieById(id));
    }

    // get Movies by genre
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<MovieResponse>> getMoviesByGenre(@PathVariable Genre genre) {
        return ResponseEntity.ok(contentService.getMoviesByGenre(genre));
    }


    //search movie
    @GetMapping("/search/{title}")
    public ResponseEntity<List<MovieResponse>> searchMovies(@RequestParam String title) {
        return ResponseEntity.ok(contentService.searchMovies(title));
    }
}

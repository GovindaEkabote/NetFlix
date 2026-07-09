package com.netflix.content_service.dto.response;

import com.netflix.content_service.model.Genre;
import com.netflix.content_service.model.VideoStatus;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {

    private String id;
    private String title;

    private String description;

    private Genre genre;

    private String director;
    private String cast;
    private  int releaseYear;
    private double rating;
    private String thumbnailUrl;
    private int durationMinutes;
    private String videoKey;
    private  String hlsUrl;
    private VideoStatus videoStatus;
    private LocalDateTime createdAt;
}

package com.netflix.content_service.dto.request;

import com.netflix.content_service.model.Genre;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {


    @NotBlank(message = "Required title")
    private String title;
    private String description;
    @NotBlank(message = "Required genre")
    private Genre genre;

    private String director;
    private String cast;
    private  int releaseYear;
    private double rating;
    private String thumbnailUrl;
    private int durationMinutes;
}

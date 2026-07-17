package com.netflix.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoEncodedEvent {

    private static final long serialVersionUID = 1L;

    private String movieId;
    private String movieTitle;
    private String hlsUrl;
    private String masterPlaylistKey;
    private boolean success;
    private String errorMessage;
    private String subtitleKey;
    private String audioKey;
    private String encodingStatus;
    private Double duration;



}

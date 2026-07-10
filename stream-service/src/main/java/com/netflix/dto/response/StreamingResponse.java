package com.netflix.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StreamingResponse {
    private String movieId;
    private String streamingURL;
    private String quality;
    private long expiredInMinutes;
}

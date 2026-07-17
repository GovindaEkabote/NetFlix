package com.netflix.service;

import com.netflix.event.VideoEncodedEvent;
import com.netflix.event.VideoUploadedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {

    private final MinioService minioService;
    private final WhisperService whisperService;
    private final KafkaTemplate<String, VideoEncodedEvent> kafkaTemplate;

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${encoding.base-path}")
    private String basePath;

    private static final String VIDEO_ENCODED_TOPIC = "video-encoded";

    private static final List<VideoQuality> VIDEO_QUALITIES = List.of(
            new VideoQuality(1920, 1080, 5000),
            new VideoQuality(1280, 720, 2800),
            new VideoQuality(854, 480, 1200),
            new VideoQuality(640, 360, 800)
    );

    public void encodeVideo(VideoUploadedEvent videoUploadedEvent) {
        log.info("========== ENCODING STARTED ==========");
        log.info("Movie ID: {}", videoUploadedEvent.getMovieId());

        String jobPath = Paths.get(basePath, videoUploadedEvent.getMovieId()).toString();
        String encodedPath = Paths.get(jobPath, "encoded").toString();

        File subtitle = null;
        File audio = null;
        String localVideoPath = null;

        try {
            // Create temp directory
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(encodedPath));

            // Step 1: Download video from MinIO
            localVideoPath = Paths.get(jobPath, UUID.randomUUID() + ".mp4").toString();
            log.info("Downloading from MinIO...");
            log.info("Bucket: {}, Object Key: {}", bucketName, videoUploadedEvent.getVideoKey());

            minioService.download(videoUploadedEvent.getVideoKey(), localVideoPath);

            File file = new File(localVideoPath);
            log.info("Downloaded: {} ({} bytes)", file.exists(), file.length());

            // Step 2: Encode multiple qualities with HLS
            for (VideoQuality quality : VIDEO_QUALITIES) {
                String qualityDir = Paths.get(encodedPath, quality.getHeight() + "p").toString();
                Files.createDirectories(Paths.get(qualityDir));
                encodeToHLS(localVideoPath, qualityDir, quality.getWidth(),
                        quality.getHeight(), quality.getBitrate());
                log.info("Encoded {}p successfully", quality.getHeight());
            }

            // Step 3: Generate master playlist
            String masterPlaylistPath = Paths.get(encodedPath, "master.m3u8").toString();
            generateMasterPlaylist(masterPlaylistPath);
            log.info("Generated master playlist successfully");

            // Step 4: Extract audio
            audio = extractAudio(localVideoPath, jobPath);
            log.info("Audio extracted: {}", audio.getAbsolutePath());

            // Step 5: Generate subtitles using Whisper
            subtitle = whisperService.generateSubtitles(audio, jobPath);
            log.info("Subtitles generated: {}", subtitle.getAbsolutePath());

            // Step 6: Upload to MinIO
            String encodedPrefix = "encoded/" + videoUploadedEvent.getMovieId() + "/";
            uploadEncodedFilesToMinIO(encodedPath, encodedPrefix);

            String subtitleKey = "subtitle/" + videoUploadedEvent.getMovieId() + "/subtitle.srt";
            minioService.upload(subtitleKey, subtitle);
            log.info("Uploaded subtitle to: {}", subtitleKey);

            String audioKey = "audio/" + videoUploadedEvent.getMovieId() + "/audio.wav";
            minioService.upload(audioKey, audio);
            log.info("Uploaded audio to: {}", audioKey);

            // Step 7: Get video duration
            Double duration = getVideoDuration(localVideoPath);

            // Step 8: Publish VideoEncodedEvent
            String masterPlaylistKey = encodedPrefix + "master.m3u8";
            String hlsUrl = "http://localhost:9000/" + bucketName + "/" + masterPlaylistKey;

            VideoEncodedEvent videoEncodedEvent = new VideoEncodedEvent(
                    videoUploadedEvent.getMovieId(),
                    videoUploadedEvent.getMovieTitle(),
                    hlsUrl,
                    masterPlaylistKey,
                    true,
                    null,
                    subtitleKey,
                    audioKey,
                    "COMPLETED",
                    duration
            );

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, videoUploadedEvent.getMovieId(), videoEncodedEvent);
            log.info("VIDEO ENCODED EVENT SENT to topic: {}", VIDEO_ENCODED_TOPIC);
            log.info("HLS URL: {}", hlsUrl);
            log.info("Subtitle Key: {}", subtitleKey);

        } catch (Exception e) {
            log.error("Encoding failed for movie: {}", videoUploadedEvent.getMovieId(), e);

            // Send failure event
            VideoEncodedEvent failureEvent = new VideoEncodedEvent(
                    videoUploadedEvent.getMovieId(),
                    videoUploadedEvent.getMovieTitle(),
                    null,
                    null,
                    false,
                    e.getMessage(),
                    null,
                    null,
                    "FAILED",
                    null
            );
            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, videoUploadedEvent.getMovieId(), failureEvent);

            throw new RuntimeException("Encoding failed", e);
        } finally {
            // Cleanup temp files
            cleanupTempFiles(jobPath);
        }
    }

    private void encodeToHLS(String inputPath, String outputDir, int width, int height, int bitrate) throws Exception {
        String playlist = outputDir + "/playlist.m3u8";
        String segments = outputDir + "/segment_%03d.ts";

        List<String> command = Arrays.asList(
                ffmpegPath,
                "-i", inputPath,
                "-vf", "scale=" + width + ":" + height,
                "-c:v", "libx264",
                "-profile:v", "main",
                "-preset", "fast",
                "-b:v", bitrate + "k",
                "-g", "48",
                "-keyint_min", "48",
                "-sc_threshold", "0",
                "-c:a", "aac",
                "-b:a", "128k",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_flags", "independent_segments",
                "-hls_segment_filename", segments,
                playlist
        );

        log.info("Starting {}p encoding", height);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFMPEG] {}", line);
            }
        }

        int exitCode = process.waitFor();
        log.info("FFmpeg Exit Code = {}", exitCode);

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg encoding failed for " + height + "p");
        }
        log.info("{}p encoding completed successfully", height);
    }

    private void generateMasterPlaylist(String masterPlaylistPath) throws Exception {
        StringBuilder master = new StringBuilder();
        master.append("#EXTM3U\n");
        master.append("#EXT-X-VERSION:3\n");

        for (VideoQuality quality : VIDEO_QUALITIES) {
            master.append("#EXT-X-STREAM-INF:BANDWIDTH=")
                    .append(quality.getBitrate() * 1000)
                    .append(",RESOLUTION=")
                    .append(quality.getWidth())
                    .append("x")
                    .append(quality.getHeight())
                    .append("\n");
            master.append(quality.getHeight())
                    .append("p/playlist.m3u8")
                    .append("\n");
        }

        Files.writeString(Paths.get(masterPlaylistPath), master.toString());
    }

    private void uploadEncodedFilesToMinIO(String localDirectory, String objectPrefix) throws Exception {
        Files.walk(Paths.get(localDirectory))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String objectName = objectPrefix +
                                Paths.get(localDirectory)
                                        .relativize(path)
                                        .toString()
                                        .replace("\\", "/");
                        minioService.upload(objectName, path.toFile());
                        log.info("Uploaded {}", objectName);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to upload: " + path, e);
                    }
                });
    }

    private File extractAudio(String input, String workDir) throws Exception {
        String audio = workDir + "/audio.wav";

        List<String> command = List.of(
                ffmpegPath,
                "-i", input,
                "-vn",
                "-acodec", "pcm_s16le",
                "-ar", "16000",
                "-ac", "1",
                audio
        );

        log.info("Extracting audio: {}", String.join(" ", command));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[FFMPEG-AUDIO] {}", line);
            }
        }

        int exitCode = process.waitFor();
        log.info("Audio extraction exit code: {}", exitCode);

        if (exitCode != 0) {
            throw new RuntimeException("Audio extraction failed with exit code: " + exitCode);
        }

        File audioFile = new File(audio);
        if (!audioFile.exists() || audioFile.length() == 0) {
            throw new RuntimeException("Audio file not created or empty: " + audio);
        }

        log.info("Audio extracted successfully: {} ({} bytes)",
                audioFile.getAbsolutePath(), audioFile.length());

        return audioFile;
    }

    private Double getVideoDuration(String videoPath) {
        try {
            List<String> command = List.of(
                    ffmpegPath,
                    "-i", videoPath,
                    "-f", "null",
                    "-"
            );

            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Duration:")) {
                        String[] parts = line.split("Duration:")[1].trim().split(",");
                        String timeStr = parts[0].trim();
                        String[] timeParts = timeStr.split(":");
                        double hours = Double.parseDouble(timeParts[0]);
                        double minutes = Double.parseDouble(timeParts[1]);
                        double seconds = Double.parseDouble(timeParts[2]);
                        return hours * 3600 + minutes * 60 + seconds;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get video duration", e);
        }
        return 0.0;
    }

    private void cleanupTempFiles(String jobPath) {
        try {
            Path path = Paths.get(jobPath);
            if (Files.exists(path)) {
                Files.walk(path)
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                                log.error("Unable to delete {}", p);
                            }
                        });
                log.info("Cleaned up temp directory: {}", jobPath);
            }
        } catch (IOException e) {
            log.error("Cleanup failed", e);
        }
    }

    private static class VideoQuality {
        private final int width;
        private final int height;
        private final int bitrate;

        public VideoQuality(int width, int height, int bitrate) {
            this.width = width;
            this.height = height;
            this.bitrate = bitrate;
        }

        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public int getBitrate() { return bitrate; }
    }
}
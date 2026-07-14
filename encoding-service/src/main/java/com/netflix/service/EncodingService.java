package com.netflix.service;

import com.netflix.event.VideoEncodedEvent;
import com.netflix.event.VideoUploadedEvent;
import io.minio.DownloadObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EncodingService {

    private final MinioClient minioClient;
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

        String jobPath = basePath + "/" + videoUploadedEvent.getMovieId();

        try {
            // Create temp directory
            Files.createDirectories(Paths.get(jobPath));
            Files.createDirectories(Paths.get(jobPath + "/encoded"));

            // Download video from MinIO
            String localVideoPath = jobPath + "/raw_video.mp4";
            log.info("Downloading from MinIO...");
            log.info("Bucket     : {}", bucketName);
            log.info("Object Key : {}", videoUploadedEvent.getVideoKey());
            log.info("Local Path : {}", localVideoPath);
            downloadFromMinIO(videoUploadedEvent.getVideoKey(), localVideoPath);
            File file = new File(localVideoPath);

            log.info("Downloaded Exists : {}", file.exists());
            log.info("Downloaded Size   : {}", file.length());

            // Encode multiple qualities with HLS
            for (VideoQuality quality : VIDEO_QUALITIES) {
                String qualityDir = jobPath + "/encoded/" + quality.getHeight() + "p";
                Files.createDirectories(Paths.get(qualityDir));

                encodeToHLS(localVideoPath, qualityDir, quality.getWidth(), quality.getHeight(), quality.getBitrate());
                log.info("Encoded {}p successfully", quality.getHeight());
            }

            // Generate master playlist (outside the loop)
            String masterPlaylistPath = jobPath + "/encoded/master.m3u8";
            generateMasterPlaylist(masterPlaylistPath);
            log.info("Generated master playlist successfully");

            // Upload all resources to MinIO
            String encodedPrefix = "encoded/" + videoUploadedEvent.getMovieId() + "/";
            uploadEncodedFilesToMinIO(jobPath + "/encoded", encodedPrefix);
            log.info("All files uploaded to MinIO");

            // Publish VideoEncodedEvent
            String masterPlaylistKey = encodedPrefix + "master.m3u8";
            String hlsUrl = "https://" + bucketName + ".minio.com/" + masterPlaylistKey;

            VideoEncodedEvent videoEncodedEvent = new VideoEncodedEvent(
                    videoUploadedEvent.getMovieId(),
                    hlsUrl,
                    masterPlaylistKey,
                    true,
                    null
            );

            kafkaTemplate.send(VIDEO_ENCODED_TOPIC, videoUploadedEvent.getMovieId(), videoEncodedEvent);
            log.info("=================================");
            log.info("VIDEO ENCODED EVENT SENT");
            log.info("Topic     : {}", VIDEO_ENCODED_TOPIC);
            log.info("Movie Id  : {}", videoUploadedEvent.getMovieId());
            log.info("HLS URL   : {}", hlsUrl);
            log.info("=================================");

        } catch (Exception e) {
            log.error("Encoding failed for movie: {}", videoUploadedEvent.getMovieId(), e);
            throw new RuntimeException("Encoding failed", e);
        } finally {
            cleanupTempFiles(jobPath);
        }
    }

    private void downloadFromMinIO(String objectName, String localPath) throws Exception {

        minioClient.downloadObject(
                DownloadObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .filename(localPath)
                        .build()
        );

        log.info("Downloaded {} to {}", objectName, localPath);
    }

    private void encodeToHLS(String inputPath,
                             String outputDir,
                             int width,
                             int height,
                             int bitrate) throws Exception {

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

        log.info("======================================");
        log.info("Starting {}p encoding", height);
        log.info("FFmpeg Path: {}", ffmpegPath);
        log.info("Input File : {}", inputPath);
        log.info("Output Dir : {}", outputDir);
        log.info("Command    : {}", String.join(" ", command));
        log.info("======================================");

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);

        Process process = builder.start();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(process.getInputStream()))) {

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

                        minioClient.uploadObject(
                                UploadObjectArgs.builder()
                                        .bucket(bucketName)
                                        .object(objectName)
                                        .filename(path.toString())
                                        .build()
                        );

                        log.info("Uploaded {}", objectName);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to upload: " + path, e);
                    }
                });
    }

    private void cleanupTempFiles(String jobPath) {
        Path path = Paths.get(jobPath);

        if (!Files.exists(path)) {
            return;
        }

        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException e) {
                            log.error("Unable to delete {}", p);
                        }
                    });
        } catch (IOException e) {
            log.error("Cleanup failed", e);
        }
    }

    // Inner class for type-safe quality definitions
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
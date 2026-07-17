package ai_search_service.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SubtitleUtil {

    // SRT timestamp pattern: 00:00:00,000 --> 00:00:00,000
    private static final Pattern SRT_TIMESTAMP_PATTERN =
            Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2}),(\\d{3})");

    // VTT timestamp pattern: 00:00:00.000 --> 00:00:00.000
    private static final Pattern VTT_TIMESTAMP_PATTERN =
            Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})");

    // ASS timestamp pattern: 0:00:00.00
    private static final Pattern ASS_TIMESTAMP_PATTERN =
            Pattern.compile("(\\d+):(\\d{2}):(\\d{2})\\.(\\d{2})");

    /**
     * Parse SRT subtitle file and return list of SubtitleEntry objects
     */
    public static List<SubtitleEntry> parseSRT(InputStream inputStream) throws IOException {
        List<SubtitleEntry> entries = new ArrayList<>();
        List<String> lines = readLines(inputStream);

        int index = 0;
        while (index < lines.size()) {
            String line = lines.get(index).trim();

            // Skip empty lines
            if (line.isEmpty()) {
                index++;
                continue;
            }

            // Check if this line is a timestamp (not a sequence number)
            Matcher timestampMatcher = SRT_TIMESTAMP_PATTERN.matcher(line);
            if (timestampMatcher.matches()) {
                // This is a timestamp line, parse it
                double startTime = parseSRTTime(timestampMatcher, 1, 2, 3, 4);
                double endTime = parseSRTTime(timestampMatcher, 5, 6, 7, 8);

                // Collect text lines
                StringBuilder text = new StringBuilder();
                int textStart = index + 1;
                int textEnd = textStart;

                while (textEnd < lines.size()) {
                    String textLine = lines.get(textEnd).trim();
                    // Break if empty line or next timestamp
                    if (textLine.isEmpty() || SRT_TIMESTAMP_PATTERN.matcher(textLine).matches()) {
                        break;
                    }
                    if (text.length() > 0) {
                        text.append(" ");
                    }
                    text.append(textLine);
                    textEnd++;
                }

                String cleanText = text.toString().trim();
                if (!cleanText.isEmpty()) {
                    SubtitleEntry entry = SubtitleEntry.builder()
                            .sequenceNumber(entries.size() + 1)
                            .startTime(startTime)
                            .endTime(endTime)
                            .text(cleanText)
                            .build();
                    entries.add(entry);
                    log.debug("Parsed scene {}: '{}' at {}",
                            entries.size(),
                            cleanText.substring(0, Math.min(30, cleanText.length())),
                            formatTime(startTime));
                }

                // Move index to the end of this subtitle block
                index = textEnd;
                continue;
            }

            // Try alternative format with dot instead of comma
            Matcher altMatcher = Pattern.compile(
                    "(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})\\s*-->\\s*(\\d{2}):(\\d{2}):(\\d{2})\\.(\\d{3})"
            ).matcher(line);

            if (altMatcher.matches()) {
                double startTime = parseSRTTime(altMatcher, 1, 2, 3, 4);
                double endTime = parseSRTTime(altMatcher, 5, 6, 7, 8);

                StringBuilder text = new StringBuilder();
                int textStart = index + 1;
                int textEnd = textStart;

                while (textEnd < lines.size()) {
                    String textLine = lines.get(textEnd).trim();
                    if (textLine.isEmpty() ||
                            SRT_TIMESTAMP_PATTERN.matcher(textLine).matches() ||
                            altMatcher.reset(textLine).matches()) {
                        break;
                    }
                    if (text.length() > 0) {
                        text.append(" ");
                    }
                    text.append(textLine);
                    textEnd++;
                }

                String cleanText = text.toString().trim();
                if (!cleanText.isEmpty()) {
                    SubtitleEntry entry = SubtitleEntry.builder()
                            .sequenceNumber(entries.size() + 1)
                            .startTime(startTime)
                            .endTime(endTime)
                            .text(cleanText)
                            .build();
                    entries.add(entry);
                }

                index = textEnd;
                continue;
            }

            // If it's a sequence number, skip it (we already handle timestamps)
            try {
                Integer.parseInt(line);
                // It's a sequence number, skip it
                index++;
                continue;
            } catch (NumberFormatException e) {
                // Not a sequence number, skip this line
                index++;
            }
        }

        log.info("Parsed {} subtitle entries from SRT", entries.size());
        return entries;
    }

    /**
     * Parse VTT subtitle file
     */
    public static List<SubtitleEntry> parseVTT(InputStream inputStream) throws IOException {
        List<SubtitleEntry> entries = new ArrayList<>();
        List<String> lines = readLines(inputStream);

        boolean foundWebVTT = false;
        int index = 0;

        while (index < lines.size()) {
            String line = lines.get(index).trim();

            // Check for WEBVTT header
            if (!foundWebVTT && line.startsWith("WEBVTT")) {
                foundWebVTT = true;
                index++;
                continue;
            }

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("NOTE") || line.startsWith("Kind:")) {
                index++;
                continue;
            }

            // Try to parse timestamp
            Matcher matcher = VTT_TIMESTAMP_PATTERN.matcher(line);
            if (matcher.matches()) {
                double startTime = parseVTTTime(matcher, 1, 2, 3, 4);
                double endTime = parseVTTTime(matcher, 5, 6, 7, 8);

                // Collect text lines
                StringBuilder text = new StringBuilder();
                int textStart = index + 1;
                int textEnd = textStart;

                while (textEnd < lines.size()) {
                    String textLine = lines.get(textEnd).trim();
                    if (textLine.isEmpty() || VTT_TIMESTAMP_PATTERN.matcher(textLine).matches()) {
                        break;
                    }
                    if (text.length() > 0) {
                        text.append(" ");
                    }
                    text.append(textLine);
                    textEnd++;
                }

                if (text.length() > 0) {
                    SubtitleEntry entry = SubtitleEntry.builder()
                            .sequenceNumber(entries.size() + 1)
                            .startTime(startTime)
                            .endTime(endTime)
                            .text(text.toString().trim())
                            .build();
                    entries.add(entry);
                }

                index = textEnd;
                continue;
            }

            index++;
        }

        log.info("Parsed {} subtitle entries from VTT", entries.size());
        return entries;
    }

    /**
     * Auto-detect subtitle format and parse
     */
    public static List<SubtitleEntry> parseSubtitle(InputStream inputStream, String fileName) throws IOException {
        String extension = getFileExtension(fileName).toLowerCase();

        log.info("Parsing subtitle file: {} with extension: {}", fileName, extension);

        switch (extension) {
            case "srt":
                return parseSRT(inputStream);
            case "vtt":
                return parseVTT(inputStream);
            case "ass":
            case "ssa":
                return parseASS(inputStream);
            default:
                // Try to detect format by content
                inputStream.mark(4096);
                try {
                    String firstLine = readFirstLine(inputStream);
                    if (firstLine != null && firstLine.trim().startsWith("WEBVTT")) {
                        return parseVTT(inputStream);
                    }
                } finally {
                    inputStream.reset();
                }
                // Default to SRT
                return parseSRT(inputStream);
        }
    }

    /**
     * Parse ASS/SSA subtitle file
     */
    public static List<SubtitleEntry> parseASS(InputStream inputStream) throws IOException {
        List<SubtitleEntry> entries = new ArrayList<>();
        List<String> lines = readLines(inputStream);

        boolean inEvents = false;

        for (String line : lines) {
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("[Events]")) {
                inEvents = true;
                continue;
            }

            if (inEvents && line.startsWith("Dialogue:")) {
                try {
                    String[] parts = line.split(",", 10);
                    if (parts.length >= 10) {
                        String startStr = parts[1].trim();
                        String endStr = parts[2].trim();
                        String text = parts[9].trim();

                        Matcher startMatcher = ASS_TIMESTAMP_PATTERN.matcher(startStr);
                        Matcher endMatcher = ASS_TIMESTAMP_PATTERN.matcher(endStr);

                        if (startMatcher.matches() && endMatcher.matches()) {
                            double startTime = parseASSTime(startMatcher);
                            double endTime = parseASSTime(endMatcher);

                            // Clean text
                            text = cleanASSText(text);

                            if (!text.isEmpty()) {
                                SubtitleEntry entry = SubtitleEntry.builder()
                                        .sequenceNumber(entries.size() + 1)
                                        .startTime(startTime)
                                        .endTime(endTime)
                                        .text(text)
                                        .build();
                                entries.add(entry);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse ASS dialogue line: {}", line, e);
                }
            }
        }

        log.info("Parsed {} subtitle entries from ASS", entries.size());
        return entries;
    }

    // ==================== Helper Methods ====================

    private static List<String> readLines(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    private static String readFirstLine(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.readLine();
        }
    }

    private static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private static double parseSRTTime(Matcher matcher, int hIdx, int mIdx, int sIdx, int msIdx) {
        int hours = Integer.parseInt(matcher.group(hIdx));
        int minutes = Integer.parseInt(matcher.group(mIdx));
        int seconds = Integer.parseInt(matcher.group(sIdx));
        int millis = Integer.parseInt(matcher.group(msIdx));
        return hours * 3600.0 + minutes * 60.0 + seconds + millis / 1000.0;
    }

    private static double parseVTTTime(Matcher matcher, int hIdx, int mIdx, int sIdx, int msIdx) {
        int hours = Integer.parseInt(matcher.group(hIdx));
        int minutes = Integer.parseInt(matcher.group(mIdx));
        int seconds = Integer.parseInt(matcher.group(sIdx));
        int millis = Integer.parseInt(matcher.group(msIdx));
        return hours * 3600.0 + minutes * 60.0 + seconds + millis / 1000.0;
    }

    private static double parseASSTime(Matcher matcher) {
        int hours = Integer.parseInt(matcher.group(1));
        int minutes = Integer.parseInt(matcher.group(2));
        int seconds = Integer.parseInt(matcher.group(3));
        int centis = Integer.parseInt(matcher.group(4));
        return hours * 3600.0 + minutes * 60.0 + seconds + centis / 100.0;
    }

    private static String formatTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    public static String toSRT(List<SubtitleEntry> entries) {
        StringBuilder sb = new StringBuilder();
        int sequenceNumber = 1;

        for (SubtitleEntry entry : entries) {
            sb.append(sequenceNumber++).append("\n");
            sb.append(formatSRTTime(entry.getStartTime()))
                    .append(" --> ")
                    .append(formatSRTTime(entry.getEndTime()))
                    .append("\n");
            sb.append(entry.getText()).append("\n\n");
        }

        return sb.toString();
    }

    public static String toVTT(List<SubtitleEntry> entries) {
        StringBuilder sb = new StringBuilder();
        sb.append("WEBVTT\n\n");

        for (SubtitleEntry entry : entries) {
            sb.append(formatVTTTime(entry.getStartTime()))
                    .append(" --> ")
                    .append(formatVTTTime(entry.getEndTime()))
                    .append("\n");
            sb.append(entry.getText()).append("\n\n");
        }

        return sb.toString();
    }

    public static List<SubtitleEntry> mergeAdjacentScenes(List<SubtitleEntry> entries, double thresholdSeconds) {
        if (entries.isEmpty()) {
            return entries;
        }

        List<SubtitleEntry> merged = new ArrayList<>();
        SubtitleEntry current = entries.get(0);

        for (int i = 1; i < entries.size(); i++) {
            SubtitleEntry next = entries.get(i);

            double gap = next.getStartTime() - current.getEndTime();
            if (gap <= thresholdSeconds && areTextsSimilar(current.getText(), next.getText())) {
                current = SubtitleEntry.builder()
                        .sequenceNumber(current.getSequenceNumber())
                        .startTime(current.getStartTime())
                        .endTime(next.getEndTime())
                        .text(current.getText() + " " + next.getText())
                        .build();
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        log.debug("Merged {} subtitle entries into {} scenes", entries.size(), merged.size());
        return merged;
    }

    public static List<SubtitleEntry> filterByMinTextLength(List<SubtitleEntry> entries, int minLength) {
        return entries.stream()
                .filter(entry -> entry.getText() != null && entry.getText().length() >= minLength)
                .collect(Collectors.toList());
    }

    public static List<SubtitleEntry> filterByMaxDuration(List<SubtitleEntry> entries, double maxDuration) {
        return entries.stream()
                .filter(entry -> (entry.getEndTime() - entry.getStartTime()) <= maxDuration)
                .collect(Collectors.toList());
    }

    public static String cleanText(String text) {
        if (text == null) return "";

        // Remove HTML tags
        text = text.replaceAll("<[^>]*>", "");

        // Remove formatting codes like {\an8}
        text = text.replaceAll("\\{[^}]*\\}", "");

        // Remove multiple spaces
        text = text.replaceAll("\\s+", " ");

        // Remove special characters but keep punctuation
        text = text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\s]", "");

        return text.trim();
    }

    private static String cleanASSText(String text) {
        text = text.replaceAll("\\{[^}]*\\}", "");
        text = text.replaceAll("\\\\N", " ");
        text = text.replaceAll("\\\\h", " ");
        text = text.replaceAll("\\\\n", " ");
        return text.trim();
    }

    private static boolean areTextsSimilar(String text1, String text2) {
        if (text1 == null || text2 == null) return false;
        String t1 = text1.toLowerCase().trim();
        String t2 = text2.toLowerCase().trim();
        return t1.equals(t2) || t1.contains(t2) || t2.contains(t1);
    }

    private static String formatSRTTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds % 1) * 1000);
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }

    private static String formatVTTTime(double seconds) {
        int hours = (int) (seconds / 3600);
        int minutes = (int) ((seconds % 3600) / 60);
        int secs = (int) (seconds % 60);
        int millis = (int) ((seconds % 1) * 1000);
        return String.format("%02d:%02d:%02d.%03d", hours, minutes, secs, millis);
    }

    // ==================== Inner Classes ====================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubtitleEntry {
        private int sequenceNumber;
        private double startTime;  // in seconds
        private double endTime;    // in seconds
        private String text;

        public double getDuration() {
            return endTime - startTime;
        }

        public String getFormattedStartTime() {
            return formatTime(startTime);
        }

        public String getFormattedEndTime() {
            return formatTime(endTime);
        }
    }
}
package util;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * feedback.txt  →  timestamp,username,uiuId,fullName,feedbackText
 */
public final class FeedbackService {

    private static final Path FILE = Paths.get("data/feedback.txt");
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private FeedbackService() {}

    /* ── WRITE ─────────────────────────────────────────────────────── */
    public static synchronized void add(String username,
                                        String uiuId,
                                        String fullName,
                                        String text) throws IOException {

        Files.createDirectories(FILE.getParent());                // ensure data/ exists
        String safe = text.replaceAll("[\\r\\n]+", " ");          // flatten to one line
        String line = String.join(",",
                TS.format(LocalDateTime.now()),
                username,
                uiuId,
                fullName,
                safe);

        Files.writeString(FILE, line + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /* ── READ ─────────────────────────────────────────────────────── */
    /** Returns newest → oldest */
    public static synchronized List<String[]> loadAll() throws IOException {
        if (!Files.exists(FILE)) return List.of();
        var lines = Files.readAllLines(FILE);
        java.util.Collections.reverse(lines);                     // newest first
        return lines.stream()
                .map(l -> l.split(",", 5))                        // ts,user,id,name,fb
                .filter(a -> a.length == 5)
                .toList();
    }

    /* ── DELETE ───────────────────────────────────────────────────── */
    public static synchronized void delete(String[] feedbackRow) throws IOException {
        if (!Files.exists(FILE)) return;
        String match = String.join(",", feedbackRow);             // exact line
        var updated = Files.readAllLines(FILE).stream()
                .filter(line -> !line.equals(match))
                .toList();
        Files.write(FILE, updated);
    }
}

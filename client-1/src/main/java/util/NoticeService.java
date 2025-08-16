package util;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/** Flat-file notices: each line = timestamp,noticeText */
public final class NoticeService {

    // ✅ File moved out of src/ to the root-level data folder
    private static final String FILE = "data/notices.txt";
    private static final DateTimeFormatter TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private NoticeService() {}

    /* ── WRITE ─────────────────────────────────────────────────────── */
    public static synchronized void addNotice(String text) throws IOException {
        String line = TS.format(LocalDateTime.now()) + "," +
                text.replaceAll("[\\r\\n]+", " ");
        Files.write(Paths.get(FILE),
                (line + System.lineSeparator()).getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /* ── READ ──────────────────────────────────────────────────────── */
    /** all notices oldest→newest */
    public static synchronized List<String> readAll() throws IOException {
        if (!Files.exists(Paths.get(FILE))) return Collections.emptyList();
        return Files.readAllLines(Paths.get(FILE));
    }

    /** latest notice or null */
    public static synchronized String getLatest() throws IOException {
        List<String> lines = readAll();
        return lines.isEmpty() ? null : lines.get(lines.size() - 1);
    }
}

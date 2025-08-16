package util;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * locations.txt CSV  ➜  timestamp,route,location,busNo
 */
public final class LocationService {

    // ✅ File moved out of src/ to the root-level data folder
    private static final String FILE = "data/locations.txt";

    private LocationService() {}

    /* ── DRIVER: append location ─────────────────────────────────────── */

    /** Post with bus number (preferred). */
    public static synchronized void postLocation(String route,
                                                 String location,
                                                 String busNo) throws IOException {
        String line = new java.util.Date() + "," + route + "," + location + "," + busNo;
        Files.write(Paths.get(FILE),
                (line + System.lineSeparator()).getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /** Legacy 2-arg call → busNo recorded as "Unknown". */
    public static void postLocation(String route, String location) throws IOException {
        postLocation(route, location, "Unknown");
    }

    /* ── STUDENT: single-route lookup ───────────────────────────────── */
    public static synchronized String[] readLatest(String route) throws IOException {
        if (!Files.exists(Paths.get(FILE))) return null;
        List<String> lines = Files.readAllLines(Paths.get(FILE));
        for (int i = lines.size() - 1; i >= 0; i--) {
            String[] p = lines.get(i).split(",", 4);
            if (p.length == 4 && p[1].equals(route)) return p;
        }
        return null;
    }

    /* ── STUDENT: latest entry for every bus ─────────────────────────── */
    public static synchronized List<String[]> readAllLocations() throws IOException {
        if (!Files.exists(Paths.get(FILE))) return List.of();

        Map<String, String[]> latest = new LinkedHashMap<>();
        for (String line : Files.readAllLines(Paths.get(FILE))) {
            String[] p = line.split(",", 4);                 // ts,route,loc,bus
            if (p.length == 4) latest.put(p[3], p);          // keep newest per bus
        }
        return new ArrayList<>(latest.values());
    }
}

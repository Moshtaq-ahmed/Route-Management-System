package util;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/** Persists driver ↔ bus assignments (CSV: driver,bus,route). */
public final class AssignmentService {

    private static final String FILE = "src/main/resources/assignments.txt";

    private AssignmentService() {}

    /* ── create / overwrite ─────────────────────────────────────────────── */
    public static synchronized void save(String driver, String bus, String route) throws IOException {
        List<String> lines = Files.exists(Paths.get(FILE))
                ? Files.readAllLines(Paths.get(FILE))
                : new ArrayList<>();

        lines = lines.stream()
                .filter(l -> !l.split(",")[0].equals(driver))
                .collect(Collectors.toList());

        lines.add(driver + "," + bus + "," + route);
        Files.write(Paths.get(FILE), lines);
    }

    /* ── NEW: delete ─────────────────────────────────────────────────────── */
    public static synchronized void delete(String driver) throws IOException {
        if (!Files.exists(Paths.get(FILE))) return;
        List<String> lines = Files.readAllLines(Paths.get(FILE)).stream()
                .filter(l -> !l.split(",")[0].equals(driver))
                .toList();
        Files.write(Paths.get(FILE), lines);
    }

    /* ── NEW: fetch single assignment ───────────────────────────────────── */
    public static synchronized String[] get(String driver) throws IOException {
        if (!Files.exists(Paths.get(FILE))) return null;
        for (String line : Files.readAllLines(Paths.get(FILE))) {
            String[] p = line.split(",", 3);
            if (p.length == 3 && p[0].equals(driver)) return p;   // [0]=drv, [1]=bus, [2]=route
        }
        return null;
    }

    /* ── table data ─────────────────────────────────────────────────────── */
    public static synchronized List<String[]> loadAll() throws IOException {
        if (!Files.exists(Paths.get(FILE))) return new ArrayList<>();
        return Files.readAllLines(Paths.get(FILE)).stream()
                .map(l -> l.split(",", 3))
                .filter(p -> p.length == 3)
                .collect(Collectors.toList());
    }


}

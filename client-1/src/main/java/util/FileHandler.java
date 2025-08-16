package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Handles basic flat-file persistence for Route Ease users.
 * CSV format in users.txt:
 *   username,password,role,fullName,uiuId,department
 */
public final class FileHandler {

    private static final String FILE_NAME = "src/main/resources/users.txt";

    private FileHandler() {
        /* utility class – no instances */ }

    /* ───────────────────────────────────────────────────────────────────────
       AUTHENTICATION / ACCOUNT METHODS
       ──────────────────────────────────────────────────────────────────── */

    public static boolean validateUser(String username, String password, String role)
            throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(FILE_NAME));
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 3 &&
                    parts[0].equals(username) &&
                    parts[1].equals(password) &&
                    parts[2].equals(role)) {
                return true;
            }
        }
        return false;
    }

    public static boolean userExists(String username) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(FILE_NAME));
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 1 && parts[0].equals(username)) {
                return true;
            }
        }
        return false;
    }

    public static boolean addUser(String username,
                                  String password,
                                  String role,
                                  String fullName,
                                  String uiuId,
                                  String department) throws IOException {
        if (userExists(username)) {
            return false;
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            StringJoiner joiner = new StringJoiner(",");
            joiner.add(username)
                    .add(password)
                    .add(role)
                    .add(fullName)
                    .add(uiuId)
                    .add(department);
            writer.write(joiner.toString());
            writer.newLine();
        }
        return true;
    }

    public static UserProfile loadUserProfile(String username) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(FILE_NAME));
        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 6 && parts[0].equals(username)) {
                return new UserProfile(
                        parts[0],   // username
                        parts[3],   // fullName
                        parts[4],   // uiuId
                        parts[5],   // department
                        parts[2]);  // role
            }
        }
        return null;
    }

    /* ───────────────────────────────────────────────────────────────────────
       NEW: list all users that match a given role (e.g., "driver")
       Used by AdminDashboardController to populate the Drivers table.
       ──────────────────────────────────────────────────────────────────── */
    public static List<String> loadAllUsersByRole(String role) throws IOException {
        List<String> result = new ArrayList<>();
        List<String> lines  = Files.readAllLines(Paths.get(FILE_NAME));

        for (String line : lines) {
            String[] parts = line.split(",");
            if (parts.length >= 3 && parts[2].equals(role)) {
                result.add(parts[0]);          // username
            }
        }
        return result;
    }
}

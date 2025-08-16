package util;

import java.io.*;
import java.net.*;
import java.util.*;

public class SocketServer {

    private static final int PORT = 12345;
    private static final Set<PrintWriter> clientWriters = new HashSet<>();
    private static final String LOCATION_FILE = "data/locations.txt";
    private static final String NOTICE_FILE = "data/notices.txt";

    public static void main(String[] args) {
        System.out.println("🚀 Server is running on port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                synchronized (clientWriters) {
                    clientWriters.add(out);
                }

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("📩 Received: " + message);

                    // ✅ Save LOCATION or NOTICE messages to file
                    if (message.startsWith("LOCATION: ")) {
                        updateLocationFile(message);
                    } else if (message.startsWith("NOTICE: ")) {
                        writeNoticeToFile(message);
                    }

                    // ✅ Broadcast to all clients
                    synchronized (clientWriters) {
                        for (PrintWriter writer : clientWriters) {
                            writer.println(message);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
                synchronized (clientWriters) {
                    clientWriters.remove(out);
                }
            }
        }

        private void updateLocationFile(String locationMessage) {
            try {
                String[] data = locationMessage.substring("LOCATION: ".length()).split(",");
                if (data.length == 3) {
                    String route = data[0].trim();
                    String location = data[1].trim();
                    String bus = data[2].trim();
                    String timestamp = new Date().toString();

                    File file = new File(LOCATION_FILE);
                    List<String> newLines = new ArrayList<>();

                    if (file.exists()) {
                        List<String> lines = new BufferedReader(new FileReader(file)).lines().toList();
                        for (String line : lines) {
                            if (!line.contains(bus)) {
                                newLines.add(line);
                            }
                        }
                    }

                    newLines.add(timestamp + "," + route + "," + location + "," + bus);

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                        for (String line : newLines) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void writeNoticeToFile(String noticeMessage) {
            try {
                String line = noticeMessage.substring("NOTICE: ".length()).trim();
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date());
                String fullLine = timestamp + "," + line;

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(NOTICE_FILE, true))) {
                    writer.write(fullLine);
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

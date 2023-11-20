import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Scope;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class server {
    public static void main(String[] args) {
        System.setProperty("otel.tracer.provider", "io.opentelemetry.api.trace.propagation.B3Propagator$Factory");

        try {
            // Create a server socket listening on port 8080
            ServerSocket serverSocket = new ServerSocket(8080);
            System.out.println("Server is listening on port 8080...");

            while (true) {
                // Wait for a client to connect
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");

                // Start a new span for the server operation
                TracerProvider tracerProvider = GlobalOpenTelemetry.getTracerProvider();
                Tracer tracer = tracerProvider.get("server-tracer");

                // Manually create a span for the server operation
                Span span = tracer.spanBuilder("server-operation").startSpan();

                // Create a scope to manage the span's lifecycle
                try (Scope scope = span.makeCurrent()) {
                    // Process the client's request
                    List<String> receivedFiles = processClientRequest(clientSocket);

                    // Save receivedFiles as JSON
                    saveFilesAsJson(receivedFiles);
                } finally {
                    // End the span when the operation is complete
                    span.end();
                }

                // Close the client socket
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> processClientRequest(Socket clientSocket) throws IOException {
        List<String> receivedFiles = new ArrayList<>();

        // Create input stream for communication with the client
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Read the number of files to expect
        int numFiles;
        try {
            numFiles = Integer.parseInt(in.readLine());
        } catch (NumberFormatException e) {
            System.err.println("Error reading the number of files from the client.");
            return receivedFiles;
        }
        System.out.println("Expecting " + numFiles + " files from the client.");

        // Receive files from the client
        for (int i = 0; i < numFiles; i++) {
            // Generate a random file name
            String fileName = UUID.randomUUID().toString() + ".txt";

            // Read the file content from the client
            StringBuilder fileContent = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null && !line.equals("END_OF_FILE")) {
                fileContent.append(line).append("\n");
            }

            // Save the file content to a file
            try (FileOutputStream fileOutputStream = new FileOutputStream("received_" + fileName)) {
                fileOutputStream.write(fileContent.toString().getBytes());
                System.out.println("File content saved to received_" + fileName);

                // Add the file name to the list of received files
                receivedFiles.add("received_" + fileName);
            } catch (IOException e) {
                System.err.println("Error saving file content to received_" + fileName);
                e.printStackTrace();
            }
        }

        // Close the input stream
        in.close();

        return receivedFiles;
    }

    private static void saveFilesAsJson(List<String> receivedFiles) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonFiles = objectMapper.writeValueAsString(receivedFiles);

            // Save the JSON string to a file
            try (FileWriter fileWriter = new FileWriter("received_files.json")) {
                fileWriter.write(jsonFiles);
                System.out.println("Received files saved as JSON in received_files.json");
            }
        } catch (IOException e) {
            System.err.println("Error saving files as JSON.");
            e.printStackTrace();
        }
    }
}

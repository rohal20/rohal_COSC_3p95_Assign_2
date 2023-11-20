import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.context.Scope;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class client {
    public static void main(String[] args) {
        System.setProperty("otel.tracer.provider", "io.opentelemetry.api.trace.propagation.B3Propagator$Factory");

        try {
            System.out.println("Connecting to the server...");

            // Create a socket and connect to the server on localhost, port 8080
            Socket socket = new Socket("localhost", 8080);

            System.out.println("Connected to the server.");

            // Start a new span for the client operation
            TracerProvider tracerProvider = GlobalOpenTelemetry.getTracerProvider();
            Tracer tracer = tracerProvider.get("client-tracer");

            // Manually create a span for the client operation
            Span span = tracer.spanBuilder("client-operation").startSpan();

            // Create a scope to manage the span's lifecycle
            try (Scope scope = span.makeCurrent()) {
                // Create input and output streams for communication with the server
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Specify the folder path on the client side
                String desktopPath = System.getProperty("user.home") + "/Desktop" + "/Files";
                File folder = new File(desktopPath);

                // List files in the folder
                File[] files = folder.listFiles();
                if (files == null) {
                    System.err.println("No files found in the folder: " + desktopPath);
                    return;
                }

                // Send the number of files to the server
                out.println(files.length);

                // Send up to 20 files at a time
                int filesToSend = Math.min(20, files.length);
                for (int i = 0; i < filesToSend; i++) {
                    // Existing code for sending files to the server
                }

                // Close the streams and socket
                in.close();
                out.close();
                socket.close();
            } finally {
                // End the span when the operation is complete
                span.end();
            }
        } catch (IOException e) {
            System.err.println("Connection failed. Make sure the server is running and check your firewall settings.");
            e.printStackTrace();
        }
    }
}

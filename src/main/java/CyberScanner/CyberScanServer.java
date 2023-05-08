package CyberScanner;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.UUID;

public class CyberScanServer {

    IngestService ingestService = IngestService.getInstance();
    public static CyberScanServer cyberScanServerInstance = new CyberScanServer();

    public CyberScanServer() {
        startWebServer();
        System.out.println("Your server is now up and running...");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        getInstance();
    }

    private static void startWebServer() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/cyber-scan", new MyHandler());
            server.setExecutor(null); // use the default executor
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert server != null;
        server.start();
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                // read the request body
                StringBuilder requestBodyBuilder = new StringBuilder();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = exchange.getRequestBody().read(buffer)) != -1) {
                    requestBodyBuilder.append(new String(buffer, 0, bytesRead));
                }
                String requestBody = requestBodyBuilder.toString();
                // generate a request ID
                String requestId = UUID.randomUUID().toString();

                // send the response
                String responseBody = "{\"Request_ID\":" + "\"" + requestId + "\"}";
                exchange.sendResponseHeaders(200, responseBody.length());
                System.out.println("server handled a new post successfully!");
                OutputStream os = exchange.getResponseBody();
                os.write(responseBody.getBytes());
                os.close();
            }
        }
    }



    public static CyberScanServer getInstance() {
        if (cyberScanServerInstance == null) cyberScanServerInstance = new CyberScanServer();
        return cyberScanServerInstance;
    }
}

package CyberScanner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class DispatcherService {
    Instant initInstant = Instant.now();
    String url = "http://localhost:8080/cyber-scan";
    Map<String, DispatcherResponse> responsesBulkQueue = new HashMap<>();
    IngestService ingestService = IngestService.getInstance();

    public String dispatchScan() {
        try {
            return sendScanRequest(url, "");
        } catch (IOException e) {
            e.printStackTrace();
            return e.toString();
        }
    }

    public String sendScanRequest(String urlString, String requestBody) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setRequestProperty("Content-Length", Integer.toString(requestBody.getBytes().length));
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }
            String uuid = getUUIDFromResponse(responseBuilder.toString());
            addEntryToPreviousAlertsMap(uuid, new DispatcherResponse(String.valueOf(responseCode), responseBuilder.toString()));
            return uuid;
        } else {
            throw new RuntimeException("Failed to send POST request: HTTP error code " + responseCode);
        }
    }

    private String getUUIDFromResponse(String res) {
        JsonReader jsonReader = Json.createReader(new StringReader(res));
        JsonObject jsonObject = jsonReader.readObject();
        return jsonObject.getString("Request_ID");
    }

    public synchronized void addEntryToPreviousAlertsMap(String responseUUID, DispatcherResponse response) {
        responsesBulkQueue.put(responseUUID, response);
        ingestService.scanStatusDB.put(responseUUID, Status.ACCEPTED.name());
        if (Instant.now().minus(1, ChronoUnit.SECONDS).toEpochMilli() > initInstant.toEpochMilli() || responsesBulkQueue.size() > 0) {
            initInstant = Instant.now();
            sendToIngest();
        }
    }

    public synchronized void sendToIngest() {
        if (responsesBulkQueue.isEmpty()) return;

        for (Map.Entry<String, DispatcherResponse> entry: responsesBulkQueue.entrySet()) {
            String resString ="{\"uuid\":"+"\""+entry.getKey()+"\","+"\""+entry.getValue().status+"\":"+entry.getValue().responseBody+"}";
            boolean isAcceptedToQueue = ingestService.scanRequestsQueue.offer(resString);
            if (isAcceptedToQueue) deleteEntryFromResponsesBulkQueue(entry.getKey());
        }
    }
    public synchronized void deleteEntryFromResponsesBulkQueue(String key) {
        responsesBulkQueue.remove(key);
    }

}

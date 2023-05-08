package CyberScanner;

import com.sun.net.httpserver.HttpServer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class IngestService implements Closeable {

    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    public BlockingDeque<String> scanRequestsQueue = new LinkedBlockingDeque<>();
    public Map<String, String> scanStatusDB = new HashMap<>();
    public BlockingDeque<String> scansToProcess = new LinkedBlockingDeque<>();

    Thread thread = new Thread(this::consumeQueue);
    public static IngestService ingestServiceSingletonInstance = new IngestService();

    public IngestService() {
        thread.start();
    }

    /**
     * Handle incoming scan requests.
     * Generates a unique scan id for each request, adds it to the
     * request queue and returns the scan id to the caller.
     */
    public synchronized String handleScanRequest(String msgToScan) {
        String scanId = getUUIDOfScan(msgToScan);
        scanStatusDB.replace(scanId, Status.RUNNING.name());
        ingestDataToProcessingQueue(msgToScan);
        return scanId;
    }

    private void consumeQueue() {
        while (!shutdownFlag.get()) {
            try {
                String msgToScan = scanRequestsQueue.poll();
                if (msgToScan != null) {
                    handleScanRequest(msgToScan);
                }
            } catch (Throwable t) {
                System.out.println("Failed unexpectedly while consuming the queue. Stopping..." + t);
                closed.set(true);
            }
        }
    }

    private void ingestDataToProcessingQueue(String msgToScan) {
        scansToProcess.offer(msgToScan);
    }

    private String getUUIDOfScan(String msgToScan) {
        JsonReader jsonReader = Json.createReader(new StringReader(msgToScan));
        JsonObject jsonObject = jsonReader.readObject();
        return jsonObject.getString("uuid");
    }

    @Override
    public void close() throws IOException {
        if (closed.get()) return;
        int slept = 1000;
        while (!scanRequestsQueue.isEmpty() && slept < 10000) {
            try {
                Thread.sleep(1000);
                slept += 1000;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        shutdownFlag.set(true);
        thread.interrupt();
        closed.set(true);
    }

    public static IngestService getInstance() {
        if (ingestServiceSingletonInstance == null) ingestServiceSingletonInstance = new IngestService();
        return ingestServiceSingletonInstance;
    }
}


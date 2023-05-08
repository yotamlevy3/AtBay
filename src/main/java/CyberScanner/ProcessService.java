package CyberScanner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProcessService {

    private final int PROCESS_TIME = 500;
    IngestService ingestService = IngestService.getInstance();
    private final AtomicBoolean shutdownFlag = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    Thread thread = new Thread(this::consumeQueue);

    public ProcessService() {
        thread.start();
    }

    private void consumeQueue() {
        while (!shutdownFlag.get()) {
            try {
                if (!ingestService.scansToProcess.isEmpty()) {
                    String msgToScan = ingestService.scansToProcess.peek();
                    if (msgToScan != null) {
                        processScanRequest(msgToScan);
                    }
                }
            } catch (Throwable t) {
                System.out.println("Failed unexpectedly while consuming the queue. Stopping..." + t);
                closed.set(true);
            }
        }
    }

     /**
     * process scan requests from the queue.
     * Gets the next scan request from the queue and updates its status to "Running".
     * Simulates a scan by incrementing the scan progress by a random amount.
     * Updates the scan status to "Complete" when scan process is done.
     * Updates the scan status to "Error" when a random error occurs during the scan.
     */
    public synchronized void processScanRequest(String msgToScan) {
        String scanId = getScanIDFromResponse(msgToScan);
        try {
            ingestService.scanStatusDB.replace(scanId, Status.RUNNING.name());
            Thread.sleep(PROCESS_TIME); //processing...
            String jsonString = ingestService.scansToProcess.peek();
            jsonString += "_DONE_SOMETHING_THEN_PROCESS_IS_OVER!";
            ingestService.scanStatusDB.replace(scanId, Status.COMPLETE.name());
            ingestService.scansToProcess.remove(msgToScan);
        } catch (Exception e) {
            ingestService.scanStatusDB.replace(scanId, Status.ERROR.name());
        }

    }

    private String getScanIDFromResponse(String res) {
        JsonReader jsonReader = Json.createReader(new StringReader(res));
        JsonObject jsonObject = jsonReader.readObject();
        return jsonObject.getString("uuid");
    }

}

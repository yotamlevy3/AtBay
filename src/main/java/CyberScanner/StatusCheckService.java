package CyberScanner;

public class StatusCheckService {
    IngestService ingestService = IngestService.getInstance();

    public StatusCheckService() {}

    public String getStatus(String scanId) {
        return ingestService.scanStatusDB.get(scanId);
    }
}

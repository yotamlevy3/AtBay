package CyberScanner;

public class DispatcherResponse {

    String status;
    String responseBody;

    public DispatcherResponse(String status, String responseBody) {
        this.status = status;
        this.responseBody = responseBody;
    }
}

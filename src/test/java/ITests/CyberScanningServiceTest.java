package ITests;

import CyberScanner.DispatcherService;
import CyberScanner.ProcessService;
import CyberScanner.StatusCheckService;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CyberScanningServiceTest {

    @Test
    public void test() {

        DispatcherService dispatcher = new DispatcherService();
        StatusCheckService statusCheckService = new StatusCheckService();
        new ProcessService();

        // Test ingestion system
        String scanId1 = dispatcher.dispatchScan();
        String scanId2 = dispatcher.dispatchScan();
        String scanId3 = dispatcher.dispatchScan();

        // Test status system
        assertThat(statusCheckService.getStatus(scanId1), anyOf(is("ACCEPTED"), is("RUNNING")));
        assertThat(statusCheckService.getStatus(scanId2), anyOf(is("ACCEPTED"), is("RUNNING")));
        assertThat(statusCheckService.getStatus(scanId3), anyOf(is("ACCEPTED"), is("RUNNING")));

        waitForServerToCompleteScans();
        // Test status Processor service
        assertEquals("COMPLETE", statusCheckService.getStatus(scanId3));
        // Test invalid scan ID
        assertEquals(null, statusCheckService.getStatus("invalid-scan-id"));

    }

    private void waitForServerToCompleteScans() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

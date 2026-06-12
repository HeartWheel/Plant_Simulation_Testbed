package org.zhaoxinlun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "testbed")
public class TestbedProperties {

    private Amhs amhs = new Amhs();
    private WebSocket webSocket = new WebSocket();
    private TransferJob transferJob = new TransferJob();
    private Points points = new Points();

    @Data
    public static class Amhs {
        private String relayUrl;
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration readTimeout = Duration.ofSeconds(5);
    }

    @Data
    public static class WebSocket {
        private boolean enabled = true;
        private String url = "ws://localhost:8081/ws";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration reconnectDelay = Duration.ofSeconds(5);
    }

    @Data
    public static class TransferJob {
        private int generationRatePerMinute = 45;
    }

    @Data
    public static class Points {
        private String classpathResource = "sample-amhs-points.json";
        private String externalFile;
    }
}

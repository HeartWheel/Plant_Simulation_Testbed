package org.zhaoxinlun.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "testbed")
public class TestbedProperties {

    private Amhs amhs = new Amhs();
    private TransferJob transferJob = new TransferJob();
    private Points points = new Points();

    @Data
    public static class Amhs {
        private String relayUrl;
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

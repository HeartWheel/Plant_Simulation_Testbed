package org.zhaoxinlun;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PlantSimulationTestbedApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlantSimulationTestbedApplication.class, args);
    }
}

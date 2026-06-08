package org.zhaoxinlun.point;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.zhaoxinlun.config.TestbedProperties;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmhsPointPool {

    private static final TypeReference<List<AmhsPoint>> POINT_LIST_TYPE = new TypeReference<>() {
    };

    private final JsonMapper jsonMapper;
    private final ResourceLoader resourceLoader;
    private final TestbedProperties properties;

    private List<AmhsPoint> allPoints = List.of();
    private List<String> loadPortAliases = List.of();

    @PostConstruct
    public void load() throws IOException {
        allPoints = loadPoints();
        loadPortAliases = allPoints.stream()
                .map(this::firstLoadPortAlias)
                .filter(Objects::nonNull)
                .toList();

        if (loadPortAliases.size() < 2) {
            throw new IllegalStateException("At least two LP points are required to generate transfer jobs.");
        }

        log.info("Loaded {} AMHS points, including {} LP candidates for transfer jobs.",
                allPoints.size(), loadPortAliases.size());
    }

    public String randomLoadPortAlias(RandomGenerator random) {
        return loadPortAliases.get(random.nextInt(loadPortAliases.size()));
    }

    public int totalPointCount() {
        return allPoints.size();
    }

    public int loadPortCount() {
        return loadPortAliases.size();
    }

    private List<AmhsPoint> loadPoints() throws IOException {
        String externalFile = properties.getPoints().getExternalFile();
        if (externalFile != null && !externalFile.isBlank()) {
            Path path = Path.of(externalFile);
            try (InputStream inputStream = Files.newInputStream(path)) {
                log.info("Loading AMHS point pool from external file: {}", path.toAbsolutePath());
                return jsonMapper.readValue(inputStream, POINT_LIST_TYPE);
            }
        }

        String resourceName = properties.getPoints().getClasspathResource();
        Resource resource = resourceLoader.getResource("classpath:" + resourceName);
        try (InputStream inputStream = resource.getInputStream()) {
            log.info("Loading AMHS point pool from classpath resource: {}", resourceName);
            return jsonMapper.readValue(inputStream, POINT_LIST_TYPE);
        }
    }

    private String firstLoadPortAlias(AmhsPoint point) {
        if (point.getTags() == null || point.getTags().isBlank()) {
            return null;
        }

        for (String alias : point.getTags().split("\\s*/\\s*")) {
            String trimmed = alias.trim();
            if (trimmed.contains("LP")) {
                return trimmed;
            }
        }

        return null;
    }
}

package org.zhaoxinlun.transfer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.zhaoxinlun.config.TestbedProperties;
import org.zhaoxinlun.point.AmhsPointPool;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferJobGenerationService {

    private static final String TRANSFER_JOB_ENQUEUE_ACTION = "transferJobEnqueue";
    private static final String STATUS_GENERATED = "GENERATED";
    private static final String STATUS_SUBMITTED = "SUBMITTED";
    private static final String STATUS_SUBMIT_FAILED = "SUBMIT_FAILED";

    private final TestbedProperties properties;
    private final AmhsPointPool amhsPointPool;
    private final TransferJobRegistry transferJobRegistry;
    private final RestClient.Builder restClientBuilder;

    private final ScheduledExecutorService generationExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "transfer-job-generator");
        thread.setDaemon(false);
        return thread;
    });
    private final ExecutorService senderExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private RestClient restClient;
    private volatile boolean running;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        int ratePerMinute = properties.getTransferJob().getGenerationRatePerMinute();
        if (ratePerMinute <= 0) {
            throw new IllegalStateException("Transfer job generation rate must be greater than 0.");
        }

        restClient = restClientBuilder.build();
        running = true;

        log.info("Starting transfer job generator. ratePerMinute={}, meanInterArrivalMillis={}, relayUrl={}",
                ratePerMinute, Math.round(60_000.0 / ratePerMinute), properties.getAmhs().getRelayUrl());

        scheduleNextGeneration();
    }

    @PreDestroy
    public void stop() {
        running = false;
        generationExecutor.shutdownNow();
        senderExecutor.shutdownNow();
    }

    private void scheduleNextGeneration() {
        Duration delay = nextInterArrivalDelay(ThreadLocalRandom.current());
        generationExecutor.schedule(this::generateAndScheduleNext,
                delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void generateAndScheduleNext() {
        if (!running) {
            return;
        }

        try {
            TransferJob transferJob = generateTransferJob(ThreadLocalRandom.current());
            transferJobRegistry.register(transferJob);
            log.info("Generated transfer job. jobId={}, source={}, destination={}, status={}",
                    transferJob.getJobId(), transferJob.getSource(), transferJob.getDestination(), transferJob.getStatus());
            senderExecutor.submit(() -> sendToAmhs(transferJob));
        } finally {
            if (running) {
                scheduleNextGeneration();
            }
        }
    }

    private TransferJob generateTransferJob(RandomGenerator random) {
        String source = amhsPointPool.randomLoadPortAlias(random);
        String destination = amhsPointPool.randomLoadPortAlias(random);
        while (source.equals(destination)) {
            destination = amhsPointPool.randomLoadPortAlias(random);
        }

        return new TransferJob(UUID.randomUUID().toString(), source, destination, STATUS_GENERATED);
    }

    private Duration nextInterArrivalDelay(RandomGenerator random) {
        int ratePerMinute = properties.getTransferJob().getGenerationRatePerMinute();
        double uniform = random.nextDouble();
        double delayMinutes = -Math.log(1.0 - uniform) / ratePerMinute;
        long delayMillis = Math.max(1L, Math.round(delayMinutes * 60_000.0));
        return Duration.ofMillis(delayMillis);
    }

    private void sendToAmhs(TransferJob transferJob) {
        TransferJobRelayRequest request = new TransferJobRelayRequest(
                TRANSFER_JOB_ENQUEUE_ACTION,
                transferJob.getJobId(),
                transferJob.getSource(),
                transferJob.getDestination()
        );

        long startNanos = System.nanoTime();
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri(properties.getAmhs().getRelayUrl())
                    .body(request)
                    .retrieve()
                    .toEntity(String.class);
            long latencyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);

            transferJobRegistry.updateStatus(transferJob.getJobId(), STATUS_SUBMITTED);
            log.info("Submitted transfer job to AMHS. jobId={}, httpStatus={}, latencyMillis={}, responseBody={}",
                    transferJob.getJobId(), response.getStatusCode().value(), latencyMillis, response.getBody());
        } catch (Exception exception) {
            long latencyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            transferJobRegistry.updateStatus(transferJob.getJobId(), STATUS_SUBMIT_FAILED);
            log.warn("Failed to submit transfer job to AMHS. jobId={}, latencyMillis={}, error={}",
                    transferJob.getJobId(), latencyMillis, exception.getMessage(), exception);
        }
    }
}

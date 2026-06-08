package org.zhaoxinlun.transfer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Component
public class TransferJobRegistry {

    private final ConcurrentMap<String, TransferJob> transferJobs = new ConcurrentHashMap<>();

    public void register(TransferJob transferJob) {
        transferJobs.put(transferJob.getJobId(), transferJob);
    }

    public Optional<TransferJob> updateStatus(String jobId, String status) {
        TransferJob transferJob = transferJobs.get(jobId);
        if (transferJob == null) {
            log.warn("Received status update for unknown transfer job. jobId={}, status={}", jobId, status);
            return Optional.empty();
        }

        transferJob.setStatus(status);
        log.info("Transfer job status updated. jobId={}, status={}", jobId, status);
        return Optional.of(transferJob);
    }

    public Optional<TransferJob> findByJobId(String jobId) {
        return Optional.ofNullable(transferJobs.get(jobId));
    }
}

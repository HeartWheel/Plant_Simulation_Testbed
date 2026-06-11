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
        log.info("Transfer job entered registry. jobId={}, activeTransferJobCount={}",
                transferJob.getJobId(), transferJobs.size());
    }

    public Optional<TransferJob> updateStatus(String jobId, String status) {
        return updateStatus(jobId, status, true);
    }

    public Optional<TransferJob> updateStatusSilently(String jobId, String status) {
        return updateStatus(jobId, status, false);
    }

    private Optional<TransferJob> updateStatus(String jobId, String status, boolean logStatusUpdate) {
        TransferJob transferJob = transferJobs.get(jobId);
        if (transferJob == null) {
            log.warn("Received status update for unknown transfer job. jobId={}, status={}", jobId, status);
            return Optional.empty();
        }
        if (!TransferJobStatuses.isKnown(status)) {
            log.warn("Received unknown transfer job status. jobId={}, status={}", jobId, status);
            return Optional.empty();
        }

        transferJob.setStatus(status);
        if (logStatusUpdate) {
            log.info("Transfer job status updated. jobId={}, status={}", jobId, status);
        }
        if (TransferJobStatuses.isTerminal(status)) {
            transferJobs.remove(jobId);
            log.info("Transfer job terminated and left registry. jobId={}, status={}, activeTransferJobCount={}",
                    jobId, status, transferJobs.size());
        }
        return Optional.of(transferJob);
    }

    public Optional<TransferJob> findByJobId(String jobId) {
        return Optional.ofNullable(transferJobs.get(jobId));
    }

    public void remove(String jobId) {
        TransferJob removedJob = transferJobs.remove(jobId);
        if (removedJob != null) {
            log.info("Transfer job left registry. jobId={}, status={}, activeTransferJobCount={}",
                    removedJob.getJobId(), removedJob.getStatus(), transferJobs.size());
        }
    }

}

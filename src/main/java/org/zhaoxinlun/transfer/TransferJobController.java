package org.zhaoxinlun.transfer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/transfer-jobs")
public class TransferJobController {

    private final TransferJobRegistry transferJobRegistry;

    @PostMapping("/status")
    public ResponseEntity<TransferJob> updateStatus(@RequestBody TransferJobStatusUpdateRequest request) {
        if (isBlank(request.getTransferJobId()) || isBlank(request.getStatus())) {
            log.warn("Received invalid transfer job status update. transferJobId={}, status={}",
                    request.getTransferJobId(), request.getStatus());
            return ResponseEntity.badRequest().build();
        }

        return transferJobRegistry.updateStatus(request.getTransferJobId(), request.getStatus())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

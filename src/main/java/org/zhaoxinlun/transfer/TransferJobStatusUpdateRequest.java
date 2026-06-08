package org.zhaoxinlun.transfer;

import lombok.Data;

@Data
public class TransferJobStatusUpdateRequest {

    private String transferJobId;
    private String status;
}

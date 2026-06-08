package org.zhaoxinlun.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransferJobRelayRequest {

    private String action;
    private String transferJobId;
    private String source;
    private String destination;
}

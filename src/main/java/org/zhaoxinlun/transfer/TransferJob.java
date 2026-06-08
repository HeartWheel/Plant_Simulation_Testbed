package org.zhaoxinlun.transfer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransferJob {

    private String jobId;
    private String source;
    private String destination;
    private String status;
}

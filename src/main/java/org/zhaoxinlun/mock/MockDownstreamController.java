package org.zhaoxinlun.mock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class MockDownstreamController {

    @PostMapping("/mock-e84/{portId}")
    public MockE84Response mockE84(
            @PathVariable Integer portId,
            @RequestParam(name = "operation") String operation,
            @RequestBody MockE84Request request
    ) {
        log.info("Mock E84 API called. portId={}, operation={}, transferJobId={}",
                portId, operation, request.getTransferJobId());
        return new MockE84Response(true, "OK");
    }

    @PostMapping("/location")
    public boolean updateCarrierLocation(@RequestBody CarrierLocationRequest request) {
        log.info("Mock carrier location API called. carrierId={}, location={}, requestBody={}",
                request.getCarrierId(), request.getLocation(), request);
        return true;
    }
}

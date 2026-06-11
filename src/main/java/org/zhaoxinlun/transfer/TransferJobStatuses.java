package org.zhaoxinlun.transfer;

import java.util.Set;

public final class TransferJobStatuses {

    public static final String GENERATED = "GENERATED";
    public static final String QUEUED = "QUEUED";
    public static final String INVALID = "INVALID";
    public static final String TO_SOURCE = "TO_SOURCE";
    public static final String TO_DESTINATION = "TO_DESTINATION";
    public static final String COMPLETE = "COMPLETE";

    private static final Set<String> ALL = Set.of(
            GENERATED,
            QUEUED,
            INVALID,
            TO_SOURCE,
            TO_DESTINATION,
            COMPLETE
    );

    public static boolean isKnown(String status) {
        return ALL.contains(status);
    }

    public static boolean isTerminal(String status) {
        return INVALID.equals(status) || COMPLETE.equals(status);
    }

    private TransferJobStatuses() {
    }
}

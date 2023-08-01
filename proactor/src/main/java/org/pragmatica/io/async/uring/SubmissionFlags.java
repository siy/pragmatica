package org.pragmatica.io.async.uring;

public enum SubmissionFlags implements Bitmask {
    IMMEDIATE(1),
    WAIT_FOR_COUNT(2);
    ;
    private final int mask;

    SubmissionFlags(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}

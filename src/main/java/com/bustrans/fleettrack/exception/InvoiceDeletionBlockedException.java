package com.bustrans.fleettrack.exception;

public class InvoiceDeletionBlockedException extends RuntimeException {
    public InvoiceDeletionBlockedException() {
        super("This invoice cannot be deleted because payment records reference it.");
    }
}


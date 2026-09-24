package com.transport.uni_transport_system.exception;

public class InvoiceDeletionBlockedException extends RuntimeException {
    public InvoiceDeletionBlockedException() {
        super("This invoice cannot be deleted because payment records reference it.");
    }
}


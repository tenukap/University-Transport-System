package com.bustrans.fleettrack.dto;

import java.time.LocalDate;

public class UpdateInvoiceDueDateRequest {
    private LocalDate dueDate;

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}

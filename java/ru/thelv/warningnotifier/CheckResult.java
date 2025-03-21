package ru.thelv.warningnotifier;

import java.util.Date;

public class CheckResult {
    private Date timestamp;
    private int status;

    public CheckResult(Date timestamp, int status) {
        this.timestamp = timestamp;
        this.status = status;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }
} 
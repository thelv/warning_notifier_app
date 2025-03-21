package ru.thelv.warningnotifier;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class UrlCheckWorker extends Worker {
    public UrlCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        MonitoringManager.getInstance(getApplicationContext()).checkUrl();
        return Result.success();
    }
} 
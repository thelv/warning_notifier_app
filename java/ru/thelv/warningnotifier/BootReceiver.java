package ru.thelv.warningnotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Просто создаем экземпляр MonitoringManager
            // Он сам проверит состояние ошибки и покажет уведомление если нужно
            MonitoringManager.getInstance(context);
        }
    }
} 
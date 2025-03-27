package ru.thelv.warningnotifier;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.work.*;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MonitoringManager {
    private static MonitoringManager instance;
    private Notifications notifications;

    private String urlGoogle ="http://www.google.com/";

    private static final String PREFS_NAME = "MonitoringPrefs";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_LAST_SUCCESS_TIME = "lastSuccessTime";
    private static final String KEY_ERROR_NOTIFICATION_ACTIVE = "errorNotificationActive";
    private static final String KEY_URL = "url";
    private static final String KEY_CHECK_INTERVAL = "checkInterval";
    private static final String KEY_OFFLINE_THRESHOLD = "offlineThreshold";
    private static final String KEY_ERROR_NOTIFICATION_TYPE = "errorNotificationType";
    private static final String KEY_CHECK_LOG = "checkLog";

    private static final String NOTIFICATION_CHANNEL_ID = "monitoring_channel";
    private static final int NOTIFICATION_ID = 1;

    private Context applicationContext;
    private MainActivity activity;
    private boolean active;
    private long lastSuccessTime;
    private boolean errorNotificationActive;
    public String url;
    public int checkInterval;
    public int offlineThreshold;
    private int errorNotificationType;
    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;
    private boolean updateViewNeeded = false;
    private List<CheckResult> checkLog; // Массив для хранения результатов проверок

    class Notifications
    {
        boolean errorShowing=false;

        Notifications()
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "Monitoring Notifications",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Notifications for URL monitoring status");

                NotificationManager notificationManager =
                        applicationContext.getSystemService(NotificationManager.class);
                notificationManager.createNotificationChannel(channel);
            }
        }

        void show()
        {
            show(false);
        }

        void show(boolean newEvent)
        {
            if(errorNotificationActive)
            {
                if(errorShowing && ! newEvent) return;
                errorShowing=true;

                Intent intent = new Intent(applicationContext, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                );

                String title, content;
                if (errorNotificationType == ERROR_NOTIFICATION_ERROR) {
                    title = "URL Check Failed";
                    content = "The URL returned an error status";
                } else {
                    title = "URL Unavailable";
                    content = "The URL has been unreachable for too long";
                }

                // Создаем уведомление
                NotificationCompat.Builder builder = new NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_alert)
                        .setContentTitle(title)
                        .setContentText(content)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setContentIntent(pendingIntent);

                // Получаем NotificationManager
                NotificationManager notificationManager =
                        (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);

                // Вибрация
                long[] vibrationPattern = {0, 200, 100, 200}; // Пауза, вибрация, пауза, вибрация
                builder.setVibrate(vibrationPattern);

                // Звук уведомления
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                builder.setSound(soundUri);

                // Показываем уведомление
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
            else if(active)
            {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());
                Intent intent = new Intent(applicationContext, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        applicationContext,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE
                );

                String title, content;
                title = "Monitoring active ("+((lastSuccessTime!=0) ? dateFormat.format(lastSuccessTime) : "never yet")+" success)";

                // Создаем уведомление
                NotificationCompat.Builder builder = new NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setContentTitle(title)
                        //.setContentText(content)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setContentIntent(pendingIntent);

                // Получаем NotificationManager
                NotificationManager notificationManager =
                        (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);

                // Вибрация

                // Звук уведомления
                builder.setSound(null);

                // Показываем уведомление
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
            else
            {
                NotificationManager notificationManager =
                        (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(NOTIFICATION_ID);
            }
        }
    }

    private MonitoringManager(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.editor = prefs.edit();
        notifications=new Notifications();
        loadSettings();
        loadCheckLog(); // Загружаем лог при создании объекта

        //notifications.show();

        if(activity!=null) activity.showLog();
    }

    public static synchronized MonitoringManager getInstance(Context context) {
        if (instance == null) {
            instance = new MonitoringManager(context);
        }
        return instance;
    }

    public void showNotification()
    {
        notifications.show(true);
    }

    public void setActivity(MainActivity activity) {
        this.activity = activity;
        if (activity != null) {
           // activity.updateView();
        }
    }

    private void loadSettings() {
        active = prefs.getBoolean(KEY_ACTIVE, false);
        lastSuccessTime = prefs.getLong(KEY_LAST_SUCCESS_TIME, 0);
        errorNotificationActive = prefs.getBoolean(KEY_ERROR_NOTIFICATION_ACTIVE, false);
        url = prefs.getString(KEY_URL, "");
        checkInterval = prefs.getInt(KEY_CHECK_INTERVAL, 1);
        offlineThreshold = prefs.getInt(KEY_OFFLINE_THRESHOLD, 24);
        errorNotificationType = prefs.getInt(KEY_ERROR_NOTIFICATION_TYPE, ERROR_NOTIFICATION_UNAVAILABLE);
    }

    public void saveSettings() {
        editor.putString(KEY_URL, url)
              .putInt(KEY_CHECK_INTERVAL, checkInterval)
              .putInt(KEY_OFFLINE_THRESHOLD, offlineThreshold)
              .apply();

        if (activity != null) {
            activity.runOnUiThread(() -> activity.updateView());
        }
    }

    public void start() {

        if(errorNotificationActive)
        {
            Toast.makeText(activity, "Reset Error before start!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!active) {

            setActive(true);
            // Обнуляем checkLog при старте
            checkLog.clear();
            saveCheckLog(); // Сохраняем пустой лог в SharedPreferences
            if (activity != null) {
                activity.runOnUiThread(() -> activity.showLog()); // Вызов showLog в UI потоке
            }

            setLastSuccessTime(0);//System.currentTimeMillis());

            // Настройка периодической работы
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                UrlCheckWorker.class,
                checkInterval,
                TimeUnit.HOURS,
                60,
                TimeUnit.MINUTES
            ).build();

            WorkManager.getInstance(applicationContext)
                .enqueueUniquePeriodicWork(
                    "urlCheck",
                    ExistingPeriodicWorkPolicy.REPLACE,
                    workRequest
                );

            //checkUrl(); // Первый запуск

            updateView(); // Вызываем обновление после старта

            notifications.show();
        }
    }

    public void stop() {
        if (active) {
            setActive(false);
            setLastSuccessTime(0);
            WorkManager.getInstance(applicationContext).cancelUniqueWork("urlCheck");
            updateView(); // Вызываем обновление после остановки
            notifications.show();
        }
    }

    public void test() {
        // Берем значения из MainActivity для теста
        if (activity != null) {
            url = activity.getUrl();
            checkInterval = activity.getCheckInterval();
            offlineThreshold = activity.getOfflineThreshold();
        }

        // Выполняем тест и показываем результат
        new Thread(() -> {
            int result = UrlChecker.checkUrlExt(url, urlGoogle);
            if (activity != null) {
                activity.runOnUiThread(() -> {
                    String message;
                    switch (result)
                    {
                        case UrlChecker.SUCCESS: message= "URL check successful (200 OK)"; break;
                        case UrlChecker.ERROR: message= "URL check failed"; break;
                        case UrlChecker.INTERNET_UNAVAILABLE: message="Internet unavailable"; break;
                        default: message= "Unknown error";
                    };
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    public void resetError() {
        setErrorNotificationActive(false);
        setLastSuccessTime(System.currentTimeMillis());
        notifications.show();
        updateView();
    }

    public void checkUrl() {
        new Thread(() -> {
            int result = UrlChecker.checkUrlExt(url, urlGoogle);

            switch (result) {
                case UrlChecker.SUCCESS:
                    setLastSuccessTime(System.currentTimeMillis());
                    break;
                case UrlChecker.ERROR:
                    errorHappenedHandler(ERROR_NOTIFICATION_ERROR);
                    break;
                case UrlChecker.INTERNET_UNAVAILABLE:
                    long currentTime = System.currentTimeMillis();
                    long threshold = TimeUnit.HOURS.toMillis(offlineThreshold);
                    if (currentTime - lastSuccessTime > threshold) {
                        errorHappenedHandler(ERROR_NOTIFICATION_UNAVAILABLE);
                    }
                    break;
                default:
            }

            // Добавляем результат в лог
            checkLog.add(new CheckResult(new Date(), result));
            saveCheckLog(); // Сохраняем лог в SharedPreferences

            // Вызов showLog в MainActivity, если она не null
            if (activity != null) {
                activity.runOnUiThread(() -> activity.showLog());
            }

            updateView(); // Вызываем обновление после проверки URL
            notifications.show(true);
        }).start();
    }

    private void errorHappenedHandler(int errorType) {
        setErrorNotificationActive(true);
        setErrorNotificationType(errorType);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isErrorNotificationActive() {
        return errorNotificationActive;
    }

    private void setActive(boolean value) {
        if (active != value) {
            active = value;
            updateViewNeeded = true; // Устанавливаем флаг
            editor.putBoolean(KEY_ACTIVE, value).apply();
        }
    }

    private void setLastSuccessTime(long value) {
        if (lastSuccessTime != value) {
            lastSuccessTime = value;
           // updateViewNeeded = true; // Устанавливаем флаг
            editor.putLong(KEY_LAST_SUCCESS_TIME, value).apply();
        }
    }

    private void setErrorNotificationActive(boolean value) {
        if (errorNotificationActive != value) {
            errorNotificationActive = value;
            updateViewNeeded = true; // Устанавливаем флаг
            editor.putBoolean(KEY_ERROR_NOTIFICATION_ACTIVE, value).apply();
        }
    }

    private void setErrorNotificationType(int value) {
        if (errorNotificationType != value) {
            errorNotificationType = value;
            updateViewNeeded = true; // Устанавливаем флаг
            editor.putInt(KEY_ERROR_NOTIFICATION_TYPE, value).apply();
        }
    }

    public void updateView() {
        if (updateViewNeeded && activity != null) {
            activity.runOnUiThread(() -> activity.updateView());
            updateViewNeeded = false; // Сбрасываем флаг после обновления
        }
    }

    private void loadCheckLog() {
        Gson gson = new Gson();
        String json = prefs.getString(KEY_CHECK_LOG, null);
        Type type = new TypeToken<ArrayList<CheckResult>>() {}.getType();
        checkLog = gson.fromJson(json, type);
        if (checkLog == null) {
            checkLog = new ArrayList<>(); // Инициализируем, если лог пуст
        }
    }

    private void saveCheckLog() {
        Gson gson = new Gson();
        String json = gson.toJson(checkLog);
        editor.putString(KEY_CHECK_LOG, json).apply();
    }

    public List<CheckResult> getCheckLog() {
        return checkLog;
    }

    public static final int ERROR_NOTIFICATION_UNAVAILABLE = 1;
    public static final int ERROR_NOTIFICATION_ERROR = 2;
}
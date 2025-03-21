package ru.thelv.warningnotifier;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private MonitoringManager monitoringManager;
    private EditText urlInput;
    private EditText checkIntervalInput;
    private EditText offlineThresholdInput;
    private Button startMonitoringButton;
    private Button stopMonitoringButton;
    private Button resetErrorButton;
    private Button testUrlButton;
    private Button saveButton;
    private TextView statusTextView;
    private TextView logTextView;

    // Добавляем поля для хранения значений
    private String url = "";
    private int checkInterval = 1;
    private int offlineThreshold = 24;
    private boolean hasUnsavedChanges = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        monitoringManager = MonitoringManager.getInstance(this);
        monitoringManager.setActivity(this);
        initializeViews();
        loadSettings();
        setupInputValidation();
        updateView();
        //logTextView = findViewById(R.id.logTextView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (monitoringManager != null) {
            monitoringManager.setActivity(null);
        }
    }

    private void initializeViews() {
        urlInput = findViewById(R.id.urlInput);
        checkIntervalInput = findViewById(R.id.checkIntervalInput);
        offlineThresholdInput = findViewById(R.id.offlineThresholdInput);
        startMonitoringButton = findViewById(R.id.startMonitoringButton);
        stopMonitoringButton = findViewById(R.id.stopMonitoringButton);
        resetErrorButton = findViewById(R.id.resetErrorButton);
        testUrlButton = findViewById(R.id.testUrlButton);
        saveButton = findViewById(R.id.saveButton);
        statusTextView = findViewById(R.id.statusTextView);

        startMonitoringButton.setOnClickListener(v -> {
            saveFields();
            monitoringManager.start();
        });
        stopMonitoringButton.setOnClickListener(v -> monitoringManager.stop());
        resetErrorButton.setOnClickListener(v -> monitoringManager.resetError());
        testUrlButton.setOnClickListener(v -> monitoringManager.test());
        saveButton.setOnClickListener(v -> saveFields());
    }

    private void setupInputValidation() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (monitoringManager.isActive()) {
                    Toast.makeText(MainActivity.this,
                        "Stop monitoring before changing settings",
                        Toast.LENGTH_SHORT).show();
                    loadSettings();
                    return;
                }
                updateFieldValues();
            }
        };

        urlInput.addTextChangedListener(watcher);
        checkIntervalInput.addTextChangedListener(watcher);
        offlineThresholdInput.addTextChangedListener(watcher);
    }

    private void updateFieldValues() {
        String newUrl = urlInput.getText().toString().trim();
        String newIntervalStr = checkIntervalInput.getText().toString().trim();
        String newThresholdStr = offlineThresholdInput.getText().toString().trim();

        try {
            int newInterval = newIntervalStr.isEmpty() ? 1 : Integer.parseInt(newIntervalStr);
            int newThreshold = newThresholdStr.isEmpty() ? 24 : Integer.parseInt(newThresholdStr);

            hasUnsavedChanges = !newUrl.equals(url) ||
                               newInterval != checkInterval ||
                               newThreshold != offlineThreshold;

            url = newUrl;
            checkInterval = newInterval;
            offlineThreshold = newThreshold;

            updateView();
        } catch (NumberFormatException e) {
            // Игнорируем неверный формат, пока пользователь не нажмет save
        }
    }

    private void loadSettings() {
        url = monitoringManager.url;
        checkInterval = monitoringManager.checkInterval;
        offlineThreshold = monitoringManager.offlineThreshold;

        urlInput.setText(url);
        checkIntervalInput.setText(String.valueOf(checkInterval));
        offlineThresholdInput.setText(String.valueOf(offlineThreshold));

        hasUnsavedChanges = false;
        updateView();
    }

    private void saveFields() {
        if (!validateFields()) return;

        monitoringManager.url = url;
        monitoringManager.checkInterval = checkInterval;
        monitoringManager.offlineThreshold = offlineThreshold;
        monitoringManager.saveSettings();

        hasUnsavedChanges = false;
        updateView();
        // Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
    }

    private boolean validateFields() {
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (checkInterval <= 0 || offlineThreshold <= 0) {
            Toast.makeText(this, "Hours must be greater than 0", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public void updateView() {
        boolean isActive = monitoringManager.isActive();
        boolean hasError = monitoringManager.isErrorNotificationActive();

        // Управление кнопками
        startMonitoringButton.setEnabled(!isActive);
        stopMonitoringButton.setEnabled(isActive);
        resetErrorButton.setEnabled(hasError);
        testUrlButton.setEnabled(true);
        saveButton.setEnabled(!isActive && hasUnsavedChanges);

        // Скрываем или показываем кнопки
        startMonitoringButton.setVisibility(isActive ? View.GONE : View.VISIBLE);
        stopMonitoringButton.setVisibility(isActive ? View.VISIBLE : View.GONE);
        resetErrorButton.setVisibility(hasError ? View.VISIBLE : View.GONE);
        saveButton.setVisibility(!isActive && hasUnsavedChanges ? View.VISIBLE : View.GONE);

        // Установка цвета кнопки "Reset Error"
        resetErrorButton.setBackgroundColor(hasError ? Color.RED : Color.LTGRAY);

        // Управление полями ввода
        urlInput.setEnabled(!isActive);
        checkIntervalInput.setEnabled(!isActive);
        offlineThresholdInput.setEnabled(!isActive);

        // Обновление статуса
        statusTextView.setText(isActive ? "Monitoring active" : "Monitoring not active");
        statusTextView.setTextColor(isActive ? Color.GREEN : Color.RED);
    }

    public void showLog() {
        List<CheckResult> log = monitoringManager.getCheckLog();
        LinearLayout logContainer = findViewById(R.id.logContainer);
        logContainer.removeAllViews(); // Очищаем предыдущие записи

        // Проверяем, есть ли элементы в логе
        if (!log.isEmpty()) {
            // Отображаем заголовок "Check Log:"
            TextView logHeader = new TextView(this);
            logHeader.setText("Check Log:");
            logHeader.setTextSize(16);
            logHeader.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            logContainer.addView(logHeader); // Добавляем заголовок

            // Добавляем кружочки в обратном порядке
            for (int i = log.size() - 1; i >= 0; i--) {
                CheckResult result = log.get(i);
                ImageView circle = new ImageView(this);
                int status = result.getStatus();
                switch (status) {
                    case UrlChecker.SUCCESS:
                        circle.setBackgroundColor(Color.GREEN); // Зеленый кружок
                        break;
                    case UrlChecker.ERROR:
                        circle.setBackgroundColor(Color.RED); // Красный кружок
                        break;
                    case UrlChecker.INTERNET_UNAVAILABLE:
                        circle.setBackgroundColor(Color.YELLOW); // Желтый кружок
                        break;
                    default:
                        circle.setBackgroundColor(Color.GRAY); // Неизвестный статус
                }

                // Устанавливаем размеры кружка
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(50, 50); // 50x50 пикселей
                params.setMargins(5, 0, 5, 0); // Отступы
                circle.setLayoutParams(params);
                circle.setScaleType(ImageView.ScaleType.FIT_XY); // Масштабирование

                logContainer.addView(circle); // Добавляем кружок в контейнер
            }
        } else {
            // Если лог пуст, скрываем заголовок
            logContainer.removeAllViews(); // Удаляем все, если нет записей
        }
    }

    // Геттеры для доступа к значениям полей
    public String getUrl() {
        return url;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public int getOfflineThreshold() {
        return offlineThreshold;
    }
}
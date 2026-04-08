package ui;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.taa.project.R;

import java.util.ArrayList;
import java.util.List;

import data.AppDatabase;
import data.TremorEntity; // ВИПРАВЛЕНО: Використовуємо нову сутність
import ui.helpers.HighPassFilter;
import ui.helpers.UIHelper;

public class TestActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor gyroSensor;
    private HighPassFilter highPassFilter;
    private AppDatabase db;

    private TextView tvStatus, tvTimer, tvCurrentScore;
    private ProgressBar progressBar;
    private Button btnStart;
    private View rootLayout;

    private boolean isTestRunning = false;
    private List<Float> testSamples = new ArrayList<>();
    private final float ALPHA = 0.2f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // ВИПРАВЛЕНО: назва методу getDatabase
        db = AppDatabase.getDatabase(this);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        highPassFilter = new HighPassFilter(ALPHA);

        initViews();
    }

    private void initViews() {
        rootLayout = findViewById(R.id.testRootLayout);
        tvStatus = findViewById(R.id.tvTestStatus);
        tvTimer = findViewById(R.id.tvTestTimer);
        tvCurrentScore = findViewById(R.id.tvCurrentTestScore);
        progressBar = findViewById(R.id.testProgressBar);
        btnStart = findViewById(R.id.btnStartTest);

        btnStart.setOnClickListener(v -> startPreparation());
    }

    private void startPreparation() {
        btnStart.setVisibility(View.GONE);
        testSamples.clear();
        highPassFilter.reset();

        new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvStatus.setText("Приготуйте руку...");
                tvTimer.setText(String.valueOf((millisUntilFinished / 1000) + 1));
            }

            @Override
            public void onFinish() {
                startActiveTest();
            }
        }.start();
    }

    private void startActiveTest() {
        isTestRunning = true;
        tvStatus.setText("Тримайте телефон нерухомо!");
        tvTimer.setTextColor(Color.RED);
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI);

        new CountDownTimer(10000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                int progress = (int) (millisUntilFinished / 100);
                progressBar.setProgress(progress);
                tvTimer.setText(String.format("%.1f", (float) millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                stopTest();
            }
        }.start();
    }

    private void stopTest() {
        isTestRunning = false;
        sensorManager.unregisterListener(this);
        progressBar.setProgress(0);

        if (testSamples.isEmpty()) {
            tvStatus.setText("Помилка: Дані не отримано");
            btnStart.setVisibility(View.VISIBLE);
            return;
        }

        // Обчислюємо середню інтенсивність тремору
        float sum = 0;
        for (float val : testSamples) sum += val;
        float averageMagnitude = sum / testSamples.size();

        // Розраховуємо бал для відображення (0-10)
        float finalScore = Math.max(0, 10 - (averageMagnitude * 7));

        // Зберігаємо в БД саме значення тремору (magnitude) для графіків
        saveResultToDb(averageMagnitude);

        // Показуємо результат (бал) користувачу
        showFinalUI(finalScore);
    }

    private void saveResultToDb(float tremorValue) {
        new Thread(() -> {
            // ВИПРАВЛЕНО: Створюємо TremorEntity замість TremorRecord
            // Використовуємо конструктор: (значення, час)
// Додаємо true, бо це результат активного тестування
            TremorEntity record = new TremorEntity(tremorValue, System.currentTimeMillis(), true);
            // ВИПРАВЛЕНО: назва методу insert збігається з DAO
            db.tremorDao().insert(record);

            runOnUiThread(() -> Toast.makeText(this, "Результат збережено!", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void showFinalUI(float score) {
        tvStatus.setText("Тест завершено!");
        tvTimer.setText(String.format("%.1f", score)); // Показуємо бал
        tvTimer.setTextColor(UIHelper.getColorForScore(score));
        rootLayout.setBackgroundColor(UIHelper.getBgColorForScore(score));
        btnStart.setText("Повторити тест");
        btnStart.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isTestRunning) {
            float[] tremorValues = highPassFilter.filter(event.values);

            float magnitude = (float) Math.sqrt(
                    tremorValues[0] * tremorValues[0] +
                            tremorValues[1] * tremorValues[1] +
                            tremorValues[2] * tremorValues[2]
            );

            testSamples.add(magnitude);

            float currentScore = Math.max(0, 10 - (magnitude * 7));
            tvCurrentScore.setText("Поточна стабільність: " + String.format("%.1f", currentScore));
            tvCurrentScore.setTextColor(UIHelper.getColorForScore(currentScore));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        isTestRunning = false;
    }
}
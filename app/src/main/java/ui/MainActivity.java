package ui;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.taa.project.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import data.AppDatabase;
import data.TremorEntity;
import service.SensorService;
import ui.helpers.ChartHelper;
import ui.helpers.UIHelper;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private LineChart chart;
    private TextView tvScore, tvCurrentYear;
    private View layoutHome, layoutAnalysis, layoutProfile, layoutYearSelector;
    private LinearLayout layoutResultCards;
    private GridLayout gridHeatmap;
    private TabLayout tabLayoutFilters;

    private AppDatabase db;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private int xValue = 0;
    private long lastUIUpdateTime = 0; // Для стабілізації цифр (1 раз/сек)

    private long selectedDateTs = getStartOfToday();

    private final BroadcastReceiver sensorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long currentTime = System.currentTimeMillis();
            // Обмежуємо оновлення екрану до 1 разу на секунду
            if (currentTime - lastUIUpdateTime < 1000) return;
            lastUIUpdateTime = currentTime;

            float mag = intent.getFloatExtra("value", 0);
            float score = calculateStability(mag);

            if (tvScore != null) {
                tvScore.setText(String.format("%.1f", score));
                tvScore.setTextColor(UIHelper.getColorForScore(score));
            }
            if (layoutHome.getVisibility() == View.VISIBLE && chart != null) {
                ChartHelper.addEntry(chart, mag, xValue++);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = AppDatabase.getDatabase(this);

        initViews();
        setupNavigation();
        setupAnalysisTabs();
        setupProfileButtons();
        startMonitoringService();
        requestAppPermissions();
    }

    private void initViews() {
        tvScore = findViewById(R.id.tvStabilityScore);
        chart = findViewById(R.id.chart);
        layoutHome = findViewById(R.id.layoutHomeSection);
        layoutAnalysis = findViewById(R.id.layoutAnalysisSection);
        layoutProfile = findViewById(R.id.layoutProfileSection);
        layoutResultCards = findViewById(R.id.layoutResultCards);
        gridHeatmap = findViewById(R.id.gridHeatmap);
        tabLayoutFilters = findViewById(R.id.tabLayoutFilters);
        layoutYearSelector = findViewById(R.id.layoutYearSelector);
        tvCurrentYear = findViewById(R.id.tvCurrentYear);

        ChartHelper.initChart(chart);

        findViewById(R.id.btnSelectDate).setOnClickListener(v -> showDatePicker());
        findViewById(R.id.btnToday).setOnClickListener(v -> goToToday());
        findViewById(R.id.btnPrevYear).setOnClickListener(v -> changeYear(-1));
        findViewById(R.id.btnNextYear).setOnClickListener(v -> changeYear(1));
        findViewById(R.id.cardPostural).setOnClickListener(v -> startActivity(new Intent(this, TestActivity.class)));
    }

    // --- АНАЛІТИКА (ГОДИНИ -> 10 ХВ -> ХВИЛИНИ + ТЕСТИ) ---

    private void loadHourlyData(long dayStart) {
        layoutResultCards.removeAllViews();
        long dayEnd = dayStart + 86400000L; // +24 години

        new Thread(() -> {
            // Отримуємо з бази середні за години (для структури) та ВСІ сирі записи
            List<TremorEntity> hourlyAverages = db.tremorDao().getHourlyAverages(dayStart);
            List<TremorEntity> allDayRecords = db.tremorDao().getAllDataForDay(dayStart, dayEnd);

            runOnUiThread(() -> {
                if (hourlyAverages.isEmpty()) {
                    showEmptyMessage(); return;
                }

                for (TremorEntity hourEntry : hourlyAverages) {
                    Calendar c = Calendar.getInstance();
                    c.setTimeInMillis(hourEntry.getTimestamp());
                    int hour = c.get(Calendar.HOUR_OF_DAY);

                    // --- РОЗРАХУНОК ТОЧНОГО СЕРЕДНЬОГО БАЛУ ---
                    float sumOfScores = 0;
                    int count = 0;

                    // Знаходимо всі записи, що належать цій годині (для точного середнього)
                    for (TremorEntity record : allDayRecords) {
                        Calendar rc = Calendar.getInstance();
                        rc.setTimeInMillis(record.getTimestamp());
                        // Рахуємо середнє тільки для фонового моніторингу (isTest = false)
                        if (rc.get(Calendar.HOUR_OF_DAY) == hour && !record.isTest()) {
                            sumOfScores += calculateStability(record.getTremorValue());
                            count++;
                        }
                    }

                    // ПРАВИЛЬНИЙ СЕРЕДНІЙ БАЛ
                    float finalHourlyScore = (count > 0) ? sumOfScores / count : 0;

                    // Отримуємо колір, що відповідає цьому балу (зелений, жовтий або червоний)
                    int backgroundColor = UIHelper.getColorForScore(finalHourlyScore);
                    // Робимо колір напівпрозорим (альфа = 30), щоб текст добре читався
                    int transparentColor = Color.argb(30, Color.red(backgroundColor), Color.green(backgroundColor), Color.blue(backgroundColor));

                    String timeLabel = String.format("%02d:00", hour);
                    // Створюємо рядок години (з можливістю розгортання)
                    View hourRow = UIHelper.createExpandableRow(this, timeLabel, finalHourlyScore,
                            v -> toggleTenMinuteDetails(v, hourEntry.getTimestamp()));

                    // --- ЗАМАЛЬОВУВАННЯ ФОНУ РЯДКА ---
                    hourRow.setBackgroundColor(transparentColor);

                    layoutResultCards.addView(hourRow);
                }
            });
        }).start();
    }

    private void toggleTenMinuteDetails(View v, long hourTs) {
        LinearLayout container = (LinearLayout) v.getParent();
        if (container.getChildCount() > 2) { container.removeViews(2, container.getChildCount() - 2); return; }

        new Thread(() -> {
            List<TremorEntity> tenMinData = db.tremorDao().getTenMinuteAverages(hourTs);
            runOnUiThread(() -> {
                for (TremorEntity e : tenMinData) {
                    if (e.getTimestamp() >= hourTs && e.getTimestamp() < hourTs + 3600000L) {
                        Calendar c = Calendar.getInstance(); c.setTimeInMillis(e.getTimestamp());
                        String label = String.format("%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
                        View sub = UIHelper.createSubDetailRow(this, label, calculateStability(e.getTremorValue()));
                        sub.setOnClickListener(v1 -> toggleMinuteDetails(v1, e.getTimestamp()));
                        container.addView(sub);
                    }
                }
            });
        }).start();
    }

    private void toggleMinuteDetails(View v, long tenMinTs) {
        LinearLayout container = (LinearLayout) v.getParent();
        int idx = container.indexOfChild(v);
        if (idx + 1 < container.getChildCount() && container.getChildAt(idx + 1).getTag() != null) {
            while (idx + 1 < container.getChildCount() && container.getChildAt(idx + 1).getTag() != null)
                container.removeViewAt(idx + 1);
            return;
        }
        new Thread(() -> {
            List<TremorEntity> minutes = db.tremorDao().getMinuteData(tenMinTs, tenMinTs + 600000L);
            runOnUiThread(() -> {
                for (int i = minutes.size() - 1; i >= 0; i--) {
                    TremorEntity e = minutes.get(i);
                    Calendar c = Calendar.getInstance(); c.setTimeInMillis(e.getTimestamp());
                    String label = String.format("  ↳ %02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
                    View minRow = UIHelper.createSubDetailRow(this, label, calculateStability(e.getTremorValue()));
                    minRow.setTag("min"); container.addView(minRow, idx + 1);
                }
            });
        }).start();
    }

    // --- КНОПКИ ПРОФІЛЮ ---

    private void setupProfileButtons() {
        // Налаштування
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        // Інформація (btnProfileInfo)
        findViewById(R.id.btnProfileInfo).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Про тремор та допомогу")
                    .setMessage("Тремор — це ритмічні коливання тіла. Додаток автоматично вимірює їх, поки ви тримаєте телефон.\n\n" +
                            "Як користуватися:\n" +
                            "1. Просто користуйтеся телефоном — дані збираються раз на хвилину.\n" +
                            "2. Використовуйте 'Швидкий тест' для точних замірів.")
                    .setPositiveButton("Зрозуміло", null).show();
        });

        // SOS (btnProfileSos)
        findViewById(R.id.btnProfileSos).setOnClickListener(v -> {
            String ph = getSharedPreferences("TremorPrefs", MODE_PRIVATE).getString("user_phone", "");
            if (!ph.isEmpty()) startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ph)));
            else Toast.makeText(this, "Вкажіть номер у налаштуваннях", Toast.LENGTH_SHORT).show();
        });
    }

    // --- КАЛЕНДАР ---

    private void renderCalendarGrid() {
        gridHeatmap.removeAllViews();
        gridHeatmap.setColumnCount(7);
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(selectedDateTs);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        int firstDay = cal.get(Calendar.DAY_OF_WEEK);
        int emptySlots = (firstDay == Calendar.SUNDAY) ? 6 : firstDay - 2;

        for (int i = 0; i < emptySlots; i++) {
            View spacer = new View(this);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0; p.height = 10; p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            spacer.setLayoutParams(p); gridHeatmap.addView(spacer);
        }

        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        Calendar limit = Calendar.getInstance();

        for (int i = 1; i <= daysInMonth; i++) {
            final int day = i;
            Calendar jump = (Calendar) cal.clone(); jump.set(Calendar.DAY_OF_MONTH, i);
            if (jump.after(limit)) continue;

            View sq = UIHelper.createHeatmapSquare(this, String.valueOf(i), 0f, 7);
            sq.setOnClickListener(v -> { selectedDateTs = jump.getTimeInMillis(); tabLayoutFilters.getTabAt(0).select(); });
            gridHeatmap.addView(sq);
        }
    }

    private void renderYearGrid() {
        gridHeatmap.removeAllViews();
        gridHeatmap.setColumnCount(4);
        Calendar cal = Calendar.getInstance(); cal.setTimeInMillis(selectedDateTs);
        Calendar limit = Calendar.getInstance();
        for (int i = 0; i < 12; i++) {
            final int m = i;
            Calendar jump = (Calendar) cal.clone(); jump.set(Calendar.MONTH, i); jump.set(Calendar.DAY_OF_MONTH, 1);
            if (jump.after(limit)) continue;
            View sq = UIHelper.createHeatmapSquare(this, getMonthName(i), 0f, 4);
            sq.setOnClickListener(v -> { selectedDateTs = jump.getTimeInMillis(); tabLayoutFilters.getTabAt(1).select(); });
            gridHeatmap.addView(sq);
        }
    }

    // --- СИСТЕМНІ ---

    private void requestAppPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS, android.Manifest.permission.ACTIVITY_RECOGNITION}, 101);
        }
    }

    private void startMonitoringService() {
        Intent i = new Intent(this, SensorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i); else startService(i);
    }

    private void reloadAnalysis() {
        int pos = tabLayoutFilters.getSelectedTabPosition();
        if (pos == 0) {
            gridHeatmap.setVisibility(View.GONE); layoutYearSelector.setVisibility(View.GONE);
            layoutResultCards.setVisibility(View.VISIBLE); loadHourlyData(selectedDateTs);
        } else if (pos == 1) {
            gridHeatmap.setVisibility(View.VISIBLE); layoutYearSelector.setVisibility(View.GONE);
            layoutResultCards.setVisibility(View.GONE); renderCalendarGrid();
        } else {
            gridHeatmap.setVisibility(View.VISIBLE); layoutYearSelector.setVisibility(View.VISIBLE);
            layoutResultCards.setVisibility(View.GONE);
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(selectedDateTs);
            tvCurrentYear.setText(String.valueOf(c.get(Calendar.YEAR)));
            renderYearGrid();
        }
    }

    private void setupNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            layoutHome.setVisibility(id == R.id.nav_home ? View.VISIBLE : View.GONE);
            layoutAnalysis.setVisibility(id == R.id.nav_analysis ? View.VISIBLE : View.GONE);
            layoutProfile.setVisibility(id == R.id.nav_profile ? View.VISIBLE : View.GONE);
            if (id == R.id.nav_analysis) reloadAnalysis();
            else if (id == R.id.nav_profile) updateProfileUI();
            return true;
        });
    }

    private void setupAnalysisTabs() {
        if (tabLayoutFilters.getTabCount() == 0) {
            tabLayoutFilters.addTab(tabLayoutFilters.newTab().setText("День"));
            tabLayoutFilters.addTab(tabLayoutFilters.newTab().setText("Місяць"));
            tabLayoutFilters.addTab(tabLayoutFilters.newTab().setText("Рік"));
        }
        tabLayoutFilters.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) { reloadAnalysis(); }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance(); c.setTimeInMillis(selectedDateTs);
        new DatePickerDialog(this, (v, y, m, d) -> {
            Calendar sel = Calendar.getInstance(); sel.set(y, m, d, 0, 0, 0);
            selectedDateTs = sel.getTimeInMillis(); reloadAnalysis();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void goToToday() { selectedDateTs = getStartOfToday(); tabLayoutFilters.getTabAt(0).select(); reloadAnalysis(); }
    private void changeYear(int delta) { Calendar c = Calendar.getInstance(); c.setTimeInMillis(selectedDateTs); c.add(Calendar.YEAR, delta); selectedDateTs = c.getTimeInMillis(); reloadAnalysis(); }
    private long getStartOfToday() { Calendar c = Calendar.getInstance(); c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0); return c.getTimeInMillis(); }
    private String getMonthName(int m) { return new String[]{"Січ", "Лют", "Бер", "Кві", "Тра", "Чер", "Лип", "Сер", "Вер", "Жов", "Лис", "Гру"}[m]; }
    private float calculateStability(float magnitude) { return magnitude <= 0.01 ? 10.0f : Math.max(0, 10 - (magnitude * 7)); }
    private void showEmptyMessage() { TextView tv = new TextView(this); tv.setText("Даних не знайдено"); tv.setPadding(50,50,50,50); layoutResultCards.addView(tv); }
    private void updateProfileUI() {
        SharedPreferences p = getSharedPreferences("TremorPrefs", MODE_PRIVATE);

        // Отримуємо дані (використовуйте ті ж ключі, що в SettingsActivity)
        String firstName = p.getString("user_first_name", "");
        String lastName = p.getString("user_last_name", "");
        String age = p.getString("user_age", "—");
        String condition = p.getString("tremor_description", "Не вказано");

        // Оновлюємо ім'я
        TextView tvFullName = findViewById(R.id.tvProfileFullName);
        if (tvFullName != null) {
            String full = (firstName + " " + lastName).trim();
            tvFullName.setText(full.isEmpty() ? "Користувач" : full);
        }

        // Оновлюємо вік
        TextView tvAge = findViewById(R.id.tvProfileAge); // Перевірте цей ID у вашому XML
        if (tvAge != null) {
            tvAge.setText(age + " років");
        }

        // Оновлюємо стан (діагноз/статус)
        TextView tvCondition = findViewById(R.id.tvProfileTremor); // Перевірте цей ID у вашому XML
        if (tvCondition != null) {
            tvCondition.setText(condition);
        }
    }
    @Override public void onSensorChanged(SensorEvent e) {}
    @Override public void onAccuracyChanged(Sensor s, int a) {}

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override protected void onResume() {
        super.onResume();
        updateProfileUI();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        IntentFilter f = new IntentFilter("SENSOR_DATA_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) registerReceiver(sensorReceiver, f, Context.RECEIVER_EXPORTED); else registerReceiver(sensorReceiver, f);
    }

    @Override protected void onPause() { super.onPause(); if(sensorManager != null) sensorManager.unregisterListener(this); try { unregisterReceiver(sensorReceiver); } catch (Exception ignored) {} }
}
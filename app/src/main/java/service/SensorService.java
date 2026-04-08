package service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;
import data.AppDatabase;
import data.TremorEntity;
import java.util.ArrayList;
import java.util.List;

public class SensorService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "TremorChannel";
    private static final int NOTIFICATION_ID = 1;

    private SensorManager sensorManager;
    private PowerManager powerManager;
    private NotificationManager notificationManager;
    private AppDatabase db;
    private Sensor accelerometer;

    private final List<Float> dbBuffer = new ArrayList<>();
    private final Handler handler = new Handler();
    private long lastNotificationUpdateTime = 0;
    private boolean isMonitoringActive = false;

    // Ресивер для відстеження стану екрана в реальному часі
    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                stopSensorMonitoring();
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                startSensorMonitoring();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        db = AppDatabase.getDatabase(this);
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification("Моніторинг очікує увімкнення екрана"));

        // Реєструємо слухача екрана
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);

        // Перевіряємо початковий стан екрана
        if (powerManager.isInteractive()) {
            startSensorMonitoring();
        }

        startDbSaveCycle();
    }

    private void startSensorMonitoring() {
        if (!isMonitoringActive && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            isMonitoringActive = true;
            updateNotification("Екран увімкнено: збір даних...");
        }
    }

    private void stopSensorMonitoring() {
        if (isMonitoringActive) {
            sensorManager.unregisterListener(this);
            isMonitoringActive = false;
            synchronized (dbBuffer) {
                dbBuffer.clear(); // Очищуємо буфер, щоб дані з кишені не потрапили в БД
            }
            updateNotification("Екран вимкнено: моніторинг призупинено");
        }
    }

    private void startDbSaveCycle() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                saveMinuteToDb();
                handler.postDelayed(this, 60000);
            }
        }, 60000);
    }

    private void saveMinuteToDb() {
        synchronized (dbBuffer) {
            if (dbBuffer.isEmpty() || !isMonitoringActive) return;

            List<Float> copy = new ArrayList<>(dbBuffer);
            dbBuffer.clear();

            new Thread(() -> {
                float sum = 0;
                for (float f : copy) sum += f;
                float average = sum / copy.size();
                db.tremorDao().insert(new TremorEntity(average, System.currentTimeMillis(), false));
            }).start();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && isMonitoringActive) {
            float x = event.values[0], y = event.values[1], z = event.values[2];
            float magnitude = (float) Math.sqrt(x*x + y*y + z*z) - 9.81f;
            float val = Math.abs(magnitude);

            synchronized (dbBuffer) {
                dbBuffer.add(val);
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNotificationUpdateTime > 1000) {
                float score = val <= 0.01 ? 10.0f : Math.max(0, 10 - (val * 7));
                updateNotification(String.format("Стабільність: %.1f/10", score));
                lastNotificationUpdateTime = currentTime;
            }

            Intent intent = new Intent("SENSOR_DATA_UPDATE");
            intent.putExtra("value", val);
            sendBroadcast(intent);
        }
    }

    private void updateNotification(String text) {
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text));
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Аналіз тремору")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Monitoring Service", NotificationManager.IMPORTANCE_LOW);
            if (notificationManager != null) notificationManager.createNotificationChannel(channel);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenReceiver);
        stopSensorMonitoring();
        handler.removeCallbacksAndMessages(null);
    }
}
package ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.taa.project.R;
import data.AppDatabase;

public class SettingsActivity extends AppCompatActivity {

    private AppDatabase db;
    private SharedPreferences prefs;
    private EditText etFirstName, etLastName, etAge, etPhone, etTremor;
    private TextView tvSensitivityValue;
    private SeekBar seekBarSensitivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Використовуємо правильний метод ініціалізації БД
        db = AppDatabase.getDatabase(this);
        prefs = getSharedPreferences("TremorPrefs", MODE_PRIVATE);

        initViews();
    }

    private void initViews() {
        // Поля профілю (Додано Ім'я та Прізвище)
        etFirstName = findViewById(R.id.etSettingsFirstName); // Переконайтеся, що цей ID є в XML
        etLastName = findViewById(R.id.etSettingsLastName);   // Переконайтеся, що цей ID є в XML
        etAge = findViewById(R.id.etUserAge);
        etPhone = findViewById(R.id.etUserPhone);
        etTremor = findViewById(R.id.etTremorInfo);

        // Налаштування сенсора
        tvSensitivityValue = findViewById(R.id.tvSensitivityValue);
        seekBarSensitivity = findViewById(R.id.seekBarSensitivity);

        // Кнопки
        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnDeleteAll = findViewById(R.id.btnDeleteAllData);
        Button btnBack = findViewById(R.id.btnBackSettings);

        // Завантаження збережених даних
        etFirstName.setText(prefs.getString("user_first_name", ""));
        etLastName.setText(prefs.getString("user_last_name", ""));
        etAge.setText(prefs.getString("user_age", ""));
        etPhone.setText(prefs.getString("user_phone", ""));
        etTremor.setText(prefs.getString("tremor_description", ""));

        int savedSensitivity = prefs.getInt("sensitivity", 20);
        seekBarSensitivity.setProgress(savedSensitivity);
        updateSensitivityText(savedSensitivity);

        // Логіка збереження профілю
        btnSave.setOnClickListener(v -> {
            prefs.edit()
                    .putString("user_first_name", etFirstName.getText().toString().trim())
                    .putString("user_last_name", etLastName.getText().toString().trim())
                    .putString("user_age", etAge.getText().toString().trim())
                    .putString("user_phone", etPhone.getText().toString().trim())
                    .putString("tremor_description", etTremor.getText().toString().trim())
                    .apply(); // Зберігаємо на диск

            Toast.makeText(this, "Профіль оновлено", Toast.LENGTH_SHORT).show();
            finish(); // Повертаємось у MainActivity, де спрацює onResume()
        });

        // Налаштування чутливості (Alpha Filter)
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSensitivityText(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                prefs.edit().putInt("sensitivity", seekBar.getProgress()).apply();
            }
        });

        // Видалення всіх записів з БД
        btnDeleteAll.setOnClickListener(v -> showDeleteConfirmation());

        // Повернення назад
        btnBack.setOnClickListener(v -> finish());
    }

    private void updateSensitivityText(int progress) {
        // Відображаємо значення від 0.01 до 1.00
        tvSensitivityValue.setText(String.format("Коефіцієнт Alpha: %.2f", progress / 100f));
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Видалити всі дані?")
                .setMessage("Це очистить всю історію вимірювань без можливості відновлення.")
                .setPositiveButton("Видалити", (d, w) -> {
                    new Thread(() -> {
                        db.tremorDao().deleteAll(); // Використовуємо правильну назву методу
                        runOnUiThread(() -> Toast.makeText(this, "Дані очищено", Toast.LENGTH_SHORT).show());
                    }).start();
                })
                .setNegativeButton("Скасувати", null)
                .show();
    }
}
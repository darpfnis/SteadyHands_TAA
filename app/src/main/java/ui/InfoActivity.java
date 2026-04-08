package ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.taa.project.R;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        TextView tvInfoContent = findViewById(R.id.tvInfoContent);
        Button btnClose = findViewById(R.id.btnCloseInfo);

        String infoText = "ЩО ТАКЕ ТРЕМОР?\n\n" +
                "Тремор — це ритмічне коливання частин тіла. У цьому додатку ми вимірюємо амплітуду цих коливань за допомогою гіроскопа вашого смартфона.\n\n" +
                "ЯК ПРАВИЛЬНО РОБИТИ ТЕСТ:\n" +
                "1. Сядьте зручно, розслабте плечі.\n" +
                "2. Візьміть телефон у руку, яку хочете протестувати.\n" +
                "3. Тримайте руку на вазі перед собою (як зі склянкою води).\n" +
                "4. Намагайтеся не рухати іншими частинами тіла під час тесту.\n\n" +
                "ПОРАДИ:\n" +
                "— Проводьте тест в один і той самий час (наприклад, щоранку).\n" +
                "— Записуйте в налаштуваннях обставини (після кави, при втомі тощо).\n" +
                "— Результати додатка мають ознайомчий характер і не є діагнозом.";

        tvInfoContent.setText(infoText);
        btnClose.setOnClickListener(v -> finish());
    }
}
package ui.helpers;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.taa.project.R;

public class UIHelper {

    /**
     * Повертає колір залежно від балу стабільності (0-10)
     * Логіка: Зелений (Добре), Помаранчевий (Середньо), Червоний (Погано)
     */
    public static int getColorForScore(float score) {
        if (score >= 8.5) return Color.parseColor("#2D6A4F"); // Темно-зелений
        if (score >= 7.0) return Color.parseColor("#FF9100"); // Помаранчевий
        return Color.parseColor("#D90429"); // Червоний
    }

    /**
     * Повертає світлий фоновий колір для елементів інтерфейсу (Heatmap)
     */
    public static int getBgColorForScore(float score) {
        if (score >= 8.5) return Color.parseColor("#E8F5E9"); // Світло-зелений
        if (score >= 7.0) return Color.parseColor("#FFF3E0"); // Світло-помаранчевий
        return Color.parseColor("#FFEBEE"); // Світло-червоний
    }

    /**
     * Створює квадрат для теплової карти (Heatmap) у GridLayout
     */
    public static View createHeatmapSquare(Context context, String label, float score, int columnCount) {
        TextView tv = new TextView(context);
        tv.setText(label);
        tv.setGravity(android.view.Gravity.CENTER);

        // В Java просто пишемо число, воно сприймається як SP
        tv.setTextSize(14);

        tv.setTextColor(android.graphics.Color.BLACK);
        tv.setPadding(4, 20, 4, 20);

        // Параметри сітки
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;

        // Встановлюємо вагу 1f, щоб 7 колонок влізли в екран
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(4, 4, 4, 4);
        tv.setLayoutParams(params);

        // Колір фону
        if (score == 0) {
            tv.setBackgroundColor(android.graphics.Color.parseColor("#EEEEEE"));
        } else {
            tv.setBackgroundColor(getColorForScore(score));
        }

        return tv;
    }

    /**
     * Створює ієрархічний рядок (Година), який можна розгорнути
     */
    public static View createExpandableRow(Context context, String title, float avg, View.OnClickListener listener) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);

        // Створюємо "шапку" години
        TextView header = new TextView(context);
        header.setText(title + "  |  Сер. бал: " + String.format("%.1f", avg));
        header.setTextSize(18f);
        header.setPadding(40, 40, 40, 40);
        header.setTextColor(Color.BLACK);
        header.setBackgroundResource(android.R.drawable.list_selector_background);

        // Маленька риска знизу для розділення
        View divider = new View(context);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.LTGRAY);

        header.setOnClickListener(listener);

        container.addView(header);
        container.addView(divider);
        return container;
    }

    /**
     * Створює вкладений рядок (10 хвилин)
     */
    public static TextView createSubDetailRow(Context context, String time, float score) {
        TextView tv = new TextView(context);
        tv.setText("   └ " + time + " : Бал " + String.format("%.1f", score));
        tv.setPadding(80, 20, 40, 20);
        tv.setTextSize(15f);
        tv.setTextColor(getColorForScore(score));
        return tv;
    }
}
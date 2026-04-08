package ui.helpers;

import android.graphics.Color;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class ChartHelper {

    /**
     * Початкове налаштування графіка (сітка, осі, кольори)
     */
    public static void initChart(LineChart chart) {
        if (chart == null) return;

        chart.getDescription().setEnabled(false); // Прибрати текст знизу
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(false);
        chart.setDrawGridBackground(false);
        chart.setBackgroundColor(Color.TRANSPARENT);

        // Налаштування осі X (Час/Відліки)
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.GRAY);
        xAxis.setDrawGridLines(false);
        xAxis.setAvoidFirstLastClipping(true);
        xAxis.setEnabled(true);

        // Налаштування осі Y (Амплітуда тремору)
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setTextColor(Color.GRAY);
        leftAxis.setAxisMaximum(5.0f); // Максимальна амплітуда (гіроскоп)
        leftAxis.setAxisMinimum(0f);   // Мінімальна
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.LTGRAY);

        chart.getAxisRight().setEnabled(false); // Вимкнути праву вісь
        chart.getLegend().setEnabled(false);    // Вимкнути легенду

        // Створення порожніх даних для початку
        chart.setData(new LineData());
    }

    /**
     * Додавання нової точки на графік у реальному часі
     * @param chart Об'єкт графіка
     * @param value Значення амплітуди (magnitude)
     * @param x Індекс (номер відліку)
     */
    public static void addEntry(LineChart chart, float value, int x) {
        LineData data = chart.getData();

        if (data != null) {
            LineDataSet set = (LineDataSet) data.getDataSetByIndex(0);

            // Якщо набору даних ще немає — створюємо його
            if (set == null) {
                set = createDataSet();
                data.addDataSet(set);
            }

            // Додаємо нову точку
            data.addEntry(new Entry(x, value), 0);
            data.notifyDataChanged();

            // Повідомляємо графік про зміни
            chart.notifyDataSetChanged();

            // Обмежуємо кількість видимих точок (наприклад, останні 50)
            // Щоб графік "біг" вліво, а не стискався
            chart.setVisibleXRangeMaximum(50);
            chart.moveViewToX(data.getEntryCount());
        }
    }

    /**
     * Створення та стилізація набору ліній
     */
    private static LineDataSet createDataSet() {
        LineDataSet set = new LineDataSet(new ArrayList<>(), "Tremor Magnitude");

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.parseColor("#457B9D")); // Синій колір лінії
        set.setLineWidth(2.5f);                    // Товщина лінії
        set.setDrawCircles(false);                 // Прибрати точки на згинах
        set.setDrawValues(false);                  // Прибрати цифри над лінією

        // Робимо лінію плавною (кубічна безьє)
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        set.setCubicIntensity(0.2f);

        // Ефект заповнення під лінією
        set.setDrawFilled(true);
        set.setFillColor(Color.parseColor("#457B9D"));
        set.setFillAlpha(30); // Напівпрозорість

        set.setHighLightColor(Color.RED); // Колір при натисканні на графік

        return set;
    }
}